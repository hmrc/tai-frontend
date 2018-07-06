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

import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.model.domain.{PersonalAllowanceAgedPAA, PersonalAllowanceElderlyPAE, PersonalAllowancePA}
import uk.gov.hmrc.tai.util.TaiConstants
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.EstimatedIncomeTaxViewModel.{NonZeroBand, TaxFree, TaxGraph, ZeroBand}

import scala.math.BigDecimal

sealed trait TaxViewType
case object ZeroTaxView extends TaxViewType
case object SimpleTaxView extends TaxViewType
case object ComplexTaxView extends TaxViewType
case object NoIncomeTaxView extends TaxViewType

case class Band(
                 colour:String,
                 barPercentage: BigDecimal = 0,
                 income: BigDecimal = 0,
                 tax: BigDecimal = 0,
                 bandType: String
               )

case class Swatch(barPercentage:BigDecimal = 0,taxAmount:BigDecimal = 0)

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

case class BandedGraph(
                        id:String,
                        bands:List[Band],
                        minBand :BigDecimal,
                        nextBand :BigDecimal,
                        incomeTotal:BigDecimal,
                        zeroIncomeAsPercentage: BigDecimal,
                        zeroIncomeTotal: BigDecimal,
                        incomeAsPercentage: BigDecimal,
                        taxTotal:BigDecimal,
                        nextBandMessage: Option[String],
                        swatch:Option[Swatch]
                      )

object BandedGraph{

  def apply(codingComponents: Seq[CodingComponent],taxBands: List[TaxBand],
            taxFreeAllowanceBandSum: BigDecimal = 0, totalEstimatedIncome: BigDecimal,
            totalEstimatedTax: BigDecimal)(implicit messages: Messages): BandedGraph = {

    val personalAllowance = personalAllowanceAmount(codingComponents)

    taxBands match {
      case Nil => BandedGraph(TaxGraph,List.empty[Band],0,0,0,0,0,0,0,None,None)
      case taxbands => createGraph(taxbands, personalAllowance, taxFreeAllowanceBandSum, totalEstimatedIncome, totalEstimatedTax)
    }
  }

  def createGraph(taxbands: List[TaxBand], personalAllowance: Option[BigDecimal] = None,
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

    BandedGraph(
      TaxGraph,
      allBands,
      0,
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
      taxBand.income, taxBand.tax, taxBand.bandType)

  def individualOtherRateBands(taxBands: List[TaxBand], personalAllowance: Option[BigDecimal] = None,
                               taxFreeAllowanceBandSum: BigDecimal = 0, totalEstimatedIncome: BigDecimal)(implicit messages: Messages): List[Band] =
    for (taxBand <- taxBands) yield Band("Band", calcBarPercentage(taxBand.income, taxBands, personalAllowance, taxFreeAllowanceBandSum,totalEstimatedIncome),
      taxBand.income, taxBand.tax, taxBand.bandType)


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
        val (bandType, incomeSum) = getBandValues(nonZeroBands)

        Band("Band", calcBarPercentage(incomeSum, taxBands, personalAllowance, taxFreeAllowanceBandSum,totalEstimatedIncome: BigDecimal),
          income = incomeSum,
          tax = nonZeroBands.map(_.tax).sum,
          bandType = bandType
        )
    }
  }

  def mergeZeroBands(bands: List[Band]): List[Band] ={

    if(bands.size > 0){
      List(Band(TaxFree,bands.map(_.barPercentage).sum,bands.map(_.income).sum,0,ZeroBand))
    }else{
      bands
    }
  }

  private def getBandValues(nonZeroBands: List[TaxBand])(implicit messages: Messages) = {
    if (nonZeroBands.size > 1) {
      (NonZeroBand, nonZeroBands.map(_.income).sum)
    } else {
      nonZeroBands.map(otherBand => (NonZeroBand, otherBand.income)).head
    }
  }

  private def createNextBandMessage(amount: BigDecimal)(implicit messages: Messages): Option[String] = {
    Option(amount > 0).collect {
      case true => Messages("tai.taxCalc.nextTaxBand", MoneyPounds(amount, 0).quantity)
    }
  }

  def personalAllowanceAmount(codingComponents: Seq[CodingComponent]) = {
    codingComponents.find { component =>
      component.componentType match {
        case compType if compType == PersonalAllowancePA || compType == PersonalAllowanceAgedPAA || compType == PersonalAllowanceElderlyPAE => true
        case _ => false
      }
    }.map(_.amount)
  }

}