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

import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.{TaxBand, TotalTax}
import uk.gov.hmrc.tai.service.estimatedIncomeTax.{EstimatedIncomeTaxService}
import uk.gov.hmrc.tai.util._
import uk.gov.hmrc.tai.viewModels.{Band, BandedGraph, Swatch}
import uk.gov.hmrc.urls.Link
import views.html.includes.link

import scala.math.BigDecimal

case class SimpleEstimatedIncomeTaxViewModel(
                                        hasCurrentIncome: Boolean,
                                        incomeTaxEstimate: BigDecimal,
                                        incomeEstimate: BigDecimal,
                                        taxFreeEstimate: BigDecimal,
                                        graph: BandedGraph,
                                        taxRegion: String,
//                                        additionalTaxTable: Seq[AdditionalTaxDetailRow],
//                                        additionalTaxTableTotal: BigDecimal,
//                                        reductionTaxTable: Seq[ReductionTaxRow],
//                                        reductionTaxTableTotal: BigDecimal,
//                                        incomeTaxReducedToZeroMessage: Option[String],
//                                        hasPotentialUnderPayment: Boolean,
//                                        ssrValue: Option[BigDecimal],
//                                        psrValue: Option[BigDecimal],
//                                        dividendsMessage: Option[String],
//                                        hasTaxRelief: Boolean = false,
                                        mergedTaxBands:List[TaxBand]
                                      ) extends ViewModelHelper

object SimpleEstimatedIncomeTaxViewModel extends BandTypesConstants with TaxRegionConstants with EstimatedIncomeTaxHelper{

  def apply(codingComponents: Seq[CodingComponent], taxAccountSummary: TaxAccountSummary, taxCodeIncomes: Seq[TaxCodeIncome],
            taxBands:List[TaxBand])(implicit messages: Messages): SimpleEstimatedIncomeTaxViewModel = {

    val personalAllowance = personalAllowanceAmount(codingComponents)
    val paBand = createPABand(taxAccountSummary.taxFreeAllowance)
    val mergedTaxBands = retrieveTaxBands(taxBands :+ paBand)
    val graph = createBandedGraph(mergedTaxBands, personalAllowance, taxAccountSummary.taxFreeAllowance, taxAccountSummary.totalEstimatedIncome, taxAccountSummary.totalEstimatedTax)
    val taxRegion = findTaxRegion(taxCodeIncomes)

//    val additionalTaxTable = createAdditionalTaxTable(codingComponents, totalTax)
//    val additionalTaxTableTotal = additionalTaxTable.map(_.amount).sum
//    val reductionTaxTable = createReductionsTable(codingComponents, totalTax)
//    val reductionTaxTableTotal = reductionTaxTable.map(_.amount).sum
//    val incomeTaxReducedToZero = incomeTaxReducedToZeroMessage(taxAccountSummary.totalEstimatedTax <= 0 && reductionTaxTable.nonEmpty)
//    val hasPotentialUnderPayment = EstimatedIncomeTaxService.hasPotentialUnderPayment(taxAccountSummary.totalInYearAdjustmentIntoCY,
//      taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne)
//    val ssrValue = fetchIncome(mergedTaxBands, StarterSavingsRate)
//    val psrValue = fetchIncome(mergedTaxBands, PersonalSavingsRate)
//    val dividends = dividendsMessage(nonTaxCodeIncome, totalTax)

//    val hasTaxRelief = EstimatedIncomeTaxService.hasTaxRelief(totalTax)


    SimpleEstimatedIncomeTaxViewModel(
      taxCodeIncomes.nonEmpty,
      taxAccountSummary.totalEstimatedTax,
      taxAccountSummary.totalEstimatedIncome,
      taxAccountSummary.taxFreeAllowance,
      graph,
      taxRegion,
//      additionalTaxTable,
//      additionalTaxTableTotal,
//      reductionTaxTable,
//      reductionTaxTableTotal,
//      incomeTaxReducedToZero,
//      hasPotentialUnderPayment,
//      ssrValue,
//      psrValue,
//      dividends
//      hasTaxRelief,
      mergedTaxBands
    )
  }

  def createBandedGraph(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None,
                        taxFreeAllowanceBandSum: BigDecimal = 0, totalEstimatedIncome: BigDecimal,
                        totalEstimatedTax: BigDecimal)(implicit messages: Messages): BandedGraph = {
    taxBands match {
      case Nil => BandedGraph(TaxGraph)
      case taxbands => createGraph(taxbands, personalAllowance, taxFreeAllowanceBandSum, totalEstimatedIncome, totalEstimatedTax)
    }
  }

  private def createGraph(taxbands: List[TaxBand], personalAllowance: Option[BigDecimal] = None,
                          taxFreeAllowanceBandSum: BigDecimal = 0,totalEstimatedIncome: BigDecimal,
                          totalEstimatedTax: BigDecimal)(implicit messages: Messages): BandedGraph = {
    val (individualBand: List[Band], mergedBand: Option[Band], swatch: Option[Swatch])  = {

      (totalEstimatedTax == 0, taxbands.exists(_.rate == 0)) match {
        case(true,true) => {(
          individualBands(taxbands, personalAllowance, taxFreeAllowanceBandSum, totalEstimatedIncome),
          None,
          None
        )}
        case(false,true) => {(
          individualBands(taxbands, personalAllowance, taxFreeAllowanceBandSum, totalEstimatedIncome),
          mergedBands(taxbands, personalAllowance, taxFreeAllowanceBandSum,totalEstimatedIncome),
          Some(createSwatch(totalEstimatedTax, totalEstimatedIncome))
          )}
        case(false,false) => {(
          individualOtherRateBands(taxbands, None, taxFreeAllowanceBandSum, totalEstimatedIncome),
          None,
          Some(createSwatch(totalEstimatedTax,totalEstimatedIncome))
          )}
      }
    }

    val mergedIndividualBands = mergeZeroBands(individualBand)

    val allBands = mergedBand match {
      case Some(band) => mergedIndividualBands :+ band
      case _ => individualBand
    }

    val nextHigherBand = getUpperBand(taxbands, personalAllowance, taxFreeAllowanceBandSum)
    val incomeTotal = allBands.map(_.income).sum
    val nextBandMessage = createNextBandMessage(nextHigherBand - incomeTotal)
    val zeroRateBands = individualBand.filter(_.tax == BigDecimal(0))

    BandedGraph(TaxGraph,
      allBands,
      nextBand = nextHigherBand,
      incomeTotal = incomeTotal,
      zeroIncomeAsPercentage = zeroRateBands.map(_.barPercentage).sum,
      zeroIncomeTotal = zeroRateBands.map(_.income).sum,
      incomeAsPercentage = allBands.map(_.barPercentage).sum,
      taxTotal = allBands.map(_.tax).sum,
      nextBandMessage = nextBandMessage,
      swatch = swatch
    )
  }

  def individualBands(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None,
                      taxFreeAllowanceBandSum: BigDecimal = 0,totalEstimatedIncome: BigDecimal)(implicit messages: Messages): List[Band] =
    for (taxBand <- taxBands.filter(_.rate == 0)) yield Band(TaxFree, calcBarPercentage(taxBand.income, taxBands, personalAllowance, taxFreeAllowanceBandSum,totalEstimatedIncome),
      "0%", taxBand.income, taxBand.tax, taxBand.bandType)

  def individualOtherRateBands(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None,
                               taxFreeAllowanceBandSum: BigDecimal = 0, totalEstimatedIncome: BigDecimal)(implicit messages: Messages): List[Band] =
    for (taxBand <- taxBands) yield Band("Band", calcBarPercentage(taxBand.income, taxBands, personalAllowance, taxFreeAllowanceBandSum,totalEstimatedIncome),
      s"${taxBand.rate}%", taxBand.income, taxBand.tax, taxBand.bandType)


  def createSwatch(totalEstimatedTax: BigDecimal, totalEstimatedIncome: BigDecimal): Swatch ={
    val swatchPercentage = ((totalEstimatedTax / totalEstimatedIncome) * 100).setScale(2, BigDecimal.RoundingMode.FLOOR)
    Swatch(swatchPercentage,totalEstimatedTax)
  }

  def getUpperBand(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None,
                   taxFreeAllowanceBandSum: BigDecimal = 0)(implicit messages: Messages): BigDecimal = {
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
                        personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0,totalEstimatedIncome: BigDecimal)(implicit messages: Messages): BigDecimal = {
    taxBands match {
      case Nil => BigDecimal(0)
      case _ =>
        val percentage = if(incomeBand > totalEstimatedIncome) percentageForIncomeBandAboveIncomeAmount(totalEstimatedIncome,taxBands,personalAllowance) else percentageForIncomeBandLowerThanIncomeAmount(totalEstimatedIncome,incomeBand)
        percentage.setScale(2, BigDecimal.RoundingMode.FLOOR)
    }
  }

  private def percentageForIncomeBandAboveIncomeAmount(totalEstimatedIncome:BigDecimal, taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None, taxFreeAllowanceBandSum: BigDecimal = 0)(implicit messages: Messages):BigDecimal = {
    val upperBand = getUpperBand(taxBands, personalAllowance, taxFreeAllowanceBandSum)
    (totalEstimatedIncome * 100) / upperBand
  }

  private def percentageForIncomeBandLowerThanIncomeAmount(totalEstimatedIncome:BigDecimal,incomeBand: BigDecimal):BigDecimal = {
    (incomeBand * 100) / totalEstimatedIncome
  }

  def mergedBands(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None,
                  taxFreeAllowanceBandSum: BigDecimal = 0,totalEstimatedIncome: BigDecimal)(implicit messages: Messages): Option[Band] = {
    val nonZeroBands = taxBands.filter(_.rate != 0)

    Option(nonZeroBands.nonEmpty).collect {
      case true =>
        val (tablePercentage, bandType, incomeSum) = getBandValues(nonZeroBands)

        Band("Band", calcBarPercentage(incomeSum, taxBands, personalAllowance, taxFreeAllowanceBandSum,totalEstimatedIncome: BigDecimal),
          tablePercentage = tablePercentage,
          income = incomeSum,
          tax = nonZeroBands.map(_.tax).sum,
          bandType = bandType
        )
    }
  }

  def mergeZeroBands(bands: List[Band]): List[Band] ={

    if(bands.size > 0){
      List(Band(TaxFree,bands.map(_.barPercentage).sum,"0%",bands.map(_.income).sum,0,ZeroBand))
    }else{
      bands
    }
  }

  private def getBandValues(nonZeroBands: List[TaxBand])(implicit messages: Messages) = {
    if (nonZeroBands.size > 1) {
      (link(
        copy = Messages("tai.mergedTaxBand.description"),
        altCopy = Some(Messages("tai.mergedTaxBand.link.wording")),
        id = Some("taxExplanation"),
        url = routes.TaxExplanationController.taxExplanationPage().toString
      ).body, NonZeroBand, nonZeroBands.map(_.income).sum)
    } else {
      nonZeroBands.map(otherBand => (otherBand.rate.toString() + "%", NonZeroBand, otherBand.income)).head
    }
  }

  private def createNextBandMessage(amount: BigDecimal)(implicit messages: Messages): Option[String] = {
    Option(amount > 0).collect {
      case true => Messages("tai.taxCalc.nextTaxBand", MoneyPounds(amount, 0).quantity)
    }
  }

//  def createAdditionalTaxTable(codingComponent: Seq[CodingComponent], totalTax: TotalTax)(implicit messages: Messages): Seq[AdditionalTaxDetailRow] = {
//
//    val underPaymentRow = createAdditionalTaxRow(underPaymentFromPreviousYear(codingComponent), Messages("tai.taxCalc.UnderpaymentPreviousYear.title"), None)
//    val inYearRow = createAdditionalTaxRow(inYearAdjustment(codingComponent), Messages("tai.taxcode.deduction.type-45"),
//      Some(routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url))
//    val outstandingDebtRow = createAdditionalTaxRow(outstandingDebt(codingComponent), Messages("tai.taxCalc.OutstandingDebt.title"), None)
//    val childBenefitRow = createAdditionalTaxRow(taxAdjustmentComp(totalTax.otherTaxDue, tax.ChildBenefit), Messages("tai.taxCalc.childBenefit.title"), None)
//    val excessGiftAidRow = createAdditionalTaxRow(taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessGiftAidTax), Messages("tai.taxCalc.excessGiftAidTax.title"), None)
//    val excessWidowAndOrphansRow = createAdditionalTaxRow(taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessWidowsAndOrphans), Messages("tai.taxCalc.excessWidowsAndOrphans.title"), None)
//    val pensionPaymentsRow = createAdditionalTaxRow(taxAdjustmentComp(totalTax.otherTaxDue, tax.PensionPaymentsAdjustment), Messages("tai.taxCalc.pensionPaymentsAdjustment.title"), None)
//
//    Seq(underPaymentRow, inYearRow, outstandingDebtRow, childBenefitRow, excessGiftAidRow, excessWidowAndOrphansRow, pensionPaymentsRow).flatten
//  }
//
//  private def createAdditionalTaxRow(row: Option[BigDecimal], description: String, url: Option[String]): Option[AdditionalTaxDetailRow] = {
//    row.map(amount => AdditionalTaxDetailRow(description, amount, url))
//  }
//
//  def createReductionsTable(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit messages: Messages): Seq[ReductionTaxRow] = {
//    val nonCodedIncome = totalTax.taxOnOtherIncome
//    val nonCodedIncomeRow = createReductionTaxRow(nonCodedIncome, Messages("tai.taxCollected.atSource.otherIncome.description"),
//      Messages("tai.taxCollected.atSource.otherIncome.title"))
//
//    val ukDividend = taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxCreditOnUKDividends)
//    val ukDividendRow = createReductionTaxRow(ukDividend, Messages("tai.taxCollected.atSource.dividends.description", 10),
//      Messages("tai.taxCollected.atSource.dividends.title"))
//
//    val bankInterest = taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxOnBankBSInterest)
//    val bankInterestRow = createReductionTaxRow(bankInterest, Messages("tai.taxCollected.atSource.bank.description", 20),
//      Messages("tai.taxCollected.atSource.bank.title"))
//
//    val marriageAllowanceRow = createMarriageAllowanceRow(codingComponents, totalTax)
//    val maintenancePaymentRow = createMaintenancePaymentRow(codingComponents, totalTax)
//
//    val enterpriseInvestmentScheme = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.EnterpriseInvestmentSchemeRelief)
//    val enterpriseInvestmentSchemeRow = createReductionTaxRow(enterpriseInvestmentScheme,
//      Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.description"),
//      Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.title"))
//
//    val concessionRelief = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.ConcessionalRelief)
//    val concessionReliefRow = createReductionTaxRow(concessionRelief,
//      Messages("tai.taxCollected.atSource.concessionalRelief.description"),
//      Messages("tai.taxCollected.atSource.concessionalRelief.title"))
//
//    val doubleTaxationRelief = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.DoubleTaxationRelief)
//    val doubleTaxationReliefRow = createReductionTaxRow(doubleTaxationRelief,
//      Messages("tai.taxCollected.atSource.doubleTaxationRelief.description"),
//      Messages("tai.taxCollected.atSource.doubleTaxationRelief.title"))
//
//    Seq(
//      nonCodedIncomeRow,
//      ukDividendRow,
//      bankInterestRow,
//      marriageAllowanceRow,
//      maintenancePaymentRow,
//      enterpriseInvestmentSchemeRow,
//      concessionReliefRow,
//      doubleTaxationReliefRow
//    ).flatten
//  }
//
//  private def createMarriageAllowanceRow(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit messages: Messages) = {
//    val marriageAllowance = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MarriedCouplesAllowance)
//    val marriageAllowanceNpsComponent = codingComponents.find { component =>
//      component.componentType match {
//        case compType if compType == MarriedCouplesAllowanceMAE ||
//          compType == MarriedCouplesAllowanceMCCP || compType == MarriedCouplesAllowanceToWifeWMA => true
//        case _ => false
//      }
//    }.map(_.amount).getOrElse(BigDecimal(0))
//    createReductionTaxRow(marriageAllowance,
//      Messages("tai.taxCollected.atSource.marriageAllowance.description", MoneyPounds(marriageAllowanceNpsComponent).quantity,
//        Link.toInternalPage(
//          url = routes.YourTaxCodeController.taxCodes().toString,
//          value = Some(Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText"))
//        ).toHtml.body),
//      Messages("tai.taxCollected.atSource.marriageAllowance.title"))
//  }
//
//  private def createMaintenancePaymentRow(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit messages: Messages) = {
//    val maintenancePayment = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MaintenancePayments)
//    val maintenancePaymentGross = codingComponents.find(_.componentType == MaintenancePayments).map(_.amount).getOrElse(BigDecimal(0))
//
//    createReductionTaxRow(maintenancePayment,
//      Messages("tai.taxCollected.atSource.maintenancePayments.description", MoneyPounds(maintenancePaymentGross).quantity,
//        routes.YourTaxCodeController.taxCodes().url),
//      Messages("tai.taxCollected.atSource.marriageAllowance.title"))
//  }
//
//  def incomeTaxReducedToZeroMessage(hasTaxReducedToZero: Boolean)(implicit messages: Messages): Option[String] = {
//    Option(hasTaxReducedToZero).collect{
//      case true => Messages("tai.estimatedIncome.reductionsTax.incomeTaxReducedToZeroMessage")
//    }
//  }
//
//  private def createReductionTaxRow(row: Option[BigDecimal], description: String, title: String)(implicit messages: Messages) = {
//    row.map(amount => ReductionTaxRow(description, amount, title))
//  }
//
//  def dividendsMessage(nonTaxCodeIncome: NonTaxCodeIncome, totalTax: TotalTax)(implicit messages: Messages): Option[String] = {
//    val ukDividend = nonTaxCodeIncome.otherNonTaxCodeIncomes.find(_.incomeComponentType == UkDividend).map(_.amount)
//
//    ukDividend flatMap { ukDivTotalIncome =>
//      val taxBands = totalTax.incomeCategories.filter(_.incomeCategoryType == tax.UkDividendsIncomeCategory).flatMap(_.taxBands)
//      val taxFreeDividend = taxBands.find(_.bandType == DividendZeroRate).flatMap(_.upperBand).getOrElse(BigDecimal(0))
//      val higherTaxRates = taxBands.filter(taxBand => taxBand.income > 0 && taxBand.rate > 0).map(_.rate)
//
//      if (ukDivTotalIncome <= taxFreeDividend) {
//        Some(Messages("tai.estimatedIncome.ukdividends.lessThanOrEqualToBasic", MoneyPounds(taxFreeDividend, 0).quantity))
//      } else if ((ukDivTotalIncome > taxFreeDividend) && higherTaxRates.nonEmpty) {
//        Some(Messages("tai.estimatedIncome.ukdividends.moreThanBasic", dividendsAllowanceRates(higherTaxRates.toList),
//          MoneyPounds(taxFreeDividend, 0).quantity))
//      } else {
//        None
//      }
//    }
//  }
//
//  private def dividendsAllowanceRates(list: List[BigDecimal]): String = list match {
//    case h :: Nil => h + "%"
//    case h :: tail if tail.size > 1 => h + "%, " + dividendsAllowanceRates(tail)
//    case h :: tail :: Nil => h + "% and " + tail + "%"
//  }
//
}
