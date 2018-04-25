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

package views.html.incomes.previousYears

import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.previousYears.UpdateIncomeDetailsCheckYourAnswersViewModel

class CheckYourAnswersSpec extends TaiViewSpec {

  "checkYourAnswers" should {

    behave like pageWithTitle(messages("tai.checkYourAnswers"))
    behave like pageWithCombinedHeader(messages("tai.income.previousYears.journey.preHeader"), messages("tai.checkYourAnswers"))
    behave like pageWithButtonForm("/check-income-tax/update-income-details/submit-your-answers", messages("tai.confirmAndSend"))
    behave like pageWithCancelLink(controllers.routes.PayeControllerHistoric.payePage(TaxYear().prev))
    behave like pageWithCheckYourAnswersSummary

    "display a back button" which {
      "links to the add telephone form page" when {
        "page is displayed" in {
          doc must haveBackLink
        }
      }
    }

    "display the header for the check your answers section" in {
      doc must haveHeadingH2WithText(viewModel.tableHeader)
    }

    "display journey confirmation lines" in {

      doc must haveCheckYourAnswersSummaryLine(1, messages("tai.checkYourAnswers.whatYouToldUs"))
      doc must haveCheckYourAnswersSummaryLineAnswer(1, viewModel.whatYouToldUs)
      doc must haveCheckYourAnswersSummaryLineChangeLink(1, controllers.income.previousYears.routes.UpdateIncomeDetailsController.details().url)

      doc must haveCheckYourAnswersSummaryLine(2, messages("tai.checkYourAnswers.contactByPhone"))
      doc must haveCheckYourAnswersSummaryLineAnswer(2, viewModel.contactByPhone)
      doc must haveCheckYourAnswersSummaryLineChangeLink(2, controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber().url)

      doc must haveCheckYourAnswersSummaryLine(3, messages("tai.phoneNumber"))
      doc must haveCheckYourAnswersSummaryLineAnswer(3, viewModel.phoneNumber.getOrElse(""))
      doc must haveCheckYourAnswersSummaryLineChangeLink(3, controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber().url)
    }

    "display the last confirmation paragraph" in {
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }
  }

  val viewModel = UpdateIncomeDetailsCheckYourAnswersViewModel("2016", "whatYouToldUs", "Yes", Some("123456789"))

  override def view = views.html.incomes.previousYears.CheckYourAnswers(viewModel)
}
