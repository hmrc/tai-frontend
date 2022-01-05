/*
 * Copyright 2022 HM Revenue & Customs
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

import com.codahale.metrics.Timer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get}
import com.kenshoo.play.metrics.Metrics
import org.joda.time.YearMonth
import org.mockito.Mockito.{times, verify, when, reset => resetMock}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.tai.model.enums.APITypes
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import uk.gov.hmrc.tai.util.TestMetrics
import uk.gov.hmrc.webchat.client.WebChatClient
import uk.gov.hmrc.webchat.testhelpers.WebChatClientStub
import utils.{BaseSpec, WireMockHelper}

class JrsConnectorSpec extends BaseSpec with WireMockHelper with ScalaFutures with IntegrationPatience {

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("microservice.services.coronavirus-jrs-published-employees.port" -> server.port)
    .overrides(
      bind[WebChatClient].toInstance(new WebChatClientStub)
    )
    .build()

  lazy val httpClient = inject[HttpClient]

  val testMetrics = new TestMetrics

  lazy val jrsConnector = new JrsConnector(httpClient, testMetrics, appConfig)

  val jrsClaimsUrl: String =
    s"/coronavirus-jrs-published-employees/employee/$nino"

  "jrsConnector" should {

    "return the jrs claim data" when {

      "when 200 is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(200).withBody(jrsClaimsJsonResponse.toString())
            ))

        jrsConnector.getJrsClaimsForIndividual(nino)(hc).value.futureValue mustBe Some(
          JrsClaims(jrsClaimsModelResponse))
      }
    }

    "return with empty jrs claim data" when {

      "when 204 is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(204)
            ))

        jrsConnector.getJrsClaimsForIndividual(nino)(hc).value.futureValue mustBe Some(JrsClaims(List.empty))
      }
    }

    "return with None" when {

      "when any other response is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(502)
            ))

        jrsConnector.getJrsClaimsForIndividual(nino)(hc).value.futureValue mustBe None
      }

      "when Bad Request exception is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(400).withBody("bad request exception")
            ))

        jrsConnector.getJrsClaimsForIndividual(nino)(hc).value.futureValue mustBe None
      }

      "when Unauthorized is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(401).withBody("bad request exception")
            ))

        jrsConnector.getJrsClaimsForIndividual(nino)(hc).value.futureValue mustBe None
      }

      "when Forbidden is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(403).withBody("bad request exception")
            ))

        jrsConnector.getJrsClaimsForIndividual(nino)(hc).value.futureValue mustBe None
      }
    }
  }

  val jrsClaimsModelResponse = List(Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-02"))))

  val jrsClaimsJsonResponse: JsValue = Json.obj(
    "employers" -> Json.arr(
      Json.obj(
        "name"              -> "ASDA",
        "employerReference" -> "ABC-DEFGHIJ",
        "claims" -> Json.arr(
          Json.obj(
            "yearAndMonth" -> "2021-02"
          )
        )
      )
    )
  )

}
