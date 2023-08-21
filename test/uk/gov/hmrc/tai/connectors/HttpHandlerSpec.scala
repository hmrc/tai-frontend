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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.http.Status._
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http._
import utils.{BaseSpec, WireMockHelper}

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class HttpHandlerSpec extends BaseSpec with WireMockHelper with ScalaFutures with IntegrationPatience {

  lazy val httpHandler = inject[HttpHandler]

  lazy val testUrl = server.url("/")

  protected case class ResponseObject(name: String, age: Int)
  implicit val responseObjectFormat = Json.format[ResponseObject]

  private val responseBodyObject = ResponseObject("Name", 24)

  case class DateRequest(date: LocalDate)

  object DateRequest {

    implicit val formatDateRequest: Format[DateRequest] = Json.format[DateRequest]
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
