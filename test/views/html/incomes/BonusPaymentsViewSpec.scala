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
import uk.gov.hmrc.tai.forms.YesNoForm
import uk.gov.hmrc.tai.forms.income.incomeCalculator.BonusPaymentsForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BonusPaymentsViewSpec extends TaiViewSpec {

  val employer = IncomeSource(id = 1, name = "Employer")

  val emptySelectionErrorMessage = messages(
    "tai.bonusPayments.error.form.incomes.radioButton.mandatory",
    TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited.replaceAll("\u00A0", " ")
  )
  val bonusPaymentsForm          = BonusPaymentsForm.createForm
  val choice                     = FormValuesConstants.YesNoChoice

  private val bonusPayments = inject[BonusPaymentsView]

  override def view: Html = bonusPayments(bonusPaymentsForm, employer, "backUrl")

  "Bonus payments view" should {
    behave like pageWithBackLinkWithUrl("backUrl")
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeController.cancel(employer.id).url))
    behave like pageWithCombinedHeaderNewTemplateNew(
      messages("tai.bonusPayments.preHeading", employer.name),
      messages(
        "tai.bonusPayments.title",
        TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited.replaceAll("\u00A0", " ")
      ),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithTitle(
      messages(
        "tai.bonusPayments.title",
        TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited.replaceAll("\u00A0", " ")
      )
    )
    behave like pageWithContinueButtonFormNew("/check-income-tax/update-income/bonus-payments/" + employer.id)

    "return no errors with valid 'yes' choice" in {
      val validYesChoice = Map(choice -> FormValuesConstants.YesValue)
      val validatedForm  = bonusPaymentsForm.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe YesNoForm(Some(FormValuesConstants.YesValue))
    }

    "return no errors with valid 'no' choice" in {
      val validNoChoice = Map(choice -> FormValuesConstants.NoValue)
      val validatedForm = bonusPaymentsForm.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe YesNoForm(Some(FormValuesConstants.NoValue))
    }

    "display an error for invalid choice" in {
      val invalidChoice   = Map(choice -> "")
      val invalidatedForm = bonusPaymentsForm.bind(invalidChoice)

      val errorView = bonusPayments(invalidatedForm, employer, "backUrl")
      doc(errorView) must haveErrorLinkWithTextNew(messages(emptySelectionErrorMessage))
      doc(errorView) must haveClassWithText(
        messages("tai.income.error.form.summary") + " " + messages(emptySelectionErrorMessage),
        "govuk-error-summary"
      )
    }
  }
}
