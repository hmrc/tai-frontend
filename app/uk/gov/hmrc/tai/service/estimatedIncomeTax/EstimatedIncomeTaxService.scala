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
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.util.BandTypesConstants
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax._

import scala.math.BigDecimal

object EstimatedIncomeTaxService extends TaxAdditionsAndReductions with EstimatedIncomeTaxBand with BandTypesConstants
  with Dividends{


  def taxViewType(codingComponents: Seq[CodingComponent],
                  totalTax: TotalTax,
                  nonTaxCodeIncome: NonTaxCodeIncome,
                  totalInYearAdjustmentIntoCY:BigDecimal,
                  totalInYearAdjustmentIntoCYPlusOne:BigDecimal,
                  totalEstimatedIncome:BigDecimal,
                  taxFreeAllowance:BigDecimal,totalEstimatedTax:BigDecimal,
                  hasCurrentIncome:Boolean): TaxViewType = {


    hasCurrentIncome match {
      case false => NoIncomeTaxView
      case true => {
        isComplexViewType(codingComponents, totalTax, nonTaxCodeIncome, totalInYearAdjustmentIntoCY, totalInYearAdjustmentIntoCYPlusOne) match {
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

  def isComplexViewType(codingComponents: Seq[CodingComponent],
                        totalTax: TotalTax,
                        nonTaxCodeIncome: NonTaxCodeIncome,
                        totalInYearAdjustmentIntoCY:BigDecimal, totalInYearAdjustmentIntoCYPlusOne:BigDecimal) :Boolean ={

    val taxBands = totalTax.incomeCategories.flatMap(_.taxBands).toList

    hasReductions(codingComponents,totalTax) ||
    hasAdditionalTax(codingComponents,totalTax) ||
    hasDividends(totalTax.incomeCategories) ||
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

  def hasTaxRelief(totalTax: TotalTax): Boolean = {
    totalTax.taxReliefComponent.isDefined
  }

  def hasSSR(taxBands: List[TaxBand]): Boolean ={
    incomeByBandType(retrieveTaxBands(taxBands), StarterSavingsRate).isDefined
  }

  def hasPSR(taxBands: List[TaxBand]): Boolean ={
    incomeByBandType(retrieveTaxBands(taxBands), PersonalSavingsRate).isDefined
  }

  def incomeByBandType(taxBands: List[TaxBand], bandType: String): Option[BigDecimal] = {
    taxBands.find(band => band.bandType == bandType && band.income > 0).map(_.income)
  }

}
