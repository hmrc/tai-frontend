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

      "contains one section for each category" in {
        doc.select(".govuk-check-your-answers").size() mustBe 4
        doc must haveElementWithId("summaryTable1")
        doc must haveElementWithId("summaryTable2")
        doc must haveElementWithId("summaryTable3")
        doc must haveElementWithId("summaryTable4")
      }

      "contains a heading for the addition section" in {
        doc must haveElementAtPathWithText("h3#summaryTable2Caption", Messages("tai.taxFreeAmount.table.additions.caption"))
      }

      "contains a heading for the deduction section" in {
        doc must haveElementAtPathWithText("h3#summaryTable3Caption", Messages("tai.taxFreeAmount.table.deductions.caption"))
      }

      "does not contain a stand alone heading for the personal allowance and total groups (which are of length 1)" in {
        doc must not(haveElementAtPathWithId("h3", "summaryTable1Caption"))
        doc must not(haveElementAtPathWithId("h3", "summaryTable4Caption"))
      }

      "contains heading for the personal allowance catergory" in {
        doc must haveElementAtPathWithText("h3#summaryTable1Row1-header", Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""))
      }

      "contains heading for the total catergory" in {
        doc must haveElementAtPathWithText("h3#summaryTable4Row1-header", Messages("tai.taxFreeAmount.table.totals.label"))
      }

      "displays a group with multiple rows as an unordered list of items" in {
        doc must haveElementAtPathWithId("#yourTaxFreeAmount ul", "summaryTable2Body")
      }

      "displays a group with a single row as a plain div" in {

        val emptyRowTaxFreeAmountSummary = createTaxFreeAmountSummaryViewModel(emptyAdditionRows, emptyDeductionsRows, "£1150")

        val emptyRowViewModel = createViewModel(taxFreeAmountSummary = emptyRowTaxFreeAmountSummary)

        val emptyRowView = createView(emptyRowViewModel)

        doc(emptyRowView) must not(haveElementAtPathWithId("#yourTaxFreeAmount ul", "summaryTable3Body"))
        doc(emptyRowView) must haveElementAtPathWithId("#yourTaxFreeAmount div", "summaryTable3Body")
      }

      "excludes a link cell from table rows, where instructed by the view model" in {
        doc must not(haveElementWithId("summaryTable2Row3ChangeLinkCell"))
      }

      "visually formats the final table" when {
        "the corresponding final summary item view model contains only a single row" in {
          doc.select("#summaryTable4 .cya-question").size() mustBe 1
          doc must haveElementAtPathWithClass("#summaryTable4 .govuk-check-your-answers", "highlight")
        }
      }
    }

    "have a 'see what happens next' button" in {
      doc must haveLinkElement(
        "seeWhatHappensNext", routes.TaxCodeChangeController.whatHappensNext.url, messages("taxCode.change.yourTaxFreeAmount.whatHappensNext.link"))
    }
  }

  val personalAllowanceCatergory = TaxFreeAmountSummaryCategoryViewModel(
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

  def createDeductionsCatergory(deductionRows: Seq[TaxFreeAmountSummaryRowViewModel]) = TaxFreeAmountSummaryCategoryViewModel(
                                Messages("tai.taxFreeAmount.table.columnOneHeader"),
                                Messages("tai.taxFreeAmount.table.columnTwoHeader"),
                                hideHeaders = true,
                                hideCaption = false,
                                Messages("tai.taxFreeAmount.table.deductions.caption"),
                                deductionRows)

  def createTotalCatergory(totalAmountFormatted: String) = TaxFreeAmountSummaryCategoryViewModel(
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
                                                  personalAllowanceCatergory,
                                                  createAdditionsCatetgory(additionRows),
                                                  createDeductionsCatergory(deductionRows),
                                                  createTotalCatergory(totalFormattedAmount)
                                              ))

  }

  val taxFreeAmountSummaryViewModel: TaxFreeAmountSummaryViewModel = createTaxFreeAmountSummaryViewModel()

  private def createViewModel(taxCodeDateRange: String = "",
                              annualTaxFreeAmount: BigDecimal = 1150,
                              taxFreeAmountSummary: TaxFreeAmountSummaryViewModel = taxFreeAmountSummaryViewModel): YourTaxFreeAmountViewModel = {

    YourTaxFreeAmountViewModel(
      TaxFreeInfo(taxCodeDateRange, 0, 0),
      TaxFreeInfo(taxCodeDateRange, annualTaxFreeAmount, 0),
      taxFreeAmountSummary,
      Seq.empty,
      Seq.empty)
  }

  val currentTaxFreeAmount: YourTaxFreeAmountViewModel = createViewModel()


  def createView(viewModel: YourTaxFreeAmountViewModel = currentTaxFreeAmount) =
    views.html.taxCodeChange.yourTaxFreeAmount(viewModel)

  override def view = createView()
}

