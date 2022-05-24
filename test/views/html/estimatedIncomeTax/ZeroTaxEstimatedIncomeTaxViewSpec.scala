/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{Band, BandedGraph, ZeroTaxEstimatedIncomeTaxViewModel}
import views.html.includes.link

class ZeroTaxEstimatedIncomeTaxViewSpec extends TaiViewSpec {
  "Zero Tax Estimated Income Tax Page" must {
    behave like pageWithCombinedHeaderNewFormat(
      messages(
        "tai.taxYear",
        Dates.formatDate(TaxYear().start).replace(" ", "\u00A0"),
        Dates.formatDate(TaxYear().end).replace(" ", "\u00A0")),
      messages("tai.estimatedIncome.title"),
      Some(messages("tai.estimatedIncome.accessiblePreHeading"))
    )

    behave like pageWithBackLinkNew

    "have a heading for the Total estimated Income" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.totalEstimatedIncome.subheading") + " £9,000")
    }

    "have a heading for the Income tax estimate" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.incomeTaxEstimate.subheading") + " £0")
    }

    "have static messages" in {

      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.whyEstimate.link"))
      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.howYouPay.heading"))

      doc(view) must haveParagraphWithText(
        Html(messages("tai.estimatedIncome.whyEstimate.desc", Dates.formatDate(TaxYear().end))).body)

      doc(view) must haveParagraphWithText(
        Html(messages("tai.estimatedIncome.howYouPay.desc", messages("tai.estimatedIncome.taxCodes.link"))).body)

      doc(view).select("#howYouPayDesc").html().replaceAll("\\s+", "") mustBe Html(
        messages(
          "tai.estimatedIncome.howYouPay.desc",
          link(
            id = Some("taxCodesLink"),
            url = routes.YourTaxCodeController.taxCodes.url.toString,
            copy = Messages("tai.estimatedIncome.taxCodes.link"))
        )).body.replaceAll("\\s+", "")
    }

    "have low estimated total income messages" when {
      "the earnings for a NINO were lower than the tax free allowance" in {
        doc(view) must haveParagraphWithText(
          Html(messages("tai.estimatedIncomeLow.desc", messages("tai.estimatedIncome.taxFree.link"), "£11,500")).body)

        doc(view).select("#estimatedIncomeLowDesc").html().replaceAll("\\s+", "") mustBe Html(
          Messages(
            "tai.estimatedIncomeLow.desc",
            link(
              id = Some("taxFreeAmountLink"),
              url = routes.TaxFreeAmountController.taxFreeAmount.url.toString,
              copy = messages("tai.estimatedIncome.taxFree.link")
            ),
            "£11,500"
          )).body.replaceAll("\\s+", "")

        doc(view).select("#balanceEarningsDesc").html() mustBe Html(
          Messages("tai.estimatedIncomeEarning.desc", "£2,500")).body
      }
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

  val defaultViewModel = {
    val bandedGraph = BandedGraph("taxGraph", List.empty[Band], 0, 0, 0, 0, 0, 0, 0, None, None)
    ZeroTaxEstimatedIncomeTaxViewModel(0, 9000, 11500, bandedGraph, "taxRegion")
  }

  def view(vm: ZeroTaxEstimatedIncomeTaxViewModel): Html = {
    val zeroTaxEstimatedIncomeTax = inject[ZeroTaxEstimatedIncomeTaxView]
    val htmlDocument = Html("<Html><head></head><body>Test</body></Html>")
    zeroTaxEstimatedIncomeTax(vm, htmlDocument)
  }

  override def view: Html = view(defaultViewModel)
}
