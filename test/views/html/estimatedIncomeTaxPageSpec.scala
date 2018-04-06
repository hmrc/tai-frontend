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

import controllers.routes
import hmrc.nps2
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.{IabdSummary, TaxComponent}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{AdditionalTaxRow, Band, BandedGraph, EstimatedIncomeViewModel}
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link

class estimatedIncomeTaxPageSpec extends TaiViewSpec {

  "EstimatedIncomeTax Page" should {

    "show tax relief message" in {
      val doc = Jsoup.parseBodyFragment(view.toString())
      doc.select("#tax-relief-title").text() mustBe Messages("tai.estimatedIncome.taxrelief.title")
      doc.select("#tax-relief-message").html() mustBe Html(Messages("tai.estimatedIncome.taxRelief", Link.toInternalPage(
        url = routes.CurrentYearPageController.reliefsPage().toString,
        value = Some("tai.estimatedIncome.taxRelief.link")
      ).toHtml)).body
    }

    "not show tax relief message" in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, taxRelief = false,
        newGraph = graphData, incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = None, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#tax-relief-title").size() mustBe 0
      doc.select("#tax-relief-message").size() mustBe 0
    }

    "show potential underpayment message" in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, potentialUnderpayment = true,
        newGraph = graphData, incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = None, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#income-potential-underpayment").text() mustBe Messages("tai.potentialUnderpayment.title")
      doc.select("#link-potential-underpayment").size() mustBe 1
      doc.select("#link-potential-underpayment a").attr("data-journey-click") mustBe
        s"link - click:${Messages("tai.estimatedIncomeTax.howWeWorkedOut")}:${Messages("tai.view.PotentialUnderpayment")}."

    }

    "not show potential underpayment message" in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, potentialUnderpayment = false,
        newGraph = graphData, incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = None, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#income-potential-underpayment").size() mustBe 0
      doc.select("#link-potential-underpayment").size() mustBe 0
    }

    "show Personal Savings Allowance and SSR messages" in {

      val bands = List(
        Band("TaxFree", 25.58, "0%", 11000, 0, "pa"),
        Band("TaxFree", 6.97, "0%", 3000, 0, "SR"),
        Band("TaxFree", 34.88, "0%", 15000, 0, "SDR"),
        Band("TaxFree", 34.88, "0%", 1000, 0, "PSR")
      )
      val nextBandMessage = "You can have £14,000 more before your income reaches the next tax band."
      val graphData = BandedGraph("taxGraph", bands, 0, 43000, 29000, 67.43, 29000, 67.43, 0, Some(nextBandMessage))
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, hasSSR = true, hasPSA = true, newGraph = graphData,
        incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = None, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#bandType0").text() mustBe "Tax-free allowance"
      doc.select("#bandType1").text() mustBe "Starting rate for savings"

      doc.select("#starting-rate-savings-title").text() mustBe Messages("tai.estimatedIncome.SSR.title")

      doc.select("#starting-rate-savings-text1").text() mustBe Messages("tai.estimatedIncome.SSR.text1", viewModel.ssrValue)

      doc.select("#personal-savings-allowance-title").text() mustBe Messages("tai.estimatedIncome.PSA.title")

      doc.select("#personal-savings-allowance-text1").html() mustBe Html(Messages("tai.estimatedIncome.PSA.text1",
        viewModel.psaValue,
        Link.toExternalPage(url = "https://www.gov.uk/apply-tax-free-interest-on-savings",
          value = Some(Messages("tai.estimatedIncome.PSA.linkText"))).toHtml)).body
    }

    "not show PSA and SSR messages" in {

      val bands = List(
        Band("TaxFree", 10.00, "0%", 3200, 0, "pa"),
        Band("Band", 50.00, "20%", 16000, 5000, "B")
      )
      val nextBandMessage = "You can have £12,800 more before your income reaches the next tax band."
      val graphData = createGraphData(Some(nextBandMessage))
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, hasSSR = false, hasPSA = false, newGraph = graphData,
        incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = None, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())

      doc.select("#starting-rate-savings-title").size() mustBe 0

      doc.select("#starting-rate-savings-text1").size() mustBe 0

      doc.select("#starting-rate-savings-text2").size() mustBe 0

      doc.select("#personal-savings-allowance-title").size() mustBe 0

      doc.select("#personal-savings-allowance-text1").size() mustBe 0

      doc.select("#personal-savings-allowance-text2").size() mustBe 0

    }

    "show dividends allowance message" in {
      val taxBands = createTaxBands(Some("SDR"))

      val dividends = TaxComponent(amount = BigDecimal(4000), componentType = 0, description = "",
        iabdSummaries = List(IabdSummary(iabdType = 76, description = "UK Dividend", amount = 4000)))

      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, newGraph = graphData,
        incomeTaxReducedToZeroMessage = None, ukDividends = Some(dividends), taxBands = Some(taxBands), taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#dividends-allowance-title").text() mustBe "Dividend Allowance"
      doc.select("#dividendZeroRateMessage").size() mustBe 1
    }

    "not show dividends allowance message" in {
      val taxBands = createTaxBands(None)

      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, newGraph = graphData,
        incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = Some(taxBands), taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#dividends-allowance-title").size() mustBe 0
      doc.select("#dividendZeroRateMessage").size() mustBe 0
    }

    "not show additional tax due table section" in {
      val taxBands = createTaxBands(None)

      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, newGraph = graphData, additionalTaxTable = Nil,
        incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = Some(taxBands), taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#additionalTaxTable").size() mustBe 0
      doc.select("#additionalTaxTable-heading").size() mustBe 0
      doc.select("#additionalTaxTable-desc").size() mustBe 0
    }

    "show additional tax due table section" in {
      val taxBands = createTaxBands(None)

      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, newGraph = graphData,
        additionalTaxTable = List(("Tax", "amount")),
        additionalTaxTableV2 = List(AdditionalTaxRow(description = "TaxDescription", amount = "amount", url = Some("http://url.to/test"))),
        incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = Some(taxBands), taxRegion = "")

      val htmlString = views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString()
      val doc = Jsoup.parseBodyFragment(htmlString)
      doc.select("#additionalTaxTable").size() mustBe 1
      doc.select("#additionalTaxTable-heading").text mustBe Messages("tai.estimatedIncome.additionalTax.title")
      doc.select("#additionalTaxTable-desc").text() mustBe Messages("tai.estimatedIncome.additionalTax.desc")
      doc.getElementsMatchingOwnText("TaxDescription").hasAttr("data-journey-click") mustBe false
      htmlString.contains("http://url.to/test") mustBe true
    }

    "not show tax reductions table section" in {
      val taxBands = createTaxBands(None)
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, newGraph = graphData, reductionsTable = Nil,
        incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = Some(taxBands), taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#taxPaidElsewhereTable").size() mustBe 0
      doc.select("#taxPaidElsewhereTable-heading").size() mustBe 0
      doc.select("#taxPaidElsewhereTable-desc").size() mustBe 0
    }

    "show tax reductions table section" in {
      val taxBands = createTaxBands(None)

      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, newGraph = graphData,
        reductionsTable = List(("Tax", "amount", "type")),
        incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = Some(taxBands), taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#taxPaidElsewhereTable").size() mustBe 1
      doc.select("#taxPaidElsewhereTable-heading").text() mustBe Messages("tai.estimatedIncome.reductionsTax.title")
      doc.select("#taxPaidElsewhereTable-desc").text() mustBe Messages("tai.estimatedIncome.reductionsTax.desc")
    }

    "show income tax reduced to zero message " in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true,
        reductionsTable = List(("", "", "")), newGraph = graphData,
        incomeTaxReducedToZeroMessage = Some(""), ukDividends = None, taxBands = None, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#reducedToZero").size() mustBe 1

    }

    "not show income tax reduced to zero message " in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, reductionsTable = Nil, newGraph = graphData,
        incomeTaxReducedToZeroMessage = Some(""), ukDividends = None, taxBands = None, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#reducedToZero").size() mustBe 0

    }

    "show no increases tax message with no currentyear amount" in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(reductionsTable = Nil, newGraph = graphData,
        incomeTaxReducedToZeroMessage = Some(""), ukDividends = None, taxBands = None, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc.select("#no-increase-tax").size() mustBe 1
      doc.select("#currentYearAmount").size() mustBe 0
    }

    "show estimated income text" in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, reductionsTable = Nil, newGraph = graphData,
        incomeTaxReducedToZeroMessage = Some(""), ukDividends = None, taxBands = None, incomeEstimate = 100, taxFreeEstimate = 100, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())

      val expectedTaxYearString =  Messages("tai.taxYear",
        nonBreakable(Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear)),
        nonBreakable(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)) )

      doc.select("#estimatedIncomeDesc").text() mustBe Messages("tai.estimatedIncome.desc",
        "£100",
        expectedTaxYearString,
        "£100")
    }

    "show correct header content" in {

      val accessiblePreHeading = doc.select("""header span[class="visuallyhidden"]""")
      accessiblePreHeading.text mustBe Messages("tai.estimatedIncome.accessiblePreHeading")

      val expectedTaxYearString =  Messages("tai.taxYear",
        nonBreakable(Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear)),
        nonBreakable(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)) )

      val preHeading = doc.select("header p")
      preHeading.text mustBe s"${Messages("tai.estimatedIncome.accessiblePreHeading")} ${expectedTaxYearString}"
    }

    "show back link" in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, reductionsTable = Nil, newGraph = graphData,
        incomeTaxReducedToZeroMessage = Some(""), ukDividends = None, taxBands = None, incomeEstimate = 100, taxFreeEstimate = 100, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("")).toString())
      doc must haveBackLink
    }

    "show iform links" in {
      val graphData = createGraphData(None)
      val viewModel = EstimatedIncomeViewModel(increasesTax = true, reductionsTable = Nil, newGraph = graphData,
        incomeTaxReducedToZeroMessage = Some(""), ukDividends = None, taxBands = None, incomeEstimate = 100, taxFreeEstimate = 100, taxRegion = "")

      val doc = Jsoup.parseBodyFragment(views.html.estimatedIncomeTax(viewModel, iFormLinks = Html("<Html><head></head><body>Test</body></Html>")).toString())

      doc.select("#iForms").text() mustBe "Test"
    }
  }

  private val createGraphData = (nextBandMessage: Option[String]) => BandedGraph("taxGraph", Nil, 0, 32000, 19200, 10.00, 3200, 60.00, 5000, nextBandMessage)
  private val createTaxBands = (bandType: Option[String]) => List(nps2.TaxBand(bandType = bandType, code = None, income = BigDecimal(0),
    tax = BigDecimal(0), lowerBand = Some(BigDecimal(0)), upperBand = Some(BigDecimal(5000)), rate = BigDecimal(0)))
  private val graph = createGraphData(None)
  private val vm = EstimatedIncomeViewModel(increasesTax = true, taxRelief = true,
    newGraph = graph, incomeTaxReducedToZeroMessage = None, ukDividends = None, taxBands = None, taxRegion = "")

  override def view: Html = views.html.estimatedIncomeTax(vm, iFormLinks = Html(""))
}