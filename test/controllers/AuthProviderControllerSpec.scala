/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Await
import scala.concurrent.duration._

class AuthProviderControllerSpec extends PlaySpec with FakeTaiPlayApplication {

  "AuthProviderController" should {

    "return a redirect url" when {

      "verifyEntryPoint is called" in {

        val SUT = createSUT

        val result = SUT.verifyEntryPoint(FakeRequest())

        Await.result(result, 5 seconds)

        status(result) mustBe SEE_OTHER

        redirectLocation(result).getOrElse("") mustBe routes.TaxAccountSummaryController.onPageLoad().url
      }
    }

    "return a redirect url" when {

      "governmentGatewayEntryPoint is called" in {

        val SUT = createSUT

        val result = SUT.governmentGatewayEntryPoint(FakeRequest())

        Await.result(result, 5 seconds)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
  }

  def createSUT = new AuthProviderControllerTest

  class AuthProviderControllerTest extends AuthProviderController {

  }
}
