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

package views.html.incomes.nextYear

import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.YesNoForm
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.{MonetaryUtil, TaxYearRangeUtil}
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{DuplicateSubmissionCYPlus1EmploymentViewModel, DuplicateSubmissionCYPlus1PensionViewModel}
import views.html.incomes.DuplicateSubmissionWarningView

class UpdateIncomeCYPlus1WarningViewSpec extends TaiViewSpec {
  val employmentName = "Employment Name"
  val empId = 1
  val duplicateSubmissionWarningForm: Form[YesNoForm] = DuplicateSubmissionWarningForm.createForm
  val choice: String = FormValuesConstants.YesNoChoice

  "duplicateSubmissionWarning" must {
    behave like pageWithTitle(messages("tai.incomes.warning.customGaTitle"))
    behave like pageWithBackLink()
    behave like pageWithCombinedHeaderNewTemplateNew(
      preHeaderText = messages("tai.incomes.warning.preHeading"),
      mainHeaderText = messages("tai.incomes.warning.cyPlus1.heading", employmentName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )

    behave like pageWithYesNoRadioButton(
      FormValuesConstants.YesNoChoice,
      s"${FormValuesConstants.YesNoChoice}-2",
      messages("tai.incomes.warning.employment.radio1", employmentName),
      messages("tai.incomes.warning.cyPlus1.radio2")
    )

    behave like pageWithContinueButtonForm(s"/check-income-tax/update-income/next-year/income/$empId/warning")
    behave like pageWithCancelLink(controllers.routes.IncomeTaxComparisonController.onPageLoad())

    "return no errors with valid 'yes' choice" in {
      val validYesChoice = Map(choice -> FormValuesConstants.YesValue)
      val validatedForm = duplicateSubmissionWarningForm.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe YesNoForm(Some(FormValuesConstants.YesValue))
    }

    "return no errors with valid 'no' choice" in {
      val validNoChoice = Map(choice -> FormValuesConstants.NoValue)
      val validatedForm = duplicateSubmissionWarningForm.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe YesNoForm(Some(FormValuesConstants.NoValue))
    }

    "display an error for invalid choice" in {
      val invalidChoice = Map(choice -> "")
      val invalidatedForm = duplicateSubmissionWarningForm.bind(invalidChoice)
      val emptySelectionErrorMessage = messages("tai.employment.warning.error")

      val warningTemplate = inject[DuplicateSubmissionWarningView]

      val errorView = warningTemplate(invalidatedForm, employmentViewModel, empId)
      doc(errorView) must haveErrorLinkWithTextNew(messages(emptySelectionErrorMessage))
      doc(errorView) must haveClassWithText(
        messages("tai.error.message") + " " + messages(emptySelectionErrorMessage),
        "govuk-error-message"
      )
    }

    "display the correct content when the income source is a pension" in {
      val pensionViewModel = DuplicateSubmissionCYPlus1PensionViewModel(employmentName, newAmount)
      val pensionView: Html =
        template(duplicateSubmissionWarningForm, pensionViewModel, empId)

      doc(pensionView) must haveParagraphWithText(
        messages(
          "tai.incomes.warning.cyPlus1.text1",
          MonetaryUtil.withPoundPrefix(newAmount),
          TaxYearRangeUtil.futureTaxYearRange(1)
        ).replaceU00A0
      )

      doc(pensionView) must haveInputLabelWithText(
        FormValuesConstants.YesNoChoice,
        messages("tai.incomes.warning.pension.radio1", employmentName)
      )
      doc(pensionView) must haveInputLabelWithText(
        s"${FormValuesConstants.YesNoChoice}-2",
        messages("tai.incomes.warning.cyPlus1.radio2")
      )
      doc(pensionView).getElementById(FormValuesConstants.YesNoChoice) must not be null
      doc(pensionView).getElementById(s"${FormValuesConstants.YesNoChoice}-2") must not be null
    }
  }

  val newAmount = 20000
  val employmentViewModel = DuplicateSubmissionCYPlus1EmploymentViewModel(employmentName, newAmount)
  private val template = inject[UpdateIncomeCYPlus1WarningView]

  override def view: Html =
    template(duplicateSubmissionWarningForm, employmentViewModel, empId)
}
