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

package views.html.employments.update

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.UpdateEmploymentCheckYourAnswersViewModel

class UpdateEmploymentCheckYourAnswersViewSpec extends TaiViewSpec {

  "check your answers page" must {
    behave like pageWithTitle(messages("tai.checkYourAnswers.title"))
    behave like pageWithCombinedHeaderNewFormatNew(
      messages("tai.updateEmployment.whatDoYouWantToTellUs.preHeading"),
      messages("tai.checkYourAnswers.title")
    )
    behave like pageWithButtonForm(
      "/check-income-tax/update-employment/check-your-answers",
      messages("tai.confirmAndSend")
    )
    behave like pageWithCheckYourAnswersSummaryNew()
    behave like pageWithCancelLink(controllers.employments.routes.UpdateEmploymentController.cancel(employmentId))

    "display a back button" which {
      "links to the add telephone form page" when {
        "page is displayed" in {
          doc must haveBackLink
        }
      }
    }

    "display the header for the check your answers section" in {
      doc must haveHeadingH2WithText(viewModel.employerName)
    }

    "display journey confirmation lines" in {
      doc must haveCheckYourAnswersSummaryLineNew(1, messages("tai.updateEmployment.cya.currentlyWorkHere"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(1, messages("tai.label.yes"))

      doc must haveCheckYourAnswersSummaryLineNew(2, messages("tai.checkYourAnswers.whatYouToldUs"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(2, viewModel.whatYouToldUs)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        2,
        controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(viewModel.id).url
      )

      doc must haveCheckYourAnswersSummaryLineNew(3, messages("tai.checkYourAnswers.contactByPhone"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(3, viewModel.contactByPhone)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        3,
        controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
      )

      doc must haveCheckYourAnswersSummaryLineNew(4, messages("tai.phoneNumber"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(4, viewModel.phoneNumber.getOrElse(""))
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        4,
        controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
      )
    }

    "display the last confirmation paragraph" in {
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }
  }

  lazy val employmentId = 1
  val viewModel         =
    UpdateEmploymentCheckYourAnswersViewModel(employmentId, "TEST", "whatYouToldUs", "Yes", Some("123456789"))

  private val template = inject[UpdateEmploymentCheckYourAnswersView]

  override def view: Html = template(viewModel)
}
