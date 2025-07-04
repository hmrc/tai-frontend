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

import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.tai.forms.income.incomeCalculator.BonusOvertimeAmountForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BonusPaymentAmountViewSpec extends TaiViewSpec {

  val employer: IncomeSource                                 = IncomeSource(id = 1, name = "Employer")
  val bonusPaymentsAmountForm: Form[BonusOvertimeAmountForm] = BonusOvertimeAmountForm.createForm
  private val bonusPaymentAmount                             = inject[BonusPaymentAmountView]

  override def view: Html = bonusPaymentAmount(bonusPaymentsAmountForm, employer)

  "Bonus payments amount view" should {
    behave like pageWithBackLink()
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeController.cancel(employer.id).url))
    behave like pageWithCombinedHeaderNewTemplateNew(
      messages("tai.bonusPaymentsAmount.preHeading", employer.name),
      messages("tai.bonusPaymentsAmount.title", TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited).replaceAll(
        "\u00A0",
        " "
      ),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )

    behave like pageWithTitle(
      messages("tai.bonusPaymentsAmount.title", TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited)
        .replaceAll("\u00A0", " ")
    )
    behave like pageWithContinueButtonFormNew("/check-income-tax/update-income/bonus-overtime-amount/" + employer.id)

    "contain a hint with text" in {
      doc must haveHintWithText("amount-hint", messages("tai.bonusPaymentsAmount.hint"))
    }

    "contain an input field with pound symbol appended" in {
      doc must haveElementAtPathWithId("input", "amount")
      doc must haveElementAtPathWithClass("div", "govuk-input__prefix")
    }

    "return no errors when a valid monetary amount is entered" in {

      val monetaryAmount        = "£10,000"
      val monetaryAmountRequest = Map("amount" -> monetaryAmount)
      val validatedForm         = bonusPaymentsAmountForm.bind(monetaryAmountRequest)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe BonusOvertimeAmountForm(Some(monetaryAmount))
    }

    "display an error" when {
      "the user continues without entering an amount" in {
        val emptySelectionErrorMessage =
          messages(
            "tai.bonusPaymentsAmount.error.form.mandatory",
            TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited.replaceAll("\u00A0", " ")
          )
        val invalidRequest             = Map("amount" -> "")
        val invalidatedForm            = bonusPaymentsAmountForm.bind(invalidRequest)

        val errorView: HtmlFormat.Appendable = bonusPaymentAmount(invalidatedForm, employer)
        doc(errorView) must haveErrorLinkWithTextNew(emptySelectionErrorMessage)
        doc(errorView) must haveClassWithText(
          messages("tai.error.message") + " " + messages(emptySelectionErrorMessage),
          "govuk-error-message"
        )
      }

      "the user enters an invalid monetary amount" in {
        val invalidAmountErrorMessage = messages("tai.bonusPaymentsAmount.error.form.input.invalid")
        val invalidRequest            = Map("amount" -> "£10,0")
        val invalidatedForm           = bonusPaymentsAmountForm.bind(invalidRequest)

        val errorView: HtmlFormat.Appendable = bonusPaymentAmount(invalidatedForm, employer)
        doc(errorView) must haveErrorLinkWithTextNew(invalidAmountErrorMessage)
        doc(errorView) must haveClassWithText(
          messages("tai.error.message") + " " + messages(invalidAmountErrorMessage),
          "govuk-error-message"
        )
      }
    }
  }
}
