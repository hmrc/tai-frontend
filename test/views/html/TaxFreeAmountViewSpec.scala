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

import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.TaiConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels._

class TaxFreeAmountViewSpec extends TaiViewSpec {

  "Tax free amount view page" must {

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
        doc must haveElementAtPathWithId("#taxFreeAmountDetail ul", "summaryTable2Body")
      }

      "displays a group with a single row as a plain div" in {
        doc must not(haveElementAtPathWithId("#taxFreeAmountDetail ul", "summaryTable3Body"))
        doc must haveElementAtPathWithId("#taxFreeAmountDetail div", "summaryTable3Body")
      }

      "displays a link & inner link element, where present in the view model" in {
        doc must haveElementWithId("summaryTable2Row1ChangeLinkCell")
        doc must haveLinkWithUrlWithID("summaryTable2Row1ChangeLink", "/dummy/url1")
        doc must haveElementAtPathWithClass("a[id=summaryTable2Row1ChangeLink] > span", "visually-hidden")
        doc must haveElementAtPathWithText("a[id=summaryTable2Row1ChangeLink] > span", messages("tai.updateOrRemove") + " context1")
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

    "display navigational links to other pages in the service" in {
      doc must haveElementAtPathWithText("nav>h2", messages("tai.taxCode.sideBar.heading"))
      doc must haveLinkElement("taxCodesLink", controllers.routes.YourTaxCodeController.taxCodes.url, messages("tai.incomeTax.taxCodes.link"))
      doc must haveLinkElement("taxableIncomeLink", controllers.routes.TaxAccountSummaryController.onPageLoad.url, messages("tai.incomeTaxSummary.link"))
    }

    "display navigational link to 'Estimated tax you owe' page if EstimatedTaxYouOweThisYear is present" in {
      val labelVm = Label("labelText", Some(HelpLink("testValue", "testHref", "estimatedTaxOwedLink")))
      val vm: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
        TaxFreeAmountSummaryRowViewModel(labelVm, "£11,500", ChangeLinkViewModel(false)))
      val svm: Seq[TaxFreeAmountSummaryCategoryViewModel] = Seq(
        TaxFreeAmountSummaryCategoryViewModel("header1", "header2", true, false, "Deductions from your Personal Allowance", vm))
      val viewModel: TaxFreeAmountViewModel = TaxFreeAmountViewModel("main heading", "main heading", "£2020", svm)

      val document = doc(views.html.taxFreeAmount(viewModel))

      document must haveLinkElement("estimatedTaxOwedLink", "testHref", "testValue")
    }

    "display a 'something missing' section" which {
      "contains a heading" in {
        doc must haveElementAtPathWithText("h2", messages("tai.incomeTaxSummary.addMissingIncome.section.heading"))
      }
      "includes a link to add a missing allowance or addition" in {
          doc must haveLinkWithUrlWithID("addMissingAddition", ApplicationConfig.taxFreeAllowanceLinkUrl)
        }
      "includes a link to add a missing company benefit" in {
        doc must haveLinkWithUrlWithID("addMissingDeduction", ApplicationConfig.companyBenefitsLinkUrl)
        }
      "includes a link to add a missing income" in {
        doc must haveLinkWithUrlWithID("addMissingIncome", ApplicationConfig.otherIncomeLinkUrl)
        }
      }
    }


  val rowViewModels: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
    TaxFreeAmountSummaryRowViewModel("An example addition benefit", "£11,500", ChangeLinkViewModel(true, "context1", "/dummy/url1")),
    TaxFreeAmountSummaryRowViewModel("Some Other Allowance", "£12,322", ChangeLinkViewModel(true, "context2", "/dummy/url2")),
    TaxFreeAmountSummaryRowViewModel("Blah Blah Random Extra Row Content", "£11,111", ChangeLinkViewModel(false))
  )
  val summaryItemViewModels: Seq[TaxFreeAmountSummaryCategoryViewModel] = Seq(
    TaxFreeAmountSummaryCategoryViewModel("header1", "header2", false, true, messages("tai.taxFreeAmount.table.allowances.caption"), Seq(TaxFreeAmountSummaryRowViewModel("Personal Allowance" , "£11,500", ChangeLinkViewModel(false)))),
    TaxFreeAmountSummaryCategoryViewModel("header3", "header4", true, false, "Additions to your Personal Allowance", rowViewModels),
    TaxFreeAmountSummaryCategoryViewModel("header5", "header6", true, false, "Deductions from your Personal Allowance", Seq(TaxFreeAmountSummaryRowViewModel("An example single deduction benefit", "£12,300", ChangeLinkViewModel(true, "context1", "/dummy/url1")))),
    TaxFreeAmountSummaryCategoryViewModel("header7", "header8", true, true, messages("tai.taxFreeAmount.table.totals.caption"), Seq(TaxFreeAmountSummaryRowViewModel("Your total tax-free amount", "£11,500", ChangeLinkViewModel(false))))
  )
  val viewModel: TaxFreeAmountViewModel = TaxFreeAmountViewModel("main heading", "main heading", "£2020", summaryItemViewModels)

  override def view = views.html.taxFreeAmount(viewModel)
}
