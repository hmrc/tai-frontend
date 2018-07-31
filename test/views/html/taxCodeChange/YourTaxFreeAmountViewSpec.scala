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
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
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

      doc(viewP2Date) must haveH2HeadingWithText(expectedMessage)

      def viewP2Date: Html = views.html.taxCodeChange.yourTaxFreeAmount(createViewModel(expectedDateRange))
    }


    "display figure for Your tax-free amount" in {
      val taxFreeAmount = "£11,500"

      doc(viewTaxFreeAmount) must haveSpanWithText(taxFreeAmount)

      def viewTaxFreeAmount: Html = views.html.taxCodeChange.yourTaxFreeAmount(createViewModel(annualTaxFreeAmount = taxFreeAmount))
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

      "contains a heading for the addition and deduction group" in {
        doc must haveElementAtPathWithText("h3#summaryTable2Caption", "Additions to your Personal Allowance")
        doc must haveElementAtPathWithText("h3#summaryTable3Caption", "Deductions from your Personal Allowance")
      }

      "does not contain a stand alone heading for the personal allowance and total groups (which are of length 1)" in {
        doc must not(haveElementAtPathWithId("h3", "summaryTable1Caption"))
        doc must not(haveElementAtPathWithId("h3", "summaryTable4Caption"))
      }

      "contains an inline heading for the personal allowance and total groups (which are of length 1)" in {
        doc must haveElementAtPathWithText("h3#summaryTable1Row1-header", "Personal Allowance")
        doc must haveElementAtPathWithText("h3#summaryTable4Row1-header", "Your total tax-free amount")
      }

      "displays a group with multiple rows as an unordered list of items" in {
        doc must haveElementAtPathWithId("#yourTaxFreeAmount ul", "summaryTable2Body")
      }

      "displays a group with a single row as a plain div" in {
        doc must not(haveElementAtPathWithId("#yourTaxFreeAmount ul", "summaryTable3Body"))
        doc must haveElementAtPathWithId("#yourTaxFreeAmount div", "summaryTable3Body")
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

    "have a 'check what happens next' button" in {

      doc must haveLinkElement(
        "checkWhatHappensNext",
        routes.TaxCodeChangeController.whatHappensNext.url,
        messages("taxCode.change.yourTaxFreeAmount.whatHappensNext.link"))
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

  val rowViewModels: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
    TaxFreeAmountSummaryRowViewModel("An example addition benefit", "£11,500", ChangeLinkViewModel(true, "context1", "/dummy/url1")),
    TaxFreeAmountSummaryRowViewModel("Some Other Allowance", "£12,322", ChangeLinkViewModel(true, "context2", "/dummy/url2")),
    TaxFreeAmountSummaryRowViewModel("Blah Blah Random Extra Row Content", "£11,111", ChangeLinkViewModel(false))
  )

  val taxFreeAmountSummaryViewModel: TaxFreeAmountSummaryViewModel =
    TaxFreeAmountSummaryViewModel(Seq(
      personalAllowanceCatergory,
      TaxFreeAmountSummaryCategoryViewModel("header3", "header4", true, false, "Additions to your Personal Allowance", rowViewModels),
      TaxFreeAmountSummaryCategoryViewModel("header5", "header6", true, false, "Deductions from your Personal Allowance", Seq(TaxFreeAmountSummaryRowViewModel("An example single deduction benefit", "£12,300", ChangeLinkViewModel(true, "context1", "/dummy/url1")))),
      TaxFreeAmountSummaryCategoryViewModel("header7", "header8", true, true, messages("tai.taxFreeAmount.table.totals.caption"), Seq(TaxFreeAmountSummaryRowViewModel("Your total tax-free amount", "£11,500", ChangeLinkViewModel(false))))
    ))

  val taxFreeAmountSummaryVM = TaxFreeAmountSummaryViewModel(Seq.empty[TaxFreeAmountSummaryCategoryViewModel])

  private def createViewModel(taxCodeDateRange: String = "",
                              annualTaxFreeAmount: String = "",
                              taxFreeAmountSummary: TaxFreeAmountSummaryViewModel = taxFreeAmountSummaryViewModel): YourTaxFreeAmountViewModel = {
    YourTaxFreeAmountViewModel(taxCodeDateRange, annualTaxFreeAmount, taxFreeAmountSummary)
  }

  override def view = views.html.taxCodeChange.yourTaxFreeAmount(createViewModel())
}
