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

import uk.gov.hmrc.tai.forms.DateForm
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BankBuildingSocietyCloseDateSpec extends TaiViewSpec {

  private val bankName:String = "TestBank"
  private val globalErrorMessage: String = "day error message"
  private val closeBankAccountDateForm = DateForm(Nil, bankName)
  private val formWithErrors: Form[LocalDate] = closeBankAccountDateForm.form.withError("", globalErrorMessage)

  override def view: Html = views.html.incomes.bbsi.close.bank_building_society_close_date(closeBankAccountDateForm.form, bankName, 1)

  "Close bank account date form" should {

    behave like pageWithTitle(messages("tai.closeBankAccount.closeDateForm.title", bankName))
    behave like pageWithHeader(messages("tai.closeBankAccount.closeDateForm.title", bankName))

    behave like pageWithCombinedHeader(
      messages("tai.closeBankAccount.preHeadingText"),
      messages("tai.closeBankAccount.closeDateForm.title", bankName)
    )

    behave like pageWithBackButton(controllers.income.bbsi.routes.BbsiController.accounts())
    behave like pageWithCancelLink(controllers.income.bbsi.routes.BbsiController.accounts())
    behave like pageWithContinueButtonForm("/check-income-tax/income/bank-building-society-savings/1/close/date")
  }

  "have a legend in the form" in {

    val legendItem = doc(view).select("legend .form-label").text

    legendItem mustBe Messages("tai.closeBankAccount.closeDateForm.label", bankName)
  }

  "have a form hint" in {

    val formHint = doc(view).select("legend .form-hint").text

    formHint mustBe Messages("tai.label.date.example")
  }

  "have a form input for day with relevant label" in {

    val labelDay = doc(view).select(".form-group-day .form-label")
    val inputLabelDay = labelDay.text
    val numberOfInputs = doc(view).select(".form-group-day input").size

    inputLabelDay mustBe Messages("tai.label.day")
    numberOfInputs mustBe 1
  }

  "have a form input for month with relevant label" in {

    val labelMonth = doc(view).select(".form-group-month .form-label")
    val inputLabelMonth = labelMonth.text
    val numberOfInputs = doc(view).select(".form-group-month input").size

    inputLabelMonth mustBe Messages("tai.label.month")
    numberOfInputs mustBe 1
  }

  "have a form input for year with relevant label" in {

    val labelYear = doc(view).select(".form-group-year .form-label")
    val inputLabelYear = labelYear.text
    val numberOfInputs = doc(view).select(".form-group-year input").size

    inputLabelYear mustBe Messages("tai.label.year")
    numberOfInputs mustBe 1
  }

  "have an error box at the top of the page with a link to the error field" when {
    "a form with errors is passed into the view" in {

      def view: Html = views.html.incomes.bbsi.close.bank_building_society_close_date(formWithErrors, bankName, 1)

      val errorSummary = doc(view).select("#error-summary-display a").text

      errorSummary mustBe globalErrorMessage
    }
  }

  "have an error message with the form inputs" when {
    "there is a form with an error" in {

      def view: Html = views.html.incomes.bbsi.close.bank_building_society_close_date(formWithErrors, bankName, 1)

      val errorMessage = doc(view).select(".error-message").text
      val fieldSetError = doc(view).select("form > div").hasClass("form-group-error")

      fieldSetError mustBe true
      errorMessage mustBe globalErrorMessage
    }
  }
}
