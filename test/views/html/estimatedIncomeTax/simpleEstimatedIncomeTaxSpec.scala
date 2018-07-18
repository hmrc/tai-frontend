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
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, TaxBand, TotalTax}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{BandedGraph, SimpleEstimatedIncomeTaxViewModel}
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link

class simpleEstimatedIncomeTaxSpec extends TaiViewSpec {

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
          id = Some("taxCodesLink"),
          url = routes.YourTaxCodeController.taxCodes.url.toString,
          value = Some(Messages("tai.estimatedIncome.taxCodes.link"))).toHtml)).body
    }
    "heading and text for non savings income section displays" should {
      "be 'Tax on your employment income' when income is only from employment" in {
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
        val taxCodeIncome: Seq[TaxCodeIncome] = List(TaxCodeIncome(EmploymentIncome, None, 0, "", "", "", OtherBasisOperation, Live))
        val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
        val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val viewModel = SimpleEstimatedIncomeTaxViewModel( Seq.empty[CodingComponent], taxAccountSummary,taxCodeIncome,List.empty[TaxBand])
        val document = doc(view(viewModel))
        val message = Messages("your.total.income.from.employment.desc",
          "£0",
          "tax-free amount", "£0")

        document must haveH2HeadingWithText(messages("tax.on.your.employment.income"))
        document must haveParagraphWithText(message)
      }

      "be 'Tax on your private pension income' when income is only from pension" in {
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
        val taxCodeIncome: Seq[TaxCodeIncome] = List(TaxCodeIncome(PensionIncome, None, 0, "", "", "", OtherBasisOperation, Live))
        val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
        val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val viewModel =  SimpleEstimatedIncomeTaxViewModel( Seq.empty[CodingComponent], taxAccountSummary,taxCodeIncome,List.empty[TaxBand])
        val document = doc(view(viewModel))
        val message = Messages("your.total.income.from.private.pension.desc",
          "£0",
          "tax-free amount", "£0")

        document must haveH2HeadingWithText(messages("tax.on.your.private.pension.income"))
        document must haveParagraphWithText(message)
      }

      "be 'Tax on your PAYE income' when income is only from any other combination" when {
        "Employment and pension income" in {
          val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
          val taxCodeIncome: Seq[TaxCodeIncome] = List(TaxCodeIncome(PensionIncome, None, 0, "", "", "", OtherBasisOperation, Live), TaxCodeIncome(EmploymentIncome, None, 0, "", "", "", OtherBasisOperation, Live))
          val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
          val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          val viewModel =  SimpleEstimatedIncomeTaxViewModel( Seq.empty[CodingComponent], taxAccountSummary,taxCodeIncome,List.empty[TaxBand])
          val document = doc(view(viewModel))
          val message = Messages("your.total.income.from.paye.desc",
            "£0",
            "tax-free amount", "£0")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }

        "JSA income" in {
          val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
          val taxCodeIncome: Seq[TaxCodeIncome] = List(TaxCodeIncome(JobSeekerAllowanceIncome, None, 0, "", "", "", OtherBasisOperation, Live))
          val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
          val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          val viewModel =  SimpleEstimatedIncomeTaxViewModel( Seq.empty[CodingComponent], taxAccountSummary,taxCodeIncome,List.empty[TaxBand])
          val document = doc(view(viewModel))
          val message = Messages("your.total.income.from.paye.desc",
            "£0",
            "tax-free amount", "£0", "PAYE")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }

        "Other income" in {
          val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
          val taxCodeIncome: Seq[TaxCodeIncome] = List(TaxCodeIncome(OtherIncome, None, 0, "", "", "", OtherBasisOperation, Live))
          val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
          val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          val viewModel =  SimpleEstimatedIncomeTaxViewModel( Seq.empty[CodingComponent], taxAccountSummary,taxCodeIncome,List.empty[TaxBand])
          val document = doc(view(viewModel))
          val message = Messages("your.total.income.from.paye.desc",
            "£0",
            "tax-free amount", "£0", "PAYE")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }
      }
    }
    "have tax on your employment income section" in {

      val document = doc(view)
      document.select("#taxOnEmploymentIncomeDesc").html() mustBe Html(Messages("your.total.income.from.employment.desc",
        "47,835",
        "tax-free amount",
        "£11,500")).body

      document.select("#employmentIncomeTaxDetails").size() mustBe 1
      document must haveTableThWithIdAndText("taxBand", messages("tai.incomeTaxBand"))
      document must haveTableThWithIdAndText("taxAmount", messages("tai.amount"))
      document must haveTableThWithIdAndText("taxRate", messages("tai.taxRate"))
      document must haveTableThWithIdAndText("tax", messages("tai.tax"))
      document.select("#bandType0").text() mustBe messages("estimate.uk.bandtype.pa")
      document.select("#bandType1").text() mustBe messages("estimate.uk.bandtype.B")
      document.select("#bandType2").text() mustBe messages("estimate.uk.bandtype.D0")
      document.select("#income0").text() mustBe "£11,500"
      document.select("#taxRate0").text() mustBe "0%"
      document.select("#tax0").text() mustBe "£0"
      document.select("#income1").text() mustBe "£32,010"
      document.select("#taxRate1").text() mustBe "20%"
      document.select("#tax1").text() mustBe "£6,402"
      document.select("#income2").text() mustBe "£36,466"
      document.select("#taxRate2").text() mustBe "40%"
      document.select("#tax2").text() mustBe "£14,586"

    }

    "show iform links" in {
      doc.select("#iForms").text() mustBe "Test"
    }
  }

  val bandedGraph = BandedGraph("taxGraph", Nil, 0, 0, 0, 0, 0, 0, 0, None, None)

  val ukTaxBands = List(
    TaxBand("pa", "", 11500, 0, None, None, 0),
    TaxBand("B", "", 32010, 6402, None, None, 20),
    TaxBand("D0", "", 36466, 14586.4, None, None, 40))


  val ukViewModel = SimpleEstimatedIncomeTaxViewModel(20988.40, 68476, 11500, bandedGraph, "UK", ukTaxBands, messages("tax.on.your.employment.income"),
    messages("your.total.income.from.employment.desc", "47,835", messages("tai.estimatedIncome.taxFree.link"), "£11,500"))

  def view(vm: SimpleEstimatedIncomeTaxViewModel): Html = views.html.estimatedIncomeTax.simpleEstimatedIncomeTax(vm, Html("<Html><head></head><body>Test</body></Html>"))

  override def view: Html = view(ukViewModel)

}