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

package views.html.incomes

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class EstimatedPayLandingPageViewSpec extends TaiViewSpec {

  val empId                           = 1
  private val estimatedPayLandingPage = inject[EstimatedPayLandingPageView]

  "Estimated Pay Landing Page" should {
    behave like pageWithBackLink()
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url))
  }

  "have an accessible heading" in {
    doc(view) must haveHeadingWithText(messages("tai.incomes.landing.Heading", employerName))
  }

  "contain the correct content when income is from employment" in {
    doc(view).getElementsByTag("p").text              must include(messages("tai.incomes.landing.intro"))
    doc(view)                                         must haveLinkWithText(messages("tai.incomes.landing.employment.ended.link", employerName))
    doc(view)                                         must haveLinkWithUrlWithID(
      "updateEmployer",
      controllers.employments.routes.EndEmploymentController.onPageLoad(empId).url
    )
    doc(view).getElementsByClass("govuk-button").text must include(
      messages("tai.income.details.updateTaxableIncome.update")
    )
  }

  "contain the correct content when income is from pension" in {
    val testView: Html = estimatedPayLandingPage(employerName, empId, isPension = true, appConfig)
    doc(testView).getElementsByTag("p").text              must include(messages("tai.incomes.landing.intro"))
    doc(testView)                                         must haveLinkWithText(messages("tai.incomes.landing.pension.ended.link"))
    doc(testView)                                         must haveLinkWithUrlWithID("updatePension", appConfig.incomeFromEmploymentPensionLinkUrl)
    doc(testView).getElementsByClass("govuk-button").text must include(
      messages("tai.income.details.updateTaxableIncome.update")
    )
  }

  override def view: Html = estimatedPayLandingPage(employerName, empId, isPension = false, appConfig)
}
