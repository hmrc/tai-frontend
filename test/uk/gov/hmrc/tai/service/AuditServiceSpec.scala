/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.tai.service

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{Authorization, ForwardedFor, HeaderCarrier, RequestId, SessionId}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.util.constants.TaiConstants._
import utils.BaseSpec

import java.time._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AuditServiceSpec extends BaseSpec {

  val appName = "test"

  private def auditDetail(
    auditType: String,
    noOfEmpAndTax: Option[String] = None
  ): Map[String, String] =
    auditType match {
      case "userEntersService"               => entryAuditDetails(noOfEmpAndTax.getOrElse("0"))
      case "startedEmploymentPensionJourney" => employmentOrPensionAuditDetails
      case "finishedCompanyCarJourney"       => endCompanyCarDetails
      case "finishedCompanyCarJourneyNoFuel" => endCompanyCarDetailsNoFuel
      case _                                 => companyBenefitsAuditDetails
    }

  private def event(
    auditType: String,
    sessionId: Option[String] = None,
    requestId: Option[String] = None,
    trueClientIp: Option[String] = None,
    path: Option[String] = None,
    deviceId: Option[String] = None,
    clientPort: Option[String] = None,
    detail: Map[String, String]
  ) =
    DataEvent(
      auditSource = "test",
      auditType = auditType,
      tags = Map(
        "clientIP"          -> trueClientIp.getOrElse("-"),
        "path"              -> path.getOrElse("NA"),
        "X-Session-ID"      -> sessionId.getOrElse("-"),
        "Akamai-Reputation" -> "-",
        "X-Request-ID"      -> requestId.getOrElse("-"),
        "deviceID"          -> deviceId.getOrElse("-"),
        "clientPort"        -> clientPort.getOrElse("-"),
        "transactionName"   -> auditType
      ),
      detail = detail
    )

  def createSUT = new SUT

  class SUT
      extends AuditService(
        appName,
        mock[AuditConnector],
        appConfig,
        ec
      )

  "AuditService" should {

    lazy val now     = Instant.now()
    lazy val eventId = "event-id"

    "create and send an audit event" when {

      "the event name, path and a map of details is supplied" in {

        val sut = createSUT

        val eventName    = "testEvent"
        val eventDetails = Map("nino" -> nino.nino)
        val testPath     = "/test-path"

        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val stuff = sut.createAndSendAuditEvent("testEvent", testPath, eventDetails)

        Await.result(stuff, 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())

        val dataEvent = argumentCaptor.getValue

        dataEvent.auditSource mustBe appName
        dataEvent.auditType mustBe eventName
        dataEvent.detail mustBe eventDetails
        dataEvent.tags("path") mustBe testPath
      }

      "the event name and a map of details is supplied. The path is supplied in the header" in {

        val sut = createSUT

        val eventName    = "testEvent"
        val eventDetails = Map("nino" -> nino.nino)
        val testPath     = "/test-path"

        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withHeaders(("Referer", testPath))

        val stuff = sut.createAndSendAuditEvent("testEvent", eventDetails)

        Await.result(stuff, 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())

        val dataEvent = argumentCaptor.getValue

        dataEvent.auditSource mustBe appName
        dataEvent.auditType mustBe eventName
        dataEvent.detail mustBe eventDetails
        dataEvent.tags("path") mustBe testPath
      }

      "the event name and a map of details is supplied. The path is not supplied." in {

        val sut = createSUT

        val eventName       = "testEvent"
        val eventDetails    = Map("nino" -> nino.nino)
        val missingTestPath = "NA"

        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()

        val stuff = sut.createAndSendAuditEvent("testEvent", eventDetails)

        Await.result(stuff, 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())

        val dataEvent = argumentCaptor.getValue

        dataEvent.auditSource mustBe appName
        dataEvent.auditType mustBe eventName
        dataEvent.detail mustBe eventDetails
        dataEvent.tags("path") mustBe missingTestPath
      }
    }

    "send audit user entry event" when {

      "one employment details has been passed" in {
        val sut                        = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val employment    =
          Employment(
            "The Man Plc",
            Live,
            None,
            Some(LocalDate.parse("2016-06-09")),
            None,
            Nil,
            "",
            "",
            1,
            None,
            hasPayrolledBenefit = false,
            receivingOccupationalPension = false,
            EmploymentIncome
          )
        val taxCodeIncome =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
        Await.result(
          sut.sendUserEntryAuditEvent(nino, "NA", List(employment), List(taxCodeIncome)),
          5.seconds
        )

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          auditType = sut.userEnterEvent,
          detail = auditDetail(sut.userEnterEvent, Some("1"))
        )
          .copy(generatedAt = now, eventId = eventId)
      }

      "there is zero employment" in {
        val sut                        = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier = HeaderCarrier()

        Await
          .result(
            sut.sendUserEntryAuditEvent(
              nino,
              "NA",
              Seq.empty[Employment],
              Seq.empty[TaxCodeIncome]
            ),
            5.seconds
          )

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          auditType = sut.userEnterEvent,
          detail = auditDetail(sut.userEnterEvent, Some("0"))
        ).copy(generatedAt = now, eventId = eventId)
      }
    }

    "send audit event and redirect uri" when {

      "user started employee-pension iform journey" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, EmployeePensionIForm), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          auditType = sut.employmentPensionEvent,
          detail = auditDetail(sut.employmentPensionEvent)
        ).copy(generatedAt = now, eventId = eventId)
      }

      "user started employee-pension iform journey with proper headers" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier(
          sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")),
          requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"),
          deviceID = Some("deviceId"),
          forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort")
        )
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, EmployeePensionIForm), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         = event(
          auditType = sut.employmentPensionEvent,
          sessionId = Some("1234"),
          requestId = Some("requestId"),
          trueClientIp = Some("trueClientIp"),
          path = Some("/test-path"),
          deviceId = Some("deviceId"),
          clientPort = Some("clientPort"),
          detail = auditDetail(
            sut.employmentPensionEvent
          )
        )
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)
      }

      "user started company-benefits iform journey" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, CompanyBenefitsIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          sut.companyBenefitsEvent,
          detail = auditDetail(sut.companyBenefitsEvent)
        ).copy(generatedAt = now, eventId = eventId)
      }

      "user started company-benefits iform journey with proper headers" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier(
          sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")),
          requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"),
          deviceID = Some("deviceId"),
          forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort")
        )
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, CompanyBenefitsIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         = event(
          auditType = sut.companyBenefitsEvent,
          sessionId = Some("1234"),
          requestId = Some("requestId"),
          trueClientIp = Some("trueClientIp"),
          path = Some("/test-path"),
          deviceId = Some("deviceId"),
          clientPort = Some("clientPort"),
          detail = auditDetail(sut.companyBenefitsEvent)
        )
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)
      }

      "user started company-car iform journey" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, CompanyCarsIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          sut.companyCarEvent,
          detail = auditDetail(sut.companyCarEvent)
        ).copy(generatedAt = now, eventId = eventId)
      }

      "user started company-car iform journey with proper headers" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier(
          sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")),
          requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"),
          deviceID = Some("deviceId"),
          forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort")
        )
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, CompanyCarsIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         = event(
          auditType = sut.companyCarEvent,
          sessionId = Some("1234"),
          requestId = Some("requestId"),
          trueClientIp = Some("trueClientIp"),
          path = Some("/test-path"),
          deviceId = Some("deviceId"),
          clientPort = Some("clientPort"),
          detail = auditDetail(sut.companyCarEvent)
        )
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)
      }

      "user started medical-benefits iform journey" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, MedicalBenefitsIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          sut.medicalBenefitsEvent,
          detail = auditDetail(sut.medicalBenefitsEvent)
        ).copy(generatedAt = now, eventId = eventId)
      }

      "user started medical-benefits iform journey with proper headers" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier(
          sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")),
          requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"),
          deviceID = Some("deviceId"),
          forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort")
        )
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, MedicalBenefitsIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         = event(
          auditType = sut.medicalBenefitsEvent,
          sessionId = Some("1234"),
          requestId = Some("requestId"),
          trueClientIp = Some("trueClientIp"),
          path = Some("/test-path"),
          deviceId = Some("deviceId"),
          clientPort = Some("clientPort"),
          detail = auditDetail(sut.medicalBenefitsEvent)
        )
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)
      }

      "user started other-income iform journey" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, OtherIncomeIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          sut.otherIncomeEvent,
          detail = auditDetail(sut.otherIncomeEvent)
        ).copy(generatedAt = now, eventId = eventId)
      }

      "user started other-income iform journey with proper headers" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier(
          sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")),
          requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"),
          deviceID = Some("deviceId"),
          forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort")
        )
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, OtherIncomeIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         = event(
          auditType = sut.otherIncomeEvent,
          sessionId = Some("1234"),
          requestId = Some("requestId"),
          trueClientIp = Some("trueClientIp"),
          path = Some("/test-path"),
          deviceId = Some("deviceId"),
          clientPort = Some("clientPort"),
          detail = auditDetail(sut.otherIncomeEvent)
        )
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)
      }

      "user started state-benefit iform journey" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, StateBenefitsIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          sut.stateBenefitEvent,
          detail = auditDetail(sut.stateBenefitEvent)
        ).copy(generatedAt = now, eventId = eventId)
      }

      "user started state-benefit iform journey with proper headers" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier(
          sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")),
          requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"),
          deviceID = Some("deviceId"),
          forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort")
        )
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, StateBenefitsIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         = event(
          auditType = sut.stateBenefitEvent,
          sessionId = Some("1234"),
          requestId = Some("requestId"),
          trueClientIp = Some("trueClientIp"),
          path = Some("/test-path"),
          deviceId = Some("deviceId"),
          clientPort = Some("clientPort"),
          detail = auditDetail(sut.stateBenefitEvent)
        )
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)
      }

      "user started investment-income iform journey" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, InvestIncomeIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          sut.investIncomeEvent,
          detail = auditDetail(sut.investIncomeEvent)
        ).copy(generatedAt = now, eventId = eventId)
      }

      "user started investment-income iform journey with proper headers" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier(
          sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")),
          requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"),
          deviceID = Some("deviceId"),
          forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort")
        )
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, InvestIncomeIform), 5.seconds)

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         = event(
          auditType = sut.investIncomeEvent,
          sessionId = Some("1234"),
          requestId = Some("requestId"),
          trueClientIp = Some("trueClientIp"),
          path = Some("/test-path"),
          deviceId = Some("deviceId"),
          clientPort = Some("clientPort"),
          detail = auditDetail(sut.investIncomeEvent)
        )
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)
      }

      "user started marriage allowance service journey" in {
        val sut                                                   = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc: HeaderCarrier                            = HeaderCarrier()
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()

        val result = Await.result(sut.sendAuditEventAndGetRedirectUri(nino, MarriageAllowanceService), 5.seconds)

        result mustBe appConfig.marriageServiceHistoryUrl

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          sut.marriageAllowanceEvent,
          detail = auditDetail(MarriageAllowanceService)
        ).copy(generatedAt = now, eventId = eventId)
      }
    }

    "send company car event with fuelEndDate" when {
      "fuelEndDate is provided" in {
        val sut                        = createSUT
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val result = Await.result(
          sut.sendEndCompanyCarAuditEvent(
            nino.toString,
            "1",
            "1",
            "2017-01-01",
            Some("2017-01-01"),
            isSuccessful = true,
            path = "NA"
          ),
          5.seconds
        )

        result mustBe AuditResult.Success

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         = event(sut.finishedCompanyCarEvent, detail = auditDetail(sut.finishedCompanyCarEvent))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)

      }
      "no fuelEndDate is provided" in {
        val sut                        = createSUT
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val result = Await.result(
          sut
            .sendEndCompanyCarAuditEvent(nino.toString, "1", "1", "2017-01-01", None, isSuccessful = true, path = "NA"),
          5.seconds
        )

        result mustBe AuditResult.Success

        val argumentCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent                         =
          event(sut.finishedCompanyCarEvent, detail = auditDetail("finishedCompanyCarJourneyNoFuel"))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent
          .copy(generatedAt = now, eventId = eventId)
      }
    }
  }

  private val entryAuditDetails = (noOfEmpAndTax: String) =>
    Map(
      "nino"                       -> nino.nino,
      "noOfCurrentYearEmployments" -> noOfEmpAndTax,
      "noOfTaxCodes"               -> noOfEmpAndTax
    )

  private val employmentOrPensionAuditDetails = Map("nino" -> nino.nino)
  private val companyBenefitsAuditDetails     = Map("nino" -> nino.nino)
  private val endCompanyCarDetails            = Map(
    "nino"          -> nino.nino,
    "employmentId"  -> "1",
    "carSequenceNo" -> "1",
    "carEndDate"    -> "2017-01-01",
    "fuelEndDate"   -> "2017-01-01",
    "isSuccessful"  -> "true"
  )
  private val endCompanyCarDetailsNoFuel      = Map(
    "nino"          -> nino.nino,
    "employmentId"  -> "1",
    "carSequenceNo" -> "1",
    "carEndDate"    -> "2017-01-01",
    "fuelEndDate"   -> "NA",
    "isSuccessful"  -> "true"
  )

}
