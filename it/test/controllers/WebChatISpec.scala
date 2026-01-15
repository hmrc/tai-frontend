/*
 * Copyright 2025 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, notFound, ok, urlEqualTo}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.Html
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.webchat.client.WebChatClient
import utils.{FileHelper, IntegrationSpec}

import java.time.LocalDate

class WebChatISpec extends IntegrationSpec {

  private val fandfDelegationUrl      = s"/delegation/get"
  private val whatYouWantToDoUrl      = "/check-income-tax/what-do-you-want-to-do"
  private val underpaymentEstimateUrl = "/check-income-tax/underpayment-estimate"
  private val personUrl               = s"/citizen-details/$generatedNino/designatory-details"
  private val partialsUrl             = s"/engagement-platform-partials/partials/%5B%22HMRC_Fixed_1%22%2C%22HMRC_Anchored_1%22%5D"
  private val taxComponentsUrl        = s"/tai/$generatedNino/tax-account/$taxYear/tax-components"
  private val taxSummaryUrl           = s"/tai/$generatedNino/tax-account/$taxYear/summary"
  private val startTaxYear            = TaxYear().start.getYear

  private val mockWebChatClient = mock[WebChatClient]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "auditing.enabled"                                              -> "false",
      "microservice.services.auth.port"                               -> server.port(),
      "microservice.services.tai.port"                                -> server.port(),
      "microservice.services.citizen-details.port"                    -> server.port(),
      "microservice.services.pertax.port"                             -> server.port(),
      "microservice.services.fandf.port"                              -> server.port(),
      "sca-wrapper.services.single-customer-account-wrapper-data.url" -> s"http://localhost:${server.port()}"
    )
    .overrides(
      bind[WebChatClient].toInstance(mockWebChatClient)
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockWebChatClient.loadRequiredElements()(any())).thenReturn(Some(Html("loadRequiredElements")))
    when(mockWebChatClient.loadWebChatContainer(any())(any()))
      .thenReturn(Some(Html("loadWebChatContainer")))
    server.stubFor(
      get(urlEqualTo(fandfDelegationUrl))
        .willReturn(notFound())
    )
  }

  "What do you want to do page" must {
    "show the webchat always" in {
      server.stubFor(
        get(urlEqualTo(personUrl))
          .willReturn(ok(FileHelper.loadFile("./it/resources/personDetails.json")))
      )

      server.stubFor(
        get(urlEqualTo("/engagement-platform-partials/partials/%5B%22HMRC_Fixed_1%22%2C%22HMRC_Anchored_1%22%5D"))
          .willReturn(ok("""
              |{"HMRCEMBEDDEDCHATSKIN":"HMRCEMBEDDEDCHATSKIN",
              |"HMRC_Fixed_1":"HMRC_Fixed_1",
              |"HMRC_Anchored_1":"HMRC_Anchored_1",
              |"HMRCPOPUPCHATSKIN":"HMRCPOPUPCHATSKIN"
              |}""".stripMargin))
      )

      val emp1        = Employment(
        "employer1",
        Live,
        None,
        Some(LocalDate.of(2016, 6, 9)),
        None,
        "taxNumber",
        "payeNumber",
        1,
        None,
        hasPayrolledBenefit = false,
        receivingOccupationalPension = false,
        EmploymentIncome
      )
      val employments = Json.obj("data" -> Json.obj("employments" -> Seq(emp1)))
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/employments/years/$startTaxYear"))
          .willReturn(ok(Json.toJson(employments).toString))
      )
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/employments-only/years/$startTaxYear"))
          .willReturn(ok(Json.toJson(employments).toString))
      )

      val taxAccountSummary = Json.obj("data" -> Json.toJson(TaxAccountSummary(0, 0, 0, 0, 0)))
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/${startTaxYear + 1}/summary"))
          .willReturn(ok(Json.toJson(taxAccountSummary).toString))
      )

      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/${startTaxYear + 1}/summary"))
          .willReturn(ok(Json.toJson(taxAccountSummary).toString))
      )

      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/tax-code-change/exists"))
          .willReturn(ok("false"))
      )

      val request = FakeRequest(GET, whatYouWantToDoUrl).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(app, request).get
      contentAsString(result) must include("loadRequiredElements")
      contentAsString(result) must include("loadWebChatContainer")
    }
  }

  "Under payment estimate page" must {
    "show the webchat always" in {
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
      server.stubFor(
        get(urlEqualTo(fandfDelegationUrl))
          .willReturn(notFound())
      )

      val request =
        FakeRequest(GET, underpaymentEstimateUrl)
          .withHeaders("referer" -> REFERER)
          .withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(app, request).get
      contentAsString(result) must include("loadRequiredElements")
      contentAsString(result) must include("loadWebChatContainer")
    }
  }
}
