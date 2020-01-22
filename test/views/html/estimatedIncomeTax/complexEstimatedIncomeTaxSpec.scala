/*
 * Copyright 2020 HM Revenue & Customs
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

package views.html.estimatedIncomeTax

import controllers.routes
import play.twirl.api.Html
import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax._

class complexEstimatedIncomeTaxSpec extends TaiViewSpec {

  "Estimated Income Tax Page" must {
    behave like pageWithCombinedHeader(
      messages(
        "tai.taxYear",
        Dates.formatDate(TaxYear().start).replace(" ", "\u00A0"),
        Dates.formatDate(TaxYear().end).replace(" ", "\u00A0")),
      messages("tai.estimatedIncome.title"),
      Some(messages("tai.estimatedIncome.accessiblePreHeading"))
    )

    behave like pageWithBackLink

    "have a heading for the Total estimated Income" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.totalEstimatedIncome.subheading") + " £48,000")
    }

    "have a heading for the Income tax estimate" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.incomeTaxEstimate.subheading") + " £15,000")
    }

    "have view detailed Income tax estimate button" in {

      doc(view) must haveLinkElement(
        "detailEstimateView",
        routes.DetailedIncomeTaxEstimateController.taxExplanationPage.url,
        messages("tai.estimatedIncome.detailedEstimate.Link"))

    }

    "show iform links" in {
      doc.select("#iForms").text() mustBe "Test"
    }

    "display navigational links to other pages in the service" in {
      doc must haveElementAtPathWithText("nav>h2", messages("tai.taxCode.sideBar.heading"))
      doc must haveLinkElement(
        "taxCodesSideLink",
        routes.YourTaxCodeController.taxCodes.url,
        messages("check.your.tax.codes"))
      doc must haveLinkElement(
        "taxFreeAmountSideLink",
        routes.TaxFreeAmountController.taxFreeAmount.url,
        messages("check.your.tax.free.amount"))
      doc must haveLinkElement(
        "taxSummarySideLink",
        controllers.routes.TaxAccountSummaryController.onPageLoad.url,
        messages("return.to.your.income.tax.summary"))
    }

  }

  val bandedGraph = BandedGraph("taxGraph", Nil, 0, 0, 0, 0, 0, 0, 0, None, None)

  val taxBands = List(
    TaxBand("PSR", "", income = 3000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
    TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
    TaxBand("D0", "", income = 30000, tax = 12000, lowerBand = Some(32000), upperBand = Some(147000), rate = 40)
  )

  val viewModel = ComplexEstimatedIncomeTaxViewModel(15000, 48000, 11500, bandedGraph, "UK")

  override def view: Html =
    views.html.estimatedIncomeTax
      .complexEstimatedIncomeTax(viewModel, Html("<Html><head></head><body>Test</body></Html>"))
}
