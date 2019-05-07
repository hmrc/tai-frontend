/*
 * Copyright 2019 HM Revenue & Customs
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

import java.net.URL

import com.github.tomakehurst.wiremock.client.WireMock.{any => _, _}
import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{Format, JsString, Json}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http._
import uk.gov.hmrc.tai.config.WSHttp
import utils.WireMockHelper

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class HttpHandlerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with WireMockHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy val handler: HttpHandler = new HttpHandler(WSHttp)
  val nino = new Generator(new Random).nextNino
  val json = Json.obj()

  def serviceUrl: String = s"http://localhost:${server.port()}"

  def url = new URL(s"${serviceUrl}/tai/${nino.nino}/tax-account/tax-free-amount-comparison")

  def getResponse = Await.result(handler.getFromApi(url.toString), 5 seconds)

  def putResponse = Await.result(handler.putToApi(url.toString, "put input"), 5 seconds)

  def postResponse = Await.result(handler.postToApi(url.toString, "post input"), 5 seconds)

  def deleteResponse = Await.result(handler.deleteFromApi(url.toString), 5 seconds)

  "getFromAPI" should {

    "return valid json" when {

      "data is successfully received from the http get call" in {

        server.stubFor(
          get(urlEqualTo(url.getPath)).willReturn(ok(json.toString()))
        )

        getResponse mustBe Json.toJson(json)

      }
    }

    "result in a BadRequest exception" when {

      "when a BadRequest http response is received from the http get call" in {

        val errorMessage = "bad request"

        server.stubFor(
          get(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.BAD_REQUEST).withBody(errorMessage))
        )

        val thrown = the[BadRequestException] thrownBy getResponse

        thrown.getMessage mustEqual errorMessage

      }
    }

    "result in a NotFound exception" when {

      "when a NotFound http response is received from the http get call" in {

        val errorMessage = "not found"

        server.stubFor(
          get(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.NOT_FOUND).withBody(errorMessage))
        )

        val thrown = the[NotFoundException] thrownBy getResponse

        thrown.getMessage mustEqual errorMessage

      }
    }

    "result in a InternalServerError exception" when {

      "when a InternalServerError http response is received from the http get call" in {

        val errorMessage = "internal server error"

        server.stubFor(
          get(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR).withBody(errorMessage))
        )

        val thrown = the[InternalServerException] thrownBy getResponse

        thrown.getMessage mustEqual errorMessage
      }
    }

    "result in a Locked exception" when {

      "when a Locked response is received from the http get call" in {

        val errorMessage = "locked"

        server.stubFor(
          get(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.LOCKED).withBody(errorMessage))
        )

        val thrown = the[LockedException] thrownBy getResponse

        thrown.getMessage mustEqual errorMessage

      }
    }

    "result in an HttpException" when {

      "when a unknown error http response is received from the http get call" in {

        val errorMessage = "unknown response"
        val unknownStatus = 418

        server.stubFor(
          get(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(unknownStatus).withBody(errorMessage))
        )

        val thrown = the[HttpException] thrownBy getResponse

        thrown.getMessage mustEqual errorMessage

      }
    }
  }

  "postToApi" should {
    "return valid json for an OK response" in {

      server.stubFor(post(urlEqualTo(url.getPath)).willReturn(ok(json.toString())))
      postResponse.json mustBe Json.toJson(json)
    }

    "return valid json for a Created response" in {
      server.stubFor(post(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.CREATED).withBody(json.toString())))

      postResponse.json mustBe Json.toJson(json)
    }

    "return Http exception" when {
      "http response is NOT_FOUND" in {

        val errorMessage = "not found"

        server.stubFor(post(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.NOT_FOUND)
          .withBody(errorMessage)))

        val thrown = the[HttpException] thrownBy postResponse
        thrown.responseCode mustBe Status.NOT_FOUND
        thrown.message mustBe errorMessage
      }

      "http response is GATEWAY_TIMEOUT" in {
        server.stubFor(post(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.GATEWAY_TIMEOUT)))

        val thrown = the[HttpException] thrownBy postResponse
        thrown.responseCode mustBe Status.GATEWAY_TIMEOUT
      }
    }
  }


  "putToApi" should {
    "return OK" when {
      "a successful put request is made" in {
        server.stubFor(put(urlEqualTo(url.getPath)).willReturn(ok(json.toString())))
        putResponse.json mustBe Json.toJson(json)
      }

    }

    "return Not Found exception" when {
      "a Not Found response is received from the http put call" in {
        val errorMessage = "not found"

        server.stubFor(
          put(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.NOT_FOUND).withBody(errorMessage))
        )

        val thrown = the[NotFoundException] thrownBy putResponse

        thrown.getMessage mustEqual errorMessage
      }
    }

    "return Internal Server exception" when {
      "an Internal Server Response is received from the http put call" in {

        val errorMessage = "internal server error"

        server.stubFor(
          put(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR).withBody(errorMessage))
        )

        val thrown = the[InternalServerException] thrownBy putResponse

        thrown.getMessage mustEqual errorMessage
      }
    }

    "return Bad Request exception" when {
      "a Bad Request response is received from the http put call" in {

        val errorMessage = "bad request"

        server.stubFor(
          put(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.BAD_REQUEST).withBody(errorMessage))
        )

        val thrown = the[BadRequestException] thrownBy putResponse

        thrown.getMessage mustEqual errorMessage
      }
    }

    "return Http exception" when {

      "an error occurs" in {

        val errorMessage = "unknown response"
        val unknownStatus = 418

        server.stubFor(
          put(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(unknownStatus).withBody(errorMessage))
        )

        val thrown = the[HttpException] thrownBy putResponse

        thrown.getMessage mustEqual errorMessage

      }

    }
  }


  "deleteFromApi" must {
    "post request to DELETE and return the http response" when {
      "http DELETE returns OK" in {
        server.stubFor(delete(urlEqualTo(url.getPath)).willReturn(ok(json.toString())))
        deleteResponse.json mustBe Json.toJson(json)
      }

      "http DELETE returns NO_CONTENT" in {
        server.stubFor(delete(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.NO_CONTENT)))
        deleteResponse.status mustBe Status.NO_CONTENT

      }

      "http DELETE returns ACCEPTED" in {
        server.stubFor(delete(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.ACCEPTED).withBody(json.toString())))
        deleteResponse.json mustBe Json.toJson(json)
      }

    }
    "return Http exception" when {
      "http response is NOT OK" in {
        server.stubFor(delete(urlEqualTo(url.getPath)).willReturn(aResponse().withStatus(Status.GATEWAY_TIMEOUT)))

        val thrown = the[HttpException] thrownBy deleteResponse
        thrown.responseCode mustBe Status.GATEWAY_TIMEOUT
      }
    }
  }
}
