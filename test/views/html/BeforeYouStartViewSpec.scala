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

package views.html

import controllers.routes
import org.jsoup.Jsoup
import play.api.test.Helpers.{contentAsString, *}
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BeforeYouStartViewSpec extends TaiViewSpec {

  private val template = inject[BeforeYouStart]

  private def render(journeyType: String): Html =
    template(journeyType)(implicitly, messages, authedUser)

  override def view: Html = render("employment")

  "BeforeYouStartView (employment journey)" must {

    "render the correct title and headings" in {
      val doc = Jsoup.parse(contentAsString(render("employment")))
      doc.title()                            must include(messages("beforeYouStart.title"))
      doc.select("h2.hmrc-caption-l").text() must include(messages("add.missing.employment"))
      doc.select("h1.govuk-heading-l").text() mustBe messages("beforeYouStart.title")
    }

    "show the intro paragraphs" in {
      val doc = Jsoup.parse(contentAsString(render("employment")))
      doc.text() must include(messages("beforeYouStart.intro"))
      doc.text() must include(messages("beforeYouStart.list.intro"))
    }

    "display the employment bullet list items" in {
      val doc  = Jsoup.parse(contentAsString(render("employment")))
      val text = doc.select("ul.govuk-list--bullet").text()
      text must include(messages("beforeYouStart.list.employerName"))
      text must include(messages("beforeYouStart.list.startDate"))
      text must include(messages("beforeYouStart.list.payrollNumber"))
      text must include(messages("beforeYouStart.list.payeReference"))
      text must include(messages("beforeYouStart.list.phoneNumber"))
    }

    "have a back link to the tax account summary" in {
      val doc  = Jsoup.parse(contentAsString(render("employment")))
      val back = doc.select("a[class=govuk-back-link]")
      back.attr("href") mustBe routes.TaxAccountSummaryController.onPageLoad().url
    }

    "have a continue button that goes to add employment name" in {
      val doc  = Jsoup.parse(contentAsString(render("employment")))
      val form = doc.select("form")
      form.attr("method").toUpperCase mustBe "GET"
      form.attr("action") mustBe controllers.employments.routes.AddEmploymentController.addEmploymentName().url
      doc.select("#continueButton").text() mustBe messages("tai.continue")
    }
  }

  "BeforeYouStartView (pension journey)" must {

    "render the correct title and headings" in {
      val doc = Jsoup.parse(contentAsString(render("pension")))
      doc.title()                            must include(messages("beforeYouStart.title"))
      doc.select("h2.hmrc-caption-l").text() must include(messages("add.missing.pension"))
      doc.select("h1.govuk-heading-l").text() mustBe messages("beforeYouStart.title")
    }

    "show the intro paragraphs" in {
      val doc = Jsoup.parse(contentAsString(render("pension")))
      doc.text() must include(messages("beforeYouStart.intro"))
      doc.text() must include(messages("beforeYouStart.list.intro"))
    }

    "display the pension bullet list items" in {
      val doc  = Jsoup.parse(contentAsString(render("pension")))
      val text = doc.select("ul.govuk-list--bullet").text()
      text must include(messages("beforeYouStart.pension.list.providerName"))
      text must include(messages("beforeYouStart.pension.list.firstPayment"))
      text must include(messages("beforeYouStart.pension.list.pensionNumber"))
      text must include(messages("beforeYouStart.list.payeReference"))
      text must include(messages("beforeYouStart.list.phoneNumber"))
    }

    "have a back link to the tax account summary" in {
      val doc  = Jsoup.parse(contentAsString(render("pension")))
      val back = doc.select("a[class=govuk-back-link]")
      back.attr("href") mustBe routes.TaxAccountSummaryController.onPageLoad().url
    }

    "have a continue button that goes to add pension provider name" in {
      val doc  = Jsoup.parse(contentAsString(render("pension")))
      val form = doc.select("form")
      form.attr("method").toUpperCase mustBe "GET"
      form.attr("action") mustBe controllers.pensions.routes.AddPensionProviderController.addPensionProviderName().url
      doc.select("#continueButton").text() mustBe messages("tai.continue")
    }
  }
}
