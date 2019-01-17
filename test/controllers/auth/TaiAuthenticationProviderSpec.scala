/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.auth

import builders.RequestBuilder
import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

class TaiAuthenticationProviderSpec extends PlaySpec with FakeTaiPlayApplication {

  "TaiAuthenticationProvider" should {
    "call ggwAuthenticationProvider" when {
      "login redirect URL is required" in {
        val SUT = createSUT
        val expectedUrl = "http://localhost:4444/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A1111%2Fpersonal-account/do-uplift?redirectUrl=%2Fcheck-income-tax%2Fwhat-do-you-want-to-do&accountType=individual"
        val result = SUT.ggwAuthenticationProvider.redirectToLogin(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe 303
        redirectLocation(result).get mustBe expectedUrl
      }
    }

    "get runtime unused exception" when {
      "ggwAuthenticationProvider is called for continueURL" in {
        val SUT = createSUT
        val result = the[RuntimeException] thrownBy SUT.ggwAuthenticationProvider.continueURL
        result.getMessage mustBe "Unused"
      }

      "ggwAuthenticationProvider is called for loginURL" in {
        val SUT = createSUT
        val result = the[RuntimeException] thrownBy SUT.ggwAuthenticationProvider.loginURL
        result.getMessage mustBe "Unused"
      }
    }

    "call verifyAuthenticationProvider" when {
      "login redirect URL is required" in {
        val SUT = createSUT
        val expectedUrl = "http://localhost:9999/ida/login"
        val result = SUT.verifyAuthenticationProvider.redirectToLogin(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe 303
        redirectLocation(result).get mustBe expectedUrl
      }

      "verifyAuthenticationProvider is called for login" in {
        val SUT = createSUT
        val result = the[RuntimeException] thrownBy SUT.verifyAuthenticationProvider.login
        result.getMessage mustBe "Unused"
      }
    }

    "get runtime unused exception for login" in {
      val SUT = createSUT
      val result = the[RuntimeException] thrownBy SUT.login
      result.getMessage mustBe "Unused"
    }
  }

  def createSUT = TaiAuthenticationProvider
}
