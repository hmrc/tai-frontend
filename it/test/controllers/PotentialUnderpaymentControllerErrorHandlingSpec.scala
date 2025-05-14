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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys
import utils.{FileHelper, IntegrationSpec}

class PotentialUnderpaymentControllerErrorHandlingSpec extends IntegrationSpec {

  override def fakeApplication() = GuiceApplicationBuilder()
    .configure(
      "auditing.enabled"                           -> "false",
      "microservice.services.auth.port"            -> server.port(),
      "microservice.services.pertax.port"          -> server.port(),
      "microservice.services.tai.port"             -> server.port(),
      "microservice.services.citizen-details.port" -> server.port()
    )
    .build()

  //    .post(url"$pertaxUrl/pertax/authorise")

  "/check-income-tax/income-summary" must {

    val url = "/check-income-tax/underpayment-estimate"

    val partialsUrl = s"/engagement-platform-partials/partials/%5B%22HMRC_Fixed_1%22%2C%22HMRC_Anchored_1%22%5D"

    val taxComponentsUrl = s"/tai/$generatedNino/tax-account/$taxYear/tax-components"

    val taxSummaryUrl = s"/tai/$generatedNino/tax-account/$taxYear/summary"

    val personUrl = s"/citizen-details/$generatedNino/designatory-details"

    "return an OK response" in {

      server.stubFor(
        get(urlEqualTo("/template/mustache"))
          .willReturn(aResponse().withStatus(200).withBody(""))
      )

      server.stubFor(
        get(urlEqualTo(partialsUrl))
          .willReturn(ok(FileHelper.loadFile("./it/resources/taxSummary.json")))
      )

      server.stubFor(
        get(urlEqualTo(taxComponentsUrl))
          .willReturn(ok(FileHelper.loadFile("./it/resources/taxComponent.json")))
      )

      server.stubFor(
        get(urlEqualTo(taxSummaryUrl))
          .willReturn(ok(FileHelper.loadFile("./it/resources/taxSummary.json")))
      )

      server.stubFor(
        get(urlEqualTo(personUrl))
          .willReturn(ok(FileHelper.loadFile("./it/resources/personDetails.json")))
      )

      val request =
        FakeRequest(GET, url).withHeaders("referer" -> REFERER).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(app, request)

      result.map(status) mustBe Some(OK)
    }

    List(
      BAD_REQUEST,
      NOT_FOUND,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpStatus =>
      s"return an Internal Server Error with 'problem with the service' message when $httpStatus is thrown" in {

        server.stubFor(
          get(urlEqualTo("/template/mustache"))
            .willReturn(aResponse().withStatus(200).withBody(""))
        )

        server.stubFor(
          get(urlEqualTo(partialsUrl))
            .willReturn(aResponse().withStatus(httpStatus))
        )

        server.stubFor(
          get(urlEqualTo(taxComponentsUrl))
            .willReturn(aResponse().withStatus(httpStatus))
        )

        server.stubFor(
          get(urlEqualTo(taxSummaryUrl))
            .willReturn(aResponse().withStatus(httpStatus))
        )

        server.stubFor(
          get(urlEqualTo(personUrl))
            .willReturn(aResponse().withStatus(httpStatus))
        )

        val request =
          FakeRequest(GET, url).withHeaders("referer" -> REFERER).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(app, request).get

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Sorry, there is a problem with the service")
      }
    }
  }
}
