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

package views.html.employments

import controllers.routes
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.AddEmploymentFirstPayForm
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class AddEmploymentFirstPaySpec extends TaiViewSpec with FormValuesConstants {

  "Add first pay form page" must {
    behave like pageWithTitle(messages("tai.addEmployment.employmentFirstPay.title", employerName))
    behave like pageWithCombinedHeader(
      messages("add.missing.employment"),
      messages("tai.addEmployment.employmentFirstPay.title", employerName))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/check-income-tax/add-employment/employment-first-pay")
    behave like pageWithYesNoRadioButton(AddEmploymentFirstPayForm.FirstPayChoice+"-yes", AddEmploymentFirstPayForm.FirstPayChoice+"-no")
    behave like pageWithCancelLink(routes.TaxAccountSummaryController.onPageLoad())

    "have an error message with the form inputs" when {
      "no first number choice is selected" in {
        val noPayrollNumberChooseError = messages("tai.error.chooseOneOption")
        val formWithErrors: Form[Option[String]] = AddEmploymentFirstPayForm.form.
          withError(AddEmploymentFirstPayForm.FirstPayChoice, noPayrollNumberChooseError)
        def view: Html = views.html.employments.add_employment_first_pay_form(formWithErrors, employerName)

        val errorMessage = doc(view).select(".error-message").text
        errorMessage mustBe noPayrollNumberChooseError
      }
    }
  }

  private lazy val employerName = "Employer"

  private val employmentFirstPayForm: Form[Option[String]] = AddEmploymentFirstPayForm.form.bind(Map(
    AddEmploymentFirstPayForm.FirstPayChoice -> YesValue
  ))

  override def view: Html = views.html.employments.add_employment_first_pay_form(employmentFirstPayForm, employerName)
}
