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

import controllers.income.bbsi.routes
import uk.gov.hmrc.tai.viewModels.income.BbsiUpdateInterestViewModel
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class UpdateBankBuildingSocietyCheckYourAnswersSpec extends TaiViewSpec {
  "Update BBSI check your answers page" must {
    behave like pageWithTitle(messages("tai.checkYourAnswers.title"))
    behave like pageWithCombinedHeader(
      messages("tai.bbsi.update.checkYourAnswers.preHeading"),
      messages("tai.checkYourAnswers.title"))

    behave like pageWithButtonForm(
      "/check-income-tax/income/bank-building-society-savings/0/update/check-your-answers",
      messages("tai.confirmAndSend"))

    behave like pageWithCheckYourAnswersSummary

    "display a back button" in {
      doc must haveBackLink
    }

    "display the header for the check your answers section with the correct account" in {
      doc must haveHeadingH2WithText(bankName)
    }

    "display the first row of check your answers with the proposed date that the account closed" in {
      doc must haveCheckYourAnswersSummaryLine(1, messages("tai.checkYourAnswers.whatYouToldUs"))
      doc must haveCheckYourAnswersSummaryLineAnswer(1, messages("tai.bbsi.update.checkYourAnswers.rowOne.answer"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(1, routes.BbsiController.decision(0).url)
    }

    "display the second row of check your answers with the proposed date that the account closed" in {
      doc must haveCheckYourAnswersSummaryLine(2, messages("tai.bbsi.update.checkYourAnswers.rowTwo"))
      doc must haveCheckYourAnswersSummaryLineAnswer(2, "£1,000")
      doc must haveCheckYourAnswersSummaryLineChangeLink(2, routes.BbsiUpdateAccountController.captureInterest(0).url)
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

  val bankName = "TEST"
  val interest = "1000.29"

  val viewModel = BbsiUpdateInterestViewModel(0, interest, bankName)
  override def view: Html = views.html.incomes.bbsi.update.bank_building_society_check_your_answers(viewModel)
}
