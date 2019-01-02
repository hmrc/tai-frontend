/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service.estimatedIncomeTax

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax._
import uk.gov.hmrc.tai.model.domain.tax
import uk.gov.hmrc.tai.model.domain.tax.{DoubleTaxationRelief, MaintenancePayments => _, _}
import uk.gov.hmrc.tai.util.constants.{BandTypesConstants, TaxRegionConstants}

import scala.collection.immutable.Seq

class EstimatedIncomeTaxServiceSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with BandTypesConstants
with TaxRegionConstants{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "hasNonCodedIncome" must {
    "return true when NonCodedIncome exists" in {
      EstimatedIncomeTaxService.hasNonCodedIncome(Seq(OtherNonTaxCodeIncome(NonCodedIncome,None,0,""))) mustBe true
    }

    "return false when no NonCodedIncome exists" in {
      EstimatedIncomeTaxService.hasNonCodedIncome(Seq(OtherNonTaxCodeIncome(PartTimeEarnings,None,0,""))) mustBe false
    }

    "return false when an empty sequence is returned" in {
      EstimatedIncomeTaxService.hasNonCodedIncome(Seq.empty[OtherNonTaxCodeIncome]) mustBe false
    }

  }

  "hasDividends" must {
    "return true" when {
      "there are UK dividends present with an income greater than zero" in {
        val incomeCategory = Seq(IncomeCategory(UkDividendsIncomeCategory,0,0,15000,
          Seq(TaxBand("SDR", "", 15000, 0, Some(14000), Some(32000), 0))))

        EstimatedIncomeTaxService.hasDividends(incomeCategory) mustBe true

      }

      "there are Foreign dividends present with an income greater than zero" in {
        val incomeCategory = Seq(IncomeCategory(ForeignDividendsIncomeCategory,0,0,15000,
          Seq(TaxBand("SDR", "", 15000, 0, Some(14000), Some(32000), 0))))

        EstimatedIncomeTaxService.hasDividends(incomeCategory) mustBe true
      }

      "there are a mixture of dividends present with an income greater than zero" in {
        val UKDividend = IncomeCategory(UkDividendsIncomeCategory,0,0,15000,
          Seq(TaxBand("SDR", "", 15000, 0, Some(14000), Some(32000), 0)))

        val foreignDividend = IncomeCategory(ForeignDividendsIncomeCategory,0,0,15000,
          Seq(TaxBand("SDR", "", 15000, 0, Some(14000), Some(32000), 0)))

        val incomeCategories = Seq(UKDividend,foreignDividend)

        EstimatedIncomeTaxService.hasDividends(incomeCategories) mustBe true
      }


    }

    "return false" when {
      "there are no dividends present which have no income" in {
        val incomeCategory = List(IncomeCategory(UkDividendsIncomeCategory,0,0,0,
          Seq(TaxBand("SDR", "", 0, 0, Some(14000), Some(32000), 0))))

        EstimatedIncomeTaxService.hasDividends(incomeCategory) mustBe false

      }

      "there are no dividends present" in {

        val taxBands = Seq(TaxBand("PSR", "", 10000, 0, Some(0), Some(11000), 0),
        TaxBand("SR", "", 10000, 0, Some(11000), Some(14000), 0),
        TaxBand("B", "", 10000, 3000, Some(14000), Some(32000), 20))

        val incomeCategory = List(IncomeCategory(UntaxedInterestIncomeCategory,3000,3000,30000,taxBands))

        EstimatedIncomeTaxService.hasDividends(incomeCategory) mustBe false

      }

      "an empty sequence is returned" in {
        EstimatedIncomeTaxService.hasDividends(Seq.empty[IncomeCategory]) mustBe false
      }
    }
  }

  "totalDividendIncome" must {

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

      EstimatedIncomeTaxService.totalDividendIncome(incomeCategories) mustEqual 9000

    }
  }


  "hasAdditionalTax" must {
    "return true" when {
      "there is an underpayment" in {

        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None)
        val codingComponents = Seq(
          CodingComponent(UnderPaymentFromPreviousYear, None, 100, "", Some(10))
        )

        EstimatedIncomeTaxService.hasAdditionalTax(codingComponents, totalTax) mustBe true
      }

      "there is an inYearAdjustment" in {

        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None)
        val codingComponents = Seq(
          CodingComponent(EstimatedTaxYouOweThisYear, None, 0, "", Some(50))
        )

        EstimatedIncomeTaxService.hasAdditionalTax(codingComponents, totalTax) mustBe true
      }

      "there is outstanding debt" in {

        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None, None)
        val codingComponents = Seq(
          CodingComponent(OutstandingDebt, None, 150, "")
        )

        EstimatedIncomeTaxService.hasAdditionalTax(codingComponents, totalTax) mustBe true
      }

      "there is childBenefit" in {

        val otherTaxDue = Seq(
          TaxAdjustmentComponent(tax.ChildBenefit, 300)
        )
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, Some(tax.TaxAdjustment(300, otherTaxDue)), None, None)
        EstimatedIncomeTaxService.hasAdditionalTax(Seq.empty[CodingComponent], totalTax) mustBe true
      }

      "there is excessGiftAid" in {

        val otherTaxDue = Seq(
          TaxAdjustmentComponent(tax.ExcessGiftAidTax, 100)
        )
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, Some(tax.TaxAdjustment(100, otherTaxDue)), None, None)
        EstimatedIncomeTaxService.hasAdditionalTax(Seq.empty[CodingComponent], totalTax) mustBe true
      }

      "there is excessWidowAndOrphans" in {

        val otherTaxDue = Seq(
          TaxAdjustmentComponent(tax.ExcessWidowsAndOrphans, 100)
        )
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, Some(tax.TaxAdjustment(100, otherTaxDue)), None, None)
        EstimatedIncomeTaxService.hasAdditionalTax(Seq.empty[CodingComponent], totalTax) mustBe true
      }

      "there are pensionPaymentsAdjustments" in {

        val otherTaxDue = Seq(
          TaxAdjustmentComponent(tax.PensionPaymentsAdjustment, 200)
        )
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, Some(tax.TaxAdjustment(200, otherTaxDue)), None, None)
        EstimatedIncomeTaxService.hasAdditionalTax(Seq.empty[CodingComponent], totalTax) mustBe true
      }



    }
    "return false" when {
      "there are no additional tax due" in {
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.hasAdditionalTax(Seq.empty[CodingComponent], totalTax) mustBe false

      }
    }
  }

  "hasReductions" must {
    "return true" when {
        "there are non coded incomes" in{

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            None,
            None,
            None,
            Some(100),
            None)

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there are UKDividends" in {

          val alreadyTaxedAtSource = Seq(
            TaxAdjustmentComponent(TaxCreditOnUKDividends, 200)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            None,
            None,
            Some(tax.TaxAdjustment(3500, alreadyTaxedAtSource)),
            None,
            None)

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there is bank interest" in {

          val alreadyTaxedAtSource = Seq(
            TaxAdjustmentComponent(TaxOnBankBSInterest, 200)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            None,
            None,
            Some(tax.TaxAdjustment(3500, alreadyTaxedAtSource)),
            None,
            None)

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there is marriage allowance" in {

          val reliefsGivingBackTax = Seq(
            TaxAdjustmentComponent(MarriedCouplesAllowance, 800)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            Some(tax.TaxAdjustment(800, reliefsGivingBackTax)),
            None,
            None,
            None,
            None)

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there are maintenance payments " in {
          val reliefsGivingBackTax = Seq(
            TaxAdjustmentComponent(uk.gov.hmrc.tai.model.domain.tax.MaintenancePayments, 800)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            Some(tax.TaxAdjustment(800, reliefsGivingBackTax)),
            None,
            None,
            None,
            None)

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there is enterpriseInvestmentScheme " in {
          val reliefsGivingBackTax = Seq(
            TaxAdjustmentComponent(EnterpriseInvestmentSchemeRelief, 500)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            Some(tax.TaxAdjustment(500, reliefsGivingBackTax)),
            None,
            None,
            None,
            None)

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there is concessionRelief " in {
          val reliefsGivingBackTax = Seq(
            TaxAdjustmentComponent(ConcessionalRelief, 600)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            Some(tax.TaxAdjustment(600, reliefsGivingBackTax)),
            None,
            None,
            None,
            None)

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there is doubleTaxationRelief " in {
          val reliefsGivingBackTax = Seq(
            TaxAdjustmentComponent(DoubleTaxationRelief, 900)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            Some(tax.TaxAdjustment(900, reliefsGivingBackTax)),
            None,
            None,
            None,
            None)

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there is giftAidPaymentsRelief " in {
          val taxReliefComponent = Seq(
            TaxAdjustmentComponent(GiftAidPaymentsRelief, 1000)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            None,
            None,
            None,
            None,
            Some(tax.TaxAdjustment(1000, taxReliefComponent)))

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }

        "there is personalPensionPaymentsRelief " in {
          val taxReliefComponent = Seq(
            TaxAdjustmentComponent(PersonalPensionPaymentRelief, 1000)
          )

          val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
            None,
            None,
            None,
            None,
            Some(tax.TaxAdjustment(1000, taxReliefComponent)))

          EstimatedIncomeTaxService.hasReductions(totalTax) mustBe true
        }
    }

    "return false" when {
      "there are no reductions" in {
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.hasReductions(totalTax) mustBe false
      }
    }
  }

  "isComplexViewType" must {
    "return true" when {
      "one complex scenario is met" when {
        "reductions exist" in{
          EstimatedIncomeTaxService.isComplexViewType(codingComponents,totalTax, nonTaxCodeIncome) mustBe true
        }
      }
    }
    "return false" when {
      "no complex scenarios exist" in {
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.isComplexViewType(Seq.empty,totalTax, nonTaxCodeIncome) mustBe false
      }
    }
  }

  "hasSavings" must {
    "return true when there are savings present in totalTax" in {
      val taxBands = Seq(
        TaxBand(bandType = DividendZeroRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 0),
        TaxBand(bandType = DividendBasicRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 10),
        TaxBand(bandType = DividendHigherRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand(bandType = DividendAdditionalRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 30)
      )
      val incomeCategories = Seq(
        IncomeCategory(UntaxedInterestIncomeCategory, 0, 6000, 0, taxBands),
        IncomeCategory(BankInterestIncomeCategory, 0, 6000, 0, taxBands),
        IncomeCategory(ForeignInterestIncomeCategory, 0, 6000, 0, taxBands)
      )
      val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

      EstimatedIncomeTaxService.hasSavings(totalTax) mustBe true

    }

    "return false when there are no savings present in totalTax" in {
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

      EstimatedIncomeTaxService.hasSavings(totalTax) mustBe false

    }
  }

  "taxViewType" must {

    "return noIncome" when{
      "there is no current income" in{
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.taxViewType(codingComponents,totalTax,nonTaxCodeIncome,0,0,0,false) mustBe NoIncomeTaxView
      }
    }
    "return complex" when {
      "isComplexViewType returns true" in{
        EstimatedIncomeTaxService.taxViewType(codingComponents,totalTax,nonTaxCodeIncome,0,11500,0, true) mustBe ComplexTaxView
      }
    }
    "return simple" when {
      "the totalEstimatedIncome is greater than the taxFreeAllowance and the totalEstimatedTax is greater than zero" in{
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.taxViewType(Seq.empty,totalTax,nonTaxCodeIncome,12000,11500,100, true) mustBe SimpleTaxView
      }
    }
    "return zero" when {
      "the totalEstimatedIncome is less than the taxFreeAllowance and the totalEstimatedTax is zero" in{
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.taxViewType(Seq.empty,totalTax,nonTaxCodeIncome,11000,11500,0, true) mustBe ZeroTaxView
      }
    }

    "createPABand must return a Personal Allowance Taxband for a given Tax Free Allowance" in {
      EstimatedIncomeTaxService.createPABand(11500) mustBe TaxBand(TaxFreeAllowanceBand, "", 11500, 0, Some(0), None, 0)
    }

    "findTaxRegion" must {
      "return UK when a UK TaxCode is present" in {
        EstimatedIncomeTaxService.findTaxRegion(ukTaxCodeIncome) mustBe UkTaxRegion
      }
      "return Scottish when a Scottish TaxCode is Present" in {
        EstimatedIncomeTaxService.findTaxRegion(ScottishTaxCodeIncome) mustBe ScottishTaxRegion
      }
    }

    "retrieve tax bands" must {

      "return tax bands" in {
        val taxBand = List(
          TaxBand("pa", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
          TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
          TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
        )

        val taxBands = EstimatedIncomeTaxService.retrieveTaxBands(taxBand)

        taxBands mustBe taxBand

      }

      "return sorted tax bands" in {
        val taxBand = List(
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
          TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45),
          TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40)
        )

        val taxBands = EstimatedIncomeTaxService.retrieveTaxBands(taxBand)

        taxBands mustBe List(
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
          TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
          TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
        )
      }

      "return merged tax bands for multiple PSR bands" in {
        val bankIntTaxBand = List(
          TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

        val untaxedTaxBand = List(TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0),
          upperBand = Some(11000), rate = 0))


        val taxBands = EstimatedIncomeTaxService.retrieveTaxBands(bankIntTaxBand ::: untaxedTaxBand)

        taxBands mustBe List(TaxBand("PSR", "", 10000, 0, Some(0), Some(11000), 0),
          TaxBand("B", "", 15000, 3000, Some(11000), Some(32000), 20))
      }

      "return ordered tax bands for multiple PSR SR SDR bands" in {
        val bankIntTaxBand = List(
          TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("SR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

        val untaxedTaxBand = List(
          TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("SDR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0))


        val taxBands = EstimatedIncomeTaxService.retrieveTaxBands(bankIntTaxBand ::: untaxedTaxBand)

        val resBands = List(TaxBand("SR", "", 5000, 0, Some(0), Some(11000), 0),
          TaxBand("PSR", "", 10000, 0, Some(0), Some(11000), 0),
          TaxBand("SDR", "", 5000, 0, Some(0), Some(11000), 0),
          TaxBand("B", "", 15000, 3000, Some(11000), Some(32000), 20))

        taxBands mustBe resBands
      }
    }

  }

  val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq(
    OtherNonTaxCodeIncome(UkDividend, None, 3000, "")
  ))

  val alreadyTaxedAtSource = Seq(
    TaxAdjustmentComponent(tax.TaxOnBankBSInterest, 100),
    TaxAdjustmentComponent(tax.TaxCreditOnUKDividends, 200),
    TaxAdjustmentComponent(tax.TaxCreditOnForeignInterest, 300),
    TaxAdjustmentComponent(tax.TaxCreditOnForeignIncomeDividends, 400)
  )

  val reliefsGivingBackTax = Seq(
    TaxAdjustmentComponent(tax.EnterpriseInvestmentSchemeRelief, 500),
    TaxAdjustmentComponent(tax.ConcessionalRelief, 600),
    TaxAdjustmentComponent(tax.MaintenancePayments, 700),
    TaxAdjustmentComponent(tax.MarriedCouplesAllowance, 800),
    TaxAdjustmentComponent(tax.DoubleTaxationRelief, 900)
  )

  val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
    Some(tax.TaxAdjustment(3500, reliefsGivingBackTax)),
    None,
    Some(tax.TaxAdjustment(1000, alreadyTaxedAtSource)),
    Some(100))

  val codingComponents = Seq(
    CodingComponent(MarriedCouplesAllowanceMAE, None, 1200, "", None),
    CodingComponent(MaintenancePayments, None, 1200, "", None)
  )

  val ukTaxCodeIncome = Seq(
    TaxCodeIncome(EmploymentIncome,Some(1),BigDecimal(15000),"EmploymentIncome","1150L","TestName",
      OtherBasisOfOperation,Live,None,Some(new LocalDate(2015,11,26)),Some(new LocalDate(2015,11,26)))
  )

  val ScottishTaxCodeIncome = Seq(
    TaxCodeIncome(EmploymentIncome,Some(1),BigDecimal(99999),"EmploymentIncome","SK723","TestName",
      OtherBasisOfOperation,Live,None,Some(new LocalDate(2015,11,26)),Some(new LocalDate(2015,11,26)))
  )

}
