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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import controllers.{FakeTaiPlayApplication, routes}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome, _}
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.model.domain.{ChildBenefit => _, DoubleTaxationRelief => _, MaintenancePayments => _, _}
import uk.gov.hmrc.tai.util.BandTypesConstants
import uk.gov.hmrc.tai.viewModels.{HelpLink, Label}
import uk.gov.hmrc.urls.Link

class DetailedIncomeTaxEstimateViewModelSpec extends PlaySpec with FakeTaiPlayApplication with BandTypesConstants with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "DetailedIncomeTaxEstimateViewModel" when {

    "looking at dividends" when {

      "totalDividendIncome is called" must {

        "return the total income from dividends" in {
          val taxBand = Seq(TaxBand(bandType = "", code = "", income = 100, tax = 0, lowerBand = None, upperBand = None, rate = 20))

          val incomeCategories = Seq(
            IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 10, taxBand),
            IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 20, taxBand),
            IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 4000, taxBand),
            IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 30, taxBand),
            IncomeCategory(BankInterestIncomeCategory, 0, 5000, 40, taxBand),
            IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 5000, taxBand)
          )

          DetailedIncomeTaxEstimateViewModel.totalDividendIncome(incomeCategories) mustEqual 9000

        }
      }

      "taxFreeDividendAllowance is called" must {
        "return the tax free dividend allowance when there is one" in {

          val taxBand = Seq(
            TaxBand(bandType = DividendZeroRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
            TaxBand(bandType = PersonalSavingsRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
            TaxBand(bandType = StarterSavingsRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20)
          )

          val incomeCategories = Seq(
            IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 10, taxBand),
            IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 20, taxBand),
            IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 4000, taxBand),
            IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 30, taxBand),
            IncomeCategory(BankInterestIncomeCategory, 0, 5000, 40, taxBand),
            IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 5000, taxBand)
          )

          DetailedIncomeTaxEstimateViewModel.taxFreeDividendAllowance(incomeCategories) mustEqual 5000

        }

        "return zero free dividend allowance when there is not one" in {

          val taxBand = Seq(
            TaxBand(bandType = PersonalSavingsRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
            TaxBand(bandType = StarterSavingsRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20)
          )

          val incomeCategories = Seq(
            IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 5000, taxBand)
          )

          DetailedIncomeTaxEstimateViewModel.taxFreeDividendAllowance(incomeCategories) mustEqual 0

        }
      }

      "return all dividend bands that have an income" in {

        val taxBands = Seq(
          TaxBand(bandType = DividendZeroRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 0),
          TaxBand(bandType = DividendBasicRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 10),
          TaxBand(bandType = DividendHigherRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
          TaxBand(bandType = DividendAdditionalRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 30)
        )
        val incomeCategories = Seq(
          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBands)
        )
        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)
        val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

        model.dividends must contain theSameElementsAs(taxBands)

      }
    }

    "looking at savings" when {
      "savingsDescription1 is called must return the correct message" when {
        "bandType is SR" in {
          val taxBand = TaxBand(bandType = "SR", code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(500), rate = 0)
          val savingsBands = Seq(taxBand)

          DetailedIncomeTaxEstimateViewModel.savingsDescription1(savingsBands) mustEqual Messages("tai.estimatedIncome.savings.desc.SR", 500)
        }
        "bandType is LSR" in {
          val taxBandPSR = TaxBand(bandType = "PSR", code = "", income = 500, tax = 0, lowerBand = None, upperBand = Some(1000), rate = 0)
          val taxBandLSR = TaxBand(bandType = "LSR", code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(500), rate = 20)
          val savingsBands = Seq(taxBandPSR, taxBandLSR)

          DetailedIncomeTaxEstimateViewModel.savingsDescription1(savingsBands) mustEqual Messages("tai.estimatedIncome.savings.desc.BRHR", 600)
        }
        "bandType is HSR1" in {
          val taxBandPSR = TaxBand(bandType = "PSR", code = "", income = 500, tax = 0, lowerBand = None, upperBand = Some(1000), rate = 0)
          val taxBandLSR = TaxBand(bandType = "HSR1", code = "", income = 7000, tax = 0, lowerBand = None, upperBand = Some(10000), rate = 20)
          val savingsBands = Seq(taxBandPSR, taxBandLSR)

          DetailedIncomeTaxEstimateViewModel.savingsDescription1(savingsBands) mustEqual Messages("tai.estimatedIncome.savings.desc.BRHR", 7500)
        }
        "bandType is HSR2" in {
          val taxBandPSR = TaxBand(bandType = "PSR", code = "", income = 500, tax = 0, lowerBand = None, upperBand = Some(1000), rate = 0)
          val taxBandLSR = TaxBand(bandType = "HSR2", code = "", income = 8000, tax = 0, lowerBand = None, upperBand = Some(11000), rate = 20)
          val savingsBands = Seq(taxBandPSR, taxBandLSR)

          DetailedIncomeTaxEstimateViewModel.savingsDescription1(savingsBands) mustEqual Messages("tai.estimatedIncome.savings.desc.BRHR", 8500)
        }
      }

      "savingsDescription2 is called must return the correct message" when {
        "bandType is LSR" in {
          val taxBandPSR = TaxBand(bandType = "PSR", code = "", income = 500, tax = 0, lowerBand = None, upperBand = Some(1000), rate = 0)
          val taxBandLSR = TaxBand(bandType = "LSR", code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(500), rate = 20)
          val savingsBands = Seq(taxBandPSR, taxBandLSR)

          DetailedIncomeTaxEstimateViewModel.savingsDescription2(savingsBands) mustEqual Messages("tai.estimatedIncome.savings.desc.BRHR2", 1000)
        }
        "bandType is HSR1" in {
          val taxBandPSR = TaxBand(bandType = "PSR", code = "", income = 500, tax = 0, lowerBand = None, upperBand = Some(1000), rate = 0)
          val taxBandLSR = TaxBand(bandType = "HSR1", code = "", income = 7000, tax = 0, lowerBand = None, upperBand = Some(10000), rate = 20)
          val savingsBands = Seq(taxBandPSR, taxBandLSR)

          DetailedIncomeTaxEstimateViewModel.savingsDescription2(savingsBands) mustEqual Messages("tai.estimatedIncome.savings.desc.BRHR2extra", 1000)
        }
//        "bandType is HSR2" in {
//          val taxBandPSR = TaxBand(bandType = "PSR", code = "", income = 500, tax = 0, lowerBand = None, upperBand = Some(1000), rate = 0)
//          val taxBandLSR = TaxBand(bandType = "HSR2", code = "", income = 8000, tax = 0, lowerBand = None, upperBand = Some(11000), rate = 20)
//          val savingsBands = Seq(taxBandPSR, taxBandLSR)
//
//          DetailedIncomeTaxEstimateViewModel.savingsDescription2(savingsBands) mustEqual Messages("tai.estimatedIncome.savings.desc.BRHR2", 1000)
//        }
      }
    }

    "createAdditionalTaxTable is called" must {

      "return additional tax detail rows" when {

        "there are additional tax due" in {
          val otherTaxDue = Seq(
            TaxAdjustmentComponent(ExcessGiftAidTax, 100),
            TaxAdjustmentComponent(ExcessWidowsAndOrphans, 100),
            TaxAdjustmentComponent(PensionPaymentsAdjustment, 200),
            TaxAdjustmentComponent(ChildBenefit, 300)
          )
          val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, Some(tax.TaxAdjustment(700, otherTaxDue)), None, None)
          val codingComponents = Seq(
            CodingComponent(UnderPaymentFromPreviousYear, None, 100, "", Some(10)),
            CodingComponent(EstimatedTaxYouOweThisYear, None, 0, "", Some(50)),
            CodingComponent(OutstandingDebt, None, 150, "")
          )

          val result = DetailedIncomeTaxEstimateViewModel.createAdditionalTaxTable(codingComponents, totalTax)

          result mustBe Seq(
            AdditionalTaxDetailRow(Label(Messages("tai.taxCalc.UnderpaymentPreviousYear.title"),
              Some(HelpLink(Messages("what.does.this.mean"),
                controllers.routes.UnderpaymentFromPreviousYearController.underpaymentExplanation.url.toString, "underPaymentFromPreviousYear"))), 10),
            AdditionalTaxDetailRow(Label(Messages("tai.taxcode.deduction.type-45"),
              Some(HelpLink(Messages("what.does.this.mean"),
                controllers.routes.PotentialUnderpaymentController.potentialUnderpaymentPage.url.toString, "estimatedTaxOwedLink"))), 50),
            AdditionalTaxDetailRow(Label(Messages("tai.taxCalc.OutstandingDebt.title")), 150),
            AdditionalTaxDetailRow(Label(Messages("tai.taxCalc.childBenefit.title")), 300),
            AdditionalTaxDetailRow(Label(Messages("tai.taxCalc.excessGiftAidTax.title")), 100),
            AdditionalTaxDetailRow(Label(Messages("tai.taxCalc.excessWidowsAndOrphans.title")), 100),
            AdditionalTaxDetailRow(Label(Messages("tai.taxCalc.pensionPaymentsAdjustment.title")), 200)
          )
        }
      }

      "return empty row" when {

        "there is no additional tax due" in {
          val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)

          val result = DetailedIncomeTaxEstimateViewModel.createAdditionalTaxTable(Seq.empty[CodingComponent], totalTax)

          result mustBe Seq.empty[AdditionalTaxDetailRow]
        }
      }
    }

    "createReductionsTable is called" must {

      "return reduction tax table" when {

        "there are components present which can reduce the tax" in {

          val alreadyTaxedAtSource = Seq(
            TaxAdjustmentComponent(TaxOnBankBSInterest, 100),
            TaxAdjustmentComponent(TaxCreditOnUKDividends, 200),
            TaxAdjustmentComponent(TaxCreditOnForeignInterest, 300),
            TaxAdjustmentComponent(TaxCreditOnForeignIncomeDividends, 400)
          )

          val reliefsGivingBackTax = Seq(
            TaxAdjustmentComponent(EnterpriseInvestmentSchemeRelief, 500),
            TaxAdjustmentComponent(ConcessionalRelief, 600),
            TaxAdjustmentComponent(MaintenancePayments, 700),
            TaxAdjustmentComponent(MarriedCouplesAllowance, 800),
            TaxAdjustmentComponent(DoubleTaxationRelief, 900)
          )

          val taxReliefComponent = Seq(
            TaxAdjustmentComponent(GiftAidPaymentsRelief, 1000),
            TaxAdjustmentComponent(PersonalPensionPaymentRelief, 1100)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            Some(tax.TaxAdjustment(3500, reliefsGivingBackTax)),
            None,
            Some(tax.TaxAdjustment(1000, alreadyTaxedAtSource)),
            Some(100),
            Some(tax.TaxAdjustment(2100, taxReliefComponent))
          )

          val codingComponents = Seq(
            CodingComponent(MarriedCouplesAllowanceMAE, None, 1200, "", None)
          )

          val result: Seq[ReductionTaxRow] = DetailedIncomeTaxEstimateViewModel.createReductionsTable(codingComponents, totalTax)

          result mustBe Seq(
            ReductionTaxRow(Messages("tai.taxCollected.atSource.otherIncome.description"), 100, Messages("tai.taxCollected.atSource.otherIncome.title")),
            ReductionTaxRow(Messages("tai.taxCollected.atSource.dividends.description", 10), 200, Messages("tai.taxCollected.atSource.dividends.title")),
            ReductionTaxRow(Messages("tai.taxCollected.atSource.bank.description", 20), 100, Messages("tai.taxCollected.atSource.bank.title")),
            ReductionTaxRow(Messages("tai.taxCollected.atSource.marriageAllowance.description", MoneyPounds(1200).quantity,
              Link.toInternalPage(
                url = routes.YourTaxCodeController.taxCodes().toString,
                value = Some(Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText"))
              ).toHtml.body), 800, Messages("tai.taxCollected.atSource.marriageAllowance.title")),
            ReductionTaxRow(Messages("tai.taxCollected.atSource.maintenancePayments.description", MoneyPounds(1200).quantity,
              routes.YourTaxCodeController.taxCodes().url), 700, Messages("tai.taxCollected.atSource.marriageAllowance.title")),
            ReductionTaxRow(Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.description"),
              500, Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.title")),
            ReductionTaxRow(Messages("tai.taxCollected.atSource.concessionalRelief.description"),
              600, Messages("tai.taxCollected.atSource.concessionalRelief.title")),
            ReductionTaxRow(Messages("tai.taxCollected.atSource.doubleTaxationRelief.description"),
              900, Messages("tai.taxCollected.atSource.doubleTaxationRelief.title")),
            ReductionTaxRow(Messages("gift.aid.tax.relief",0,1000),
              1000, Messages("gift.aid.savings")),
            ReductionTaxRow(Messages("personal.pension.payment.relief",0,1100),
              1100, Messages("personal.pension.payments"))
          )
        }
      }

      "return empty reduction tax table" when {

        "there is no reduction in tax" in {

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
          val result = DetailedIncomeTaxEstimateViewModel.createReductionsTable(Seq.empty[CodingComponent], totalTax)

          result mustBe Seq.empty[ReductionTaxRow]
        }
      }
    }



    "return tax bands from all categories" in {
      val taxBand = Seq(TaxBand(bandType = "", code = "", income = 100, tax = 0, lowerBand = None, upperBand = None, rate = 20))
      val incomeCategories = Seq(
        IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
        IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
        IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
        IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
        IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
        IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
      )
      val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

      val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

      model.nonSavings mustEqual Seq(TaxBand(TaxFreeAllowanceBand,"",0,0,Some(0),None,0)) ++ taxBand
      model.savings mustEqual  taxBand ++ taxBand ++ taxBand
      model.dividends mustEqual taxBand ++ taxBand
    }

    "return empty tax bands"  ignore {
      "only zero rate bands are present with no income" in {
        val taxBand = Seq(TaxBand(bandType = "", code = "", income = 100, tax = 0, lowerBand = None, upperBand = None, rate = 0))
        val incomeCategories = Seq(
          IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
          IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
          IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
          IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
          IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
        )
        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

        val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

        model.nonSavings mustBe Seq.empty[TaxBand]
        model.savings mustBe  Seq.empty[TaxBand]
        model.dividends mustBe Seq.empty[TaxBand]
      }

      "income is zero" in {
        val taxBand = Seq(TaxBand(bandType = "", code = "", income = 0, tax = 0, lowerBand = None, upperBand = None, rate = 20))
        val incomeCategories = Seq(
          IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
          IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
          IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
          IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
          IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
        )
        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

        val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

        model.nonSavings mustBe Seq.empty[TaxBand]
        model.savings mustBe  Seq.empty[TaxBand]
        model.dividends mustBe Seq.empty[TaxBand]
      }
    }

    "additional Income Tax Self Assessment text" should {
      "be returned when Non-Coded Income is present" in {
        val nonTaxCodeIncome = NonTaxCodeIncome(None, List(OtherNonTaxCodeIncome(NonCodedIncome,None,0,"")))
        val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

        model.selfAssessmentAndPayeText mustEqual Some(messagesApi("tai.estimatedIncome.selfAssessmentAndPayeText"))
      }

      "not returned when Non-Coded Income is absent" in {
        val nonTaxCodeIncome = NonTaxCodeIncome(None, List.empty)
        val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

        model.selfAssessmentAndPayeText mustEqual None
      }
    }
  }

  private val totalTax = TotalTax(100, Seq.empty[IncomeCategory], None, None, None)
  private val taxCodeIncomes = Seq.empty[TaxCodeIncome]
  private val taxCodeSummary = TaxAccountSummary(0,0,0,0,0,0,0)
  private val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq.empty)

  val basicModel = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

}
