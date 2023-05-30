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

package views.html.benefits

import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.tai.util.Money
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.benefit.RemoveCompanyBenefitsCheckYourAnswersViewModel

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RemoveCompanyBenefitCheckYourAnswersViewSpec extends TaiViewSpec {

  "checkYourAnswers" should {

    behave like pageWithTitle(messages("tai.checkYourAnswers.title"))
    behave like pageWithCombinedHeaderNewFormat(
      messages("tai.benefits.ended.journey.preHeader"),
      messages("tai.checkYourAnswers.title")
    )
    behave like pageWithButtonForm(
      "/check-income-tax/remove-company-benefit/submit-your-answers",
      messages("tai.confirmAndSend")
    )
    behave like pageWithCancelLink(controllers.benefits.routes.RemoveCompanyBenefitController.cancel)
    behave like pageWithCheckYourAnswersSummaryNew

    "display a back button" in {
      doc must haveLinkWithUrlWithID(
        "backLink",
        controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber.url
      )
    }

    "display the header for the check your answers section" in {
      doc must haveHeadingH2WithText(viewModel.tableHeader)
    }

    "display journey confirmation lines" in {

      doc must haveCheckYourAnswersSummaryLineNew(1, messages("tai.checkYourAnswers.whatYouToldUs"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(1, messages("tai.noLongerGetBenefit"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(1, "")

      doc must haveCheckYourAnswersSummaryLineNew(2, messages("tai.checkYourAnswers.dateBenefitEnded"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(
        2,
        viewModel.stopDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
      )
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        2,
        controllers.benefits.routes.RemoveCompanyBenefitController.stopDate.url
      )

      val benefitValue =
        Money.pounds(BigDecimal(viewModel.valueOfBenefit.getOrElse("0"))).toString().trim.replace("&pound;", "\u00A3")
      doc must haveCheckYourAnswersSummaryLineNew(3, messages("tai.checkYourAnswers.valueOfBenefit"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(3, benefitValue)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        3,
        controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit.url
      )

      doc must haveCheckYourAnswersSummaryLineNew(4, messages("tai.checkYourAnswers.contactByPhone"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(4, viewModel.contactByPhone)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        4,
        controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber.url
      )

      doc must haveCheckYourAnswersSummaryLineNew(5, messages("tai.phoneNumber"))
      doc must haveCheckYourAnswersSummaryLineAnswerNew(5, viewModel.phoneNumber.getOrElse(""))
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        5,
        controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber.url
      )

    }

    "display the last confirmation paragraph" in {
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }

  }

  private val viewModel =
    RemoveCompanyBenefitsCheckYourAnswersViewModel(
      "Awesome benefit from TestCompany",
      "Hello",
      LocalDate.now(),
      Some("10000"),
      "Yes",
      Some("123456789")
    )

  private val template = inject[RemoveCompanyBenefitCheckYourAnswersView]

  override def view: Html = template(viewModel)
}
