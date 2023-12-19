/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{BaseSpec, WireMockHelper}

import java.time.LocalDate
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class HttpHandlerSpec extends BaseSpec with WireMockHelper with ScalaFutures with IntegrationPatience {

  private lazy val httpHandler: HttpHandler = inject[HttpHandler]
  private lazy val httpClientV2: HttpClientV2 = inject[HttpClientV2]

  private lazy val testUrl: String = server.url("/")

  protected case class ResponseObject(name: String, age: Int)

  private implicit val responseObjectFormat: OFormat[ResponseObject] = Json.format[ResponseObject]

  private val responseBodyObject = ResponseObject("Name", 24)

  private case class DateRequest(date: LocalDate)

  private object DateRequest {
    implicit val formatDateRequest: Format[DateRequest] = Json.format[DateRequest]
  }

  private val mockLogger: Logger = mock[Logger]

  private lazy val httpHandlerUsingMockLogger: HttpHandler = new HttpHandler(httpClientV2) {
    override protected val logger: Logger = mockLogger
  }

  "read" must {
    Set(NOT_FOUND, UNPROCESSABLE_ENTITY, UNAUTHORIZED).foreach { httpResponseCode =>
      s"log message: 1 info & 1 error level when response code is $httpResponseCode" in {
        reset(mockLogger)
        doNothing.when(mockLogger).warn(ArgumentMatchers.any())(ArgumentMatchers.any())
        doNothing.when(mockLogger).error(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())

        val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
          Future(Left(UpstreamErrorResponse("", httpResponseCode)))
        whenReady(httpHandlerUsingMockLogger.read(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse("", httpResponseCode))

          Mockito
            .verify(mockLogger, times(1))
            .info(ArgumentMatchers.any())(ArgumentMatchers.any())
          Mockito
            .verify(mockLogger, times(0))
            .error(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())

        }
      }
    }
    "log message: 1 error & 0 info level when response is BAD_REQUEST" in {
      reset(mockLogger)
      doNothing.when(mockLogger).warn(ArgumentMatchers.any())(ArgumentMatchers.any())
      doNothing.when(mockLogger).error(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())

      val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
        Future(Left(UpstreamErrorResponse("", BAD_REQUEST)))
      whenReady(httpHandlerUsingMockLogger.read(response).value) { actual =>
        actual mustBe Left(UpstreamErrorResponse("", BAD_REQUEST))

        Mockito
          .verify(mockLogger, times(0))
          .info(ArgumentMatchers.any())(ArgumentMatchers.any())
        Mockito
          .verify(mockLogger, times(1))
          .error(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())

      }
    }
  }

  "getFromApiV2" must {

    "should return a json when OK" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withBody(Json.toJson(responseBodyObject).toString()))
      )

      val responseFuture = httpHandler.getFromApiV2(testUrl)
      val response = Await.result(responseFuture, 5 seconds)

      response mustBe Json.toJson(responseBodyObject)

    }

    "should return a NotFoundException when NOT_FOUND response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("not found"))
      )

      val responseFuture = httpHandler.getFromApiV2(testUrl).failed.futureValue
      responseFuture mustBe a[NotFoundException]

    }

    "should return a UpstreamErrorResponse when INTERNAL_SERVER_ERROR response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("internal server error"))
      )

      val responseFuture = httpHandler.getFromApiV2(testUrl).failed.futureValue
      responseFuture mustBe a[UpstreamErrorResponse]
    }

    "should return a BadRequestException when BAD_REQUEST response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("bad request"))
      )

      val responseFuture = httpHandler.getFromApiV2(testUrl).failed.futureValue
      responseFuture mustBe a[BadRequestException]

    }

    "should return a UpstreamErrorResponse when LOCKED response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(LOCKED).withBody("locked"))
      )

      val responseFuture = httpHandler.getFromApiV2(testUrl).failed.futureValue
      responseFuture mustBe a[UpstreamErrorResponse]
    }

    "should return a UnauthorizedException when UNAUTHORIZED response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(UNAUTHORIZED).withBody("unauthorized"))
      )

      val responseFuture = httpHandler.getFromApiV2(testUrl).failed.futureValue
      responseFuture mustBe a[UnauthorizedException]
    }

    "should return a UpstreamErrorResponse when unknown response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(IM_A_TEAPOT).withBody("unknown response"))
      )

      val responseFuture = httpHandler.getFromApiV2(testUrl).failed.futureValue
      responseFuture mustBe a[UpstreamErrorResponse]
    }

  }

  "putToApi" must {

    "return OK" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(OK))
      )

      val result = Await.result(httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())), 5.seconds)

      result.status mustBe OK

    }

    "should return a NotFoundException when NOT_FOUND response" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val result = httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())).failed.futureValue
      result mustBe a[NotFoundException]

    }

    "should return a UpstreamErrorResponse when INTERNAL_SERVER_ERROR response" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("internal server exception"))
      )

      val result = httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())).failed.futureValue
      result mustBe a[UpstreamErrorResponse]

    }

    "should return a BadRequestException when BAD_REQUEST response" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(BAD_REQUEST))
      )

      val result = httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())).failed.futureValue
      result mustBe a[BadRequestException]

    }

    "should return a UpstreamErrorResponse when unknown response" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(IM_A_TEAPOT).withBody("unknown response"))
      )

      val result = httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())).failed.futureValue
      result mustBe a[UpstreamErrorResponse]

    }
  }

  "postToApi" must {
    val userInput = "userInput"

    List(
      CREATED,
      OK
    ).foreach { httpStatus =>
      s"return $httpStatus response status for $httpStatus response" in {

        server.stubFor(
          post(anyUrl())
            .willReturn(aResponse().withStatus(httpStatus).withBody(userInput))
        )

        val response = Await.result(httpHandler.postToApi[String](testUrl, userInput), 5 seconds)

        response.status mustBe httpStatus

      }
    }

    List(
      GATEWAY_TIMEOUT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpStatus =>
      s"return UpstreamErrorResponse for $httpStatus response" in {

        server.stubFor(
          post(anyUrl())
            .willReturn(aResponse().withStatus(httpStatus).withBody("error response"))
        )

        val responseFuture = httpHandler.postToApi(testUrl, userInput).failed.futureValue
        responseFuture mustBe a[UpstreamErrorResponse]

      }
    }

    "should return a BAD_REQUEST response code when BAD_REQUEST response" in {
      server.stubFor(
        post(anyUrl())
          .willReturn(aResponse().withStatus(BAD_REQUEST))
      )
      val result = the[BadRequestException] thrownBy Await
        .result(httpHandler.postToApi[String](testUrl, userInput), 5 seconds)

      result.responseCode mustBe BAD_REQUEST
    }

    "should return a NOT_FOUND response code when NOT_FOUND response" in {
      server.stubFor(
        post(anyUrl())
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val result = the[NotFoundException] thrownBy Await
        .result(httpHandler.postToApi[String](testUrl, userInput), 5 seconds)

      result.responseCode mustBe NOT_FOUND

    }

  }

  "deleteFromApi" must {
    List(
      ACCEPTED,
      OK,
      NO_CONTENT
    ).foreach { httpStatus =>
      s"return $httpStatus response status from http delete call for $httpStatus response" in {

        server.stubFor(
          delete(anyUrl())
            .willReturn(aResponse().withStatus(httpStatus))
        )
        val result = Await.result(httpHandler.deleteFromApi(testUrl), 5 seconds)
        result.status mustBe httpStatus

      }
    }

    List(
      GATEWAY_TIMEOUT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpStatus =>
      s"return UpstreamErrorResponse for $httpStatus response" in {

        server.stubFor(
          delete(anyUrl())
            .willReturn(aResponse().withStatus(httpStatus).withBody("error response"))
        )

        val responseFuture = httpHandler.deleteFromApi(testUrl).failed.futureValue
        responseFuture mustBe a[UpstreamErrorResponse]

      }
    }

    "return BadRequestException for BAD_REQUEST response" in {
      server.stubFor(
        delete(anyUrl())
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("bad request"))
      )
      val responseFuture = httpHandler.deleteFromApi(testUrl)
      val ex = the[BadRequestException] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("bad request")
    }

    "return NotFoundException for NOT_FOUND response" in {
      server.stubFor(
        delete(anyUrl())
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("not found"))
      )
      val responseFuture = httpHandler.deleteFromApi(testUrl)
      val ex = the[NotFoundException] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("not found")
    }

  }

}
