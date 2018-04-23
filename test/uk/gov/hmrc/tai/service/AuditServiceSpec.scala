/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.FakeTaiPlayApplication
import org.joda.time.{DateTime, LocalDate}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}
import uk.gov.hmrc.http.{HeaderCarrier, UserId}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.service.AuditService._
import uk.gov.hmrc.tai.util.TaiConstants._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AuditServiceSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "AuditService" should {

    lazy val now = DateTime.now()
    lazy val eventId = "event-id"

    "create and send an audit event" when {

      "the event name, path and a map of details is supplied" in {

        val sut = createSUT

        val eventName = "testEvent"
        val eventDetails = Map("nino" -> nino.nino)
        val testPath = "/test-path"
        val ingoredPath = "/ignored-path"

        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest().withHeaders(("Referer", ingoredPath))

        val stuff = sut.createAndSendAuditEvent("testEvent", testPath, eventDetails)

        Await.result(stuff, 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])

        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())

        val dataEvent = argumentCaptor.getValue

        dataEvent.auditSource mustBe sut.appName
        dataEvent.auditType mustBe eventName
        dataEvent.detail mustBe eventDetails
        dataEvent.tags("path") mustBe testPath
      }

      "the event name and a map of details is supplied. The path is supplied in the header" in {

        val sut = createSUT

        val eventName = "testEvent"
        val eventDetails = Map("nino" -> nino.nino)
        val testPath = "/test-path"

        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest().withHeaders(("Referer", testPath))

        val stuff = sut.createAndSendAuditEvent("testEvent", eventDetails)

        Await.result(stuff, 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])

        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())

        val dataEvent = argumentCaptor.getValue

        dataEvent.auditSource mustBe sut.appName
        dataEvent.auditType mustBe eventName
        dataEvent.detail mustBe eventDetails
        dataEvent.tags("path") mustBe testPath
      }

      "the event name and a map of details is supplied. The path is not supplied." in {

        val sut = createSUT

        val eventName = "testEvent"
        val eventDetails = Map("nino" -> nino.nino)
        val missingTestPath = "NA"

        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        val stuff = sut.createAndSendAuditEvent("testEvent", eventDetails)

        Await.result(stuff, 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])

        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())

        val dataEvent = argumentCaptor.getValue

        dataEvent.auditSource mustBe sut.appName
        dataEvent.auditType mustBe eventName
        dataEvent.detail mustBe eventDetails
        dataEvent.tags("path") mustBe missingTestPath
      }
    }

    "send audit user entry event" when {

      "one employment details has been passed" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))

        val employment = Employment("The Man Plc", None, new LocalDate("2016-06-09"), None, Nil, "", "", 1, None, false)
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOperation, Live)
        Await.result(sut.sendUserEntryAuditEvent(nino, "NA", List(employment),List(taxCodeIncome)), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          auditType = userEnterEvent,detail = auditDetail(userEnterEvent,Some("1")))
          .copy(generatedAt = now, eventId = eventId)
      }

      "there is zero employment" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))

        Await.result(sut.sendUserEntryAuditEvent(nino, "NA", Seq.empty[Employment], Seq.empty[TaxCodeIncome]), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(auditType = userEnterEvent,
          detail = auditDetail(userEnterEvent, Some("0"))).copy(generatedAt = now, eventId = eventId)
      }
    }

    "send audit event and redirect uri" when {

      "user started employee-pension iform journey" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, EmployeePensionIForm), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          auditType = employmentPensionEvent, detail = auditDetail(employmentPensionEvent)).copy(generatedAt = now, eventId = eventId)
      }

      "user started employee-pension iform journey with proper headers" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")), sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")), requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"), deviceID = Some("deviceId"), forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort"))
        implicit val request = FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, EmployeePensionIForm), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(employmentPensionEvent, Some("1234"), Some("123"), Some("requestId"),
          Some("trueClientIp"), Some("/test-path"), Some("deviceId"), Some("ip"), Some("clientPort"), auditDetail(
            employmentPensionEvent
          ))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)
      }

      "user started company-benefits iform journey" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, CompanyBenefitsIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          companyBenefitsEvent, detail = auditDetail(companyBenefitsEvent)).copy(generatedAt = now, eventId = eventId)
      }

      "user started company-benefits iform journey with proper headers" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")), sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")), requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"), deviceID = Some("deviceId"), forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort"))
        implicit val request = FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, CompanyBenefitsIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(companyBenefitsEvent, Some("1234"), Some("123"), Some("requestId"),
          Some("trueClientIp"), Some("/test-path"), Some("deviceId"), Some("ip"), Some("clientPort"), auditDetail(companyBenefitsEvent))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)
      }

      "user started company-car iform journey" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, CompanyCarsIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          companyCarEvent,detail=auditDetail(companyCarEvent)).copy(generatedAt = now, eventId = eventId)
      }

      "user started company-car iform journey with proper headers" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")), sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")), requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"), deviceID = Some("deviceId"), forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort"))
        implicit val request = FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, CompanyCarsIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(companyCarEvent, Some("1234"), Some("123"), Some("requestId"),
          Some("trueClientIp"), Some("/test-path"), Some("deviceId"), Some("ip"), Some("clientPort"),
          auditDetail(companyCarEvent))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)
      }

      "user started medical-benefits iform journey" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, MedicalBenefitsIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          medicalBenefitsEvent, detail = auditDetail(medicalBenefitsEvent)).copy(generatedAt = now, eventId = eventId)
      }

      "user started medical-benefits iform journey with proper headers" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")), sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")), requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"), deviceID = Some("deviceId"), forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort"))
        implicit val request = FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, MedicalBenefitsIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(medicalBenefitsEvent, Some("1234"), Some("123"), Some("requestId"),
          Some("trueClientIp"), Some("/test-path"), Some("deviceId"), Some("ip"), Some("clientPort"),auditDetail(medicalBenefitsEvent))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)
      }

      "user started other-income iform journey" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, OtherIncomeIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          otherIncomeEvent, detail = auditDetail(otherIncomeEvent)).copy(generatedAt = now, eventId = eventId)
      }

      "user started other-income iform journey with proper headers" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")), sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")), requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"), deviceID = Some("deviceId"), forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort"))
        implicit val request = FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, OtherIncomeIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(otherIncomeEvent, Some("1234"), Some("123"), Some("requestId"),
          Some("trueClientIp"), Some("/test-path"), Some("deviceId"), Some("ip"), Some("clientPort"),
        auditDetail(otherIncomeEvent))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)
      }

      "user started state-benefit iform journey" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, StateBenefitsIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          stateBenefitEvent, detail = auditDetail(stateBenefitEvent)).copy(generatedAt = now, eventId = eventId)
      }

      "user started state-benefit iform journey with proper headers" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")), sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")), requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"), deviceID = Some("deviceId"), forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort"))
        implicit val request = FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, StateBenefitsIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(stateBenefitEvent, Some("1234"), Some("123"), Some("requestId"),
          Some("trueClientIp"), Some("/test-path"), Some("deviceId"), Some("ip"), Some("clientPort"), detail = auditDetail(stateBenefitEvent))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)
      }

      "user started investment-income iform journey" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, InvestIncomeIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          investIncomeEvent, detail = auditDetail(investIncomeEvent)).copy(generatedAt = now, eventId = eventId)
      }

      "user started investment-income iform journey with proper headers" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")), sessionId = Some(SessionId("1234")),
          authorization = Some(Authorization("123")), requestId = Some(RequestId("requestId")),
          trueClientIp = Some("trueClientIp"), deviceID = Some("deviceId"), forwarded = Some(ForwardedFor("ip")),
          trueClientPort = Some("clientPort"))
        implicit val request = FakeRequest().withHeaders(("Referer", "/test-path"))

        Await.result(sut.sendAuditEventAndGetRedirectUri(nino, InvestIncomeIform), 5.seconds)

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(investIncomeEvent, Some("1234"), Some("123"), Some("requestId"),
          Some("trueClientIp"), Some("/test-path"), Some("deviceId"), Some("ip"), Some("clientPort"), detail = auditDetail(investIncomeEvent))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)
      }

      "user started marriage allowance service journey" in {
        val sut = createSUT
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        implicit val request = FakeRequest()

        val result = Await.result(sut.sendAuditEventAndGetRedirectUri(nino, MarriageAllowanceService), 5.seconds)

        result mustBe ApplicationConfig.marriageServiceHistoryUrl

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe event(
          marriageAllowanceEvent, detail = auditDetail(MarriageAllowanceService)).copy(generatedAt = now, eventId = eventId)
      }
    }

    "send company car event with fuelEndDate" when {
      "fuelEndDate is provided" in {
        val sut = createSUT
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val result = Await.result(sut.sendEndCompanyCarAuditEvent(nino.toString, "1", "1", "2017-01-01", Some("2017-01-01"), true, path = "NA"), 5.seconds)

        result mustBe AuditResult.Success

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(finishedCompanyCarEvent ,detail = auditDetail(finishedCompanyCarEvent))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)

      }
      "no fuelEndDate is provided" in {
        val sut = createSUT
        implicit val hc = HeaderCarrier(userId = Some(UserId("ABC")))
        when(sut.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val result = Await.result(sut.sendEndCompanyCarAuditEvent(nino.toString, "1", "1", "2017-01-01", None, true, path = "NA"), 5.seconds)

        result mustBe AuditResult.Success

        val argumentCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(sut.auditConnector, times(1)).sendEvent(argumentCaptor.capture())(any(), any())
        val expectedDataEvent = event(finishedCompanyCarEvent ,detail = auditDetail("finishedCompanyCarJourneyNoFuel"))
        argumentCaptor.getValue.copy(generatedAt = now, eventId = eventId) mustBe expectedDataEvent.copy(generatedAt = now, eventId = eventId)
      }
    }
  }

  private val nino = new Generator().nextNino

  private val entryAuditDetails = (noOfEmpAndTax: String) => Map("authProviderId" -> "ABC", "nino" -> nino.nino,
    "noOfCurrentYearEmployments" -> noOfEmpAndTax, "noOfTaxCodes" -> noOfEmpAndTax)

  private val employmentOrPensionAuditDetails = Map("authProviderId" -> "ABC", "nino" -> nino.nino)
  private val companyBenefitsAuditDetails = Map("authProviderId" -> "ABC", "nino" -> nino.nino)
  private val endCompanyCarDetails = Map(
    "authProviderId" -> "ABC",
    "nino" -> nino.nino,
    "employmentId" -> "1",
    "carSequenceNo" -> "1",
    "carEndDate" -> "2017-01-01",
    "fuelEndDate" -> "2017-01-01",
    "isSuccessful" -> "true"
  )
  private val endCompanyCarDetailsNoFuel = Map(
    "authProviderId" -> "ABC",
    "nino" -> nino.nino,
    "employmentId" -> "1",
    "carSequenceNo" -> "1",
    "carEndDate" -> "2017-01-01",
    "fuelEndDate" -> "NA",
    "isSuccessful" -> "true"
  )


  private def auditDetail(auditType: String, noOfEmpAndTax: Option[String] = None): Map[String, String] = {
    auditType match {
      case "userEntersService" => entryAuditDetails(noOfEmpAndTax.getOrElse("0"))
      case "startedEmploymentPensionJourney" => employmentOrPensionAuditDetails
      case "finishedCompanyCarJourney" => endCompanyCarDetails
      case "finishedCompanyCarJourneyNoFuel" => endCompanyCarDetailsNoFuel
      case _ => companyBenefitsAuditDetails
    }
  }

  private def event(auditType: String, sessionId: Option[String] = None,
                    authorization: Option[String] = None, requestId: Option[String] = None,
                    trueClientIp: Option[String] = None, path: Option[String] = None,
                    deviceId: Option[String] = None, ipAddress: Option[String] = None,
                    clientPort: Option[String] = None, detail: Map[String, String]) =
    DataEvent(auditSource = "test", auditType = auditType,
      tags = Map("clientIP" -> trueClientIp.getOrElse("-"),
        "path" -> path.getOrElse("NA"), "X-Session-ID" -> sessionId.getOrElse("-"),
        "Akamai-Reputation" -> "-", "X-Request-ID" -> requestId.getOrElse("-"),
        "deviceID" -> deviceId.getOrElse("-"), "ipAddress" -> ipAddress.getOrElse("-"),
        "token" -> "-", "clientPort" -> clientPort.getOrElse("-"), "Authorization" -> authorization.getOrElse("-"),
        "transactionName" -> auditType), detail = detail

    )

  def createSUT = new SUT

  class SUT extends AuditService {

    override val appName: String = "test"
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val taiService: TaiService = mock[TaiService]
  }
}
