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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.tai.config.ApplicationConfig
import utils.{BaseSpec, WireMockHelper}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class FandFConnectorSpec extends BaseSpec with WireMockHelper with DefaultAwaitTimeout {
  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("microservice.services.fandf.port" -> server.port())
    .build()

  private val trustedHelperNino = new Generator().nextNino

  val trustedHelper: TrustedHelper =
    TrustedHelper("principal Name", "attorneyName", "returnLink", Some(trustedHelperNino.nino))

  val fandfTrustedHelperResponse: String =
    s"""
       |{
       |   "principalName": "principal Name",
       |   "attorneyName": "attorneyName",
       |   "returnLinkUrl": "returnLink",
       |   "principalNino": "$trustedHelperNino"
       |}
       |""".stripMargin

  trait SpecSetup {

    lazy val connector: FandFConnector = {
      val configDecorator = app.injector.instanceOf[ApplicationConfig]
      new FandFConnector(
        inject[HttpClientV2],
        configDecorator
      )
    }
  }

  "Calling FandFConnector.getTrustedHelper" must {

    "return as Some(trustedHelper) when trustedHelper json returned" in new SpecSetup {
      server.stubFor(
        WireMock.get(urlEqualTo("/delegation/get")).willReturn(ok(fandfTrustedHelperResponse))
      )

      val result: Option[TrustedHelper] = Await.result(connector.getTrustedHelper(), Duration.Inf)

      result mustBe Some(trustedHelper)
    }

    "return as None when empty json returned" in new SpecSetup {
      server.stubFor(
        WireMock.get(urlEqualTo("/delegation/get")).willReturn(notFound())
      )

      val result: Option[TrustedHelper] = Await.result(connector.getTrustedHelper(), Duration.Inf)

      result mustBe None
    }

  }
}
