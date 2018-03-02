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

package views.html.benefits

import uk.gov.hmrc.play.views.formatting.Money
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.benefit.RemoveCompanyBenefitCheckYourAnswersViewModel

class RemoveCompanyBenefitCheckYourAnswersSpec extends TaiViewSpec {

  "checkYourAnswers" should {

    behave like pageWithTitle(messages("tai.checkYourAnswers"))
    behave like pageWithCombinedHeader(messages("tai.benefits.ended.journey.preHeader"), messages("tai.checkYourAnswers"))
    behave like pageWithButtonForm("/check-income-tax/remove-company-benefit/submit-your-answers", messages("tai.confirmAndSend"))
    behave like pageWithCancelLink(controllers.routes.TaxAccountSummaryController.onPageLoad())
    behave like pageWithCheckYourAnswersSummary

    "display a back button" which {
      "links to the add telephone form page" when {
        "page is displayed" in {
          doc must haveBackButtonWithUrl(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url)
        }
      }
    }

    "display the header for the check your answers section" in {
      doc must haveHeadingH2WithText(viewModel.tableHeader)
    }

    "display journey confirmation lines" in {

      doc must haveCheckYourAnswersSummaryLine(1, messages("tai.checkYourAnswers.whatYouToldUs"))
      doc must haveCheckYourAnswersSummaryLineAnswer(1, messages("tai.noLongerGetBenefit"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(1, "")

      doc must haveCheckYourAnswersSummaryLine(2, messages("tai.checkYourAnswers.dateBenefitEnded"))
      doc must haveCheckYourAnswersSummaryLineAnswer(2, viewModel.stopDate)
      doc must haveCheckYourAnswersSummaryLineChangeLink(2, controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url)

      val benefitValue = Money.pounds(BigDecimal(viewModel.valueOfBenefit.getOrElse("0"))).toString().trim.replace("&pound;","\u00A3")
      doc must haveCheckYourAnswersSummaryLine(3, messages("tai.checkYourAnswers.valueOfBenefit"))
      doc must haveCheckYourAnswersSummaryLineAnswer(3, benefitValue)
      doc must haveCheckYourAnswersSummaryLineChangeLink(3, controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit().url)

      doc must haveCheckYourAnswersSummaryLine(4, messages("tai.checkYourAnswers.contactByPhone"))
      doc must haveCheckYourAnswersSummaryLineAnswer(4, viewModel.contactByPhone)
      doc must haveCheckYourAnswersSummaryLineChangeLink(4, controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url)

      doc must haveCheckYourAnswersSummaryLine(5, messages("tai.phoneNumber"))
      doc must haveCheckYourAnswersSummaryLineAnswer(5, viewModel.phoneNumber.getOrElse(""))
      doc must haveCheckYourAnswersSummaryLineChangeLink(5, controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url)

    }

    "display the last confirmation paragraph" in {
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }

  }

  def viewModel = RemoveCompanyBenefitCheckYourAnswersViewModel("Awesome benefit from TestCompany","Hello","Before 6 April",Some("10000"),"Yes",Some("123456789"))

  override def view = views.html.benefits.removeCompnanyBenefitCheckYourAnswers(viewModel)
}
