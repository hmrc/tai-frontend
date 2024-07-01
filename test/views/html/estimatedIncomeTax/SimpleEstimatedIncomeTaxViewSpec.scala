/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{BandedGraph, SimpleEstimatedIncomeTaxViewModel}
import views.html.includes.link

class SimpleEstimatedIncomeTaxViewSpec extends TaiViewSpec {

  val bandedGraph: BandedGraph = BandedGraph("taxGraph", Nil, 0, 0, 0, 0, 0, 0, 0, None, None)

  val ukTaxBands: List[TaxBand] = List(
    TaxBand("pa", "", 11500, 0, None, None, 0),
    TaxBand("B", "", 32010, 6402, None, None, 20),
    TaxBand("D0", "", 36466, 14586.4, None, None, 40)
  )

  val ukViewModel: SimpleEstimatedIncomeTaxViewModel = SimpleEstimatedIncomeTaxViewModel(
    20988.40,
    68476,
    11500,
    bandedGraph,
    "UK",
    ukTaxBands,
    messages("tax.on.your.employment.income"),
    messages(
      "your.total.income.from.employment.desc",
      "47,835",
      messages("tai.estimatedIncome.taxFree.link"),
      "£11,500"
    )
  )

  private val template = inject[SimpleEstimatedIncomeTaxView]

  def view(vm: SimpleEstimatedIncomeTaxViewModel): Html =
    template(vm, Html("<Html><head></head><body>Test</body></Html>"))

  override def view: Html = view(ukViewModel)

  "Estimated Income Tax Page" must {
    behave like pageWithCombinedHeaderNewFormatNew(
      messages("tai.taxYear", Dates.formatDate(TaxYear().start), Dates.formatDate(TaxYear().end)),
      messages("tai.estimatedIncome.title"),
      Some(messages("tai.estimatedIncome.accessiblePreHeading"))
    )

    behave like pageWithBackLink()

    "have a heading for the Total estimated Income" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.totalEstimatedIncome.subheading") + " £68,476")
    }

    "have a heading for the Income tax estimate" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.incomeTaxEstimate.subheading") + " £20,988")
    }

    "have static messages" in {

      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.whyEstimate.link"))
      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.howYouPay.heading"))

      doc(view) must haveParagraphWithText(
        Html(messages("tai.estimatedIncome.whyEstimate.desc", Dates.formatDate(TaxYear().end))).body
      )

      doc(view) must haveParagraphWithText(
        Html(messages("tai.estimatedIncome.howYouPay.desc", messages("tai.estimatedIncome.taxCodes.link"))).body
      )

      doc(view).select("#howYouPayDesc").html().replaceAll("\\s+", "") mustBe Html(
        messages(
          "tai.estimatedIncome.howYouPay.desc",
          link(
            id = Some("taxCodesLink"),
            url = routes.YourTaxCodeController.taxCodes().url,
            copy = Messages("tai.estimatedIncome.taxCodes.link")
          )
        )
      ).body.replaceAll("\\s+", "")
    }
    "heading and text for non savings income section displays" should {
      "be 'Tax on your employment income' when income is only from employment" in {
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(EmploymentIncome, None, 0, "", "", "", OtherBasisOfOperation, Live))
        val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
        val viewModel = SimpleEstimatedIncomeTaxViewModel(
          Seq.empty[CodingComponent],
          taxAccountSummary,
          taxCodeIncome,
          List.empty[TaxBand]
        )
        val document = doc(view(viewModel))
        val message = Messages("your.total.income.from.employment.desc", "£0", "tax-free amount", "£0")

        document must haveH2HeadingWithText(messages("tax.on.your.employment.income"))
        document must haveParagraphWithText(message)
      }

      "be 'Tax on your private pension income' when income is only from pension" in {
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(PensionIncome, None, 0, "", "", "", OtherBasisOfOperation, Live))
        val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
        val viewModel = SimpleEstimatedIncomeTaxViewModel(
          Seq.empty[CodingComponent],
          taxAccountSummary,
          taxCodeIncome,
          List.empty[TaxBand]
        )
        val document = doc(view(viewModel))
        val message = Messages("your.total.income.from.private.pension.desc", "£0", "tax-free amount", "£0")

        document must haveH2HeadingWithText(messages("tax.on.your.private.pension.income"))
        document must haveParagraphWithText(message)
      }

      "be 'Tax on your PAYE income' when income is only from any other combination" when {
        "Employment and pension income" in {
          val taxCodeIncome: Seq[TaxCodeIncome] = List(
            TaxCodeIncome(PensionIncome, None, 0, "", "", "", OtherBasisOfOperation, Live),
            TaxCodeIncome(EmploymentIncome, None, 0, "", "", "", OtherBasisOfOperation, Live)
          )
          val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
          val viewModel = SimpleEstimatedIncomeTaxViewModel(
            Seq.empty[CodingComponent],
            taxAccountSummary,
            taxCodeIncome,
            List.empty[TaxBand]
          )
          val document = doc(view(viewModel))
          val message = Messages("your.total.income.from.paye.desc", "£0", "tax-free amount", "£0")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }

        "JSA income" in {
          val taxCodeIncome: Seq[TaxCodeIncome] =
            List(TaxCodeIncome(JobSeekerAllowanceIncome, None, 0, "", "", "", OtherBasisOfOperation, Live))
          val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
          val viewModel = SimpleEstimatedIncomeTaxViewModel(
            Seq.empty[CodingComponent],
            taxAccountSummary,
            taxCodeIncome,
            List.empty[TaxBand]
          )
          val document = doc(view(viewModel))
          val message = Messages("your.total.income.from.paye.desc", "£0", "tax-free amount", "£0", "PAYE")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }

        "Other income" in {
          val taxCodeIncome: Seq[TaxCodeIncome] =
            List(TaxCodeIncome(OtherIncome, None, 0, "", "", "", OtherBasisOfOperation, Live))
          val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
          val viewModel = SimpleEstimatedIncomeTaxViewModel(
            Seq.empty[CodingComponent],
            taxAccountSummary,
            taxCodeIncome,
            List.empty[TaxBand]
          )
          val document = doc(view(viewModel))
          val message = Messages("your.total.income.from.paye.desc", "£0", "tax-free amount", "£0", "PAYE")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }
      }
    }
    "have tax on your employment income section" in {

      val document = doc(view)
      document.select("#taxOnEmploymentIncomeDesc").html() mustBe Html(
        Messages("your.total.income.from.employment.desc", "47,835", "tax-free amount", "£11,500")
      ).body

      document.select(".employmentIncomeTaxDetails").size() mustBe 1
      document must haveTableThWithClassAndText("govuk-table__header taxBand", messages("tai.incomeTaxBand"))
      document must haveTableThWithClassAndText(
        "govuk-table__header govuk-table__header--numeric numeric taxAmount",
        messages("tai.amount")
      )
      document must haveTableThWithClassAndText(
        "govuk-table__header govuk-table__header--numeric numeric taxRate",
        messages("tai.taxRate")
      )
      document must haveTableThWithClassAndText(
        "govuk-table__header govuk-table__header--numeric numeric tax",
        messages("tai.tax")
      )
      document.select(".bandType0").text() mustBe "Tax-free amount"
      document.select(".bandType1").text() mustBe "Basic rate"
      document.select(".bandType2").text() mustBe "Higher rate"
      document.select(".income0").text() mustBe "£11,500"
      document.select(".taxRate0").text() mustBe "0%"
      document.select(".tax0").text() mustBe "£0"
      document.select(".income1").text() mustBe "£32,010"
      document.select(".taxRate1").text() mustBe "20%"
      document.select(".tax1").text() mustBe "£6,402"
      document.select(".income2").text() mustBe "£36,466"
      document.select(".taxRate2").text() mustBe "40%"
      document.select(".tax2").text() mustBe "£14,586"

    }

    "show iform links" in {
      doc.select("#iForms").text() mustBe "Test"
    }

    "display navigational links to other pages in the service" in {
      doc must haveElementAtPathWithText("nav>h2", messages("tai.taxCode.sideBar.heading"))
      doc must haveLinkElement(
        "taxCodesSideLink",
        routes.YourTaxCodeController.taxCodes().url,
        messages("check.your.tax.codes")
      )
      doc must haveLinkElement(
        "taxFreeAmountSideLink",
        routes.TaxFreeAmountController.taxFreeAmount().url,
        messages("check.your.tax.free.amount")
      )
      doc must haveLinkElement(
        "taxSummarySideLink",
        controllers.routes.TaxAccountSummaryController.onPageLoad().url,
        messages("return.to.your.income.tax.summary")
      )
    }
  }

}
