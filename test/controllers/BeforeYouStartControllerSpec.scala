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

import builders.RequestBuilder
import org.jsoup.Jsoup
import play.api.test.Helpers.*
import utils.BaseSpec
import views.html.BeforeYouStart

class BeforeYouStartControllerSpec extends BaseSpec {

  private val view = inject[BeforeYouStart]

  private def sut =
    new BeforeYouStartController(
      mockAuthJourney,
      mcc,
      view
    )

  "BeforeYouStartController.onPageLoad" must {

    "render the employment page" in {
      val result = sut.onPageLoad("employment")(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title()                            must include(messages("beforeYouStart.title"))
      doc.select("h2.hmrc-caption-l").text() must include(messages("add.missing.employment"))
      doc.select("h1.govuk-heading-l").text() mustBe messages("beforeYouStart.title")
    }

    "render the pension page" in {
      val result = sut.onPageLoad("pension")(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title()                            must include(messages("beforeYouStart.title"))
      doc.select("h2.hmrc-caption-l").text() must include(messages("add.missing.pension"))
      doc.select("h1.govuk-heading-l").text() mustBe messages("beforeYouStart.title")
    }

    "return NOT_FOUND for invalid journey type" in {
      val result = sut.onPageLoad("nope")(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe NOT_FOUND
    }
  }
}
