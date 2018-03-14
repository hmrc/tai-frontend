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

package views.html

import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{ChangeLinkViewModel, TaxFreeAmountSummaryCategoryViewModel, TaxFreeAmountSummaryRowViewModel, TaxFreeAmountViewModelNew}

class TaxFreeAmountNewViewSpec extends TaiViewSpec {

  "Tax code view page" must {

    behave like pageWithTitle("main heading")
    behave like pageWithCombinedHeader(messages("tai.taxCode.preHeader"), "main heading")
    behave like pageWithBackLink

    "display a summary section" which {
      "contains a tax free amount explanation" in {
        doc must haveParagraphWithText(messages("tai.taxFreeAmount.summarysection.taxFreeExplanation"))
      }

      "contains title and highlighted amount" in {
        doc must haveSectionWithId("taxFreeAmountSummary")
        doc must haveElementAtPathWithText("section[id=taxFreeAmountSummary]>h2", s"${messages("tai.taxFreeAmount.summarysection.heading")} £2020")
        val summarySection = doc.select("section[id=taxFreeAmountSummary]")
        summarySection.select("span").get(1) must haveText("£2020")
      }

      "contains the correct text content" in {
        doc must haveElementAtPathWithText("section[id=taxFreeAmountSummary] p", messages("tai.taxFreeAmount.summarysection.p2"))
        doc must haveElementAtPathWithText("section[id=taxFreeAmountSummary] li", messages("tai.taxFreeAmount.summarysection.bullet1"))
        doc must haveElementAtPathWithText("section[id=taxFreeAmountSummary] li", messages("tai.taxFreeAmount.summarysection.bullet2"))
      }
    }

    "display a detail section" which {

      "contains a title" in {
        doc must haveSectionWithId("taxFreeAmountDetail")
        doc must haveElementAtPathWithText("section[id=taxFreeAmountDetail]>h2", messages("tai.taxFreeAmount.detailsection.heading"))
      }

      "contains one html table per summary category view model" in {
        doc.select("table").size() mustBe 4
        doc must haveTableWithId("summaryTable1")
        doc must haveTableWithId("summaryTable2")
        doc must haveTableWithId("summaryTable3")
        doc must haveTableWithId("summaryTable4")
      }

      "contains a header row per table" in {
        doc must haveTableTheadWithId("summaryTable1Head")
        doc must haveTableThWithIdAndText("summaryTable1Col1Header", "header1")
        doc must haveTableThWithIdAndText("summaryTable1Col2Header", "header2")

        doc must haveTableTheadWithId("summaryTable2Head")
        doc must haveTableThWithIdAndText("summaryTable2Col1Header", "header3")
        doc must haveTableThWithIdAndText("summaryTable2Col2Header", "header4")

        doc must haveTableTheadWithId("summaryTable3Head")
        doc must haveTableThWithIdAndText("summaryTable3Col1Header", "header5")
        doc must haveTableThWithIdAndText("summaryTable3Col2Header", "header6")

        doc must haveTableTheadWithId("summaryTable4Head")
        doc must haveTableThWithIdAndText("summaryTable4Col1Header", "header7")
        doc must haveTableThWithIdAndText("summaryTable4Col2Header", "header8")
      }

      "visually hides the table header row, where instructed by the view model" in {
        doc must not(haveElementAtPathWithAttribute("table[id=summaryTable1] thead", "class", "visuallyhidden"))
        doc must haveElementAtPathWithAttribute("table[id=summaryTable2] thead", "class", "visuallyhidden")
        doc must haveElementAtPathWithAttribute("table[id=summaryTable3] thead", "class", "visuallyhidden")
        doc must haveElementAtPathWithAttribute("table[id=summaryTable4] thead", "class", "visuallyhidden")
      }

      "contains a caption per table" in {
        doc must haveTableCaptionWithIdAndText("summaryTable1Caption", "Personal Allowance base amount")
        doc must haveTableCaptionWithIdAndText("summaryTable2Caption", "Additions to your Personal Allowance")
        doc must haveTableCaptionWithIdAndText("summaryTable3Caption", "Deductions from your Personal Allowance")
        doc must haveTableCaptionWithIdAndText("summaryTable4Caption", "Overall")
      }

      "visually hides the table caption, where instructed by the view model" in {
        doc must haveElementAtPathWithClass("caption[id=summaryTable1Caption]", "visuallyhidden")
        doc must not(haveElementAtPathWithClass("caption[id=summaryTable2Caption]", "visuallyhidden"))
        doc must not(haveElementAtPathWithClass("caption[id=summaryTable3Caption]", "visuallyhidden"))
        doc must haveElementAtPathWithClass("caption[id=summaryTable4Caption]", "visuallyhidden")
      }

      "displays a link cell & inner link element within table rows, where present in the view model" in {
        doc.select("tr[id=summaryTable1Row2] > *").size mustBe 3
        doc must haveTableTdWithId("summaryTable1Row2ChangeLinkCell")
        doc must haveLinkWithUrlWithID("summaryTable1Row2ChangeLink", "/dummy/url2")
        doc must haveElementAtPathWithClass("a[id=summaryTable1Row2ChangeLink] > span", "visuallyhidden")
        doc must haveElementAtPathWithText("a[id=summaryTable1Row2ChangeLink] > span", "context2")
      }

      "excludes a link cell from table rows, where instructed by the view model" in {
        doc.select("tr[id=summaryTable1Row3] > *").size mustBe 2
        doc must not(haveTableTdWithId("summaryTable1Row3ChangeLinkCell"))
      }

      "visually formats the final table" when {
        "the corresponding final summary item view model contains only a single row" in {
          doc.select("table[id=summaryTable4] tbody tr").size() mustBe 1
          doc must haveElementAtPathWithClass("table[id=summaryTable4]", "table--spaced-top")
          doc must haveElementAtPathWithClass("table[id=summaryTable4] tbody tr", "table__row--top-border table__footer--highlight highlight")
        }
      }
    }

    "display navigational links to other pages in the service" in {
      doc must haveElementAtPathWithText("nav>h2", messages("tai.taxCode.sideBar.heading"))
      doc must haveLinkElement("taxCodesLink", controllers.routes.YourTaxCodeController.taxCodes.url, messages("tai.incomeTax.taxCodes.link"))
      doc must haveLinkElement("taxableIncomeLink", controllers.routes.TaxAccountSummaryController.onPageLoad.url, messages("tai.incomeTaxSummary.link"))
    }

    "display navigational link to underpayment page if EstimatedTaxYouOweThisYear is present"in{
      val vm: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
        TaxFreeAmountSummaryRowViewModel("Estimated tax you owe this year", "£11,500", ChangeLinkViewModel(false)))
      val svm :Seq[TaxFreeAmountSummaryCategoryViewModel] = Seq(
        TaxFreeAmountSummaryCategoryViewModel("header1", "header2", true, false, "Deductions from your Personal Allowance", vm))
      val viewModel: TaxFreeAmountViewModelNew = TaxFreeAmountViewModelNew("main heading", "main heading", "£2020", svm)

      val document = doc(views.html.taxFreeAmountNew(viewModel))

      document must haveLinkElement("estimatedTaxOwedLink", controllers.routes.CurrentYearPageController.potentialUnderpaymentPage.url, messages("tai.taxFreeAmount.summarysection.EstimatedTaxYouOweThisYear"))
    }
  }

  val rowViewModels: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
    TaxFreeAmountSummaryRowViewModel("Personal Allowance", "£11,500", ChangeLinkViewModel(true, "context1", "/dummy/url1")),
    TaxFreeAmountSummaryRowViewModel("Some Other Allowance", "£12,322", ChangeLinkViewModel(true, "context2", "/dummy/url2")),
    TaxFreeAmountSummaryRowViewModel("Blah Blah Random Extra Row Content", "£11,111", ChangeLinkViewModel(false))
  )
  val summaryItemViewModels: Seq[TaxFreeAmountSummaryCategoryViewModel] = Seq(
    TaxFreeAmountSummaryCategoryViewModel("header1", "header2", false, true, "Personal Allowance base amount", rowViewModels),
    TaxFreeAmountSummaryCategoryViewModel("header3", "header4", true, false, "Additions to your Personal Allowance", rowViewModels),
    TaxFreeAmountSummaryCategoryViewModel("header5", "header6", true, false, "Deductions from your Personal Allowance", rowViewModels),
    TaxFreeAmountSummaryCategoryViewModel("header7", "header8", true, true, "Overall", Seq(TaxFreeAmountSummaryRowViewModel("Your total tax-free amount", "£11,500", ChangeLinkViewModel(false))))
  )
  val viewModel: TaxFreeAmountViewModelNew = TaxFreeAmountViewModelNew("main heading", "main heading", "£2020", summaryItemViewModels)

  override def view = views.html.taxFreeAmountNew(viewModel)
}
