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

import akka.http.scaladsl.model.StatusCodes.OK
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, anyUrl, get, urlEqualTo}

import scala.concurrent.duration._
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}
import play.api.test.Injecting
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import utils.WireMockHelper

import scala.concurrent.Await

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

  }

}
