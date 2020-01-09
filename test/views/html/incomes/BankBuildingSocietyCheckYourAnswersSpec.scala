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

import uk.gov.hmrc.tai.viewModels.income.BbsiClosedCheckYourAnswersViewModel
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.TaxYear

import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BankBuildingSocietyCheckYourAnswersSpec extends TaiViewSpec {

  "End BBSI check your answers page" must {
    behave like pageWithTitle(messages("tai.checkYourAnswers.title"))
    behave like pageWithCombinedHeader(messages("account.closed"), messages("tai.checkYourAnswers.title"))

    behave like pageWithButtonForm(
      "/check-income-tax/income/bank-building-society-savings/0/close/check-your-answers",
      messages("tai.confirmAndSend"))

    behave like pageWithCheckYourAnswersSummary

    "display a back button" which {
      "links to the BBSI end date form page" when {
        "the account is closed in the current tax year" in {
          doc must haveBackLink
        }
      }

      "links to the BBSI end date form page" when {
        "the account is closed before the current tax year" in {

          val date = TaxYear().endPrev
          val formattedDate = date.toString("yyyy-MM-dd")

          val viewModel = BbsiClosedCheckYourAnswersViewModel(0, formattedDate, Some(account), Some("123.45"))
          def view: Html = views.html.incomes.bbsi.close.bank_building_society_check_your_answers(viewModel)

          doc(view) must haveBackLink
        }
      }
    }

    "display the header for the check your answers section with the correct account" in {
      doc must haveHeadingH2WithText(account)
    }

    "display the first row of check your answers with the proposed date that the account closed" in {
      doc must haveCheckYourAnswersSummaryLine(1, messages("tai.checkYourAnswers.whatYouToldUs"))
      doc must haveCheckYourAnswersSummaryLineAnswer(1, messages("tai.bbsi.end.checkYourAnswers.rowOne.answer"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        1,
        controllers.income.bbsi.routes.BbsiController.decision(0).url + "?edit=true")
      doc must haveCheckYourAnswersSummaryLine(2, messages("tai.bbsi.end.checkYourAnswers.rowTwo.question"))
      doc must haveCheckYourAnswersSummaryLineAnswer(2, displayedDate)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        2,
        controllers.income.bbsi.routes.BbsiCloseAccountController.captureCloseDate(0).url + "?edit=true")
    }

    "display the closing interest section with a closing interest amount" when {
      "the closing interest is provided in the view model" in {

        val viewModel = BbsiClosedCheckYourAnswersViewModel(0, formattedDate, Some(account), Some("123"))
        def view: Html = views.html.incomes.bbsi.close.bank_building_society_check_your_answers(viewModel)

        doc(view) must haveCheckYourAnswersSummaryLine(
          3,
          messages("tai.bbsi.end.checkYourAnswers.rowThree.question", TaxYear().year.toString))
        doc(view) must haveCheckYourAnswersSummaryLineAnswer(3, "£123")
        doc(view) must haveCheckYourAnswersSummaryLineChangeLink(
          3,
          controllers.income.bbsi.routes.BbsiCloseAccountController.captureClosingInterest(0).url)
      }
    }

    "display the closing interest section without a closing interest amount" when {
      "the closing interest is not provided in the view model" in {

        val viewModel = BbsiClosedCheckYourAnswersViewModel(0, formattedDate, Some(account), None)
        def view: Html = views.html.incomes.bbsi.close.bank_building_society_check_your_answers(viewModel)

        doc(view) must haveCheckYourAnswersSummaryLine(
          3,
          messages("tai.bbsi.end.checkYourAnswers.rowThree.question", TaxYear().year.toString))
        doc(view) must haveCheckYourAnswersSummaryLineAnswer(
          3,
          messages("tai.closeBankAccount.closingInterest.notKnown"))
        doc(view) must haveCheckYourAnswersSummaryLineChangeLink(
          3,
          controllers.income.bbsi.routes.BbsiCloseAccountController.captureClosingInterest(0).url)
      }
    }

    "display the last confirmation paragraph" in {
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }

    "display a cancel link" in {
      val cancelButton = doc.select("#cancelLink")
      cancelButton.size === 1
      cancelButton.get(0).attributes.get("href") mustBe controllers.income.bbsi.routes.BbsiController.accounts().url
    }
  }

  val account = "bbsiAccount"

  val date = TaxYear().start
  val formattedDate = date.toString("yyyy-MM-dd")
  val displayedDate = date.toString("d MMMM yyyy")

  val viewModel = BbsiClosedCheckYourAnswersViewModel(0, formattedDate, Some(account), None)
  override def view: Html = views.html.incomes.bbsi.close.bank_building_society_check_your_answers(viewModel)
}
