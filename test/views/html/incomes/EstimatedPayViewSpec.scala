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

package views.html.incomes

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.EstimatedPayViewModel

import java.time.LocalDate

class EstimatedPayViewSpec extends TaiViewSpec {

  private val estimatedPay = inject[EstimatedPayView]
  override def view: Html  = estimatedPay(createViewModel())

  val employer = IncomeSource(id = 1, name = "Employer")

  def createViewModel(employmentStartDate: Option[LocalDate] = None) = {

    val grossAnnualPay = Some(BigDecimal(20000))
    val netAnnualPay   = Some(BigDecimal(20000))
    EstimatedPayViewModel(grossAnnualPay, netAnnualPay, false, Some(20000), employmentStartDate, employer)
  }

  "Estimated Pay" must {
    behave like pageWithBackLinkWithUrl(
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
        .checkYourAnswersPage(employer.id)
        .url
    )
    behave like pageWithCancelLink(
      Call("GET", controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url)
    )
    behave like pageWithCombinedHeaderNewFormatNew(
      messages("tai.estimatedPay.preHeading", employer.name),
      messages("tai.estimatedPay.heading", TaxYearRangeUtil.currentTaxYearRangeBreak)
    )
    behave like pageWithTitle(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeBreak))

    "display summary sub-title paragraph" in {
      doc must haveParagraphWithText(messages("tai.estimatedPay.weHaveCalculated"))
    }

    "display heading subtitle" in {
      doc must haveH2HeadingWithText(messages("tai.estimatedPay.yourPay"))
    }

    "display basic pay explanation" in {
      doc must haveParagraphWithText(messages("tai.estimatedPay.basicPayExplanation"))
    }

    "display your taxable pay paragraph" in {
      doc must haveParagraphWithText(messages("tai.estimatedPay.yourTaxablePay.text"))
    }

    "display confirmation static text" in {
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }

    "contain summary with text and a hidden text" when {
      "the gross pay is apportioned" in {
        val employmentStartDate = TaxYear().start.plusMonths(2)

        val detailedSummaryView = estimatedPay(createViewModel(Some(employmentStartDate)))

        doc(detailedSummaryView) must haveSummaryWithText(messages("tai.estimatedPay.whyLower.title"))

      }

      "the grossAnnualPay equals the netAnnualPay" in {

        val grossEqualsNetView = estimatedPay(createViewModel())

        doc(grossEqualsNetView) must haveSummaryWithText(messages("tai.estimatedPay.whySame.title"))

      }
    }

    "display a tax code change explanation statement" in {
      doc must haveParagraphWithText(messages("tai.estimatedPay.taxCodeChange.explanation.para1"))
      doc must haveParagraphWithText(messages("tai.estimatedPay.taxCodeChange.explanation.para2"))
    }

    "confirm and send" in {
      doc must haveLinkElement(
        "confirmAndSend",
        controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.handleCalculationResult().url,
        messages("tai.estimatedPay.checkTaxEstimate")
      )
    }
  }
}
