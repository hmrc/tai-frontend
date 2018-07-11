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
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.SimpleEstimatedIncomeTaxViewModel
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link

class simpleIncomeTaxSpec extends TaiViewSpec {

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

    "have a heading for the Total estimated Income" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.totalEstimatedIncome.subheading") + " £68,476")
    }

    "have a heading for the Income tax estimate" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.incomeTaxEstimate.subheading") + " £20,988")
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
          id=Some("taxCodesLink"),
          url=routes.YourTaxCodeController.taxCodes.url.toString,
          value=Some(Messages("tai.estimatedIncome.taxCodes.link"))).toHtml)).body
    }

    "have tax on your employment income section" in {

      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.taxOnEmploymentIncome.subHeading"))
      doc(view) must haveParagraphWithText(Html(messages("tai.estimatedIncome.desc",
        "£68,476",
        messages("tai.estimatedIncome.taxFree.link"),
        "£11,500")).body)

      doc(view).select("#taxOnEmploymentIncomeDesc").html() mustBe Html(Messages("tai.estimatedIncome.desc",
        "£68,476",
        Link.toInternalPage(
          id = Some("taxFreeAmountLink"),
          url = routes.TaxFreeAmountController.taxFreeAmount.url.toString,
          value = Some("tai.estimatedIncome.taxFree.link")
        ).toHtml,
        "£11,500")).body
      doc(view).select("#tax-on-your-income").size() mustBe 1
      doc(view) must haveTableThWithIdAndText("taxBand", messages("tai.incomeTaxBand"))
      doc(view) must haveTableThWithIdAndText("taxAmount", messages("tai.amount"))
      doc(view) must haveTableThWithIdAndText("taxRate", messages("tai.taxRate"))
      doc(view) must haveTableThWithIdAndText("tax", messages("tai.tax"))
      doc(view).select("#bandType0").text() mustBe messages("estimate.uk.bandtype.pa")
      doc(view).select("#bandType1").text() mustBe messages("estimate.uk.bandtype.B")
      doc(view).select("#bandType2").text() mustBe messages("estimate.uk.bandtype.D0")
      doc(view).select("#income0").text() mustBe "£11,500"
      doc(view).select("#taxRate0").text() mustBe "0%"
      doc(view).select("#tax0").text() mustBe "£0"
      doc(view).select("#income1").text() mustBe "£32,010"
      doc(view).select("#taxRate1").text() mustBe "20%"
      doc(view).select("#tax1").text() mustBe "£6,402"
      doc(view).select("#income2").text() mustBe "£36,466"
      doc(view).select("#taxRate2").text() mustBe "40%"
      doc(view).select("#tax2").text() mustBe "£14,586"

    }

    "show iform links" in {
      doc.select("#iForms").text() mustBe "Test"
    }
  }

  val bandedGraph = BandedGraph("taxGraph", Nil, 0, 0, 0, 0, 0, 0, 0, None)

  val ukTaxBands = List(
    TaxBand("pa", "", 11500, 0, None, None, 0),
    TaxBand("B", "", 32010, 6402, None, None, 20),
    TaxBand("D0", "", 36466, 14586.4, None, None, 40))


  val ukViewModel = SimpleEstimatedIncomeTaxViewModel(20988.40, 68476, 11500, bandedGraph,"UK",ukTaxBands)


  override def view: Html = views.html.estimatedIncomeTax.simpleEstimatedIncomeTax(ukViewModel, Html("<Html><head></head><body>Test</body></Html>"))
}
