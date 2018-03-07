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

import uk.gov.hmrc.tai.forms.income.bbsi.{BankAccountsDecisionForm, BankAccountsDecisionFormData}
import uk.gov.hmrc.tai.viewModels.income.BbsiAccountsDecisionViewModel
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.BankAccountDecisionConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BankBuildingSocietyAccountsDecisionSpec extends TaiViewSpec
  with BankAccountDecisionConstants {

  "bbsi accounts decision view" should {

    behave like pageWithTitle(messages("tai.bbsi.decision.preHeading"))
    behave like pageWithHeader(messages("tai.bbsi.decision.heading", "TestBank1"))
    behave like pageWithBackLink
    behave like pageWithCancelLink(controllers.income.bbsi.routes.BbsiController.accounts())
    behave like pageWithContinueButtonForm("/check-income-tax/income/bank-building-society-savings/accounts/1/decision")

    "have an error box on the top of the page with link a to error field" when {
      "a form with errors is passed into the view" in {

        val formWithError: Form[BankAccountsDecisionFormData] = BankAccountsDecisionForm.createForm.bind(Map(BankAccountDecision -> ""))

        val viewModel = BbsiAccountsDecisionViewModel(id, bankName)
        def view: Html = views.html.incomes.bbsi.bank_building_society_accounts_decision(viewModel, formWithError)

        doc(view).select(".error-summary--show > ul > li > #bankAccountDecision-error-summary").text mustBe Messages("tai.bbsi.decision.error.selectOption")
      }
    }

    "have error message with the radio buttons" in {

      val formWithError: Form[BankAccountsDecisionFormData] = BankAccountsDecisionForm.createForm.bind(Map(BankAccountDecision -> ""))

      val viewModel = BbsiAccountsDecisionViewModel(id, bankName)
      def view: Html = views.html.incomes.bbsi.bank_building_society_accounts_decision(viewModel, formWithError)

      val errorDoc = doc(view)

      errorDoc must haveElementAtPathWithText(".error-notification", Messages("tai.bbsi.decision.error.selectOption"))
      errorDoc must haveElementAtPathWithClass("form div", "form-field--error")
    }

    "have three radio buttons with relevant text" in {

      doc(view) must haveInputLabelWithText(s"${BankAccountDecision}-${UpdateInterest}", Messages("tai.bbsi.decision.radio1"))
      doc(view) must haveInputLabelWithText(s"${BankAccountDecision}-${CloseAccount}", Messages("tai.bbsi.decision.radio2"))
      doc(view) must haveInputLabelWithText(s"${BankAccountDecision}-${RemoveAccount}", Messages("tai.bbsi.decision.radio3"))
    }
  }

  private val id = 1
  private val bankName = "TestBank1"

  private lazy val viewModel = BbsiAccountsDecisionViewModel(id, bankName)

  private lazy val form = BankAccountsDecisionForm.createForm

  override def view = views.html.incomes.bbsi.bank_building_society_accounts_decision(viewModel, form)
}
