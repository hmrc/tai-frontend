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

package views.html.employments

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.AddEmploymentPayrollNumberForm
import uk.gov.hmrc.tai.util.constants.{AddEmploymentPayrollNumberConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.PayrollNumberViewModel

class AddEmploymentPayrollNumberFormViewSpec extends TaiViewSpec {

  private val add_employment_payroll_number_form = inject[AddEmploymentPayrollNumberFormView]

  override def view: Html = add_employment_payroll_number_form(employmentPayrollForm, payrollNumberViewModel)

  "Add payroll number form page" must {
    behave like pageWithTitle(messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
    behave like pageWithCombinedHeaderNewTemplate(
      messages("add.missing.employment"),
      messages("tai.addEmployment.employmentPayrollNumber.title", employerName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithBackLinkNew
    behave like pageWithContinueButtonFormNew("/check-income-tax/add-employment/employment-payroll-number")
    behave like pageWithYesNoRadioButton(
      AddEmploymentPayrollNumberConstants.PayrollNumberChoice,
      AddEmploymentPayrollNumberConstants.PayrollNumberChoice + "-2")
    behave like pageWithCancelLink(controllers.employments.routes.AddEmploymentController.cancel)

    "have gone back to firstPayChoice page" in {
      val payrollNumberViewModel = PayrollNumberViewModel(
        employerName,
        true,
        controllers.employments.routes.AddEmploymentController.addEmploymentStartDate.url)
      def view: Html = add_employment_payroll_number_form(employmentPayrollForm, payrollNumberViewModel)
      def doc: Document = Jsoup.parse(view.toString())
      doc must haveBackLinkNew
    }

    "have an input field for payroll number" in {
      doc.getElementById("payrollNumberEntry") must not be null
    }

    "have an error message with the form inputs" when {
      "no payroll number choice is selected" in {
        val noPayrollNumberChooseError = messages("tai.addEmployment.employmentPayrollNumber.error.selectOption")
        val expectedErrorMessage = messages("tai.error.message") + " " + messages(
          "tai.addEmployment.employmentPayrollNumber.error.selectOption")
        val formWithErrors: Form[AddEmploymentPayrollNumberForm] = AddEmploymentPayrollNumberForm.form
          .withError(AddEmploymentPayrollNumberConstants.PayrollNumberChoice, noPayrollNumberChooseError)
        val view = add_employment_payroll_number_form(formWithErrors, payrollNumberViewModel)

        val errorMessage = doc(view).select(".govuk-error-message").text
        errorMessage mustBe expectedErrorMessage
      }

      "no payroll number is provided" in {
        val noPayrollNumberChooseError = messages("tai.addEmployment.employmentPayrollNumber.error.blank")
        val expectedErrorMessage = messages("tai.error.message") + " " + messages(
          "tai.addEmployment.employmentPayrollNumber.error.blank")
        val formWithErrors: Form[AddEmploymentPayrollNumberForm] = AddEmploymentPayrollNumberForm.form
          .withError(AddEmploymentPayrollNumberConstants.PayrollNumberEntry, noPayrollNumberChooseError)
        val view = add_employment_payroll_number_form(formWithErrors, payrollNumberViewModel)

        val errorMessage = doc(view).select(".govuk-error-message").text
        errorMessage mustBe expectedErrorMessage
      }
    }
  }

  private val employmentPayrollForm: Form[AddEmploymentPayrollNumberForm] = AddEmploymentPayrollNumberForm.form.bind(
    Map(
      AddEmploymentPayrollNumberConstants.PayrollNumberChoice -> FormValuesConstants.NoValue
    ))

  private lazy val employerName = "Employer"
  private lazy val payrollNumberViewModel = PayrollNumberViewModel(
    employerName,
    firstPayChoice = false,
    controllers.employments.routes.AddEmploymentController.addEmploymentStartDate.url
  )
}
