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

package uk.gov.hmrc.tai.connectors

import org.joda.time.LocalDate
import data.TaiData
import uk.gov.hmrc.tai.model.SessionData
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{CoreDelete, CoreGet, CorePost, CorePut}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpDelete, HttpException, HttpGet, HttpPost, HttpPut, HttpResponse, InternalServerException, NotFoundException, ServiceUnavailableException}
import uk.gov.hmrc.tai.model.{SessionData, TaxSummaryDetails}


class TaiConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "get taxSummary data " should {

    "return status OK, for successful return from backend " in {
      val taxSummary = TaiData.getEverythingJson
      val sut = createSUT
      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(taxSummary))))

      val result = sut.taxSummary(nino, 2016)(hc)

      await(result) mustBe taxSummary.as[TaxSummaryDetails]
      verify(sut.http, times(1)).GET[HttpResponse](any())(any(), any(), any())
    }

    "return status as NOT_FOUND, when bad request returned from backend " in {
      val notFoundErrorResponse = Json.parse(
        s"""{"message": "Not Found Exception",
          |"statusCode": 404,"appStatusMessage": "Not Found Exception",
          |"requestUri": "nps/person/$nino/tax-account/2014/calculation"}""".stripMargin)
      val sut = createSUT

      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundErrorResponse))))

      val result = sut.taxSummary(nino, 2016)(hc)
      val thrown = the[NotFoundException] thrownBy await(result)

      thrown.getMessage mustBe Json.stringify(notFoundErrorResponse)
      verify(sut.http, times(1)).GET[HttpResponse](any())(any(), any(), any())
    }

    "return status as BAD_REQUEST, when bad request returned from backend " in {
      val badRequestErrorResponse = Json.parse(
        """{"message": "BAD_REQUEST",
          |  "statusCode": 400,
          |  "appStatusMessage": "Failed NINO check due to malformed NINO",
          |  "requestUri": "/nps-hod-service/services/nps/person/JZ01361D/tax-account/2016/calculation"}""".stripMargin)
      val sut = createSUT

      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestErrorResponse))))

      val result = sut.taxSummary(nino, 2016)(hc)
      val thrown = the[BadRequestException] thrownBy await(result)

      thrown.getMessage mustBe Json.stringify(badRequestErrorResponse)
      verify(sut.http, times(1)).GET[HttpResponse](any())(any(), any(), any())
    }

    "return status as SERVICE_UNAVAILABLE, when bad request returned from backend " in {
      val serviceUnavailableErrorResponse = Json.parse(
        s"""{"message": "Service Unavailable",
          |  "statusCode": 503,
          |  "appStatusMessage": "Service Unavailable",
          |  "requestUri": "nps/person/$nino/tax-account/2016/calculation"}""".stripMargin)
      val sut = createSUT

      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailableErrorResponse))))

      val result = sut.taxSummary(nino, 2016)(hc)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)

      thrown.getMessage mustBe Json.stringify(serviceUnavailableErrorResponse)
      verify(sut.http, times(1)).GET[HttpResponse](any())(any(), any(), any())
    }

    "return status as INTERNAL_SERVER_ERROR, when bad request returned from backend " in {
      val internalServerErrorResponse = Json.parse(
        s"""{"message": "Internal Server Error",
          |  "statusCode": 500,
          |  "appStatusMessage": "BFM Failure",
          |  "requestUri": "nps/person/$nino/tax-account/2016/calculation"}""".stripMargin)
      val sut = createSUT

      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))

      val result = sut.taxSummary(nino, 2016)(hc)
      val thrown = the[InternalServerException] thrownBy await(result)

      thrown.getMessage mustBe Json.stringify(internalServerErrorResponse)
      verify(sut.http, times(1)).GET[HttpResponse](any())(any(), any(), any())
    }

  }

  "update tai data" should {

    val ninoString = nino.nino

    val sessionData = SessionData(
      nino = ninoString,
      taxSummaryDetailsCY = TaxSummaryDetails(nino = ninoString, version = 0))

    "return the persisted session data model" when {

      "the invocation is successful" in {
        val sut = createSUT

        when(sut.http.PUT[SessionData, HttpResponse](any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        val result = sut.updateTaiData(sessionData)
        await(result) mustBe sessionData
      }
    }

    "invoke an HTTP PUT operation with the correct arguments" in {
      val sut = createSUT
      val urlCaptor = ArgumentCaptor.forClass(classOf[String])
      val bodyCaptor = ArgumentCaptor.forClass(classOf[SessionData])

      when(sut.http.PUT[SessionData, HttpResponse](urlCaptor.capture, bodyCaptor.capture)(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      sut.updateTaiData(sessionData)
      verify(sut.http, times(1)).PUT[SessionData, HttpResponse](any(), any())(any(), any(), any(), any())

      urlCaptor.getValue must include(s"tai/$ninoString/tai-data")
      bodyCaptor.getValue mustBe sessionData
    }

    "result in an HTTPException" when {

      "a non 200 response code is returned from the HTTP transport layer" in {

        val internalServerErrorResponse = Json.parse(
          """{"message": "Internal Server Error",
            |  "statusCode": 500,
            |  "appStatusMessage": "A random failure",
            |  "requestUri": "a/nested/connector/uri"}""".stripMargin)
        val sut = createSUT

        when(sut.http.PUT[SessionData, HttpResponse](any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))

        val result = sut.updateTaiData(sessionData)
        val thrown = the[HttpException] thrownBy await(result)

        thrown.getMessage mustBe internalServerErrorResponse.toString()
        thrown.responseCode mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "get tai data " should {

    "return status OK, for successful return from backend " in {
      val sessionData = Json.toJson(SessionData(
        nino = nino.nino,
        taxSummaryDetailsCY = TaxSummaryDetails(nino = nino.nino, version = 0)))
      val sut = createSUT
      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(sessionData))))

      val result = sut.getTaiData(nino)(hc)

      await(result) mustBe sessionData.as[SessionData]
      verify(sut.http, times(1)).GET[HttpResponse](Matchers.eq(sut.url(s"/tai/$nino/tai-data")))(any(), any(), any())
    }

    "return status as NOT_FOUND, when bad request returned from backend " in {
      val notFoundErrorResponse = Json.parse(
        s"""{"message": "Not Found Exception",
          |"statusCode": 404,"appStatusMessage": "Not Found Exception",
          |"requestUri": "nps/person/$nino/tax-account/2014/calculation"}""".stripMargin)
      val sut = createSUT

      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundErrorResponse))))

      val result = sut.getTaiData(nino)(hc)
      val thrown = the[NotFoundException] thrownBy await(result)

      thrown.getMessage mustBe Json.stringify(notFoundErrorResponse)
      verify(sut.http, times(1)).GET[HttpResponse](Matchers.eq(sut.url(s"/tai/$nino/tai-data")))(any(), any(), any())
    }

    "return status as BAD_REQUEST, when bad request returned from backend " in {
      val badRequestErrorResponse = Json.parse(
        """{"message": "BAD_REQUEST",
          |  "statusCode": 400,
          |  "appStatusMessage": "Failed NINO check due to malformed NINO",
          |  "requestUri": "/nps-hod-service/services/nps/person/JZ01361D/tax-account/2016/calculation"}""".stripMargin)
      val sut = createSUT

      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestErrorResponse))))

      val result = sut.getTaiData(nino)(hc)
      val thrown = the[BadRequestException] thrownBy await(result)

      thrown.getMessage mustBe Json.stringify(badRequestErrorResponse)
      verify(sut.http, times(1)).GET[HttpResponse](Matchers.eq(sut.url(s"/tai/$nino/tai-data")))(any(), any(), any())
    }

    "return status as SERVICE_UNAVAILABLE, when bad request returned from backend " in {
      val serviceUnavailableErrorResponse = Json.parse(
        s"""{"message": "Service Unavailable",
          |  "statusCode": 503,
          |  "appStatusMessage": "Service Unavailable",
          |  "requestUri": "nps/person/$nino/tax-account/2016/calculation"}""".stripMargin)
      val sut = createSUT

      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailableErrorResponse))))

      val result = sut.getTaiData(nino)(hc)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)

      thrown.getMessage mustBe Json.stringify(serviceUnavailableErrorResponse)
      verify(sut.http, times(1)).GET[HttpResponse](Matchers.eq(sut.url(s"/tai/$nino/tai-data")))(any(), any(), any())
    }

    "return status as INTERNAL_SERVER_ERROR, when bad request returned from backend " in {
      val internalServerErrorResponse = Json.parse(
        s"""{"message": "Internal Server Error",
          |  "statusCode": 500,
          |  "appStatusMessage": "BFM Failure",
          |  "requestUri": "nps/person/$nino/tax-account/2016/calculation"}""".stripMargin)
      val sut = createSUT

      when(sut.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(internalServerErrorResponse))))

      val result = sut.getTaiData(nino)(hc)
      val thrown = the[InternalServerException] thrownBy await(result)

      thrown.getMessage mustBe Json.stringify(internalServerErrorResponse)
      verify(sut.http, times(1)).GET[HttpResponse](Matchers.eq(sut.url(s"/tai/$nino/tai-data")))(any(), any(), any())
    }

  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy val nino: Nino = new Generator().nextNino

  def createSUT = new SUT

  class SUT extends TaiConnector {
    trait VerbMocks extends CoreGet with CorePut with CorePost with CoreDelete
    override val http = mock[VerbMocks]

    override def serviceUrl: String = "http://dummy"
  }

}
