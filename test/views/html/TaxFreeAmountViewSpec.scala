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

package views.html

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels._

class TaxFreeAmountViewSpec extends TaiViewSpec {
  val rowViewModels: Seq[TaxFreeAmountSummaryRowViewModel] = Seq(
    TaxFreeAmountSummaryRowViewModel(
      "An example addition benefit",
      "£11,500",
      ChangeLinkViewModel(isDisplayed = true, "context1", "/dummy/url1")
    ),
    TaxFreeAmountSummaryRowViewModel(
      "Some Other Allowance",
      "£12,322",
      ChangeLinkViewModel(isDisplayed = true, "context2", "/dummy/url2")
    ),
    TaxFreeAmountSummaryRowViewModel(
      "Blah Blah Random Extra Row Content",
      "£11,111",
      ChangeLinkViewModel(isDisplayed = false)
    )
  )

  val taxFreeAmountSummaryViewModel: TaxFreeAmountSummaryViewModel =
    TaxFreeAmountSummaryViewModel(
      Seq(
        TaxFreeAmountSummaryCategoryViewModel(
          "header1",
          "header2",
          hideHeaders = false,
          hideCaption = true,
          messages("tai.taxFreeAmount.table.allowances.caption"),
          Seq(
            TaxFreeAmountSummaryRowViewModel("Personal Allowance", "£11,500", ChangeLinkViewModel(isDisplayed = false))
          )
        ),
        TaxFreeAmountSummaryCategoryViewModel(
          "header3",
          "header4",
          hideHeaders = true,
          hideCaption = false,
          "Additions to your Personal Allowance",
          rowViewModels
        ),
        TaxFreeAmountSummaryCategoryViewModel(
          "header5",
          "header6",
          hideHeaders = true,
          hideCaption = false,
          "Deductions from your Personal Allowance",
          Seq(
            TaxFreeAmountSummaryRowViewModel(
              "An example single deduction benefit",
              "£12,300",
              ChangeLinkViewModel(isDisplayed = true, "context1", "/dummy/url1")
            )
          )
        ),
        TaxFreeAmountSummaryCategoryViewModel(
          "header7",
          "header8",
          hideHeaders = true,
          hideCaption = true,
          messages("tai.taxFreeAmount.table.totals.caption"),
          Seq(
            TaxFreeAmountSummaryRowViewModel(
              "Your total tax-free amount",
              "£11,500",
              ChangeLinkViewModel(isDisplayed = false)
            )
          )
        )
      )
    )

  val viewModel: TaxFreeAmountViewModel =
    TaxFreeAmountViewModel("main heading", "main heading", "£2020", taxFreeAmountSummaryViewModel)

  private val template = inject[TaxFreeAmountView]

  override def view: Html = template(viewModel, appConfig, "test user")

  "Tax free amount view page" must {

    behave like pageWithTitle("main heading")
    behave like pageWithCombinedHeaderNewFormatNew(messages("tai.taxCode.preHeader"), "main heading")
    behave like pageWithBackLink()

    "display a summary section" which {
      "contains a tax free amount explanation" in {
        doc must haveParagraphWithText(messages("tai.taxFreeAmount.summarysection.taxFreeExplanation"))
      }

      "contains title and highlighted amount" in {
        doc must haveDivWithId("taxFreeAmountSummary")
        doc must haveElementAtPathWithText(
          "div[id=taxFreeAmountSummary]>h2",
          messages("tai.taxFreeAmount.summarysection.heading")
        )
        val summarySection = doc.select("div[id=taxFreeAmountSummary]")
        summarySection.select("p").get(1) must haveText("£2020")
      }

      "contains the correct text content" in {
        doc must haveElementAtPathWithText(
          "div[id=taxFreeAmountSummary] p",
          messages("tai.taxFreeAmount.summarysection.p2")
        )
        doc must haveElementAtPathWithText(
          "div[id=taxFreeAmountSummary] li",
          messages("tai.taxFreeAmount.summarysection.bullet1")
        )
        doc must haveElementAtPathWithText(
          "div[id=taxFreeAmountSummary] li",
          messages("tai.taxFreeAmount.summarysection.bullet2")
        )
      }
    }

    "display a detail section" which {

      "contains a title" in {
        doc must haveDivWithId("taxFreeAmountDetail")
        doc must haveElementAtPathWithText(
          "div[id=taxFreeAmountDetail]>h2",
          messages("tai.taxFreeAmount.detailsection.heading")
        )
      }

      "contains one group per summary category view model" in {
        doc.select(".govuk-summary-list").size() mustBe 4
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
        val personalAllowanceRow = doc.select("#summaryTable1 .govuk-summary-list__row dt").text()
        val totalTaxFreeAmountRow = doc.select("#summaryTable4 .govuk-summary-list__row dt").text()

        personalAllowanceRow must include("Personal Allowance")
        totalTaxFreeAmountRow must include("Your total tax-free amount")
      }

      "displays a group with multiple rows as an unordered list of items" in {
        val dlElements = doc.select("#summaryTable2 .govuk-summary-list")
        dlElements.size() mustNot be(0)
      }

      "displays a link & inner link element, where present in the view model" in {
        doc must haveLinkWithTextAndUrl("Update or remove", "/dummy/url1")

        val link = doc.select("a.govuk-link[href='/dummy/url1']").first()
        link must not be null

        val span = link.select("span.govuk-visually-hidden").first()
        span must not be null
        span.text() mustBe "context1"
      }

      "excludes a link cell from table rows, where instructed by the view model" in {
        doc must not(haveElementWithId("summaryTable2Row3ChangeLinkCell"))
      }

      "visually formats the final table" when {
        "the corresponding final summary item view model contains only a single row" in {
          doc.select("#summaryTable4 .govuk-summary-list__key").size() mustBe 1

        }
      }
    }

    "display navigational links to other pages in the service" in {
      doc must haveElementAtPathWithText("nav>h2", messages("tai.taxCode.sideBar.heading"))
      doc must haveLinkElement(
        "taxCodesLink",
        controllers.routes.YourTaxCodeController.taxCodes().url,
        messages("check.your.tax.codes")
      )
      doc must haveLinkElement(
        "incomeTaxEstimateLink",
        controllers.routes.EstimatedIncomeTaxController.estimatedIncomeTax().url,
        messages("check.your.income.tax.estimate")
      )
      doc must haveLinkElement(
        "taxableIncomeLink",
        controllers.routes.TaxAccountSummaryController.onPageLoad().url,
        messages("return.to.your.income.tax.summary")
      )
    }

    "display navigational link to 'Estimated tax you owe' page if EstimatedTaxYouOweThisYear is present" in {
      val labelVm = TaxSummaryLabel("labelText", Some(HelpLink("testValue", "testHref", "estimatedTaxOwedLink")))
      val vm: Seq[TaxFreeAmountSummaryRowViewModel] =
        Seq(TaxFreeAmountSummaryRowViewModel(labelVm, "£11,500", ChangeLinkViewModel(isDisplayed = false)))
      val svm: TaxFreeAmountSummaryViewModel = TaxFreeAmountSummaryViewModel(
        Seq(
          TaxFreeAmountSummaryCategoryViewModel(
            "header1",
            "header2",
            hideHeaders = true,
            hideCaption = false,
            "Deductions from your Personal Allowance",
            vm
          )
        )
      )
      val viewModel: TaxFreeAmountViewModel = TaxFreeAmountViewModel("main heading", "main heading", "£2020", svm)

      val document = doc(template(viewModel, appConfig, "test user"))

      document must haveLinkElement("estimatedTaxOwedLink", "testHref", "View underpayments")
    }

    "display a 'something missing' section" which {
      "contains a heading" in {
        doc must haveElementAtPathWithText("h2", messages("tai.incomeTaxSummary.addMissingIncome.section.heading"))
      }
      "includes a link to add a missing allowance or addition" in {
        doc must haveLinkWithUrlWithID("addMissingAddition", appConfig.taxFreeAllowanceLinkUrl)
      }
      "includes a link to add a missing company benefit" in {
        doc must haveLinkWithUrlWithID("addMissingDeduction", appConfig.companyBenefitsLinkUrl)
      }
      "includes a link to add a missing income" in {
        doc must haveLinkWithUrlWithID("addMissingIncome", appConfig.otherIncomeLinkUrl)
      }
    }
  }

}
