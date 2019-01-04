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

package views.html.employments


import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.EmploymentNameForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec


class AddEmploymentNameFormSpec extends TaiViewSpec {
  "Add employment name form page" should {
    behave like pageWithTitle(messages("tai.addEmployment.addNameForm.title"))
    behave like pageWithCombinedHeader(
      messages("add.missing.employment"),
      messages("tai.addEmployment.addNameForm.title"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/check-income-tax/add-employment/employment-name")
    behave like pageWithCancelLink(controllers.routes.TaxAccountSummaryController.onPageLoad())

    "have an error box at the top of the page with a link to the error field" when {
      "a form with errors is passed into the view" in {
        val view: Html = views.html.employments.add_employment_name_form(formWithErrors)

        doc(view) must haveErrorLinkWithText(Messages("tai.employmentName.error.blank"))
      }
    }
  }

  private lazy val formWithErrors: Form[String] = EmploymentNameForm.form.bind(Map(
    "employmentName" -> ""
  ))

  private lazy val employmentNameForm: Form[String] = EmploymentNameForm.form.bind(Map(
    "employmentName" -> "the company"
  ))


  override def view: Html = views.html.employments.add_employment_name_form(employmentNameForm)
}
