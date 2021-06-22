/*
 * Copyright 2021 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{INTERNAL_SERVER_ERROR, _}
import play.api.i18n.Messages
import play.api.libs.json.{Format, Json}
import play.api.test.Injecting
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http._
import utils.WireMockHelper

import scala.concurrent.Await
import scala.concurrent.duration._

class HttpHandlerSpec2
    extends WordSpec with GuiceOneAppPerSuite with MustMatchers with WireMockHelper with ScalaFutures
    with IntegrationPatience with Injecting {

  val generatedNino = new Generator().nextNino

  val generatedSaUtr = new Generator().nextAtedUtr

  lazy val messages = inject[Messages]

  lazy val httpHandler = inject[HttpHandler]

  lazy val testUrl = server.url("/")

  protected case class ResponseObject(name: String, age: Int)
  implicit val responseObjectFormat = Json.format[ResponseObject]
  implicit val hc = HeaderCarrier()
  private val responseBodyObject = ResponseObject("Name", 24)

  case class DateRequest(date: LocalDate)

  object DateRequest {
    import play.api.libs.json.JodaWrites._
    import play.api.libs.json.JodaReads._
    implicit val formatDateRequest: Format[DateRequest] = Json.format[DateRequest]
  }

  "getFromApiV2" must {

    "should return a json when OK" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withBody(Json.toJson(responseBodyObject).toString())))

      val responseFuture = httpHandler.getFromApiV2(testUrl)
      val response = Await.result(responseFuture, 5 seconds)

      response mustBe Json.toJson(responseBodyObject)

    }

    "should return a NotFoundException when NOT_FOUND response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("not found")))

      val responseFuture = httpHandler.getFromApiV2(testUrl)
      val ex = the[NotFoundException] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("not found")

    }

    "should return a InternalServerError when INTERNAL_SERVER_ERROR response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("internal server error")))

      val responseFuture = httpHandler.getFromApiV2(testUrl)
      val ex = the[Upstream5xxResponse] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("internal server error")

    }

    "should return a BadRequestException when BAD_REQUEST response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("bad request")))

      val responseFuture = httpHandler.getFromApiV2(testUrl)
      val ex = the[BadRequestException] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("bad request")

    }

    "should return a LockedException when LOCKED response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(LOCKED).withBody("locked")))

      val responseFuture = httpHandler.getFromApiV2(testUrl)
      val ex = the[Upstream4xxResponse] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("locked")

    }

    "should return a UnauthorizedException when UNAUTHORIZED response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(UNAUTHORIZED).withBody("unauthorized")))

      val responseFuture = httpHandler.getFromApiV2(testUrl)
      val ex = the[UnauthorizedException] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("unauthorized")

    }

    "should return a HttpException when unknown response" in {

      server.stubFor(
        get(anyUrl())
          .willReturn(aResponse().withStatus(418).withBody("unknown response")))

      val responseFuture = httpHandler.getFromApiV2(testUrl)
      val ex = the[Upstream4xxResponse] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("unknown response")

    }

  }

  "putToApi" must {

    "return OK" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(OK)))

      val result = Await.result(httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())), 5.seconds)

      result.status mustBe OK

    }

    "should return a NotFoundException when NOT_FOUND response" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(NOT_FOUND)))

      val result = the[NotFoundException] thrownBy Await
        .result(httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())), 5.seconds)

      result.responseCode mustBe NOT_FOUND

    }

    "should return a InternalServerException when INTERNAL_SERVER_ERROR response" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("internal server exception")))

      val ex = the[Upstream5xxResponse] thrownBy Await
        .result(httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())), 5.seconds)

      ex.message must include("internal server exception")

    }

    "should return a BadRequestException when BAD_REQUEST response" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(BAD_REQUEST)))

      val result = the[BadRequestException] thrownBy Await
        .result(httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())), 5.seconds)

      result.responseCode mustBe BAD_REQUEST

    }

    "should return a HttpException when unknown response" in {

      server.stubFor(
        put(anyUrl())
          .willReturn(aResponse().withStatus(418).withBody("unknown response")))

      val ex = the[Upstream4xxResponse] thrownBy Await
        .result(httpHandler.putToApi[DateRequest](testUrl, DateRequest(LocalDate.now())), 5.seconds)

      ex.message must include("unknown response")

    }
  }

  "postToApi" must {
    val userInput = "userInput"

    List(
      CREATED,
      OK
    ).foreach { httpStatus =>
      s"return json which is coming from http post call for $httpStatus response" in {

        server.stubFor(
          post(anyUrl())
            .willReturn(aResponse().withStatus(httpStatus).withBody(userInput)))

        val createdResponse = Await.result(httpHandler.postToApi[String](testUrl, userInput), 5 seconds)

        createdResponse.status mustBe httpStatus

      }
    }

    "return Http exception for NOT_FOUND response" in {

      server.stubFor(
        post(anyUrl())
          .willReturn(aResponse().withStatus(NOT_FOUND)))

      val result = the[NotFoundException] thrownBy Await
        .result(httpHandler.postToApi[String](testUrl, userInput), 5 seconds)

      result.responseCode mustBe NOT_FOUND

    }

  }

  "deleteFromApi" must {
    val userInput = "userInput"

    List(
      ACCEPTED,
      OK,
      NO_CONTENT
    ).foreach { httpStatus =>
      s"return json which is coming from http post call for $httpStatus response" in {

        server.stubFor(
          delete(anyUrl())
            .willReturn(aResponse().withStatus(httpStatus)))

        val result = Await.result(httpHandler.deleteFromApi(testUrl), 5 seconds)
        result.status mustBe httpStatus

      }
    }

    "return Http exception for GATEWAY_TIMEOUT response" in {

      server.stubFor(
        delete(anyUrl())
          .willReturn(aResponse().withStatus(GATEWAY_TIMEOUT).withBody("gateway timeout")))

      val responseFuture = httpHandler.deleteFromApi(testUrl)
      val ex = the[Upstream5xxResponse] thrownBy Await.result(responseFuture, 5 seconds)
      ex.message must include("gateway timeout")

    }

  }

}
