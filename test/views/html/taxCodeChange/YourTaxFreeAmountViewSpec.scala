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

package views.html.taxCodeChange

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.TaxFreeInfo
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.tai.viewModels.{ChangeLinkViewModel, TaxFreeAmountSummaryCategoryViewModel, TaxFreeAmountSummaryRowViewModel, TaxFreeAmountSummaryViewModel}

class YourTaxFreeAmountViewSpec extends TaiViewSpec {

  "your tax free amount" should {
    behave like pageWithBackLink()

    behave like pageWithTitle(Messages("taxCode.change.yourTaxFreeAmount.title"))

    "have explanation of tax-free amount" in {
      doc must haveParagraphWithText(Messages("taxCode.change.yourTaxFreeAmount.desc"))
    }

    "have table caption for how tax-free amount is calculated" in {
      doc must haveCaptionWithText(Messages("taxCode.change.yourTaxFreeAmount.summaryHeading"))
    }

    "display a detail section" which {

      "contains 4 columns when previous is present" in {
        doc.select("th").size() mustBe 8
      }

      "contains no header when previous is not present" in {
        doc(currentOnlyView).select("thead").size() mustBe 0
      }

      "contains 4 cells for each addition and deduction" in {
        doc.select(".tax-free-amount-comparison-blank-row td").size() mustBe 8
      }

      "contains 3 cells for each addition and deduction when previous is not present" in {
        doc(currentOnlyView).select(".tax-free-amount-comparison-blank-row td").size() mustBe 6
      }

      "contains 4 cells for total tax free amount" in {
        doc.select(".tax-free-amount-comparison-total th").size() mustBe 4
      }

      "contains 3 cells for total tax free amount is not present" in {
        doc(currentOnlyView).select(".tax-free-amount-comparison-total th").size() mustBe 3
      }

      "contains a heading for the addition section" in {
        doc must haveElementAtPathWithText(
          "td.tax-free-amount-comparison-row-heading",
          Messages("tai.taxFreeAmount.table.additions.caption")
        )
      }

      "contains a heading for the deduction section" in {
        doc must haveElementAtPathWithText(
          "td.tax-free-amount-comparison-row-heading",
          Messages("tai.taxFreeAmount.table.deductions.caption")
        )
      }

      "contains heading for the total category" in {
        doc must haveElementAtPathWithText("#totals-label", Messages("tai.taxFreeAmount.table.totals.label"))
      }

      "visually formats the final table" when {
        "the corresponding final summary item view model contains only a single row" in {
          doc.select(".tax-free-amount-comparison-total").size() mustBe 1
        }
      }
    }

    "have a 'see what happens next' button" in {
      doc must haveLinkElement(
        "seeWhatHappensNext",
        routes.TaxCodeChangeController.whatHappensNext().url,
        messages("taxCode.change.yourTaxFreeAmount.whatHappensNext.link")
      )
    }
  }

  val personalAllowanceCategory: TaxFreeAmountSummaryCategoryViewModel = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = false,
    hideCaption = true,
    Messages("tai.taxFreeAmount.table.allowances.caption"),
    Seq(
      TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""),
        "£1150",
        ChangeLinkViewModel(isDisplayed = false)
      )
    )
  )

  val emptyAdditionRows: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
    TaxFreeAmountSummaryRowViewModel(
      Messages("tai.taxFreeAmount.table.additions.noAddition"),
      "£0",
      ChangeLinkViewModel(isDisplayed = false)
    )
  )

  val nonEmptyAdditionRows: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
    TaxFreeAmountSummaryRowViewModel(
      Messages("tai.taxFreeAmount.table.taxComponent.MarriageAllowanceReceived"),
      "£200",
      ChangeLinkViewModel(isDisplayed = false)
    ),
    TaxFreeAmountSummaryRowViewModel(
      Messages("tai.taxFreeAmount.table.additions.total"),
      "£200",
      ChangeLinkViewModel(isDisplayed = false)
    )
  )

  def createAdditionsCategory(
    additionRows: Seq[TaxFreeAmountSummaryRowViewModel]
  ): TaxFreeAmountSummaryCategoryViewModel =
    TaxFreeAmountSummaryCategoryViewModel(
      Messages("tai.taxFreeAmount.table.columnOneHeader"),
      Messages("tai.taxFreeAmount.table.columnTwoHeader"),
      hideHeaders = true,
      hideCaption = false,
      Messages("tai.taxFreeAmount.table.additions.caption"),
      additionRows
    )

  val emptyDeductionsRows: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
    TaxFreeAmountSummaryRowViewModel(
      Messages("tai.taxFreeAmount.table.deductions.noDeduction"),
      "£0",
      ChangeLinkViewModel(isDisplayed = false)
    )
  )

  val nonEmptyDeductionRows: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
    TaxFreeAmountSummaryRowViewModel(
      Messages("tai.taxFreeAmount.table.deductions.total"),
      "£100",
      ChangeLinkViewModel(isDisplayed = false)
    ),
    TaxFreeAmountSummaryRowViewModel(
      Messages("tai.taxFreeAmount.table.taxComponent.DividendTax"),
      "£100",
      ChangeLinkViewModel(isDisplayed = false)
    )
  )

  def createDeductionsCategory(
    deductionRows: Seq[TaxFreeAmountSummaryRowViewModel]
  ): TaxFreeAmountSummaryCategoryViewModel =
    TaxFreeAmountSummaryCategoryViewModel(
      Messages("tai.taxFreeAmount.table.columnOneHeader"),
      Messages("tai.taxFreeAmount.table.columnTwoHeader"),
      hideHeaders = true,
      hideCaption = false,
      Messages("tai.taxFreeAmount.table.deductions.caption"),
      deductionRows
    )

  def createTotalCategory(totalAmountFormatted: String): TaxFreeAmountSummaryCategoryViewModel =
    TaxFreeAmountSummaryCategoryViewModel(
      Messages("tai.taxFreeAmount.table.columnOneHeader"),
      Messages("tai.taxFreeAmount.table.columnTwoHeader"),
      hideHeaders = true,
      hideCaption = true,
      Messages("tai.taxFreeAmount.table.totals.caption"),
      Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.totals.label"),
          totalAmountFormatted,
          ChangeLinkViewModel(isDisplayed = false)
        )
      )
    )

  def createTaxFreeAmountSummaryViewModel(
    additionRows: Seq[TaxFreeAmountSummaryRowViewModel] = nonEmptyAdditionRows,
    deductionRows: Seq[TaxFreeAmountSummaryRowViewModel] = nonEmptyDeductionRows,
    totalFormattedAmount: String = "£1250"
  ): TaxFreeAmountSummaryViewModel =
    TaxFreeAmountSummaryViewModel(
      Seq(
        personalAllowanceCategory,
        createAdditionsCategory(additionRows),
        createDeductionsCategory(deductionRows),
        createTotalCategory(totalFormattedAmount)
      )
    )

  val taxFreeAmountSummaryViewModel: TaxFreeAmountSummaryViewModel = createTaxFreeAmountSummaryViewModel()

  private def createViewModel(
    taxCodeDateRange: String = "",
    annualTaxFreeAmount: BigDecimal = 1150,
    showPreviousTaxFreeInfo: Boolean = true
  ): YourTaxFreeAmountViewModel = {

    val previous = if (showPreviousTaxFreeInfo) {
      Some(TaxFreeInfo(taxCodeDateRange, 0, 0))
    } else {
      None
    }

    YourTaxFreeAmountViewModel(previous, TaxFreeInfo(taxCodeDateRange, annualTaxFreeAmount, 0), Seq.empty, Seq.empty)
  }

  val taxFreeAmount: YourTaxFreeAmountViewModel            = createViewModel()
  val currentOnlyTaxFreeAmount: YourTaxFreeAmountViewModel = createViewModel(showPreviousTaxFreeInfo = false)

  private val yourTaxFreeAmount                                                                = inject[YourTaxFreeAmountView]
  def createView(viewModel: YourTaxFreeAmountViewModel = taxFreeAmount): HtmlFormat.Appendable =
    yourTaxFreeAmount(viewModel)

  override def view: HtmlFormat.Appendable = createView()
  private lazy val currentOnlyView         = createView(viewModel = currentOnlyTaxFreeAmount)
}
