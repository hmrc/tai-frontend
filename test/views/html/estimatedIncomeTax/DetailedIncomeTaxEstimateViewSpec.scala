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
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax.{TaxBand, _}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{AdditionalTaxDetailRow, DetailedIncomeTaxEstimateViewModel, ReductionTaxRow}
import uk.gov.hmrc.tai.viewModels.{HelpLink, TaxSummaryLabel}
import views.html.includes.link

class DetailedIncomeTaxEstimateViewSpec extends TaiViewSpec {

  private val detailedIncomeTaxEstimate = inject[DetailedIncomeTaxEstimateView]

  val ukTaxBands: List[TaxBand] = List(
    TaxBand("pa", "", 11500, 0, None, None, 0),
    TaxBand("B", "", 32010, 6402, None, None, 20),
    TaxBand("D0", "", 36466, 14586.4, None, None, 40)
  )

  val defaultViewModel: DetailedIncomeTaxEstimateViewModel = DetailedIncomeTaxEstimateViewModel(
    ukTaxBands,
    Seq.empty[TaxBand],
    List(TaxBand("LDR", "", 32010, 6402, None, None, 20)),
    "UK",
    18573,
    68476,
    11500,
    Seq.empty[AdditionalTaxDetailRow],
    Seq.empty[ReductionTaxRow],
    20000,
    5000,
    None,
    messages("tax.on.your.employment.income"),
    messages(
      "your.total.income.from.employment.desc",
      "£68,476",
      messages("tai.estimatedIncome.taxFree.link"),
      "£11,500"
    )
  )

  def view(vm: DetailedIncomeTaxEstimateViewModel = defaultViewModel): Html = detailedIncomeTaxEstimate(vm)

  override def view: Html = view(defaultViewModel)

  def createViewModel(
    additionalTaxTable: Seq[AdditionalTaxDetailRow],
    reductionTaxTable: Seq[ReductionTaxRow]
  ): DetailedIncomeTaxEstimateViewModel =
    DetailedIncomeTaxEstimateViewModel(
      nonSavings = List.empty[TaxBand],
      savings = Seq.empty[TaxBand],
      dividends = List.empty[TaxBand],
      taxRegion = "uk",
      incomeTaxEstimate = 900,
      incomeEstimate = 16000,
      taxFreeAllowance = 11500,
      additionalTaxTable = additionalTaxTable,
      reductionTaxTable = reductionTaxTable,
      totalDividendIncome = 0,
      taxFreeDividendAllowance = 0,
      selfAssessmentAndPayeText = None,
      taxOnIncomeTypeHeading = "",
      taxOnIncomeTypeDescription = ""
    )

  "view" must {

    behave like pageWithTitle(messages("tai.estimatedIncome.detailedEstimate.title"))
    behave like pageWithHeader(messages("tai.estimatedIncome.detailedEstimate.heading"))
    behave like pageWithBackLink()

    behave like pageWithCombinedHeaderNewFormatNew(
      Messages(
        "tai.taxYear",
        Dates.formatDate(TaxYear().start),
        Dates.formatDate(TaxYear().end)
      ),
      Messages("tai.estimatedIncome.detailedEstimate.heading"),
      Some(messages("tai.estimatedIncome.accessiblePreHeading"))
    )

    "have a heading for the Total Income Tax Estimate" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTax.totalIncomeTaxEstimate") + " £18,573")
    }

    "paragraph with additional Income Tax payable not being included in estimate" should {
      "be shown when text is provided" in {
        val vm = defaultViewModel.copy(selfAssessmentAndPayeText = Some("Stub addition Income Payable Text"))

        doc(view(vm)) must haveParagraphWithText("Stub addition Income Payable Text")
      }
    }

    "heading and text for non savings income section displays" should {
      "be 'Tax on your employment income' when income is only from employment" in {
        val totalTax                          = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(EmploymentIncome, None, 0, "", "", "", OtherBasisOfOperation, Live))
        val taxAccountSummary                 = TaxAccountSummary(0, 0, 0, 0, 0)
        val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val viewModel                         = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncome,
          taxAccountSummary,
          Seq.empty[CodingComponent],
          nonTaxCodeIncome
        )
        val document                          = doc(view(viewModel))
        val message                           = Messages("your.total.income.from.employment.desc", "£0", "tax-free amount", "£0")

        document must haveH2HeadingWithText(messages("tax.on.your.employment.income"))
        document must haveParagraphWithText(message)
      }

      "be 'Tax on your private pension income' when income is only from pension" in {
        val totalTax                          = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(PensionIncome, None, 0, "", "", "", OtherBasisOfOperation, Live))
        val taxAccountSummary                 = TaxAccountSummary(0, 0, 0, 0, 0)
        val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val viewModel                         = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncome,
          taxAccountSummary,
          Seq.empty[CodingComponent],
          nonTaxCodeIncome
        )
        val document                          = doc(view(viewModel))
        val message                           = Messages("your.total.income.from.private.pension.desc", "£0", "tax-free amount", "£0")

        document must haveH2HeadingWithText(messages("tax.on.your.private.pension.income"))
        document must haveParagraphWithText(message)
      }

      "be 'Tax on your PAYE income' when income is only from any other combination" when {
        "Employment and pension income" in {
          val totalTax                          = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
          val taxCodeIncome: Seq[TaxCodeIncome] = List(
            TaxCodeIncome(PensionIncome, None, 0, "", "", "", OtherBasisOfOperation, Live),
            TaxCodeIncome(EmploymentIncome, None, 0, "", "", "", OtherBasisOfOperation, Live)
          )
          val taxAccountSummary                 = TaxAccountSummary(0, 0, 0, 0, 0)
          val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          val viewModel                         = DetailedIncomeTaxEstimateViewModel(
            totalTax,
            taxCodeIncome,
            taxAccountSummary,
            Seq.empty[CodingComponent],
            nonTaxCodeIncome
          )
          val document                          = doc(view(viewModel))
          val message                           = Messages("your.total.income.from.paye.desc", "£0", "tax-free amount", "£0")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }

        "JSA income" in {
          val totalTax                          = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
          val taxCodeIncome: Seq[TaxCodeIncome] =
            List(TaxCodeIncome(JobSeekerAllowanceIncome, None, 0, "", "", "", OtherBasisOfOperation, Live))
          val taxAccountSummary                 = TaxAccountSummary(0, 0, 0, 0, 0)
          val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          val viewModel                         = DetailedIncomeTaxEstimateViewModel(
            totalTax,
            taxCodeIncome,
            taxAccountSummary,
            Seq.empty[CodingComponent],
            nonTaxCodeIncome
          )
          val document                          = doc(view(viewModel))
          val message                           = Messages("your.total.income.from.paye.desc", "£0", "tax-free amount", "£0", "PAYE")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }

        "Other income" in {
          val totalTax                          = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None, None)
          val taxCodeIncome: Seq[TaxCodeIncome] =
            List(TaxCodeIncome(OtherIncome, None, 0, "", "", "", OtherBasisOfOperation, Live))
          val taxAccountSummary                 = TaxAccountSummary(0, 0, 0, 0, 0)
          val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          val viewModel                         = DetailedIncomeTaxEstimateViewModel(
            totalTax,
            taxCodeIncome,
            taxAccountSummary,
            Seq.empty[CodingComponent],
            nonTaxCodeIncome
          )
          val document                          = doc(view(viewModel))
          val message                           = Messages("your.total.income.from.paye.desc", "£0", "tax-free amount", "£0", "PAYE")

          document must haveH2HeadingWithText(messages("tax.on.your.paye.income"))
          document must haveParagraphWithText(message)
        }
      }
    }

    "display table body" when {
      "UK user have non-savings" in {

        val taxBands = List(
          TaxBand("B", "", 32010, 6402, None, None, 20),
          TaxBand("D0", "", 36466, 36466, None, None, 40)
        )

        val incomeCategories                  = Seq(
          IncomeCategory(NonSavingsIncomeCategory, 42868, 42868, 68476, taxBands)
        )
        val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val totalTax                          = TotalTax(0, incomeCategories, None, None, None, None, None)
        val taxAccountSummary                 = TaxAccountSummary(42868, 11500, 0, 0, 0, 68476, 11500)
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(OtherIncome, None, 0, "", "1150L", "", OtherBasisOfOperation, Live))

        val viewModel = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncome,
          taxAccountSummary,
          Seq.empty[CodingComponent],
          nonTaxCodeIncome
        )

        val viewWithNonSavings: Html = detailedIncomeTaxEstimate(viewModel)
        doc(viewWithNonSavings) must haveTdWithText("£32,010")
        doc(viewWithNonSavings) must haveTdWithText("Basic rate")
        doc(viewWithNonSavings) must haveTdWithText("20%")
        doc(viewWithNonSavings) must haveTdWithText("£6,402")
        doc(viewWithNonSavings) must haveTdWithText("£36,466")
        doc(viewWithNonSavings) must haveTdWithText("Higher rate")
        doc(viewWithNonSavings) must haveTdWithText("40%")
        doc(viewWithNonSavings) must haveTdWithText("£36,466")
      }

      "Scottish user have non-savings" in {

        val taxBands = List(
          TaxBand("B", "", 32010, 6402, None, None, 20),
          TaxBand("D0", "", 36466, 36466, None, None, 40),
          TaxBand("D1", "", 36466, 36466, None, None, 40),
          TaxBand("D2", "", 36466, 36466, None, None, 40),
          TaxBand("D3", "", 36466, 36466, None, None, 40)
        )

        val incomeCategories                  = Seq(
          IncomeCategory(NonSavingsIncomeCategory, 42868, 42868, 68476, taxBands)
        )
        val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val totalTax                          = TotalTax(0, incomeCategories, None, None, None, None, None)
        val taxAccountSummary                 = TaxAccountSummary(42868, 11500, 0, 0, 0, 68476, 11500)
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(OtherIncome, None, 0, "", "S1150L", "", OtherBasisOfOperation, Live))

        val viewModel = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncome,
          taxAccountSummary,
          Seq.empty[CodingComponent],
          nonTaxCodeIncome
        )

        val viewWithNonSavings: Html = detailedIncomeTaxEstimate(viewModel)

        doc(viewWithNonSavings) must haveTdWithText("£32,010")
        doc(viewWithNonSavings) must haveTdWithText("Basic rate")
        doc(viewWithNonSavings) must haveTdWithText("20%")
        doc(viewWithNonSavings) must haveTdWithText("£6,402")
        doc(viewWithNonSavings) must haveTdWithText("£36,466")
        doc(viewWithNonSavings) must haveTdWithText("Intermediate rate")
        doc(viewWithNonSavings) must haveTdWithText("40%")
        doc(viewWithNonSavings) must haveTdWithText("Basic rate")
        doc(viewWithNonSavings) must haveTdWithText("Intermediate rate")
        doc(viewWithNonSavings) must haveTdWithText("Higher rate")
        doc(viewWithNonSavings) must haveTdWithText("Advanced rate")
        doc(viewWithNonSavings) must haveTdWithText("Top rate")
      }

      "UK user have savings" in {
        val taxBands = List(
          TaxBand("LSR", "", 32010, 6402, None, None, 20),
          TaxBand("HSR1", "", 36466, 36466, None, None, 40)
        )

        val incomeCategories                  = Seq(
          IncomeCategory(UntaxedInterestIncomeCategory, 42868, 42868, 68476, taxBands)
        )
        val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val totalTax                          = TotalTax(0, incomeCategories, None, None, None, None, None)
        val taxAccountSummary                 = TaxAccountSummary(42868, 11500, 0, 0, 0, 68476, 11500)
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(OtherIncome, None, 0, "", "1150L", "", OtherBasisOfOperation, Live))

        val viewModel = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncome,
          taxAccountSummary,
          Seq.empty[CodingComponent],
          nonTaxCodeIncome
        )

        val viewWithSavings: Html = detailedIncomeTaxEstimate(viewModel)

        doc(viewWithSavings) must haveTdWithText("£32,010")
        doc(viewWithSavings) must haveTdWithText("Basic rate")
        doc(viewWithSavings) must haveTdWithText("20%")
        doc(viewWithSavings) must haveTdWithText("£6,402")
        doc(viewWithSavings) must haveTdWithText("£36,466")
        doc(viewWithSavings) must haveTdWithText("Higher rate")
        doc(viewWithSavings) must haveTdWithText("40%")
        doc(viewWithSavings) must haveTdWithText("£36,466")
      }

      "Scottish user have savings" in {
        val taxBands = List(
          TaxBand("LSR", "", 32010, 6402, None, None, 20),
          TaxBand("HSR1", "", 36466, 36466, None, None, 40)
        )

        val incomeCategories                  = Seq(
          IncomeCategory(UntaxedInterestIncomeCategory, 42868, 42868, 68476, taxBands)
        )
        val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val totalTax                          = TotalTax(0, incomeCategories, None, None, None, None, None)
        val taxAccountSummary                 = TaxAccountSummary(42868, 11500, 0, 0, 0, 68476, 11500)
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(OtherIncome, None, 0, "", "S1150L", "", OtherBasisOfOperation, Live))

        val viewModel: DetailedIncomeTaxEstimateViewModel = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncome,
          taxAccountSummary,
          Seq.empty[CodingComponent],
          nonTaxCodeIncome
        )

        val viewWithSavings: Html = detailedIncomeTaxEstimate(viewModel)

        doc(viewWithSavings) must haveTdWithText("£32,010")
        doc(viewWithSavings) must haveTdWithText("Basic rate")
        doc(viewWithSavings) must haveTdWithText("20%")
        doc(viewWithSavings) must haveTdWithText("£6,402")
        doc(viewWithSavings) must haveTdWithText("£36,466")
        doc(viewWithSavings) must haveTdWithText("Higher rate")
        doc(viewWithSavings) must haveTdWithText("40%")
        doc(viewWithSavings) must haveTdWithText("£36,466")
      }

      "UK user have dividends" in {
        val taxBands = List(
          TaxBand("LDR", "", 32010, 6402, None, None, 20),
          TaxBand("HDR1", "", 36466, 36466, None, None, 40)
        )

        val incomeCategories                  = Seq(
          IncomeCategory(UkDividendsIncomeCategory, 42868, 42868, 68476, taxBands)
        )
        val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val totalTax                          = TotalTax(0, incomeCategories, None, None, None, None, None)
        val taxAccountSummary                 = TaxAccountSummary(42868, 11500, 0, 0, 0, 68476, 11500)
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(OtherIncome, None, 0, "", "1150L", "", OtherBasisOfOperation, Live))

        val viewModel: DetailedIncomeTaxEstimateViewModel = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncome,
          taxAccountSummary,
          Seq.empty[CodingComponent],
          nonTaxCodeIncome
        )

        val viewWithDividends: Html = detailedIncomeTaxEstimate(viewModel)

        doc(viewWithDividends) must haveTdWithText("£32,010")
        doc(viewWithDividends) must haveTdWithText("Basic rate")
        doc(viewWithDividends) must haveTdWithText("20%")
        doc(viewWithDividends) must haveTdWithText("£6,402")
        doc(viewWithDividends) must haveTdWithText("£36,466")
        doc(viewWithDividends) must haveTdWithText("Higher rate")
        doc(viewWithDividends) must haveTdWithText("40%")
        doc(viewWithDividends) must haveTdWithText("£36,466")
      }

      "scottish user have dividends" in {

        val taxBands = List(
          TaxBand("LDR", "", 32010, 6402, None, None, 20),
          TaxBand("HDR1", "", 36466, 36466, None, None, 40)
        )

        val incomeCategories                  = Seq(
          IncomeCategory(UkDividendsIncomeCategory, 42868, 42868, 68476, taxBands)
        )
        val nonTaxCodeIncome                  = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
        val totalTax                          = TotalTax(0, incomeCategories, None, None, None, None, None)
        val taxAccountSummary                 = TaxAccountSummary(42868, 11500, 0, 0, 0, 68476, 11500)
        val taxCodeIncome: Seq[TaxCodeIncome] =
          List(TaxCodeIncome(OtherIncome, None, 0, "", "S1150L", "", OtherBasisOfOperation, Live))

        val viewModel: DetailedIncomeTaxEstimateViewModel = DetailedIncomeTaxEstimateViewModel(
          totalTax,
          taxCodeIncome,
          taxAccountSummary,
          Seq.empty[CodingComponent],
          nonTaxCodeIncome
        )

        val viewWithDividends: Html = detailedIncomeTaxEstimate(viewModel)

        doc(viewWithDividends) must haveTdWithText("£32,010")
        doc(viewWithDividends) must haveTdWithText("Basic rate")
        doc(viewWithDividends) must haveTdWithText("20%")
        doc(viewWithDividends) must haveTdWithText("£6,402")
        doc(viewWithDividends) must haveTdWithText("£36,466")
        doc(viewWithDividends) must haveTdWithText("Higher rate")
        doc(viewWithDividends) must haveTdWithText("40%")
        doc(viewWithDividends) must haveTdWithText("£36,466")
      }

    }

    "have tax on your employment income section" in {

      doc(view) must haveH2HeadingWithText(messages("tax.on.your.employment.income"))
      doc(view) must haveParagraphWithText(
        Html(
          messages(
            "your.total.income.from.employment.desc",
            "£68,476",
            messages("tai.estimatedIncome.taxFree.link"),
            "£11,500"
          )
        ).body
      )
    }

    "have tax on your dividend income section" in {

      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.detailedEstimate.dividendIncome.subHeading"))
      doc(view) must haveParagraphWithText(messages("tai.estimatedIncome.dividend.para.desc", "20,000", "5,000"))

    }

    "have tax on your savings income section" when {

      val incomeFromSavingsId = "income-from-savings"
      val higherRateParaId    = "higher-rate-para"

      "savings income does NOT include HSR1 or HSR2 bands" in {
        val taxBandSR    =
          TaxBand(bandType = "SR", code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(500), rate = 0)
        val taxBandPSR   =
          TaxBand(bandType = "PSR", code = "", income = 500, tax = 0, lowerBand = None, upperBand = Some(500), rate = 0)
        val savingsBands = Seq(taxBandSR, taxBandPSR)
        val model        = defaultViewModel.copy(savings = savingsBands)

        def view: HtmlFormat.Appendable = detailedIncomeTaxEstimate(model)

        doc(view) must haveH2HeadingWithText(
          messages("tai.estimatedIncome.detailedEstimate.savingsInterest.subHeading")
        )

        doc(view) must haveElementWithId(incomeFromSavingsId)
        doc(view) must haveParagraphWithText(messages("tai.estimatedIncome.savings.desc.totalIncomeEstimate", "£600"))
        doc(view) mustNot haveElementWithId(higherRateParaId)

      }

      "savings income does include HSR1 or HSR2 bands" in {
        val taxBandPSR   =
          TaxBand(bandType = "PSR", code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(500), rate = 0)
        val taxBandSR    =
          TaxBand(bandType = "SR", code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(500), rate = 0)
        val taxBandHSR1  = TaxBand(
          bandType = "HSR1",
          code = "",
          income = 500,
          tax = 0,
          lowerBand = None,
          upperBand = Some(500),
          rate = 0
        )
        val savingsBands = Seq(taxBandSR, taxBandPSR, taxBandHSR1)
        val model        = defaultViewModel.copy(savings = savingsBands)

        def view: Html = detailedIncomeTaxEstimate(model)

        doc(view) must haveH2HeadingWithText(
          messages("tai.estimatedIncome.detailedEstimate.savingsInterest.subHeading")
        )

        doc(view) must haveElementWithId(incomeFromSavingsId)
        doc(view) must haveParagraphWithText(messages("tai.estimatedIncome.savings.desc.totalIncomeEstimate", "£700"))
        doc(view) must haveElementWithId(higherRateParaId)
        doc(view) must haveParagraphWithText(messages("tai.estimatedIncome.savings.desc.higherRate", "200"))

      }
    }
  }

  "have additional tax table" in {

    val additionalRows = Seq(
      AdditionalTaxDetailRow(
        TaxSummaryLabel(
          Messages("tai.taxCalc.UnderpaymentPreviousYear.title"),
          Some(
            HelpLink(
              Messages("what.is.underpayment"),
              controllers.routes.UnderpaymentFromPreviousYearController.underpaymentExplanation().url,
              "underPaymentFromPreviousYear"
            )
          )
        ),
        100
      ),
      AdditionalTaxDetailRow(
        TaxSummaryLabel(
          Messages("tai.taxcode.deduction.type-45"),
          Some(
            HelpLink(
              Messages("what.is.tax.estimation"),
              controllers.routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url,
              "estimatedTaxOwedLink"
            )
          )
        ),
        50
      ),
      AdditionalTaxDetailRow(TaxSummaryLabel(Messages("tai.taxCalc.OutstandingDebt.title")), 150),
      AdditionalTaxDetailRow(TaxSummaryLabel(Messages("tai.taxCalc.childBenefit.title")), 300),
      AdditionalTaxDetailRow(TaxSummaryLabel(Messages("tai.taxCalc.excessGiftAidTax.title")), 100),
      AdditionalTaxDetailRow(TaxSummaryLabel(Messages("tai.taxCalc.excessWidowsAndOrphans.title")), 100),
      AdditionalTaxDetailRow(TaxSummaryLabel(Messages("tai.taxCalc.pensionPaymentsAdjustment.title")), 200)
    )
    val model          = createViewModel(additionalRows, Seq.empty[ReductionTaxRow])

    def additionalDetailView: Html = detailedIncomeTaxEstimate(model)

    doc(additionalDetailView).select("#additionalTaxTable").size() mustBe 1
    doc(additionalDetailView).select("#additionalTaxTable-heading").text mustBe Messages(
      "tai.estimatedIncome.additionalTax.title"
    )
    doc(additionalDetailView).select("#additionalTaxTable-desc").text() mustBe Messages(
      "tai.estimatedIncome.additionalTax.desc"
    )
    doc(additionalDetailView) must haveThWithText(messages("tax.adjustments"))
    doc(additionalDetailView) must haveTdWithText(messages("tai.taxCalc.OutstandingDebt.title"))
    doc(additionalDetailView) must haveTdWithText(messages("tai.taxCalc.childBenefit.title"))
    doc(additionalDetailView) must haveTdWithText(messages("tai.taxCalc.excessGiftAidTax.title"))
    doc(additionalDetailView) must haveTdWithText(messages("tai.taxCalc.excessWidowsAndOrphans.title"))
    doc(additionalDetailView) must haveTdWithText(messages("tai.taxCalc.pensionPaymentsAdjustment.title"))
    doc(additionalDetailView) must haveTdWithText(
      s"${messages("tai.taxCalc.UnderpaymentPreviousYear.title")} ${messages("what.is.underpayment")}"
    )
    doc(additionalDetailView)
      .select("#underPaymentFromPreviousYear")
      .attr(
        "href"
      ) mustBe controllers.routes.UnderpaymentFromPreviousYearController.underpaymentExplanation().url
    doc(additionalDetailView) must haveTdWithText(
      s"${messages("tai.taxcode.deduction.type-45")} ${messages("what.is.tax.estimation")}"
    )
    doc(additionalDetailView)
      .select("#estimatedTaxOwedLink")
      .attr("href") mustBe controllers.routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url
  }

  "have reduction tax table" in {
    val taxCodeLink = link(
      url = routes.YourTaxCodeController.taxCodes().toString,
      copy = Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText")
    ).body

    val reductionTaxRows = Seq(
      ReductionTaxRow(
        Messages("tai.taxCollected.atSource.otherIncome.description"),
        100,
        Messages("tai.taxCollected.atSource.otherIncome.title")
      ),
      ReductionTaxRow(
        Messages("tai.taxCollected.atSource.dividends.description", 10),
        200,
        Messages("tai.taxCollected.atSource.dividends.title")
      ),
      ReductionTaxRow(
        Messages("tai.taxCollected.atSource.bank.description", 20),
        100,
        Messages("tai.taxCollected.atSource.bank.title")
      ),
      ReductionTaxRow(
        Messages("tai.taxCollected.atSource.marriageAllowance.description", 0, taxCodeLink),
        135,
        Messages("tai.taxCollected.atSource.marriageAllowance.title")
      ),
      ReductionTaxRow(
        Messages("tai.taxCollected.atSource.maintenancePayments.description"),
        200,
        Messages("tai.taxCollected.atSource.marriageAllowance.title")
      ),
      ReductionTaxRow(
        Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.description"),
        100,
        Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.title")
      ),
      ReductionTaxRow(
        Messages("tai.taxCollected.atSource.concessionalRelief.description"),
        600,
        Messages("tai.taxCollected.atSource.concessionalRelief.title")
      ),
      ReductionTaxRow(
        Messages("tai.taxCollected.atSource.doubleTaxationRelief.description"),
        900,
        Messages("tai.taxCollected.atSource.doubleTaxationRelief.title")
      ),
      ReductionTaxRow(Messages("gift.aid.tax.relief", 0, 1000), 1000, Messages("gift.aid.savings")),
      ReductionTaxRow(Messages("personal.pension.payment.relief", 0, 1100), 1100, Messages("personal.pension.payments"))
    )

    val model = createViewModel(Seq.empty[AdditionalTaxDetailRow], reductionTaxRows)

    def reductionTaxDetailView: Html = detailedIncomeTaxEstimate(model)

    doc(reductionTaxDetailView).select("#taxPaidElsewhereTable").size() mustBe 1
    doc(reductionTaxDetailView).select("#taxPaidElsewhereTable-heading").text() mustBe Messages(
      "tai.estimatedIncome.reductionsTax.title"
    )
    doc(reductionTaxDetailView).select("#taxPaidElsewhereTable-desc").text() mustBe Messages(
      "tai.estimatedIncome.reductionsTax.desc"
    )
    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("tai.taxCollected.atSource.otherIncome.title")} ${messages("tai.taxCollected.atSource.otherIncome.description")}"
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("tai.taxCollected.atSource.dividends.title")} ${messages("tai.taxCollected.atSource.dividends.description", 10)}"
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("tai.taxCollected.atSource.bank.title")} ${messages("tai.taxCollected.atSource.bank.description", 20)}"
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("tai.taxCollected.atSource.marriageAllowance.title")} ${messages("tai.taxCollected.atSource.marriageAllowance.description", 0, Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText"))}"
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("tai.taxCollected.atSource.marriageAllowance.title")} ${messages("tai.taxCollected.atSource.maintenancePayments.description", 200)}"
    )

    doc(reductionTaxDetailView) must haveLinkWithText(
      Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText")
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.title")} ${messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.description", 100)}"
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("tai.taxCollected.atSource.concessionalRelief.title")} ${messages("tai.taxCollected.atSource.concessionalRelief.description")}"
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("tai.taxCollected.atSource.doubleTaxationRelief.title")} ${messages("tai.taxCollected.atSource.doubleTaxationRelief.description")}"
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("gift.aid.savings")} ${messages("gift.aid.tax.relief", 0, 1000)}"
    )

    doc(reductionTaxDetailView) must haveDetailsWithTextNew(
      s"${messages("personal.pension.payments")} ${messages("personal.pension.payment.relief", 0, 1100)}"
    )

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
