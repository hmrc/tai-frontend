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

import cats.data.EitherT
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.{BaseSpec, WireMockHelper}

import scala.concurrent.Future

class IvUpliftFrontendConnectorSpec extends BaseSpec with WireMockHelper {

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("microservice.services.identity-verification-frontend.port" -> server.port())
    .build()

  lazy val ivFrontendConnector: IvUpliftFrontendConnector = inject[IvUpliftFrontendConnector]

  val metricId = "get-iv-journey-status"

  override implicit val hc: HeaderCarrier = HeaderCarrier()

  def stubGet(url: String, responseStatus: Int, responseBody: Option[String]): StubMapping = server.stubFor {
    val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, "application/json")
    val response = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))

    WireMock.get(url).willReturn(response)
  }

  "Calling IvUpliftFrontend getIVJourneyStatus" must {

    "return an JsValue containing a journey status object when called with a journeyId" in {
      stubGet(
        "/mdtp/journey/journeyId/1234",
        OK,
        Some("""{"result": "InsufficientEvidence"}""")
      )

      val result: EitherT[Future, UpstreamErrorResponse, JsValue] = ivFrontendConnector
        .getIVJourneyStatus("1234")(hc, ec)

      result.value.futureValue mustBe Right(Json.parse("""{"result": "InsufficientEvidence"}"""))
    }

    List(
      INTERNAL_SERVER_ERROR,
      UNAUTHORIZED
    ).foreach { statusCode =>
      s"return Left when a $statusCode is retrieved" in {
        stubGet("/mdtp/journey/journeyId/1234", statusCode, None)

        val result: EitherT[Future, UpstreamErrorResponse, JsValue] = ivFrontendConnector
          .getIVJourneyStatus("1234")(hc, ec)

        result.value.futureValue mustBe a[Left[_, JsValue]]
      }
    }
  }
}
