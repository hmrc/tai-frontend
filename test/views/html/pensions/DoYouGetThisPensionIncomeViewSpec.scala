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

package views.html.pensions

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.pensions.UpdateRemovePensionForm
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.pensions.PensionProviderViewModel
import views.html.pensions.update.DoYouGetThisPensionIncomeView

class DoYouGetThisPensionIncomeViewSpec extends TaiViewSpec with FormValuesConstants {

  private val doYouGetThisPensionIncome = inject[DoYouGetThisPensionIncomeView]

  "IncorrectPension page" must {
    behave like pageWithTitle(messages("tai.updatePension.decision.pagetitle"))
    behave like pageWithBackLink
    behave like pageWithCombinedHeaderNewTemplate(
      messages("tai.updatePension.preHeading"),
      messages("tai.updatePension.decision.heading", model.pensionName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )

    behave like pageWithYesNoRadioButton(
      UpdateRemovePensionForm.IncorrectPensionDecision,
      UpdateRemovePensionForm.IncorrectPensionDecision + "-2",
      messages("tai.label.yes"),
      messages("tai.label.no")
    )

    behave like pageWithContinueButtonFormNew("/check-income-tax/incorrect-pension/decision")
    behave like pageWithCancelLink(controllers.pensions.routes.UpdatePensionProviderController.cancel(model.id))

    "show error" when {
      "form contains error" in {
        val pensionUpdateRemoveFormWithError =
          UpdateRemovePensionForm.form.bind(Map(UpdateRemovePensionForm.IncorrectPensionDecision -> ""))
        val viewWithError: Html = doYouGetThisPensionIncome(model, pensionUpdateRemoveFormWithError)

        val errorDoc = doc(viewWithError)

        errorDoc must haveElementAtPathWithText(
          ".govuk-error-message",
          Messages("tai.error.message") + " " + Messages("tai.error.chooseOneOption"))
        errorDoc must haveElementAtPathWithClass("form div", "govuk-form-group--error")
      }
    }
  }

  private lazy val pensionUpdateRemoveForm =
    UpdateRemovePensionForm.form.bind(Map(UpdateRemovePensionForm.IncorrectPensionDecision -> YesValue))
  private lazy val model = PensionProviderViewModel(1, "Test Pension")
  override def view: Html = doYouGetThisPensionIncome(model, pensionUpdateRemoveForm)
}
