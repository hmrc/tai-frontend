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

package uk.gov.hmrc.tai.service.estimatedIncomeTax

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax.{MaintenancePayments => _, _}
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{ComplexTaxView, NoIncomeTaxView, SimpleTaxView, ZeroTaxView}

import scala.collection.immutable.Seq

class EstimatedIncomeTaxServiceSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "hasTaxRelief" must {
    "return true" when {
      "tax relief components are present" in {
        val totalTax = TotalTax(100, Seq.empty[IncomeCategory], None, None, None, None,
          Some(tax.TaxAdjustment(100, Seq(TaxAdjustmentComponent(tax.PersonalPensionPayment, 100)))))
        EstimatedIncomeTaxService.hasTaxRelief(totalTax) mustBe true
      }
    }

    "return false" when {
      "tax relief components are not present" in {
        val totalTax = TotalTax(100, Seq.empty[IncomeCategory], None, None, None, None, None)
        EstimatedIncomeTaxService.hasTaxRelief(totalTax) mustBe false
      }
    }
  }



  "hasPotentialUnderpayment" must {
    "return true" when {
      "totalInYearAdjustmentIntoCY is less than or equal to zero and totalInYearAdjustmentIntoCYPlusOne is greater than zero" in {
        val taxAccountSummary = TaxAccountSummary(0,0,0,0,1)
        EstimatedIncomeTaxService.hasPotentialUnderPayment(
          taxAccountSummary.totalInYearAdjustment,taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne) mustBe true
      }
    }

    "return false" when {
      "totalInYearAdjustmentIntoCY is greater than zero and totalInYearAdjustmentIntoCYPlusOne is zero" in {
        val taxAccountSummary = TaxAccountSummary(0,0,1,0,0)
        EstimatedIncomeTaxService.hasPotentialUnderPayment(taxAccountSummary.totalInYearAdjustmentIntoCY,
          taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne) mustBe false
      }
    }
  }


  "hasSSR" must {
    "return true" when{
      "starter service rate tax band exists" in {

        val taxBands:List[TaxBand] = List(
          TaxBand("SR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

        EstimatedIncomeTaxService.hasSSR(taxBands) mustBe true
      }
    }

    "return false" when{
      "starter service rate tax band does not exist" in {

        val taxBands:List[TaxBand] = List(
          TaxBand("D0","", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

        EstimatedIncomeTaxService.hasSSR(taxBands) mustBe false
      }
    }
  }

  "hasPSR" must {
    "return true" when{
      "starter service rate tax band exists" in {

        val taxBands:List[TaxBand] = List(
          TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("SR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

        EstimatedIncomeTaxService.hasPSR(taxBands) mustBe true
      }
    }

    "return false" when{
      "starter service rate tax band does not exist" in {

        val taxBands:List[TaxBand] = List(
          TaxBand("D0","", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
          TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

        EstimatedIncomeTaxService.hasSSR(taxBands) mustBe false
      }
    }
  }

  "hasDividends" must {
    "return true" when {
      "ukDivTotalIncome is less than taxFreeDividend" in {

        val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq(
          OtherNonTaxCodeIncome(UkDividend, None, 100, "")
        ))

        val totalTax = TotalTax(100, Seq(
          IncomeCategory(tax.UkDividendsIncomeCategory, 50, 50, 0, Seq(
            tax.TaxBand("SDR", "", 50, 0, None, Some(2000), 0)
          ))
        ), None, None, None)

        EstimatedIncomeTaxService.hasDividends(nonTaxCodeIncome, totalTax) mustBe true

      }

      "uk div total income is equal to tax free dividend" in {
        val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq(
          OtherNonTaxCodeIncome(UkDividend, None, 2000, "")
          ))

          val totalTax = TotalTax(100, Seq(
            IncomeCategory(tax.UkDividendsIncomeCategory, 50, 50, 0, Seq(
              tax.TaxBand("SDR", "", 50, 0, None, Some(2000), 0)
            ))
          ), None, None, None)
        EstimatedIncomeTaxService.hasDividends(nonTaxCodeIncome, totalTax) mustBe true

      }

      "uk div total income is greater than tax free dividend and higher rate are present" in {
        val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq(
          OtherNonTaxCodeIncome(UkDividend, None, 3000, "")
        ))

        val totalTax = TotalTax(100, Seq(
          IncomeCategory(tax.UkDividendsIncomeCategory, 50, 50, 0, Seq(
            tax.TaxBand("SDR", "", 50, 0, None, Some(2000), 0),
            tax.TaxBand("SDR", "", 50, 0, Some(2000), Some(3000), 20),
            tax.TaxBand("SDR", "", 50, 0, Some(3000), Some(5000), 40)
          ))
        ), None, None, None)

        EstimatedIncomeTaxService.hasDividends(nonTaxCodeIncome, totalTax) mustBe true

      }
    }

    "return false" when {
      "uk div total income is greater than tax free dividend and higher rate are not present" in {

        val totalTax = TotalTax(100, Seq(
          IncomeCategory(tax.UkDividendsIncomeCategory, 50, 50, 0, Seq(
            tax.TaxBand("SDR", "", 50, 0, None, Some(2000), 0)
          ))
        ), None, None, None)

        EstimatedIncomeTaxService.hasDividends(nonTaxCodeIncome, totalTax) mustBe false

      }
    }
  }

  "hasAdditionalTax" must {
    "return true" when {
      "there are additional tax due" in {
        val otherTaxDue = Seq(
          TaxAdjustmentComponent(tax.ExcessGiftAidTax, 100),
          TaxAdjustmentComponent(tax.ExcessWidowsAndOrphans, 100),
          TaxAdjustmentComponent(tax.PensionPaymentsAdjustment, 200),
          TaxAdjustmentComponent(tax.ChildBenefit, 300)
        )
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, Some(tax.TaxAdjustment(700, otherTaxDue)), None, None)
        val codingComponents = Seq(
          CodingComponent(UnderPaymentFromPreviousYear, None, 100, "", Some(10)),
          CodingComponent(EstimatedTaxYouOweThisYear, None, 0, "", Some(50)),
          CodingComponent(OutstandingDebt, None, 150, "")
        )

        EstimatedIncomeTaxService.hasAdditionalTax(codingComponents, totalTax) mustBe true
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
      "there are components present which can reduce the tax" in {

        EstimatedIncomeTaxService.hasReductions(codingComponents, totalTax) mustBe true

      }
    }

    "return false" when {
      "there are no reductions" in {
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.hasReductions(Seq.empty[CodingComponent], totalTax) mustBe false
      }
    }
  }

  "isComplexViewType" must {
    "return true" when {
      "one complex scenario is met" when {
        "reductions exist" in{
          EstimatedIncomeTaxService.isComplexViewType(codingComponents,totalTax,nonTaxCodeIncome,List.empty,0,0) mustBe true
        }
      }
    }
    "return false" when {
      "no complex scenarios exist" in {
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.isComplexViewType(Seq.empty,totalTax,nonTaxCodeIncome,List.empty,0,0) mustBe false
      }
    }
  }

  "taxViewType" must {

    "return noIncome" when{
      "there is no current income" in{
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.taxViewType(codingComponents,totalTax,nonTaxCodeIncome,List.empty,0,0,0,0,0, false) mustBe NoIncomeTaxView
      }
    }
    "return complex" when {
      "isComplexViewType returns true" in{
        EstimatedIncomeTaxService.taxViewType(codingComponents,totalTax,nonTaxCodeIncome,List.empty,0,0,0,11500,0, true) mustBe ComplexTaxView
      }
    }
    "return simple" when {
      "the totalEstimatedIncome is greater than the taxFreeAllowance and the totalEstimatedTax is greater than zero" in{
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.taxViewType(Seq.empty,totalTax,nonTaxCodeIncome,List.empty,0,0,12000,11500,100, true) mustBe SimpleTaxView
      }
    }
    "return zero" when {
      "the totalEstimatedIncome is less than the taxFreeAllowance and the totalEstimatedTax is zero" in{
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        EstimatedIncomeTaxService.taxViewType(Seq.empty,totalTax,nonTaxCodeIncome,List.empty,0,0,11000,11500,0, true) mustBe ZeroTaxView
      }
    }

    "taxBand" must {
      "return a sequence of tax bands" when {
        "income category tax bands exist" in {
          val taxBands = Seq(TaxBand("D0", "", 0, 0, None, None, 20),
                             TaxBand("1150L", "1150L", 10000, 500, Some(5000), Some(20000), 10))

          val totalTax = TotalTax(1000,
            List(IncomeCategory(UkDividendsIncomeCategory, 10, 20, 30, List(TaxBand("D0", "", 0, 0, None, None, 20),
              TaxBand("1150L", "1150L", 10000, 500, Some(5000), Some(20000), 10)))),
            None, None, None)

          EstimatedIncomeTaxService.taxBand(totalTax) mustBe taxBands
        }
        "return an empty sequence" when {
          "income categories does not contain any tax bands" in {
            EstimatedIncomeTaxService.taxBand(totalTax) mustBe Seq.empty

          }
        }
      }
    }

    "hasCurrentIncome" must {
      "return true" when{
        "tax code incomes is not empty" in{
          val taxCodeIncomes = Seq(TaxCodeIncome(EmploymentIncome,Some(1),BigDecimal(39107),"EmploymentIncome","277L","TestName",OtherBasisOperation,Live,None,Some(new LocalDate(2015,11,26)),Some(new LocalDate(2015,11,26))))
          EstimatedIncomeTaxService.hasIncome(taxCodeIncomes) mustBe true
        }
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

}
