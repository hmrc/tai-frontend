/*
 * Copyright 2022 HM Revenue & Customs
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

package views.html.pensions.updatePensions

import play.api.data.Form
import play.api.libs.json.Json
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.tai.forms.YesNoForm
import uk.gov.hmrc.tai.forms.pensions.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import views.html.pensions.DuplicateSubmissionWarningView

class DuplicateSubmissionWarningViewSpec extends TaiViewSpec with FormValuesConstants {
  val pensionName = "pension Name"
  val pensionId = 1
  val duplicateSubmissionWarningForm: Form[YesNoForm] = DuplicateSubmissionWarningForm.createForm
  val choice = YesNoForm.YesNoChoice
  private val duplicateSubmissionWarningView = inject[DuplicateSubmissionWarningView]

  "duplicateSubmissionWarning" must {
    behave like pageWithTitle(messages("tai.pension.warning.customGaTitle"))
    behave like pageWithBackLink
    behave like pageWithCombinedHeader(
      preHeaderText = messages("tai.pension.warning.preHeading"),
      mainHeaderText = messages("tai.pension.warning.heading", pensionName))

    behave like pageWithYesNoRadioButton(
      s"$YesNoChoice-yes",
      s"$YesNoChoice-no",
      messages("tai.pension.warning.radio1", pensionName),
      messages("tai.pension.warning.radio2"))

    behave like pageWithContinueButtonForm("/check-income-tax/incorrect-pension/warning")
    behave like pageWithCancelLink(controllers.routes.IncomeSourceSummaryController.onPageLoad(pensionId))

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
      val emptySelectionErrorMessage = messages("tai.pension.warning.error")

      val errorView: HtmlFormat.Appendable = duplicateSubmissionWarningView(invalidatedForm, pensionName, pensionId)
      doc(errorView) must haveErrorLinkWithText(messages(emptySelectionErrorMessage))
      doc(errorView) must haveClassWithText(
        messages("tai.error.message") + " " + messages(emptySelectionErrorMessage),
        "error-message")
    }
  }

  override def view: Html = duplicateSubmissionWarningView(duplicateSubmissionWarningForm, pensionName, pensionId)
}
