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

import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.AddEmploymentFirstPayForm
import uk.gov.hmrc.tai.util.constants.{AddEmploymentFirstPayChoiceConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class AddEmploymentFirstPayFormViewSpec extends TaiViewSpec {

  "Add first pay form page" must {
    behave like pageWithTitle(messages("tai.addEmployment.employmentFirstPay.title", employerName))
    behave like pageWithCombinedHeaderNewTemplateNew(
      messages("add.missing.employment"),
      messages("tai.addEmployment.employmentFirstPay.title", employerName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like haveBackLinkWithUrl(controllers.employments.routes.AddEmploymentController.addEmploymentStartDate().url)
    behave like pageWithContinueButtonForm("/check-income-tax/add-employment/employment-first-pay")
    behave like pageWithYesNoRadioButton(
      AddEmploymentFirstPayChoiceConstants.FirstPayChoice,
      AddEmploymentFirstPayChoiceConstants.FirstPayChoice + "-2"
    )
    behave like pageWithCancelLink(controllers.employments.routes.AddEmploymentController.cancel())

    "have an error message with the form inputs" when {
      "no first number choice is selected" in {
        val noPayrollNumberChooseError           = messages("tai.error.chooseOneOption")
        val expectedErrorMessage                 = messages("tai.error.message") + " " + messages("tai.error.chooseOneOption")
        val formWithErrors: Form[Option[String]] =
          AddEmploymentFirstPayForm.form
            .withError(AddEmploymentFirstPayChoiceConstants.FirstPayChoice, noPayrollNumberChooseError)
        def view: Html                           = template(formWithErrors, employerName)

        val errorMessage = doc(view).select(".govuk-error-message").text
        errorMessage mustBe expectedErrorMessage
      }
    }
  }

  private val employmentFirstPayForm: Form[Option[String]] = AddEmploymentFirstPayForm.form.bind(
    Map(
      AddEmploymentFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.YesValue
    )
  )
  private val template                                     = inject[AddEmploymentFirstPayFormView]

  override def view: Html = template(employmentFirstPayForm, employerName)
}
