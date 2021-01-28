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

import com.codahale.metrics.Timer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get}
import org.mockito.Mockito.{times, verify, when, reset => resetMock}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.tai.metrics.Metrics
import uk.gov.hmrc.tai.model.enums.APITypes
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import utils.{BaseSpec, WireMockHelper}

class JrsConnectorSpec extends BaseSpec with WireMockHelper with ScalaFutures with IntegrationPatience {

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("microservice.services.coronavirus-jrs-published-employees.port" -> server.port)
    .build()

  lazy val mockMetrics = mock[Metrics]
  lazy val httpClient = inject[HttpClient]

  lazy val jrsConnector = new JrsConnector(httpClient, mockMetrics, servicesConfig)

  val jrsClaimsUrl: String =
    s"/coronavirus-jrs-published-employees/employee/$nino"

  override def beforeEach(): Unit = {
    resetMock(mockMetrics, mockTimerContext)
    super.beforeEach()
  }

  val mockTimerContext = mock[Timer.Context]

  "jrsConnector" should {

    "return the jrs claim data" when {

      "when 200 is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(200).withBody(jrsClaimsJsonResponse.toString())
            ))

        when(mockTimerContext.stop())
          .thenReturn(123L)
        when(mockMetrics.startTimer(APITypes.JrsClaimAPI))
          .thenReturn(mockTimerContext)

        jrsConnector.getJrsClaims(nino).futureValue mustBe Some(JrsClaims(jrsClaimsModelResponse))

        verify(mockMetrics, times(1)).startTimer(APITypes.JrsClaimAPI)
        verify(mockTimerContext, times(1)).stop()
        verify(mockMetrics, times(1)).incrementSuccessCounter(APITypes.JrsClaimAPI)
      }
    }

    "return with empty jrs claim data" when {

      "when 204 is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(204)
            ))

        when(mockTimerContext.stop())
          .thenReturn(123L)
        when(mockMetrics.startTimer(APITypes.JrsClaimAPI))
          .thenReturn(mockTimerContext)

        jrsConnector.getJrsClaims(nino).futureValue mustBe Some(JrsClaims(List.empty))

        verify(mockMetrics, times(1)).startTimer(APITypes.JrsClaimAPI)
        verify(mockTimerContext, times(1)).stop()
        verify(mockMetrics, times(1)).incrementSuccessCounter(APITypes.JrsClaimAPI)
      }
    }

    "return with None" when {

      "when any other response is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(502)
            ))

        when(mockTimerContext.stop())
          .thenReturn(123L)
        when(mockMetrics.startTimer(APITypes.JrsClaimAPI))
          .thenReturn(mockTimerContext)

        jrsConnector.getJrsClaims(nino).futureValue mustBe None

        verify(mockMetrics, times(1)).startTimer(APITypes.JrsClaimAPI)
        verify(mockTimerContext, times(1)).stop()
        verify(mockMetrics, times(1)).incrementFailedCounter(APITypes.JrsClaimAPI)
      }

      "when any exception is received from jrs API" in {

        server.stubFor(
          get(jrsClaimsUrl)
            .willReturn(
              aResponse.withStatus(400).withBody("bad request exception")
            ))

        when(mockTimerContext.stop())
          .thenReturn(123L)
        when(mockMetrics.startTimer(APITypes.JrsClaimAPI))
          .thenReturn(mockTimerContext)

        jrsConnector.getJrsClaims(nino).futureValue mustBe None

        verify(mockMetrics, times(1)).startTimer(APITypes.JrsClaimAPI)
        verify(mockTimerContext, times(1)).stop()
        verify(mockMetrics, times(1)).incrementFailedCounter(APITypes.JrsClaimAPI)
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
