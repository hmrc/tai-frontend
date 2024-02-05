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

import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, urlEqualTo, urlMatching}
import play.api.cache.AsyncCacheApi
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, status, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.Week1Month1BasisOfOperation
import uk.gov.hmrc.tai.util.TaxYearRangeUtil.formatDate
import utils.JsonGenerator.{taxCodeChangeJson, taxCodeIncomesJson}
import utils.{FakeAsyncCacheApi, IntegrationSpec}

import scala.util.Random

class WhatDoYouWantToDoControllerSpec extends IntegrationSpec {

  lazy val fakeAsyncCacheApi = new FakeAsyncCacheApi()

  val url = "/check-income-tax/what-do-you-want-to-do"
  private val startTaxYear = TaxYear().start.getYear

  "What do you want to do page" must {
    "show the webchat" when {
      "it is enabled" in {
        lazy val app = new GuiceApplicationBuilder()
          .configure(
            "auditing.enabled"                                                -> "false",
            "microservice.services.auth.port"                                 -> server.port(),
            "microservice.services.tai.port"                                  -> server.port(),
            "microservice.services.digital-engagement-platform-partials.port" -> server.port(),
            "feature.web-chat.enabled"                                        -> true
          )
          .overrides(bind[AsyncCacheApi].toInstance(fakeAsyncCacheApi))
          .build()

        val person = Person(
          generatedNino,
          "Firstname",
          "Surname",
          isDeceased = false,
          manualCorrespondenceInd = false,
          Address("", "", "", "", "")
        )
        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/person"))
            .willReturn(ok(Json.obj("data" -> Json.toJson(person)).toString))
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

        val employments = Json.obj("data" -> Json.obj("employments" -> Seq.empty[JsValue]))
        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/employments/years/$startTaxYear"))
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

        val request =
          FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(app, request).get
        contentAsString(result) must include("HMRC_Anchored_1")
      }
    }
  }

  "not show the webchat" when {
    "it is disabled" in {
      lazy val app = new GuiceApplicationBuilder()
        .configure(
          "auditing.enabled"                                                -> "false",
          "microservice.services.auth.port"                                 -> server.port(),
          "microservice.services.tai.port"                                  -> server.port(),
          "microservice.services.digital-engagement-platform-partials.port" -> server.port(),
          "feature.web-chat.enabled"                                        -> false
        )
        .overrides(bind[AsyncCacheApi].toInstance(fakeAsyncCacheApi))
        .build()

      val person = Person(
        generatedNino,
        "Firstname",
        "Surname",
        isDeceased = false,
        manualCorrespondenceInd = false,
        Address("", "", "", "", "")
      )
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/person"))
          .willReturn(ok(Json.obj("data" -> Json.toJson(person)).toString))
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

      val employments = Json.obj("data" -> Json.obj("employments" -> Seq.empty[JsValue]))
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/employments/years/$startTaxYear"))
          .willReturn(ok(Json.toJson(employments).toString))
      )

      val taxAccountSummary = Json.obj("data" -> Json.toJson(TaxAccountSummary(0, 0, 0, 0, 0)))
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/$startTaxYear/summary"))
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

      val request =
        FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(app, request).get
      contentAsString(result) mustNot include("HMRC_Anchored_1")
    }
  }
  "show the WhatDoYouWantToDo page" should {
    lazy val app = new GuiceApplicationBuilder()
      .configure(
        "auditing.enabled"                  -> "false",
        "microservice.services.auth.port"   -> server.port(),
        "microservice.services.pertax.port" -> server.port(),
        "microservice.services.tai.port"    -> server.port(),
        "feature.web-chat.enabled"          -> false
      )
      .overrides(bind[AsyncCacheApi].toInstance(fakeAsyncCacheApi))
      .build()

    val person = Person(
      generatedNino,
      "Firstname",
      "Surname",
      isDeceased = false,
      manualCorrespondenceInd = false,
      Address("", "", "", "", "")
    )
    server.stubFor(
      get(urlEqualTo(s"/tai/$generatedNino/person"))
        .willReturn(ok(Json.obj("data" -> Json.toJson(person)).toString))
    )

    val employments = Json.obj("data" -> Json.obj("employments" -> Seq.empty[JsValue]))
    server.stubFor(
      get(urlEqualTo(s"/tai/$generatedNino/employments/years/$startTaxYear"))
        .willReturn(ok(Json.toJson(employments).toString))
    )

    "not see the tax code banner" when {
      "the user has no tax code mismatch and no tax code change" in {
        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/person"))
            .willReturn(ok(Json.obj("data" -> Json.toJson(person)).toString))
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/employments/years/$startTaxYear"))
            .willReturn(ok(Json.toJson(employments).toString))
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/tax-code-change/exists"))
            .willReturn(ok("false"))
        )

        val request =
          FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(app, request).get
        contentAsString(result) mustNot include("We changed your tax code on")
      }

      "the user has a tax code mismatch and no reported tax code change" in {
        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/$startTaxYear/income/tax-code-incomes"))
            .willReturn(
              ok(taxCodeIncomesJson)
            )
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/person"))
            .willReturn(ok(Json.obj("data" -> Json.toJson(person)).toString))
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/employments/years/$startTaxYear"))
            .willReturn(ok(Json.toJson(employments).toString))
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/tax-code-change/exists"))
            .willReturn(ok("false"))
        )

        val request =
          FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(app, request).get
        contentAsString(result) mustNot include("We changed your tax code on")
      }

    }
    "see the tax code change banner with the latest tax code change date" when {

      val startYear = 2023
      val numberOfYears = Random.between(2, 10)

      def taxCodeRecord(year: Int) = TaxCodeRecord(
        s"${year}X",
        TaxYear.apply(year).start,
        TaxYear.apply(year).end,
        Week1Month1BasisOfOperation,
        s"employer$year",
        pensionIndicator = false,
        None,
        primary = true
      )

      lazy val taxCodeChange: TaxCodeChange = {
        val previousYears = (startYear - numberOfYears until startYear).map(taxCodeRecord).toList
        val currentYears = (startYear - numberOfYears to startYear).map(taxCodeRecord).toList
        TaxCodeChange(previousYears, currentYears)
      }

      "the user has a tax code change and mismatch with more than 1 confirmed tax code" in {
        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/person"))
            .willReturn(ok(Json.obj("data" -> Json.toJson(person)).toString))
        )

        server.stubFor(
          get(urlMatching(s"/tai/$generatedNino/employments/years/.*"))
            .willReturn(ok(Json.toJson(employments).toString))
        )

        val taxAccountSummary = Json.obj("data" -> Json.toJson(TaxAccountSummary(0, 0, 0, 0, 0)))
        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/$startYear/summary"))
            .willReturn(ok(Json.toJson(taxAccountSummary).toString))
        )
        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/${startYear + 1}/summary"))
            .willReturn(ok(Json.toJson(taxAccountSummary).toString))
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/tax-code-change/exists"))
            .willReturn(ok("true"))
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/tax-code-change"))
            .willReturn(
              ok(taxCodeChangeJson(taxCodeChange))
            )
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/$startYear/income/tax-code-incomes"))
            .willReturn(
              ok(taxCodeIncomesJson)
            )
        )

        val request =
          FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(app, request).get
        status(result) mustBe OK
        contentAsString(result) must include("We changed your tax code on")
        contentAsString(result).replace("\u00A0", " ") must include(formatDate(taxCodeRecord(startYear).startDate))
      }
    }
  }
}
