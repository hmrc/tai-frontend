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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.model.domain.{PersonalAllowanceAgedPAA, PersonalAllowanceElderlyPAE, PersonalAllowancePA}
import uk.gov.hmrc.tai.util.MoneyPounds
import uk.gov.hmrc.tai.util.constants.{BandTypesConstants, TaiConstants}

case class BandedGraph(
  id: String,
  bands: List[Band],
  minBand: BigDecimal,
  nextBand: BigDecimal,
  incomeTotal: BigDecimal,
  zeroIncomeAsPercentage: BigDecimal,
  zeroIncomeTotal: BigDecimal,
  incomeAsPercentage: BigDecimal,
  taxTotal: BigDecimal,
  nextBandMessage: Option[String],
  swatch: Option[Swatch]
)

object BandedGraph {

  def apply(
    codingComponents: Seq[CodingComponent],
    taxBands: List[TaxBand],
    taxFreeAllowanceBandSum: BigDecimal = 0,
    totalEstimatedTax: BigDecimal,
    totalEstimatedIncome: BigDecimal = 0,
    taxViewType: TaxViewType
  )(implicit messages: Messages): BandedGraph = {

    val personalAllowance = personalAllowanceAmount(codingComponents)

    taxBands match {
      case Nil      => BandedGraph(BandTypesConstants.TaxGraph, List.empty[Band], 0, 0, 0, 0, 0, 0, 0, None, None)
      case taxbands =>
        createGraph(
          taxbands,
          personalAllowance,
          taxFreeAllowanceBandSum,
          totalEstimatedTax,
          totalEstimatedIncome,
          taxViewType
        )
    }
  }

  def createGraph(
    taxbands: List[TaxBand],
    personalAllowance: Option[BigDecimal] = None,
    taxFreeAllowanceBandSum: BigDecimal = 0,
    totalEstimatedTax: BigDecimal,
    totalEstimatedIncome: BigDecimal = 0,
    taxViewType: TaxViewType
  )(implicit messages: Messages): BandedGraph = {

    val totalIncome = taxbands.map(_.income).sum

    val (individualBand: List[Band], mergedBand: Option[Band], swatch: Option[Swatch]) =
      (taxViewType == ZeroTaxView, taxbands.exists(_.rate == 0)) match {
        case (true, true)   =>
          (
            individualBands(
              taxbands,
              personalAllowance,
              taxFreeAllowanceBandSum,
              totalIncome,
              totalEstimatedIncome,
              taxViewType
            ),
            None,
            None
          )
        case (false, true)  =>
          (
            individualBands(
              taxbands,
              personalAllowance,
              taxFreeAllowanceBandSum,
              totalIncome,
              taxViewType = taxViewType
            ),
            mergedBands(taxbands, personalAllowance, taxFreeAllowanceBandSum, totalIncome, taxViewType),
            Some(createSwatch(totalEstimatedTax, totalIncome))
          )
        case (false, false) =>
          (
            individualOtherRateBands(taxbands, None, taxFreeAllowanceBandSum, totalIncome, taxViewType),
            None,
            Some(createSwatch(totalEstimatedTax, totalIncome))
          )
      }

    val mergedIndividualBands = mergeZeroBands(individualBand)

    val allBands = mergedBand match {
      case Some(band) => mergedIndividualBands :+ band
      case _          => individualBand
    }

    val nextHigherBand  = getUpperBand(taxbands, personalAllowance, taxFreeAllowanceBandSum)
    val nextBandMessage = createNextBandMessage(nextHigherBand - totalIncome)
    val zeroRateBands   = individualBand.filter(_.tax == BigDecimal(0))

    BandedGraph(
      BandTypesConstants.TaxGraph,
      allBands,
      0,
      nextBand = nextHigherBand,
      totalIncome,
      zeroIncomeAsPercentage = zeroRateBands.map(_.barPercentage).sum,
      zeroIncomeTotal = zeroRateBands.map(_.income).sum,
      incomeAsPercentage = allBands.map(_.barPercentage).sum,
      taxTotal = allBands.map(_.tax).sum,
      nextBandMessage = nextBandMessage,
      swatch = swatch
    )
  }

  def individualBands(
    taxBands: List[TaxBand],
    personalAllowance: Option[BigDecimal] = None,
    taxFreeAllowanceBandSum: BigDecimal = 0,
    totalTaxBandIncome: BigDecimal,
    totalEstimatedIncome: BigDecimal = 0,
    taxViewType: TaxViewType
  ): List[Band] =
    for (taxBand <- taxBands.filter(_.rate == 0))
      yield Band(
        BandTypesConstants.TaxFree,
        calcBarPercentage(
          taxBand.income,
          taxBands,
          personalAllowance,
          taxFreeAllowanceBandSum,
          totalTaxBandIncome,
          totalEstimatedIncome,
          taxViewType
        ),
        taxBand.income,
        taxBand.tax,
        taxBand.bandType
      )

  def individualOtherRateBands(
    taxBands: List[TaxBand],
    personalAllowance: Option[BigDecimal] = None,
    taxFreeAllowanceBandSum: BigDecimal = 0,
    totalTaxBandIncome: BigDecimal,
    taxViewType: TaxViewType
  ): List[Band] =
    for (taxBand <- taxBands)
      yield Band(
        "Band",
        calcBarPercentage(
          taxBand.income,
          taxBands,
          personalAllowance,
          taxFreeAllowanceBandSum,
          totalTaxBandIncome,
          taxViewType = taxViewType
        ),
        taxBand.income,
        taxBand.tax,
        taxBand.bandType
      )

  def createSwatch(totalEstimatedTax: BigDecimal, totalTaxBandIncome: BigDecimal): Swatch = {
    val swatchPercentage = ((totalEstimatedTax / totalTaxBandIncome) * 100).setScale(2, BigDecimal.RoundingMode.FLOOR)
    Swatch(swatchPercentage, totalEstimatedTax)
  }

  def getUpperBand(
    taxBands: List[TaxBand],
    personalAllowance: Option[BigDecimal],
    taxFreeAllowanceBandSum: BigDecimal
  ): BigDecimal =
    taxBands match {
      case Nil => BigDecimal(0)
      case _   =>
        val lstBand               = taxBands.last
        val income                = taxBands.map(_.income).sum
        val upperBand: BigDecimal = {
          if (lstBand.upperBand.contains(BigDecimal(0))) {
            lstBand.lowerBand.map(lBand => lBand + taxFreeAllowanceBandSum)
          } else {
            lstBand.upperBand.map { upBand =>
              if (upBand >= TaiConstants.HigherRateBandIncome) {
                upBand + taxFreeAllowanceBandSum - personalAllowance.getOrElse(0)
              } else {
                upBand + taxFreeAllowanceBandSum
              }
            }
          }
        }.getOrElse(taxFreeAllowanceBandSum)

        if (income > upperBand) income else upperBand
    }

  def calcBarPercentage(
    incomeBand: BigDecimal,
    taxBands: List[TaxBand],
    personalAllowance: Option[BigDecimal] = None,
    taxFreeAllowanceBandSum: BigDecimal = 0,
    totalTaxBandIncome: BigDecimal,
    totalEstimatedIncome: BigDecimal = 0,
    taxViewType: TaxViewType
  ): BigDecimal =
    taxBands match {
      case Nil => BigDecimal(0)
      case _   =>
        val percentage =
          if (taxViewType == ZeroTaxView) {
            percentageForZeroTaxBar(
              taxBands,
              personalAllowance,
              taxFreeAllowanceBandSum,
              totalEstimatedIncome
            )
          } else {
            percentageForNonZeroTaxView(totalTaxBandIncome, incomeBand)
          }
        percentage.setScale(2, BigDecimal.RoundingMode.FLOOR)
    }

  private def percentageForZeroTaxBar(
    taxBands: List[TaxBand],
    personalAllowance: Option[BigDecimal],
    taxFreeAllowanceBandSum: BigDecimal,
    totalEstimatedIncome: BigDecimal
  ): BigDecimal = {
    val upperBand = getUpperBand(taxBands, personalAllowance, taxFreeAllowanceBandSum)
    (totalEstimatedIncome * 100) / upperBand
  }

  private def percentageForNonZeroTaxView(totalTaxBandIncome: BigDecimal, incomeBand: BigDecimal): BigDecimal =
    (incomeBand * 100) / totalTaxBandIncome

  def mergedBands(
    taxBands: List[TaxBand],
    personalAllowance: Option[BigDecimal],
    taxFreeAllowanceBandSum: BigDecimal,
    totalTaxBandIncome: BigDecimal,
    taxViewType: TaxViewType
  ): Option[Band] = {
    val nonZeroBands = taxBands.filter(_.rate != 0)

    Option(nonZeroBands.nonEmpty).collect { case true =>
      val (bandType, incomeSum) = getBandValues(nonZeroBands)

      Band(
        "Band",
        calcBarPercentage(
          incomeSum,
          taxBands,
          personalAllowance,
          taxFreeAllowanceBandSum,
          totalTaxBandIncome,
          taxViewType = taxViewType
        ),
        income = incomeSum,
        tax = nonZeroBands.map(_.tax).sum,
        bandType = bandType
      )
    }
  }

  def mergeZeroBands(bands: List[Band]): List[Band] =
    if (bands.nonEmpty) {
      List(
        Band(
          BandTypesConstants.TaxFree,
          bands.map(_.barPercentage).sum,
          bands.map(_.income).sum,
          0,
          BandTypesConstants.ZeroBand
        )
      )
    } else {
      bands
    }

  private def getBandValues(nonZeroBands: List[TaxBand]) =
    if (nonZeroBands.size > 1) {
      (BandTypesConstants.NonZeroBand, nonZeroBands.map(_.income).sum)
    } else {
      nonZeroBands.map(otherBand => (BandTypesConstants.NonZeroBand, otherBand.income)).head
    }

  private def createNextBandMessage(amount: BigDecimal)(implicit messages: Messages): Option[String] =
    Option(amount > 0).collect { case true =>
      Messages("tai.taxCalc.nextTaxBand", MoneyPounds(amount, 0).quantity)
    }

  def personalAllowanceAmount(codingComponents: Seq[CodingComponent]): Option[BigDecimal] =
    codingComponents
      .find { component =>
        component.componentType match {
          case compType
              if compType == PersonalAllowancePA || compType == PersonalAllowanceAgedPAA || compType == PersonalAllowanceElderlyPAE =>
            true
          case _ => false
        }
      }
      .map(_.amount)

}
