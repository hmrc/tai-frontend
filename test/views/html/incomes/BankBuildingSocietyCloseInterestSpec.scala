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

package views.html.incomes

import uk.gov.hmrc.tai.forms.income.bbsi.BankAccountClosingInterestForm
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.tai.util.{BankAccountClosingInterestConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BankBuildingSocietyCloseInterestSpec extends TaiViewSpec with FormValuesConstants with BankAccountClosingInterestConstants {

  override def view: Html = views.html.incomes.bbsi.close.bank_building_society_closing_interest(id, bankAccountClosingInterestForm)

  "Add bank account closing interest page" must {
    behave like pageWithTitle(messages("tai.closeBankAccount.closingInterest.title"))
    behave like pageWithHeader(messages("tai.closeBankAccount.closingInterest.heading", TaxYearResolver.currentTaxYear.toString))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm(s"/check-income-tax/income/bank-building-society-savings/$id/close/interest")
    behave like pageWithYesNoRadioButton(BankAccountClosingInterestForm.ClosingInterestChoice+"-yes", BankAccountClosingInterestForm.ClosingInterestChoice+"-no")
    behave like pageWithCancelLink(controllers.income.bbsi.routes.BbsiController.accounts())

    "have an input field for payroll number" in {
      doc.getElementById(ClosingInterestEntry) must not be null
    }

    "have an error message with the form inputs" when {

      "no income choice is selected" in {

        val closingInterestChoiceError = messages("tai.closeBankAccount.closingInterest.error.selectOption")

        val formWithErrors: Form[BankAccountClosingInterestForm] = BankAccountClosingInterestForm.form.
          withError(BankAccountClosingInterestForm.ClosingInterestChoice, closingInterestChoiceError)

        def view: Html = views.html.incomes.bbsi.close.bank_building_society_closing_interest(id, formWithErrors)

        val errorMessage = doc(view).select(".error-message").text
        errorMessage mustBe closingInterestChoiceError
      }
    }

    "no closing interest is provided" in {

      val closingInterestEntryError = messages("tai.closeBankAccount.closingInterest.error.selectOption")

      val formWithErrors: Form[BankAccountClosingInterestForm] = BankAccountClosingInterestForm.form.
        withError(BankAccountClosingInterestForm.ClosingInterestEntry, closingInterestEntryError)

      def view: Html = views.html.incomes.bbsi.close.bank_building_society_closing_interest(0, formWithErrors)

      val errorMessage = doc(view).select(".error-notification").text
      errorMessage mustBe closingInterestEntryError
    }
  }

  private lazy val bankAccountClosingInterestForm: Form[BankAccountClosingInterestForm] = BankAccountClosingInterestForm.form.bind(Map(
    BankAccountClosingInterestForm.ClosingInterestChoice -> NoValue))

  private val id = 0
}
