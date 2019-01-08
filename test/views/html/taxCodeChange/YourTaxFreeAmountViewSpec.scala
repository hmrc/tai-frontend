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

package views.html.taxCodeChange

import controllers.routes
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{AllowancesAndDeductions, TaxFreeInfo}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.viewModels.{ChangeLinkViewModel, TaxFreeAmountSummaryCategoryViewModel, TaxFreeAmountSummaryRowViewModel, TaxFreeAmountSummaryViewModel}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel

class YourTaxFreeAmountViewSpec extends TaiViewSpec {

  "your tax free amount" should {
    behave like pageWithBackLink

    behave like pageWithTitle(Messages("taxCode.change.yourTaxFreeAmount.title"))

    behave like pageWithCombinedHeader(Messages("taxCode.change.journey.preHeading"), Messages("taxCode.change.yourTaxFreeAmount.title"))

    "have explanation of tax-free amount" in {
      doc must haveParagraphWithText(Messages("taxCode.change.yourTaxFreeAmount.desc"))
    }

    "have h2 heading showing the date period for tax-free amount" in {
      val fromDate = new LocalDate()
      val toDate = TaxYearResolver.endOfCurrentTaxYear
      val expectedDateRange = ViewModelHelper.dynamicDateRangeHtmlNonBreak(fromDate, toDate)
      val expectedMessage = Messages("taxCode.change.yourTaxFreeAmount.dates", expectedDateRange)

      doc(viewP2Date) must haveH2HeadingWithText(s"$expectedMessage £1,150")

      def viewP2Date: Html = createView(createViewModel(expectedDateRange))
    }


    "display figure for Your tax-free amount" in {
      val taxFreeAmount = "£11,500"

      doc(viewTaxFreeAmount) must haveSpanWithText(taxFreeAmount)

      def viewTaxFreeAmount: Html = createView(createViewModel(annualTaxFreeAmount =  11500))
    }

    "have h2 heading for how tax-free amount is calculated" in {
      doc must haveH2HeadingWithText(Messages("taxCode.change.yourTaxFreeAmount.summaryHeading"))
    }

    "display a detail section" which {

      "contains a heading for the addition section" in {
        doc must haveElementAtPathWithText("h3.tax-free-amount-comparison-row-heading", Messages("tai.taxFreeAmount.table.additions.caption"))
      }

      "contains a heading for the deduction section" in {
        doc must haveElementAtPathWithText("h3.tax-free-amount-comparison-row-heading", Messages("tai.taxFreeAmount.table.deductions.caption"))
      }

      "contains heading for the personal allowance category" in {
        doc must haveElementAtPathWithText(".tax-free-amount-comparison-row-title", Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""))
      }

      "contains heading for the total category" in {
        doc must haveElementAtPathWithText("h3.tax-free-amount-comparison-row-heading", Messages("tai.taxFreeAmount.table.totals.label"))
      }

      "visually formats the final table" when {
        "the corresponding final summary item view model contains only a single row" in {
          doc.select(".tax-free-amount-comparison-total").size() mustBe 1
          doc must haveElementAtPathWithClass("tr", "tax-free-amount-comparison-total highlight")
        }
      }
    }

    "have a 'see what happens next' button" in {
      doc must haveLinkElement(
        "seeWhatHappensNext", routes.TaxCodeChangeController.whatHappensNext.url, messages("taxCode.change.yourTaxFreeAmount.whatHappensNext.link"))
    }
  }

  val personalAllowanceCategory = TaxFreeAmountSummaryCategoryViewModel(
                                      Messages("tai.taxFreeAmount.table.columnOneHeader"),
                                      Messages("tai.taxFreeAmount.table.columnTwoHeader"),
                                      hideHeaders = false,
                                      hideCaption = true,
                                      Messages("tai.taxFreeAmount.table.allowances.caption"),
                                      Seq(TaxFreeAmountSummaryRowViewModel(
                                        Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""),
                                        "£1150",
                                        ChangeLinkViewModel(false, "", "")
                                      )))

  val emptyAdditionRows = Seq(TaxFreeAmountSummaryRowViewModel(
    Messages("tai.taxFreeAmount.table.additions.noAddition"), "£0", ChangeLinkViewModel(false, "", "")
  ))

  val nonEmptyAdditionRows = Seq(
      TaxFreeAmountSummaryRowViewModel(Messages("tai.taxFreeAmount.table.taxComponent.MarriageAllowanceReceived"), "£200", ChangeLinkViewModel(false, "", "")),
      TaxFreeAmountSummaryRowViewModel(Messages("tai.taxFreeAmount.table.additions.total"), "£200", ChangeLinkViewModel(false, "", "")))

  def createAdditionsCatetgory(additionRows: Seq[TaxFreeAmountSummaryRowViewModel]) = TaxFreeAmountSummaryCategoryViewModel(
                                Messages("tai.taxFreeAmount.table.columnOneHeader"),
                                Messages("tai.taxFreeAmount.table.columnTwoHeader"),
                                hideHeaders = true,
                                hideCaption = false,
                                Messages("tai.taxFreeAmount.table.additions.caption"),
                                additionRows)

  val emptyDeductionsRows = Seq(TaxFreeAmountSummaryRowViewModel(
    Messages("tai.taxFreeAmount.table.deductions.noDeduction"), "£0", ChangeLinkViewModel(false, "", "")
  ))

  val nonEmptyDeductionRows = Seq(
    TaxFreeAmountSummaryRowViewModel(Messages("tai.taxFreeAmount.table.deductions.total"), "£100", ChangeLinkViewModel(false, "", "")),
    TaxFreeAmountSummaryRowViewModel(Messages("tai.taxFreeAmount.table.taxComponent.DividendTax"), "£100", ChangeLinkViewModel(false, "", ""))
  )

  def createDeductionsCategory(deductionRows: Seq[TaxFreeAmountSummaryRowViewModel]) = TaxFreeAmountSummaryCategoryViewModel(
                                Messages("tai.taxFreeAmount.table.columnOneHeader"),
                                Messages("tai.taxFreeAmount.table.columnTwoHeader"),
                                hideHeaders = true,
                                hideCaption = false,
                                Messages("tai.taxFreeAmount.table.deductions.caption"),
                                deductionRows)

  def createTotalCategory(totalAmountFormatted: String) = TaxFreeAmountSummaryCategoryViewModel(
                           Messages("tai.taxFreeAmount.table.columnOneHeader"),
                           Messages("tai.taxFreeAmount.table.columnTwoHeader"),
                           hideHeaders = true,
                           hideCaption = true,
                           Messages("tai.taxFreeAmount.table.totals.caption"),
                           Seq(TaxFreeAmountSummaryRowViewModel(
                             Messages("tai.taxFreeAmount.table.totals.label"),
                             totalAmountFormatted,
                             ChangeLinkViewModel(false, "", "")
                           )))


  def createTaxFreeAmountSummaryViewModel(additionRows: Seq[TaxFreeAmountSummaryRowViewModel] = nonEmptyAdditionRows,
                                          deductionRows: Seq[TaxFreeAmountSummaryRowViewModel] = nonEmptyDeductionRows,
                                          totalFormattedAmount: String = "£1250") = {

                                              TaxFreeAmountSummaryViewModel(Seq(
                                                  personalAllowanceCategory,
                                                  createAdditionsCatetgory(additionRows),
                                                  createDeductionsCategory(deductionRows),
                                                  createTotalCategory(totalFormattedAmount)
                                              ))

  }

  val taxFreeAmountSummaryViewModel: TaxFreeAmountSummaryViewModel = createTaxFreeAmountSummaryViewModel()

  private def createViewModel(taxCodeDateRange: String = "",
                              annualTaxFreeAmount: BigDecimal = 1150
                             ): YourTaxFreeAmountViewModel = {

    YourTaxFreeAmountViewModel(
      TaxFreeInfo(taxCodeDateRange, 0, 0),
      TaxFreeInfo(taxCodeDateRange, annualTaxFreeAmount, 0),
      Seq.empty,
      Seq.empty)
  }

  val currentTaxFreeAmount: YourTaxFreeAmountViewModel = createViewModel()


  def createView(viewModel: YourTaxFreeAmountViewModel = currentTaxFreeAmount) =
    views.html.taxCodeChange.yourTaxFreeAmount(viewModel)

  override def view = createView()
}

