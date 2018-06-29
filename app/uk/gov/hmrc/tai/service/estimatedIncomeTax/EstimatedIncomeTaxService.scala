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

import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.{TaxAdjustment, TaxAdjustmentType, TaxBand, TotalTax}
import uk.gov.hmrc.tai.viewModels.EstimatedIncomeTaxViewModel._
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.ComplexEstimatedIncomeTaxViewModel.{ScottishTaxRegion, UkTaxRegion}

import scala.math.BigDecimal

trait EtimatedIncomeTaxServiceTemp{

  def createPABand(taxFreeAllowance: BigDecimal) = {
    TaxBand(TaxFreeAllowanceBand, "", taxFreeAllowance, 0, Some(0), None, 0)
  }

  def personalAllowanceAmount(codingComponents: Seq[CodingComponent]) = {
    codingComponents.find { component =>
      component.componentType match {
        case compType if compType == PersonalAllowancePA || compType == PersonalAllowanceAgedPAA || compType == PersonalAllowanceElderlyPAE => true
        case _ => false
      }
    }.map(_.amount)
  }

  def hasIncome(taxCodeIncomes: Seq[TaxCodeIncome]) = taxCodeIncomes.nonEmpty


  def retrieveTaxBands(taxBands: List[TaxBand]): List[TaxBand] = {
    val mergedPsaBands = mergeAllowanceTaxBands(taxBands, PersonalSavingsRate)
    val mergedSrBands = mergeAllowanceTaxBands(mergedPsaBands, StarterSavingsRate)
    val bands = mergeAllowanceTaxBands(mergedSrBands, TaxFreeAllowanceBand)
    bands.filter(_.income > 0).sortBy(_.rate)
  }

  def mergeAllowanceTaxBands(taxBands: List[TaxBand], bandType: String) = {
    val (bands, remBands) = taxBands.partition(_.bandType == bandType)
    bands match {
      case Nil => remBands
      case _ => TaxBand(bands.map(_.bandType).head,
        bands.map(_.code).head,
        bands.map(_.income).sum,
        bands.map(_.tax).sum,
        bands.map(_.lowerBand).head,
        bands.map(_.upperBand).head,
        bands.map(_.rate).head) :: remBands
    }
  }

  def findTaxRegion(taxCodeIncomes: Seq[TaxCodeIncome]): String = {
    if(taxCodeIncomes.exists(_.taxCode.startsWith("S"))) ScottishTaxRegion else UkTaxRegion
  }

}


trait EtimatedIncomeTaxServiceComplex{

  def taxAdjustmentComp(taxAdjustment: Option[TaxAdjustment], adjustmentType: TaxAdjustmentType) = {
    taxAdjustment.
      flatMap(_.taxAdjustmentComponents.find(_.taxAdjustmentType == adjustmentType))
      .map(_.taxAdjustmentAmount)
  }

  def underPaymentFromPreviousYear(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == UnderPaymentFromPreviousYear).flatMap(_.inputAmount)

  def inYearAdjustment(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == EstimatedTaxYouOweThisYear).flatMap(_.inputAmount)

  def outstandingDebt(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == OutstandingDebt).map(_.amount)

  def fetchIncome(mergedTaxBands: List[TaxBand], bandType: String): Option[BigDecimal] = {
    mergedTaxBands.find(band => band.bandType == bandType && band.income > 0).map(_.income)
  }
}




object EstimatedIncomeTaxService extends EtimatedIncomeTaxServiceComplex with EtimatedIncomeTaxServiceTemp{

  def taxBand(totalTax: TotalTax) = totalTax.incomeCategories.flatMap(_.taxBands)

  def taxViewType(codingComponents: Seq[CodingComponent],
                  totalTax: TotalTax,
                  nonTaxCodeIncome: NonTaxCodeIncome,
                  taxBands: List[TaxBand],
                  totalInYearAdjustmentIntoCY:BigDecimal,
                  totalInYearAdjustmentIntoCYPlusOne:BigDecimal,
                  totalEstimatedIncome:BigDecimal,
                  taxFreeAllowance:BigDecimal,totalEstimatedTax:BigDecimal): TaxViewType = {

    isComplexViewType(codingComponents, totalTax, nonTaxCodeIncome, taxBands, totalInYearAdjustmentIntoCY, totalInYearAdjustmentIntoCYPlusOne) match {
      case true => ComplexTaxView
      case false => {
        (totalEstimatedIncome < taxFreeAllowance) && totalEstimatedTax == 0 match {
          case true => ZeroTaxView
          case false => SimpleTaxView
        }
      }
    }
  }

  def isComplexViewType(codingComponents: Seq[CodingComponent],
                        totalTax: TotalTax,
                        nonTaxCodeIncome: NonTaxCodeIncome,
                        taxBands: List[TaxBand],
                        totalInYearAdjustmentIntoCY:BigDecimal, totalInYearAdjustmentIntoCYPlusOne:BigDecimal) :Boolean ={

    hasReductions(codingComponents,totalTax) ||
    hasAdditionalTax(codingComponents,totalTax) ||
    hasDividends(nonTaxCodeIncome,totalTax) ||
    hasPotentialUnderPayment(totalInYearAdjustmentIntoCY, totalInYearAdjustmentIntoCYPlusOne) ||
    hasTaxRelief(totalTax) ||
    hasSSR(taxBands) ||
    hasPSR(taxBands)
  }

  def hasReductions(codingComponents: Seq[CodingComponent], totalTax: TotalTax): Boolean = {
    totalTax.taxOnOtherIncome.isDefined ||
    taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxCreditOnUKDividends).isDefined ||
    taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxOnBankBSInterest).isDefined ||
    taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.EnterpriseInvestmentSchemeRelief).isDefined ||
    taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.ConcessionalRelief).isDefined ||
    taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.DoubleTaxationRelief).isDefined
  }

  def hasAdditionalTax(codingComponent: Seq[CodingComponent], totalTax: TotalTax): Boolean = {

    underPaymentFromPreviousYear(codingComponent).isDefined ||
    inYearAdjustment(codingComponent).isDefined ||
    outstandingDebt(codingComponent).isDefined ||
    taxAdjustmentComp(totalTax.otherTaxDue, tax.ChildBenefit).isDefined ||
    taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessGiftAidTax).isDefined ||
    taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessWidowsAndOrphans).isDefined ||
    taxAdjustmentComp(totalTax.otherTaxDue, tax.PensionPaymentsAdjustment).isDefined

  }

  def hasDividends(nonTaxCodeIncome: NonTaxCodeIncome, totalTax: TotalTax): Boolean = {
    val ukDividend = nonTaxCodeIncome.otherNonTaxCodeIncomes.find(_.incomeComponentType == UkDividend).map(_.amount)

    ukDividend map { ukDivTotalIncome =>
      val taxBands = totalTax.incomeCategories.filter(_.incomeCategoryType == tax.UkDividendsIncomeCategory).flatMap(_.taxBands)
      val taxFreeDividend = taxBands.find(_.bandType == DividendZeroRate).flatMap(_.upperBand).getOrElse(BigDecimal(0))
      val higherTaxRates = taxBands.filter(taxBand => taxBand.income > 0 && taxBand.rate > 0).map(_.rate)

      (ukDivTotalIncome <= taxFreeDividend) || ((ukDivTotalIncome > taxFreeDividend) && higherTaxRates.nonEmpty) match {
        case true => true
        case _ => false
      }
    } match {
      case Some(true) => true
      case _ => false
    }
  }


  def hasPotentialUnderPayment(totalInYearAdjustmentIntoCY:BigDecimal, totalInYearAdjustmentIntoCYPlusOne:BigDecimal) =
    totalInYearAdjustmentIntoCY <=0 && totalInYearAdjustmentIntoCYPlusOne > 0


  def hasTaxRelief(totalTax: TotalTax): Boolean = {
    totalTax.taxReliefComponent.isDefined
  }

  def hasSSR(taxBands: List[TaxBand]): Boolean ={
    fetchIncome(retrieveTaxBands(taxBands), StarterSavingsRate).isDefined
  }

  def hasPSR(taxBands: List[TaxBand]): Boolean ={
    fetchIncome(retrieveTaxBands(taxBands), PersonalSavingsRate).isDefined
  }

}
