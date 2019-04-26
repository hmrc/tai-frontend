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
import play.api.libs.json.Json
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.YesNoForm
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.util.{MonetaryUtil, TaxYearRangeUtil}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{DuplicateSubmissionCYPlus1EmploymentViewModel, DuplicateSubmissionCYPlus1PensionViewModel, DuplicateSubmissionEmploymentViewModel, DuplicateSubmissionPensionViewModel}

class UpdateIncomeCYPlus1WarningSpec extends TaiViewSpec with FormValuesConstants {
  val employmentName = "Employment Name"
  val empId = 1
  val duplicateSubmissionWarningForm: Form[YesNoForm] = DuplicateSubmissionWarningForm.createForm
  val choice = YesNoForm.YesNoChoice

  "duplicateSubmissionWarning" must {
    behave like pageWithTitle(messages("tai.incomes.warning.customGaTitle"))
    behave like pageWithBackLink
    behave like pageWithCombinedHeader(
      preHeaderText = messages("tai.incomes.warning.preHeading"),
      mainHeaderText = messages("tai.incomes.warning.cyPlus1.heading", employmentName))

    behave like pageWithYesNoRadioButton(
      s"$YesNoChoice-yes",
      s"$YesNoChoice-no",
      messages("tai.incomes.warning.employment.radio1", employmentName),
      messages("tai.incomes.warning.cyPlus1.radio2"))

    behave like pageWithContinueButtonForm(s"/check-income-tax/update-income/next-year/income/$empId/warning")
    behave like pageWithCancelLink(controllers.routes.IncomeTaxComparisonController.onPageLoad())

    "return no errors with valid 'yes' choice" in {
      val validYesChoice = Json.obj(choice -> YesValue)
      val validatedForm = duplicateSubmissionWarningForm.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe YesNoForm(Some(YesValue))
    }

    "return no errors with valid 'no' choice" in {
      val validNoChoice = Json.obj(choice -> NoValue)
      val validatedForm = duplicateSubmissionWarningForm.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe YesNoForm(Some(NoValue))
    }

    "display an error for invalid choice" in {
      val invalidChoice = Json.obj(choice -> "")
      val invalidatedForm = duplicateSubmissionWarningForm.bind(invalidChoice)
      val emptySelectionErrorMessage = messages("tai.employment.warning.error")

      val errorView = views.html.incomes.duplicateSubmissionWarning(invalidatedForm,employmentViewModel,empId)
      doc(errorView) must haveErrorLinkWithText(messages(emptySelectionErrorMessage))
      doc(errorView) must haveClassWithText(messages(emptySelectionErrorMessage),"error-message")
    }

    "display the correct content when the income source is a pension" in {
      val pensionViewModel = DuplicateSubmissionCYPlus1PensionViewModel(employmentName, newAmount)
      val pensionView: Html = views.html.incomes.nextYear.updateIncomeCYPlus1Warning(duplicateSubmissionWarningForm, pensionViewModel, empId)

      doc(pensionView) must haveHeadingWithText(messages("tai.incomes.warning.cyPlus1.heading", employmentName))
      doc(pensionView) must haveParagraphWithText(messages("tai.incomes.warning.cyPlus1.text1", MonetaryUtil.withPoundPrefix(newAmount),
        TaxYearRangeUtil.futureTaxYearRangeHtmlNonBreak(1)))

      doc(pensionView) must haveInputLabelWithText(s"$YesNoChoice-yes", messages("tai.incomes.warning.pension.radio1", employmentName))
      doc(pensionView) must haveInputLabelWithText(s"$YesNoChoice-no", messages("tai.incomes.warning.cyPlus1.radio2"))
      doc(pensionView).getElementById(s"$YesNoChoice-yes") must not be null
      doc(pensionView).getElementById(s"$YesNoChoice-no") must not be null
    }
  }

  val newAmount = 20000
  val employmentViewModel = DuplicateSubmissionCYPlus1EmploymentViewModel(employmentName, newAmount)
  override def view: Html = views.html.incomes.nextYear.updateIncomeCYPlus1Warning(duplicateSubmissionWarningForm,employmentViewModel,empId)
}