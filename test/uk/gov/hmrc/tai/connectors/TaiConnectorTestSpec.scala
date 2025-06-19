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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, anyUrl, post}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.tai.model.{CalculatedPay, PayDetails}
import utils.{BaseSpec, WireMockHelper}

import scala.concurrent.Await
import scala.concurrent.duration._

class TaiConnectorTestSpec extends BaseSpec with WireMockHelper with ScalaFutures with IntegrationPatience {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.tai.port" -> server.port())
    .overrides(
      bind[FeatureFlagService].toInstance(mockFeatureFlagService)
    )
    .build()

  "TaiConnector" must {
    "return estimated pay" in {
      val expectedResponse = CalculatedPay(Some(23000), Some(16000), None, None)
      val taiConnector     = inject[TaiConnector]
      val payDetails       = PayDetails("monthly", Some(1000), Some(500), Some(4), Some(10000), None)
      implicit val hc      = HeaderCarrier()
      server.stubFor(
        post(anyUrl())
          .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(expectedResponse).toString()))
      )
      val response         = Await.result(taiConnector.calculateEstimatedPay(payDetails), 5.seconds)
      response mustBe expectedResponse
    }
  }
}
