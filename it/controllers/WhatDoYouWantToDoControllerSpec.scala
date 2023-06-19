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

import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, urlEqualTo}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.tai.model.domain.{Address, Person, TaxAccountSummary}
import utils.IntegrationSpec

import java.time.LocalDate

class WhatDoYouWantToDoControllerSpec extends IntegrationSpec {

  val url = "/check-income-tax/what-do-you-want-to-do"

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
          .build()

        val person = Person(generatedNino, "Firstname", "Surname", false, false, Address("", "", "", "", ""))
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
          get(urlEqualTo(s"/tai/$generatedNino/employments/years/${LocalDate.now().getYear}"))
            .willReturn(ok(Json.toJson(employments).toString))
        )

        val taxAccountSummary = Json.obj("data" -> Json.toJson(TaxAccountSummary(0, 0, 0, 0, 0)))
        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/${LocalDate.now().getYear}/summary"))
            .willReturn(ok(Json.toJson(taxAccountSummary).toString))
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$generatedNino/tax-account/${LocalDate.now().getYear + 1}/summary"))
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
        .build()

      val person = Person(generatedNino, "Firstname", "Surname", false, false, Address("", "", "", "", ""))
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
        get(urlEqualTo(s"/tai/$generatedNino/employments/years/${LocalDate.now().getYear}"))
          .willReturn(ok(Json.toJson(employments).toString))
      )

      val taxAccountSummary = Json.obj("data" -> Json.toJson(TaxAccountSummary(0, 0, 0, 0, 0)))
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/${LocalDate.now().getYear}/summary"))
          .willReturn(ok(Json.toJson(taxAccountSummary).toString))
      )

      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/${LocalDate.now().getYear + 1}/summary"))
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

}
