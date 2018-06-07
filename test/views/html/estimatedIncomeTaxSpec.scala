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
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{AdditionalTaxDetailRow, BandedGraph, EstimatedIncomeTaxViewModel, ReductionTaxRow}
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link
import uk.gov.hmrc.play.views.formatting.Money._

class estimatedIncomeTaxSpec extends TaiViewSpec {


  "Estimated Income Tax Page" must {
    behave like pageWithCombinedHeader(
      messages(
        "tai.taxYear",
        Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear).replace(" ", "\u00A0"),
        Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear).replace(" ", "\u00A0")),
        messages("tai.estimatedIncome.title"),
        Some(messages("tai.estimatedIncome.accessiblePreHeading")
      )
    )

    behave like pageWithBackLink

    "have a heading for the Income tax estimate" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.incomeTaxEstimate.subheading") + " £100")
    }

    "have a heading for the Total estimated Income" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.totalEstimatedIncome.subheading") + " £100")
    }

    "have static messages" in {
      doc(view) must haveParagraphWithText(Html(messages("tai.estimatedIncome.desc",
        "£100",
        viewModel.currentTaxYearRangeHtmlNonBreak,
        "£100")).body)
    }

    "have potential underpayment" in {
      doc.select("#income-potential-underpayment").text() mustBe Messages("tai.potentialUnderpayment.title")
      doc.select("#link-potential-underpayment").size() mustBe 1
      doc.select("#link-potential-underpayment a").attr("data-journey-click") mustBe
        s"link - click:${Messages("tai.estimatedIncomeTax.howWeWorkedOut")}:${Messages("tai.view.PotentialUnderpayment")}."
    }

    "have ssr and psr and dividends message" in {
      doc.select("#starting-rate-savings-title").text() mustBe Messages("tai.estimatedIncome.SSR.title")
      doc.select("#starting-rate-savings-text1").text() mustBe Messages("tai.estimatedIncome.SSR.text1", viewModel.ssrValue.getOrElse(0))

      doc.select("#personal-savings-allowance-title").text() mustBe Messages("tai.estimatedIncome.PSA.title")
      doc.select("#personal-savings-allowance-text1").html() mustBe Html(Messages("tai.estimatedIncome.PSA.text1",
        viewModel.psrValue.getOrElse(0),
        Link.toExternalPage(url = "https://www.gov.uk/apply-tax-free-interest-on-savings",
          value = Some(Messages("tai.estimatedIncome.PSA.linkText"))).toHtml)).body

      doc must haveParagraphWithText(viewModel.dividendsMessage.getOrElse(""))
      doc must haveHeadingH3WithText(messages("tai.estimatedIncome.DIV.title"))
    }

    "have additional tax table" in {
      val additionalRows = Seq(
        AdditionalTaxDetailRow(Messages("tai.taxCalc.UnderpaymentPreviousYear.title"), 100, None),
        AdditionalTaxDetailRow(Messages("tai.taxcode.deduction.type-45"), 50, Some(routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url)),
        AdditionalTaxDetailRow(Messages("tai.taxCalc.OutstandingDebt.title"), 150, None),
        AdditionalTaxDetailRow(Messages("tai.taxCalc.childBenefit.title"), 300, None),
        AdditionalTaxDetailRow(Messages("tai.taxCalc.excessGiftAidTax.title"), 100, None),
        AdditionalTaxDetailRow(Messages("tai.taxCalc.excessWidowsAndOrphans.title"), 100, None),
        AdditionalTaxDetailRow(Messages("tai.taxCalc.pensionPaymentsAdjustment.title"), 200, None)
      )
      val model = createViewModel(true, additionalRows, Seq.empty[ReductionTaxRow])

      def additionalDetailView: Html = views.html.estimatedIncomeTax(model, Html("<title/>"))


      doc(additionalDetailView).select("#additionalTaxTable").size() mustBe 1
      doc(additionalDetailView).select("#additionalTaxTable-heading").text mustBe Messages("tai.estimatedIncome.additionalTax.title")
      doc(additionalDetailView).select("#additionalTaxTable-desc").text() mustBe Messages("tai.estimatedIncome.additionalTax.desc")
      doc(additionalDetailView).getElementsMatchingOwnText("TaxDescription").hasAttr("data-journey-click") mustBe false
    }

    "have reduction tax table" in {
      val reductionTaxRows = Seq(
        ReductionTaxRow(Messages("tai.taxCollected.atSource.otherIncome.description"), 100, Messages("tai.taxCollected.atSource.otherIncome.title")),
        ReductionTaxRow(Messages("tai.taxCollected.atSource.dividends.description", 10), 200, Messages("tai.taxCollected.atSource.dividends.title")),
        ReductionTaxRow(Messages("tai.taxCollected.atSource.bank.description", 20), 100, Messages("tai.taxCollected.atSource.bank.title"))
      )

      val model = createViewModel(true, Seq.empty[AdditionalTaxDetailRow], reductionTaxRows)

      def reductionTaxDetailView: Html = views.html.estimatedIncomeTax(model, Html("<title/>"))

      doc(reductionTaxDetailView).select("#taxPaidElsewhereTable").size() mustBe 1
      doc(reductionTaxDetailView).select("#taxPaidElsewhereTable-heading").text() mustBe Messages("tai.estimatedIncome.reductionsTax.title")
      doc(reductionTaxDetailView).select("#taxPaidElsewhereTable-desc").text() mustBe Messages("tai.estimatedIncome.reductionsTax.desc")
      doc(reductionTaxDetailView) must haveParagraphWithText(viewModel.incomeTaxReducedToZeroMessage.getOrElse(""))
    }

    "show no increases tax message" when {
      "there is no current income" in {
        val model = createViewModel(false, Seq.empty[AdditionalTaxDetailRow], Seq.empty[ReductionTaxRow])

        def noCurrentIncomeView: Html = views.html.estimatedIncomeTax(model, Html("<title/>"))

        doc(noCurrentIncomeView) must haveParagraphWithText(messages("tai.no.increasesTax"))
      }
    }

    "show iform links" in {
      doc.select("#iForms").text() mustBe "Test"
    }

    "show tax relief section" in {
      doc.select("#tax-relief-title").text() mustBe Messages("tai.estimatedIncome.taxrelief.title")
      doc.select("#tax-relief-message").html() mustBe Html(Messages("tai.estimatedIncome.taxRelief", Link.toInternalPage(
        url = routes.EstimatedIncomeTaxController.taxRelief().toString,
        value = Some("tai.estimatedIncome.taxRelief.link")
      ).toHtml)).body
    }
  }


  val bandedGraph = BandedGraph("taxGraph", Nil, 0, 0, 0, 0, 0, 0, 0, None)

  def createViewModel(hasCurrentIncome: Boolean,
                      additionalRows: Seq[AdditionalTaxDetailRow],
                      reductionRows: Seq[ReductionTaxRow]): EstimatedIncomeTaxViewModel = {
    EstimatedIncomeTaxViewModel(
      hasCurrentIncome, 100, 100, 100, bandedGraph, additionalRows, additionalRows.map(_.amount).sum,
      reductionRows, reductionRows.map(_.amount).sum, Some("Income Tax Reduced to Zero"), true, Some(100), Some(100), Some("Test"), "uk", true)
  }

  val viewModel = createViewModel(true, Seq.empty[AdditionalTaxDetailRow], Seq.empty[ReductionTaxRow])


  override def view: Html = views.html.estimatedIncomeTax(viewModel, Html("<Html><head></head><body>Test</body></Html>"))
}
