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

package views.html.incomes

import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.BonusOvertimeAmountForm
import uk.gov.hmrc.tai.model.domain.income.Employer
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BonusPaymentsAmountSpec extends TaiViewSpec with MockitoSugar {

  val employer = Employer(id = 1, name = "Employer")
  val bonusPaymentsAmountForm = BonusOvertimeAmountForm.createForm()

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override def view: Html = views.html.incomes.bonusPaymentAmount(bonusPaymentsAmountForm, employer)

  "Bonus payments amount view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeController.cancel(employer.id).url))
    behave like pageWithCombinedHeader(
      messages("tai.bonusPaymentsAmount.preHeading", employer.name),
      messages("tai.bonusPaymentsAmount.title",TaxYearRangeUtil.currentTaxYearRangeSingleLineBetweenDelimited))
    behave like pageWithTitle(messages("tai.bonusPaymentsAmount.title", TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited))
    behave like pageWithContinueButtonForm("/check-income-tax/update-income/bonus-overtime-amount")

    "contain a paragraph with static text" in {
      doc must haveParagraphWithText(messages("tai.bonusPaymentsAmount.hint"))
    }

    "contain an input field with pound symbol appended" in {
      doc must haveElementAtPathWithId("input", "amount")
      doc must haveElementAtPathWithClass("input", "form-control-currency")
    }

    "return no errors when a valid monetary amount is entered" in {

      val monetaryAmount = "£10,000"
      val monetaryAmountRequest = Json.obj("amount" -> monetaryAmount)
      val validatedForm = bonusPaymentsAmountForm.bind(monetaryAmountRequest)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe BonusOvertimeAmountForm(Some(monetaryAmount))
    }

    "display an error" when {
      "the user continues without entering an amount" in {
        val emptySelectionErrorMessage = messages("tai.bonusPaymentsAmount.error.form.mandatory",
          TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited)
        val invalidRequest = Json.obj("amount" -> "")
        val invalidatedForm = bonusPaymentsAmountForm.bind(invalidRequest)

        val errorView = views.html.incomes.bonusPaymentAmount(invalidatedForm, employer)
        doc(errorView) must haveErrorLinkWithText(emptySelectionErrorMessage)
        doc(errorView) must haveClassWithText(messages(emptySelectionErrorMessage), "error-message")
      }

      "the user enters an invalid monetary amount" in {
        val invalidAmountErrorMessage = messages("tai.bonusPaymentsAmount.error.form.input.invalid")
        val invalidRequest = Json.obj("amount" -> "£10,0")
        val invalidatedForm = bonusPaymentsAmountForm.bind(invalidRequest)

        val errorView = views.html.incomes.bonusPaymentAmount(invalidatedForm, employer)
        doc(errorView) must haveErrorLinkWithText(invalidAmountErrorMessage)
        doc(errorView) must haveClassWithText(messages(invalidAmountErrorMessage), "error-message")
      }
    }

  }

}