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

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import uk.gov.hmrc.tai.viewModels.pensions.CheckYourAnswersViewModel

class AddPensionCheckYourAnswersViewSpec extends TaiViewSpec {

  val preHeading = "add missing pension"

  "Income Check Your Answers page" must {

    behave like pageWithTitle(messages("tai.checkYourAnswers.title"))

    behave like pageWithCombinedHeaderNewTemplate(preHeading, messages("tai.checkYourAnswers.title"))

    behave like pageWithButtonForm("/fake/submission/url", messages("tai.confirmAndSend"))

    behave like pageWithCheckYourAnswersSummaryNew

    "display a back button" in {
      doc must haveBackLink
    }

    "display a cancel button" which {
      "redirects to the taxabel income summary page" in {
        doc must haveCancelLinkWithUrl("/fake/cancel/url")
      }
    }

    "display a confirm summary paragraph" in {
      doc must haveParagraphWithText("confirmation text")
    }

    "display a cya title" in {
      doc must haveH2HeadingWithText("cya title")
    }

    "display confirmation lines within the summary information, with corresponding change links" in {
      doc must haveCheckYourAnswersSummaryLineNew(1, "q1")
      doc must haveCheckYourAnswersSummaryLineChangeLink(1, "q1/url?edit=true")

      doc must haveCheckYourAnswersSummaryLineNew(2, "q2")
      doc must haveCheckYourAnswersSummaryLineChangeLink(2, "q2/url?edit=true")

      doc must haveCheckYourAnswersSummaryLineNew(3, "q3")
      doc must haveCheckYourAnswersSummaryLineChangeLink(3, "q3/url?edit=true")

      doc must haveCheckYourAnswersSummaryLineNew(4, "q4")
      doc must haveCheckYourAnswersSummaryLineChangeLink(4, "q4/url?edit=true")

      doc must haveCheckYourAnswersSummaryLineNew(5, "q5")
      doc must haveCheckYourAnswersSummaryLineChangeLink(5, "q5/url?edit=true")
    }
  }

  val lines = Seq(
    CheckYourAnswersConfirmationLine("q1", "a1", "q1/url"),
    CheckYourAnswersConfirmationLine("q2", "a2", "q2/url"),
    CheckYourAnswersConfirmationLine("q3", "a3", "q3/url"),
    CheckYourAnswersConfirmationLine("q4", "a4", "q4/url"),
    CheckYourAnswersConfirmationLine("q5", "a5", "q5/url")
  )

  val viewModel: CheckYourAnswersViewModel = CheckYourAnswersViewModel(
    preHeading,
    "fake/backlink/url",
    "cya title",
    lines,
    "confirmation text",
    "/fake/submission/url",
    "/fake/cancel/url")

  private val addPensionCheckYourAnswers = inject[AddPensionCheckYourAnswersView]
  override def view: Html = addPensionCheckYourAnswers(viewModel)
}
