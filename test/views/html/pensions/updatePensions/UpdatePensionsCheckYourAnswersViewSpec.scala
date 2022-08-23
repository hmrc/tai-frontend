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

package views.html.pensions.updatePensions

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.pensions.update.UpdatePensionCheckYourAnswersViewModel
import views.html.pensions.update.UpdatePensionCheckYourAnswersView

class UpdatePensionsCheckYourAnswersViewSpec extends TaiViewSpec {

  "check your answers page" must {
    behave like pageWithTitle(messages("tai.checkYourAnswers.title"))
    behave like pageWithCombinedHeaderNewTemplate(
      messages("tai.updatePension.preHeading"),
      messages("tai.checkYourAnswers.title"))
    behave like pageWithButtonForm(
      "/check-income-tax/incorrect-pension/check-your-answers",
      messages("tai.confirmAndSend"))
    behave like pageWithCheckYourAnswersSummaryNew
    behave like pageWithCancelLink(controllers.pensions.routes.UpdatePensionProviderController.cancel(pensionId))

    "display a back button" which {
      "links to the add telephone form page" when {
        "page is displayed" in {
          behave like haveLinkWithUrlWithID(
            "backLink",
            controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url)
        }
      }
    }

    "display the header for the check your answers section" in {
      doc must haveHeadingH2WithText(viewModel.pensionProviderName)
    }

    "display journey confirmation lines" in {
      doc must haveCheckYourAnswersSummaryLineNew(1, messages("tai.updatePension.cya.currentlyReceivePension"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(1, messages("tai.label.yes"))

      doc must haveCheckYourAnswersSummaryLineNew(2, messages("tai.checkYourAnswers.whatYouToldUs"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(2, viewModel.whatYouToldUs)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        2,
        controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs().url)

      doc must haveCheckYourAnswersSummaryLineNew(3, messages("tai.checkYourAnswers.contactByPhone"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(3, viewModel.contactByPhone)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        3,
        controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url)

      doc must haveCheckYourAnswersSummaryLineNew(4, messages("tai.phoneNumber"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(4, viewModel.phoneNumber.getOrElse(""))
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        4,
        controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url)
    }

    "display the last confirmation paragraph" in {
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }
  }

  lazy val pensionId = 1
  val viewModel: UpdatePensionCheckYourAnswersViewModel =
    UpdatePensionCheckYourAnswersViewModel(pensionId, "TEST", "Yes", "whatYouToldUs", "Yes", Some("123456789"))
  private val updatePensionCheckYourAnswersView = inject[UpdatePensionCheckYourAnswersView]

  override def view: Html = updatePensionCheckYourAnswersView(viewModel)
}
