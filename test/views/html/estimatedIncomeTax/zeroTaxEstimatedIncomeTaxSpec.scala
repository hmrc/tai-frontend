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

package views.html.estimatedIncomeTax

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{Band, BandedGraph, SimpleEstimatedIncomeTaxViewModel, ZeroTaxEstimatedIncomeTaxViewModel}
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link

class zeroTaxEstimatedIncomeTaxSpec extends TaiViewSpec {
  "Zero Tax Estimated Income Tax Page" must {
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

    "have a heading for the Total estimated Income" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.totalEstimatedIncome.subheading") + " £9,000")
    }

    "have a heading for the Income tax estimate" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.incomeTaxEstimate.subheading") + " £0")
    }

    "have static messages" in {

      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.whyEstimate.link"))
      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.howYouPay.heading"))

      doc(view) must haveParagraphWithText(Html(messages("tai.estimatedIncome.whyEstimate.desc",
        TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM yyyy"))).body)

      doc(view) must haveParagraphWithText(Html(messages("tai.estimatedIncome.howYouPay.desc",
        messages("tai.estimatedIncome.taxCodes.link"))).body)

      doc(view).select("#howYouPayDesc").html() mustBe Html(messages("tai.estimatedIncome.howYouPay.desc",
        Link.toInternalPage(
          id = Some("taxCodesLink"),
          url = routes.YourTaxCodeController.taxCodes.url.toString,
          value = Some(Messages("tai.estimatedIncome.taxCodes.link"))).toHtml)).body
    }

    "have low estimated total income messages" when {
      "the earnings for a NINO were lower than the tax free allowance" in {
        doc(view) must haveParagraphWithText(Html(messages("tai.estimatedIncomeLow.desc",
          messages("tai.estimatedIncome.taxFree.link"),
          "£11,500 ")).body)

        doc(view).select("#estimatedIncomeLowDesc").html() mustBe Html(Messages("tai.estimatedIncomeLow.desc",
          Link.toInternalPage(
            id = Some("taxFreeAmountLink"),
            url = routes.TaxFreeAmountController.taxFreeAmount.url.toString,
            value = Some("tai.estimatedIncome.taxFree.link")
          ).toHtml,
          "£11,500 ")).body

        doc(view).select("#balanceEarningsDesc").html() mustBe Html(Messages("tai.estimatedIncomeEarning.desc",
          "£2,500")).body
      }
    }
  }


  val defaultViewModel = {
    val bandedGraph = BandedGraph("taxGraph", List.empty[Band], 0, 0, 0, 0, 0, 0, 0, None, None)
    ZeroTaxEstimatedIncomeTaxViewModel(0, 9000, 11500, bandedGraph, "taxRegion")
  }

  def view(vm: ZeroTaxEstimatedIncomeTaxViewModel): Html = {
    val htmlDocument = Html("<Html><head></head><body>Test</body></Html>")
    views.html.estimatedIncomeTax.zeroTaxEstimatedIncomeTax(vm, htmlDocument)
  }

  override def view: Html = view(defaultViewModel)
}
