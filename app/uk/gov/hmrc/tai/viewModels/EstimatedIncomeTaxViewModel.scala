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

package uk.gov.hmrc.tai.viewModels

import controllers.routes
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.{TaxAdjustment, TaxAdjustmentType, TaxBand, TotalTax}
import uk.gov.hmrc.tai.model.nps2.DeductionType.InYearAdjustment
import uk.gov.hmrc.tai.util._
import uk.gov.hmrc.urls.Link

import scala.math.BigDecimal

case class AdditionalTaxDetailRow(
                                   description: String,
                                   amount: BigDecimal,
                                   url: Option[String] = None
                                 )

case class ReductionTaxRow(
                            description: String,
                            amount: BigDecimal,
                            title: String
                          )

case class EstimatedIncomeTaxViewModel(
                                        hasCurrentIncome: Boolean,
                                        incomeTaxEstimate: BigDecimal,
                                        incomeEstimate: BigDecimal,
                                        taxFreeEstimate: BigDecimal,
                                        graph: BandedGraph,
                                        additionalTaxTable: Seq[AdditionalTaxDetailRow],
                                        additionalTaxTableTotal: BigDecimal,
                                        reductionTaxTable: Seq[ReductionTaxRow],
                                        reductionTaxTableTotal: BigDecimal,
                                        incomeTaxReducedToZeroMessage: Option[String],
                                        hasPotentialUnderPayment: Boolean,
                                        ssrValue: Option[BigDecimal],
                                        psrValue: Option[BigDecimal],
                                        dividendsMessage: Option[String],
                                        taxRegion: String
                                      ) extends ViewModelHelper

object EstimatedIncomeTaxViewModel extends BandTypesConstants with TaxRegionConstants {

  def apply(codingComponents: Seq[CodingComponent], taxAccountSummary: TaxAccountSummary,
            totalTax: TotalTax, nonTaxCodeIncome: NonTaxCodeIncome, taxCodeIncomes: Seq[TaxCodeIncome]): EstimatedIncomeTaxViewModel = {
    val taxBands = totalTax.incomeCategories.flatMap(_.taxBands)
    val personalAllowance = personalAllowanceAmount(codingComponents)
    val paBand = createPABand(taxAccountSummary.taxFreeAllowance)
    val graph = createBandedGraph(retrieveTaxBands(taxBands.toList :+ paBand), personalAllowance, taxAccountSummary.taxFreeAllowance)
    val additionalTaxTable = createAdditionalTaxTable(codingComponents, totalTax)
    val additionalTaxTableTotal = additionalTaxTable.map(_.amount).sum
    val reductionTaxTable = createReductionsTable(codingComponents, totalTax)
    val reductionTaxTableTotal = reductionTaxTable.map(_.amount).sum
    val incomeTaxReducedToZero = incomeTaxReducedToZeroMessage(taxAccountSummary.totalEstimatedTax <= 0 && reductionTaxTable.nonEmpty)
    val hasPotentialUnderPayment = taxAccountSummary.totalInYearAdjustmentIntoCY <= 0 && taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne > 0
    val ssrValue = taxBands.find(band => band.bandType == StarterSavingsRate && band.income > 0).map(_.income)
    val psrValue = taxBands.find(band => band.bandType == PersonalSavingsRate && band.income > 0).map(_.income)
    val dividends = dividendsMessage(nonTaxCodeIncome, totalTax)
    val taxRegion = findTaxRegion(taxCodeIncomes)

    EstimatedIncomeTaxViewModel(taxCodeIncomes.nonEmpty,
      taxAccountSummary.totalEstimatedTax,
      taxAccountSummary.totalEstimatedIncome,
      taxAccountSummary.taxFreeAllowance,
      graph,
      additionalTaxTable,
      additionalTaxTableTotal,
      reductionTaxTable,
      reductionTaxTableTotal,
      incomeTaxReducedToZero,
      hasPotentialUnderPayment,
      ssrValue,
      psrValue,
      dividends,
      taxRegion)
  }

  def findTaxRegion(taxCodeIncomes: Seq[TaxCodeIncome]): String = {
    if(taxCodeIncomes.exists(_.taxCode.startsWith("S"))) ScottishTaxRegion else UkTaxRegion
  }

  private def personalAllowanceAmount(codingComponents: Seq[CodingComponent]) = {
    codingComponents.find { component =>
      component.componentType match {
        case compType if compType == PersonalAllowancePA || compType == PersonalAllowanceAgedPAA || compType == PersonalAllowanceElderlyPAE => true
        case _ => false
      }
    }.map(_.amount)
  }

  private def createPABand(taxFreeAllowance: BigDecimal) = {
    TaxBand(TaxFreeAllowanceBand, "", taxFreeAllowance, 0, Some(0), None, 0)
  }

  def retrieveTaxBands(taxBands: List[TaxBand]): List[TaxBand] = {
    val mergedPsaBands = mergeAllowanceTaxBands(taxBands, PersonalSavingsRate)
    val mergedSrBands = mergeAllowanceTaxBands(mergedPsaBands, StarterSavingsRate)
    val bands = mergeAllowanceTaxBands(mergedSrBands, TaxFreeAllowanceBand)
    bands.filter(_.income > 0).sortBy(_.rate)
  }

  private def mergeAllowanceTaxBands(taxBands: List[TaxBand], bandType:  String) = {
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


  def createBandedGraph(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0): BandedGraph = {
    taxBands match {
      case Nil => BandedGraph("taxGraph")
      case taxbands => createGraph(taxbands, personalAllowance, taxFreeAllowanceBandSum)
    }
  }

  private def createGraph(taxbands: List[TaxBand], personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0): BandedGraph = {
    val (individualBand: List[Band], mergedBand: Option[Band])  = {
      if(taxbands.exists(_.rate == 0)){
        (individualBands(taxbands, personalAllowance, taxFreeAllowanceBandSum), mergedBands(taxbands, personalAllowance, taxFreeAllowanceBandSum))
      } else{
        (individualOtherRateBands(taxbands, None, taxFreeAllowanceBandSum), None)
      }
    }

    val allBands = mergedBand match {
      case Some(band) => individualBand :+ band
      case _ => individualBand
    }

    val nextHigherBand = getUpperBand(taxbands, personalAllowance, taxFreeAllowanceBandSum)
    val incomeTotal = allBands.map(_.income).sum
    val nextBandMessage = createNextBandMessage(nextHigherBand - incomeTotal)
    val zeroRateBands = individualBand.filter(_.tax == BigDecimal(0))

    BandedGraph("taxGraph",
      allBands,
      nextBand = nextHigherBand,
      incomeTotal = incomeTotal,
      zeroIncomeAsPercentage = zeroRateBands.map(_.barPercentage).sum,
      zeroIncomeTotal = zeroRateBands.map(_.income).sum,
      incomeAsPercentage = allBands.map(_.barPercentage).sum,
      taxTotal = allBands.map(_.tax).sum,
      nextBandMessage = nextBandMessage
    )
  }

  def individualBands(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0): List[Band] =
    for (taxBand <- taxBands.filter(_.rate == 0)) yield Band("TaxFree", calcBarPercentage(taxBand.income, taxBands, personalAllowance, taxFreeAllowanceBandSum),
      "0%", taxBand.income, taxBand.tax, taxBand.bandType)

  def individualOtherRateBands(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0): List[Band] =
    for (taxBand <- taxBands) yield Band("Band", calcBarPercentage(taxBand.income, taxBands, personalAllowance, taxFreeAllowanceBandSum),
      s"${taxBand.rate}%", taxBand.income, taxBand.tax, taxBand.bandType)


  def getUpperBand(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0): BigDecimal = {
    taxBands match  {
      case Nil => BigDecimal(0)
      case _ =>
        val lstBand = taxBands.last
        val income = taxBands.map(_.income).sum
        val upperBand: BigDecimal = {
          if (lstBand.upperBand.contains(0)) {
            lstBand.lowerBand.map(lBand => lBand + taxFreeAllowanceBandSum)
          } else {
            lstBand.upperBand.map(upBand => {
              if(upBand >= TaiConstants.HigherRateBandIncome){
                upBand + taxFreeAllowanceBandSum - personalAllowance.getOrElse(0)
              }
              else {
                upBand + taxFreeAllowanceBandSum
              }
            })
          }
        }.getOrElse(taxFreeAllowanceBandSum)

        if (income > upperBand) income else upperBand
    }

  }

  def calcBarPercentage(incomeBand: BigDecimal, taxBands: List[TaxBand],
                        personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0): BigDecimal = {
    taxBands match {
      case Nil => BigDecimal(0)
      case _ =>
        val percentage = (incomeBand * 100) / getUpperBand(taxBands, personalAllowance, taxFreeAllowanceBandSum)
        percentage.setScale(2, BigDecimal.RoundingMode.FLOOR)
    }
  }

  def mergedBands(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0): Option[Band] = {
    val nonZeroBands = taxBands.filter(_.rate != 0)

    Option(nonZeroBands.nonEmpty).collect {
      case true =>
        val (tablePercentage, bandType, incomeSum) = getBandValues(nonZeroBands)

        Band("Band", calcBarPercentage(incomeSum, taxBands, personalAllowance, taxFreeAllowanceBandSum),
          tablePercentage = tablePercentage,
          income = incomeSum,
          tax = nonZeroBands.map(_.tax).sum,
          bandType = bandType
        )
    }
  }

  private def getBandValues(nonZeroBands: List[TaxBand]) = {
    if (nonZeroBands.size > 1) {
      (Link.toInternalPage(
        url = routes.TaxExplanationControllerNew.taxExplanationPage().toString,
        value = Some(Messages("tai.mergedTaxBand.description")),
        id = Some("taxExplanation")
      ).toHtml.body, Messages("tai.taxedIncome.desc"), nonZeroBands.map(_.income).sum)
    } else {
      nonZeroBands.map(otherBand => (otherBand.rate.toString() + "%", otherBand.bandType, otherBand.income)).head
    }
  }

  private def createNextBandMessage(amount: BigDecimal): Option[String] = {
    Option(amount > 0).collect {
      case true => Messages("tai.taxCalc.nextTaxBand", MoneyPounds(amount, 0).quantity)
    }
  }

  def createAdditionalTaxTable(codingComponent: Seq[CodingComponent], totalTax: TotalTax): Seq[AdditionalTaxDetailRow] = {

    val underPaymentFromPreviousYear = codingComponent.find(_.componentType == UnderPaymentFromPreviousYear).map(_.amount)
    val underPaymentRow = createAdditionalTaxRow(underPaymentFromPreviousYear, Messages("tai.taxCalc.UnderpaymentPreviousYear.title"), None)

    val inYearAdjustment = codingComponent.find(_.componentType == InYearAdjustment).map(_.amount)
    val inYearRow = createAdditionalTaxRow(inYearAdjustment, Messages("tai.taxcode.deduction.type-45"),
      Some(routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url))

    val outstandingDebt = codingComponent.find(_.componentType == OutstandingDebt).map(_.amount)
    val outstandingDebtRow = createAdditionalTaxRow(outstandingDebt, Messages("tai.taxCalc.OutstandingDebt.title"), None)

    val childBenefit = taxAdjustmentComp(totalTax.otherTaxDue, tax.ChildBenefit)
    val childBenefitRow = createAdditionalTaxRow(childBenefit, Messages("tai.taxCalc.childBenefit.title"), None)

    val excessGiftAid = taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessGiftAidTax)
    val excessGiftAidRow = createAdditionalTaxRow(excessGiftAid, Messages("tai.taxCalc.excessGiftAidTax.title"), None)

    val excessWidowAndOrphans = taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessWidowsAndOrphans)
    val excessWidowAndOrphansRow = createAdditionalTaxRow(excessWidowAndOrphans, Messages("tai.taxCalc.excessWidowsAndOrphans.title"), None)

    val pensionPaymentAdjustment = taxAdjustmentComp(totalTax.otherTaxDue, tax.PensionPaymentsAdjustment)
    val pensionPaymentsRow = createAdditionalTaxRow(pensionPaymentAdjustment, Messages("tai.taxCalc.pensionPaymentsAdjustment.title"), None)

    Seq(underPaymentRow, inYearRow, outstandingDebtRow, childBenefitRow, excessGiftAidRow, excessWidowAndOrphansRow, pensionPaymentsRow).flatten
  }

  private def taxAdjustmentComp(taxAdjustment: Option[TaxAdjustment], adjustmentType: TaxAdjustmentType) = {
    taxAdjustment.
      flatMap(_.taxAdjustmentComponents.find(_.taxAdjustmentType == adjustmentType))
      .map(_.taxAdjustmentAmount)
  }

  private def createAdditionalTaxRow(row: Option[BigDecimal], description: String, url: Option[String]): Option[AdditionalTaxDetailRow] = {
    row.map(amount => AdditionalTaxDetailRow(description, amount, url))
  }

  def createReductionsTable(codingComponents: Seq[CodingComponent], totalTax: TotalTax): Seq[ReductionTaxRow] = {
    val nonCodedIncome = totalTax.taxOnOtherIncome
    val nonCodedIncomeRow = createReductionTaxRow(nonCodedIncome, Messages("tai.taxCollected.atSource.otherIncome.description"),
      Messages("tai.taxCollected.atSource.otherIncome.title"))

    val ukDividend = taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxCreditOnUKDividends)
    val ukDividendRow = createReductionTaxRow(ukDividend, Messages("tai.taxCollected.atSource.dividends.description", 10),
      Messages("tai.taxCollected.atSource.dividends.title"))

    val bankInterest = taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxOnBankBSInterest)
    val bankInterestRow = createReductionTaxRow(bankInterest, Messages("tai.taxCollected.atSource.bank.description", 20),
      Messages("tai.taxCollected.atSource.bank.title"))

    val marriageAllowanceRow = createMarriageAllowanceRow(codingComponents, totalTax)
    val maintenancePaymentRow = createMaintenancePaymentRow(codingComponents, totalTax)

    val enterpriseInvestmentScheme = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.EnterpriseInvestmentSchemeRelief)
    val enterpriseInvestmentSchemeRow = createReductionTaxRow(enterpriseInvestmentScheme,
      Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.description"),
      Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.title"))

    val concessionRelief = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.ConcessionalRelief)
    val concessionReliefRow = createReductionTaxRow(concessionRelief,
      Messages("tai.taxCollected.atSource.concessionalRelief.description"),
      Messages("tai.taxCollected.atSource.concessionalRelief.title"))

    val doubleTaxationRelief = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.DoubleTaxationRelief)
    val doubleTaxationReliefRow = createReductionTaxRow(doubleTaxationRelief,
      Messages("tai.taxCollected.atSource.doubleTaxationRelief.description"),
      Messages("tai.taxCollected.atSource.doubleTaxationRelief.title"))

    Seq(
      nonCodedIncomeRow,
      ukDividendRow,
      bankInterestRow,
      marriageAllowanceRow,
      maintenancePaymentRow,
      enterpriseInvestmentSchemeRow,
      concessionReliefRow,
      doubleTaxationReliefRow
    ).flatten
  }

  private def createMarriageAllowanceRow(codingComponents: Seq[CodingComponent], totalTax: TotalTax) = {
    val marriageAllowance = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MarriedCouplesAllowance)
    val marriageAllowanceNpsComponent = codingComponents.find { component =>
      component.componentType match {
        case compType if compType == MarriedCouplesAllowanceMAE ||
          compType == MarriedCouplesAllowanceMCCP || compType == MarriedCouplesAllowanceToWifeWMA => true
        case _ => false
      }
    }.map(_.amount).getOrElse(BigDecimal(0))
    createReductionTaxRow(marriageAllowance,
      Messages("tai.taxCollected.atSource.marriageAllowance.description", MoneyPounds(marriageAllowanceNpsComponent).quantity,
        Link.toInternalPage(
          url = routes.YourTaxCodeController.taxCodes().toString,
          value = Some(Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText"))
        ).toHtml.body),
      Messages("tai.taxCollected.atSource.marriageAllowance.title"))
  }

  private def createMaintenancePaymentRow(codingComponents: Seq[CodingComponent], totalTax: TotalTax) = {
    val maintenancePayment = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MaintenancePayments)
    val maintenancePaymentGross = codingComponents.find(_.componentType == MaintenancePayments).map(_.amount).getOrElse(BigDecimal(0))

    createReductionTaxRow(maintenancePayment,
      Messages("tai.taxCollected.atSource.maintenancePayments.description", MoneyPounds(maintenancePaymentGross).quantity,
        routes.YourTaxCodeController.taxCodes().url),
      Messages("tai.taxCollected.atSource.marriageAllowance.title"))
  }

  private def incomeTaxReducedToZeroMessage(hasTaxReducedToZero: Boolean): Option[String] = {
    Option(hasTaxReducedToZero).collect{
      case true => Messages("tai.estimatedIncome.reductionsTax.incomeTaxReducedToZeroMessage")
    }
  }

  private def createReductionTaxRow(row: Option[BigDecimal], description: String, title: String) = {
    row.map(amount => ReductionTaxRow(description, amount, title))
  }

  def dividendsMessage(nonTaxCodeIncome: NonTaxCodeIncome, totalTax: TotalTax): Option[String] = {
    val ukDividend = nonTaxCodeIncome.otherNonTaxCodeIncomes.find(_.incomeComponentType == UkDividend).map(_.amount)

    ukDividend flatMap { ukDivTotalIncome =>
      val taxBands = totalTax.incomeCategories.filter(_.incomeCategoryType == tax.UkDividendsIncomeCategory).flatMap(_.taxBands)
      val taxFreeDividend = taxBands.find(_.bandType == DividendZeroRate).flatMap(_.upperBand).getOrElse(BigDecimal(0))
      val higherTaxRates = taxBands.filter(taxBand => taxBand.income > 0 && taxBand.rate > 0).map(_.rate)

      if (ukDivTotalIncome <= taxFreeDividend) {
        Some(Messages("tai.estimatedIncome.ukdividends.lessThanOrEqualToBasic", MoneyPounds(taxFreeDividend, 0).quantity))

      } else if ((ukDivTotalIncome > taxFreeDividend) && higherTaxRates.nonEmpty) {
        Some(Messages("tai.estimatedIncome.ukdividends.moreThanBasic", dividendsAllowanceRates(higherTaxRates.toList),
          MoneyPounds(taxFreeDividend, 0).quantity))
      } else {
        None
      }
    }
  }

  private def dividendsAllowanceRates(list: List[BigDecimal]): String = list match {
    case h :: Nil => h + "%"
    case h :: tail if tail.size > 1 => h + "%, " + dividendsAllowanceRates(tail)
    case h :: tail :: Nil => h + "% and " + tail + "%"
  }

}
