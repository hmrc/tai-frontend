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

      "contains one group per summary category view model" in {
        doc.select(".govuk-check-your-answers").size() mustBe 4
        doc must haveElementWithId("summaryTable1")
        doc must haveElementWithId("summaryTable2")
        doc must haveElementWithId("summaryTable3")
        doc must haveElementWithId("summaryTable4")
      }

      "contains a heading per group" in {
        doc must haveElementAtPathWithText("#summaryTable1Caption", "Personal Allowance base amount")
        doc must haveElementAtPathWithText("#summaryTable2Caption", "Additions to your Personal Allowance")
        doc must haveElementAtPathWithText("#summaryTable3Caption", "Deductions from your Personal Allowance")
      }

      "displays a link & inner link element, where present in the view model" in {
        doc must haveElementWithId("summaryTable1Row2ChangeLinkCell")
        doc must haveLinkWithUrlWithID("summaryTable1Row2ChangeLink", "/dummy/url2")
        doc must haveElementAtPathWithClass("a[id=summaryTable1Row2ChangeLink] > span", "visually-hidden")
        doc must haveElementAtPathWithText("a[id=summaryTable1Row2ChangeLink] > span", messages("tai.updateOrRemove") + " context2")
      }

      "excludes a link cell from table rows, where instructed by the view model" in {
        doc must not(haveElementWithId("summaryTable1Row3ChangeLinkCell"))
      }

      "visually formats the final table" when {
        "the corresponding final summary item view model contains only a single row" in {
          doc.select("#summaryTable4 .cya-question").size() mustBe 1
          doc must haveElementAtPathWithClass("#summaryTable4 .govuk-check-your-answers", "highlight")
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

      document must haveLinkElement("estimatedTaxOwedLink", controllers.routes.PotentialUnderpaymentController.potentialUnderpaymentPage.url, messages("tai.taxFreeAmount.summarysection.EstimatedTaxYouOweThisYear"))
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
