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

import controllers.routes
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class IncomeCheckYourAnswersSpec extends TaiViewSpec {

  val preHeading = "add missing income"

  "Income Check Your Answers page" must {

    behave like pageWithTitle(messages("tai.checkYourAnswers"))

    behave like pageWithCombinedHeader(
      preHeading,
      messages("tai.checkYourAnswers"))

    behave like pageWithButtonForm("/fake/submission/url",
      messages("tai.confirmAndSend"))

    behave like pageWithCheckYourAnswersSummary

    "display a back button" which {
      "redirects to the previous page defined in the view model" in {
        doc must haveBackButtonWithUrl("fake/backlink/url")
      }
    }

    "display a cancel button" which {
      "redirects to the taxabel income summary page" in {
        doc must haveCancelLinkWithUrl("/fake/cancel/url")
      }
    }

    "display a confirm summary paragraph" in {
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }

    "do no display confirm summary paragraph" when {
      "no text is present in view model" in {
        val viewModel = IncomeCheckYourAnswersViewModel(preHeading, "fake/backlink/url", lines, None, "/fake/submission/url", "/fake/cancel/url")
        def view: Html = views.html.incomes.addIncomeCheckYourAnswers(viewModel)
        doc(view) must not(haveParagraphWithText(messages("tai.checkYourAnswers.confirmText")))
      }

    }

    "display confirmation lines within the summary information, with corresponding change links" in {
      doc must haveCheckYourAnswersSummaryLine(1, messages("tai.addEmployment.cya.q1"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(1, "q1/url?edit=true")

      doc must haveCheckYourAnswersSummaryLine(2, messages("tai.addEmployment.cya.q2"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(2, "q2/url?edit=true")

      doc must haveCheckYourAnswersSummaryLine(3, messages("tai.addEmployment.cya.q3"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(3, "q3/url?edit=true")

      doc must haveCheckYourAnswersSummaryLine(4, messages("tai.addEmployment.cya.q4"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(4, "q4/url?edit=true")

      doc must haveCheckYourAnswersSummaryLine(5, messages("tai.phoneNumber"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(5, "q5/url?edit=true")
    }
  }

  val lines = Seq(
    CheckYourAnswersConfirmationLine(messages("tai.addEmployment.cya.q1"), "some answer", "q1/url"),
    CheckYourAnswersConfirmationLine(messages("tai.addEmployment.cya.q2"), "some answer", "q2/url"),
    CheckYourAnswersConfirmationLine(messages("tai.addEmployment.cya.q3"), "some answer", "q3/url"),
    CheckYourAnswersConfirmationLine(messages("tai.addEmployment.cya.q4"), "some answer", "q4/url"),
    CheckYourAnswersConfirmationLine(messages("tai.phoneNumber"), "some answer", "q5/url")
  )

  val viewModel = IncomeCheckYourAnswersViewModel(preHeading, "fake/backlink/url", lines, Some(messages("tai.checkYourAnswers.confirmText")), "/fake/submission/url", "/fake/cancel/url")

  override def view: Html = views.html.incomes.addIncomeCheckYourAnswers(viewModel)
}
