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

package controllers

import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

class UnauthorisedControllerSpec extends PlaySpec with FakeTaiPlayApplication {
  implicit val templateRenderer = MockTemplateRenderer
  implicit val partialRetriever = MockPartialRetriever

  val controller = new UnauthorisedController

  "onPageLoad" must {
    "return OK for a GET request" in {
      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe OK
    }

    "return the unauthorised error page" in {
      val result = controller.onPageLoad(fakeRequest)

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      val title = doc.select("title").text()
      title must include("Sorry, we are experiencing technical difficulties - 500")
    }
  }

  "login" must {
    "redirect to a login page" in {
      val result = controller.login(fakeRequest)
      val expectedUrl = "http://localhost:4444/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A1111%2Fpersonal-account/do-uplift?redirectUrl=%2Fcheck-income-tax%2Fwhat-do-you-want-to-do&accountType=individual"

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe expectedUrl
    }
  }
}
