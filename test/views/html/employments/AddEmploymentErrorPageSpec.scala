/*
 * Copyright 2021 HM Revenue & Customs
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

package views.html.employments

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import controllers.routes

class AddEmploymentErrorPageSpec extends TaiViewSpec {
  "Display error page when employment cannot be added" must {
    behave like pageWithTitle(messages("tai.addEmployment.employmentErrorPage.title"))
    behave like pageWithCombinedHeader(
      messages("add.missing.employment"),
      messages("tai.addEmployment.employmentErrorPage.title"))

    "have link" in {
      doc must haveLinkWithUrlWithID("returnToYourIncomeDetails", routes.TaxAccountSummaryController.onPageLoad().url)
    }

    "have paragraph" in {
      doc must haveParagraphWithText(messages("tai.addEmployment.employmentErrorPage.para1", "fake employer"))
      doc must haveParagraphWithText(messages("tai.addEmployment.employmentErrorPage.para2"))
    }

    behave like pageWithBackLink
  }
  private val template = inject[AddEmploymentErrorPageView]

  override def view: Html = template("fake employer")
}
