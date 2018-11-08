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
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, OtherNonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.util.constants.{BandTypesConstants, TaxRegionConstants}
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax._

import scala.math.BigDecimal

object EstimatedIncomeTaxService extends TaxRegionConstants with BandTypesConstants{


  def taxViewType(codingComponents: Seq[CodingComponent],
                  totalTax: TotalTax,
                  nonTaxCodeIncome: NonTaxCodeIncome,
                  totalEstimatedIncome:BigDecimal,
                  taxFreeAllowance:BigDecimal,totalEstimatedTax:BigDecimal,
                  hasCurrentIncome:Boolean): TaxViewType = {


    hasCurrentIncome match {
      case false => NoIncomeTaxView
      case true => {
        isComplexViewType(codingComponents, totalTax, nonTaxCodeIncome) match {
          case true => ComplexTaxView
          case false => {
            (totalEstimatedIncome < taxFreeAllowance) && totalEstimatedTax == 0 match {
              case true => ZeroTaxView
              case false => SimpleTaxView
            }
          }
        }
      }
    }
  }

  def isComplexViewType(codingComponents: Seq[CodingComponent], totalTax: TotalTax, nonTaxCodeIncome: NonTaxCodeIncome) :Boolean ={

    val reductionsExist = hasReductions(totalTax)
    val additionalTaxDue = hasAdditionalTax(codingComponents,totalTax)
    val dividendsExist = hasDividends(totalTax.incomeCategories)
    val nonCodedIncomeExists = hasNonCodedIncome(nonTaxCodeIncome.otherNonTaxCodeIncomes)
    val savingsExist = hasSavings(totalTax)

    reductionsExist ||
    additionalTaxDue ||
    dividendsExist ||
    nonCodedIncomeExists ||
    savingsExist
  }

  def hasReductions(totalTax: TotalTax): Boolean = {

    val nonCodedIncome = totalTax.taxOnOtherIncome
    val ukDividend = taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxCreditOnUKDividends)
    val bankInterest = taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxOnBankBSInterest)
    val marriageAllowance = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MarriedCouplesAllowance)
    val maintenancePayment = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MaintenancePayments)
    val enterpriseInvestmentScheme = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.EnterpriseInvestmentSchemeRelief)
    val concessionRelief = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.ConcessionalRelief)
    val doubleTaxationRelief = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.DoubleTaxationRelief)
    val giftAidPaymentsRelief = taxAdjustmentComp(totalTax.taxReliefComponent, tax.GiftAidPaymentsRelief)
    val personalPensionPaymentsRelief = taxAdjustmentComp(totalTax.taxReliefComponent, tax.PersonalPensionPaymentRelief)

    nonCodedIncome.isDefined ||
    ukDividend.isDefined ||
    bankInterest.isDefined ||
    marriageAllowance.isDefined ||
    maintenancePayment.isDefined ||
    enterpriseInvestmentScheme.isDefined ||
    concessionRelief.isDefined ||
    doubleTaxationRelief.isDefined  ||
    giftAidPaymentsRelief.isDefined ||
    personalPensionPaymentsRelief.isDefined

  }

  def hasAdditionalTax(codingComponent: Seq[CodingComponent], totalTax: TotalTax): Boolean = {

    val underPayment = underPaymentFromPreviousYear(codingComponent)
    val inYearAdjust = inYearAdjustment(codingComponent)
    val debtOutstanding = outstandingDebt(codingComponent)
    val childBenefit = taxAdjustmentComp(totalTax.otherTaxDue, tax.ChildBenefit)
    val excessGiftAid = taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessGiftAidTax)
    val excessWidowAndOrphans = taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessWidowsAndOrphans)
    val pensionPayments = taxAdjustmentComp(totalTax.otherTaxDue, tax.PensionPaymentsAdjustment)

    underPayment.isDefined ||
    inYearAdjust.isDefined ||
    debtOutstanding.isDefined ||
    childBenefit.isDefined ||
    excessGiftAid.isDefined ||
    excessWidowAndOrphans.isDefined ||
    pensionPayments.isDefined
  }

  def savingsBands(totalTax: TotalTax) = {
    totalTax.incomeCategories.filter {
      category =>
        category.incomeCategoryType == UntaxedInterestIncomeCategory ||
          category.incomeCategoryType == BankInterestIncomeCategory || category.incomeCategoryType == ForeignInterestIncomeCategory
    }.flatMap(_.taxBands).filter(_.income > 0)
  }

  def hasSavings(totalTax: TotalTax): Boolean = {
    savingsBands(totalTax).nonEmpty
  }
  def hasNonCodedIncome(otherNonTaxCodeIncomes: Seq[OtherNonTaxCodeIncome]): Boolean = {
    otherNonTaxCodeIncomes.exists(_.incomeComponentType == NonCodedIncome)
  }

  def hasDividends(incomeCategories:Seq[IncomeCategory]): Boolean = {
    totalDividendIncome(incomeCategories) > 0
  }

  def totalDividendIncome(incomeCategories: Seq[IncomeCategory]): BigDecimal = {
    incomeCategories.filter {
      category => category.incomeCategoryType == UkDividendsIncomeCategory ||
        category.incomeCategoryType == ForeignDividendsIncomeCategory
    }.map(_.totalIncome).sum
  }

  def taxAdjustmentComp(taxAdjustment: Option[TaxAdjustment], adjustmentType: TaxAdjustmentType) = {
    taxAdjustment.
      flatMap(_.taxAdjustmentComponents.find(_.taxAdjustmentType == adjustmentType))
      .map(_.taxAdjustmentAmount)
  }

  def underPaymentFromPreviousYear(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == UnderPaymentFromPreviousYear).flatMap(_.inputAmount)

  def inYearAdjustment(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == EstimatedTaxYouOweThisYear).flatMap(_.inputAmount)

  def outstandingDebt(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == OutstandingDebt).map(_.amount)

  def createPABand(taxFreeAllowance: BigDecimal) = {
    TaxBand(TaxFreeAllowanceBand, "", taxFreeAllowance, 0, Some(0), None, 0)
  }

  def retrieveTaxBands(taxBands: List[TaxBand]): List[TaxBand] = {
    val mergedPsaBands = mergeAllowanceTaxBands(taxBands, PersonalSavingsRate)
    val mergedSrBands = mergeAllowanceTaxBands(mergedPsaBands, StarterSavingsRate)
    val bands = mergeAllowanceTaxBands(mergedSrBands, TaxFreeAllowanceBand)
    bands.filter(_.income > 0).sortBy(_.rate)
  }

  private def mergeAllowanceTaxBands(taxBands: List[TaxBand], bandType: String) = {
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
