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

package uk.gov.hmrc.tai.util

import hmrc.nps2
import hmrc.nps2.{TaxAccount, TaxBand}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.tai.Employment
import uk.gov.hmrc.tai.util.TaiConstants.DividendZeroBandType

object TaxSummaryHelper {

  def getMatchingNpsEmployments(rtiData: Option[rti.RtiData], nps: Option[TaxAccount]) : List[Employment]= {
    val rtiEmps = rtiData.map{_.employments}.getOrElse(Nil)
    val npsIncomes = nps.map(_.incomes.filter(_.employmentRecord.isDefined)).getOrElse(Seq())
    rtiEmps.flatMap{ rtiData =>
      npsIncomes.filter { npsData =>
        npsData.payeRef == rtiData.payeRef &&
          npsData.taxDistrict.contains(rtiData.officeRefNo.toInt)
      } match {
        case Seq(oneEmp) => Some(Employment(oneEmp,Some(rtiData)))
        case Nil => None
        case multipleEmp => multipleEmp.find(_.worksNumber == rtiData.currentPayId &&
          rtiData.currentPayId.isDefined).map{
          nps => Employment(nps,Some(rtiData))
        }

      }
    }
  }

  def sortedTaxableIncomes(incomes: TaxCodeIncomes): List[TaxCodeIncomeSummary] = {
    val taxableIncomes = incomes.employments.map(_.taxCodeIncomes).getOrElse(Nil) :::
      incomes.occupationalPensions.map(_.taxCodeIncomes).getOrElse(Nil) :::
      incomes.taxableStateBenefitIncomes.map(_.taxCodeIncomes).getOrElse(Nil) :::
      incomes.ceasedEmployments.map(_.taxCodeIncomes).getOrElse(Nil)

    taxableIncomes.sortBy(employment => (employment.employmentType.getOrElse(0) * 1000) + employment.employmentId.getOrElse(0))
  }


  def getEditableIncomes(incomes: Option[Incomes]): List[TaxCodeIncomeSummary] = {
    val allIncomes = incomes.map { payeIncomes =>
      sortedTaxableIncomes(payeIncomes.taxCodeIncomes)
    }
    allIncomes.map(_.filter(_.isEditable)).getOrElse(Nil)
  }


  def hasNoTaxableIncome(taxSummaryDetails: TaxSummaryDetails): Boolean = {
    taxSummaryDetails.increasesTax.map(_.total).getOrElse(BigDecimal(0)) > taxSummaryDetails.decreasesTax.map(_.total).getOrElse(BigDecimal(0))
  }


  def getPPR(taxSummaryDetails: TaxSummaryDetails): (BigDecimal, BigDecimal) = {

    val pprSource = taxSummaryDetails.extensionReliefs.flatMap(extensionRelief =>
      extensionRelief.personalPension.map(_.sourceAmount)
    ).getOrElse(BigDecimal(0))

    val pprRelief = taxSummaryDetails.extensionReliefs.flatMap(extensionRelief =>
      extensionRelief.personalPension.map(_.reliefAmount)
    ).getOrElse(BigDecimal(0))

    (pprSource, pprRelief)
  }

  def getGiftAid(taxSummaryDetails: TaxSummaryDetails): (BigDecimal, BigDecimal) = {

    val giftAidSource = taxSummaryDetails.extensionReliefs.flatMap(extensionRelief =>
      extensionRelief.giftAid.map(_.sourceAmount)
    ).getOrElse(BigDecimal(0))

    val giftAidRelief = taxSummaryDetails.extensionReliefs.flatMap(extensionRelief =>
      extensionRelief.giftAid.map(_.reliefAmount)
    ).getOrElse(BigDecimal(0))

    (giftAidSource, giftAidRelief)
  }


  def hasMultipleIncomes(details: TaxSummaryDetails): Boolean = {
    getEditableIncomes(details.increasesTax.flatMap(_.incomes)).size > 1
  }

  def getTaxablePayYTD(details: TaxSummaryDetails, employerId: BigDecimal): BigDecimal = {
    val incomeExplanations = details.incomeData.map(x => x.incomeExplanations)

    val taxablePayYTD: BigDecimal = incomeExplanations match {
      case Some(incomeExplanations) =>
        val income = incomeExplanations.find(_.incomeId == employerId)
        income.map(_.payToDate).getOrElse(BigDecimal(0))
      case _ => BigDecimal(0)
    }
    taxablePayYTD
  }

  def getSingularIncomeId(details: TaxSummaryDetails): Option[Int] = {
    val editableIncomes = getEditableIncomes(details.increasesTax.flatMap(_.incomes))
    if (editableIncomes.size == 1) {
      editableIncomes.flatMap { income =>
        income.employmentId
      }.headOption
    } else {
      None
    }
  }


  //method to decide if the user can see info about cy+1 or not. Can be updated to include Gatekeeper rules
  def cyPlusOneAvailable(taxSummaryDetals: TaxSummaryDetails): Boolean = {
    taxSummaryDetals.cyPlusOneChange.isDefined
  }

  implicit def strToMoneyPounds(str: String): MoneyPounds = {
    MoneyPounds(BigDecimal(str))
  }


  def displaySplitAllowanceMessage(splitAllowance: Boolean, primaryIncome: BigDecimal,
                                   taxFreeAmount: BigDecimal): Option[(String, Option[String], Option[String])] = {
    splitAllowance match {
      case true if primaryIncome > taxFreeAmount => Some((Messages("tai.split.allowance.income.greater.message.p1"),
        Some(Messages("tai.split.allowance.income.greater.message.p2")), Some(Messages("tai.split.allowance.income.greater.message.p3"))))
      case true => Some((Messages("tai.split.allowance.income.less.message"), None, None))
      case _ => None
    }
  }

  private def dividendsAllowanceRates(list: List[BigDecimal]): String = list match {
    case h :: Nil => h + "%"
    case h :: tail if tail.size > 1 => h + "%, " + dividendsAllowanceRates(tail)
    case h :: tail :: Nil => h + "% and " + tail + "%"
  }

  def displayZeroTaxRateMessage(dividends: Option[TaxComponent], taxBands: Option[List[nps2.TaxBand]]): Option[String] = {
    val iabds = dividends.map(_.iabdSummaries)
    val ukDivs = iabds.flatMap(_.find(_.iabdType == TaiConstants.IABD_TYPE_UKDIVIDENDS))

    ukDivs match {
      case Some(ukDiv) => getDividendsMessage(taxBands.getOrElse(Nil), ukDiv.amount)
      case None => None
    }
  }

  private def getDividendsMessage(taxBandsList: List[TaxBand], ukDivTotalIncome: BigDecimal) = {
    val taxFreeDividend = taxBandsList.find(_.bandType.contains(DividendZeroBandType)).flatMap(_.upperBand).getOrElse(BigDecimal(0))
    val higherTaxRates = taxBandsList.filter(taxBand => taxBand.income > 0 && taxBand.rate > 0).map(_.rate)

    if (ukDivTotalIncome <= taxFreeDividend) {
      Some(Messages("tai.estimatedIncome.ukdividends.lessThanOrEqualToBasic", MoneyPounds(taxFreeDividend, 0).quantity))

    } else if ((ukDivTotalIncome > taxFreeDividend) && higherTaxRates.nonEmpty) {
      Some(Messages("tai.estimatedIncome.ukdividends.moreThanBasic", dividendsAllowanceRates(higherTaxRates),
        MoneyPounds(taxFreeDividend, 0).quantity))
    } else {
      None
    }
  }

  def getTotalIya(details:TaxSummaryDetails) :BigDecimal = {
    extractTotalValue(details){ tax => tax.totalInYearAdjustment }
  }

  def hasIyaCY(details:TaxSummaryDetails) :Boolean = {
    extractTotalValue(details){ tax => tax.inYearAdjustmentIntoCY } > BigDecimal(0)
  }

  def getIyaCY(details:TaxSummaryDetails) :BigDecimal = {
    extractTotalValue(details){ tax => tax.inYearAdjustmentIntoCY }
  }

  def getIyaCYPlusOne(details:TaxSummaryDetails) :BigDecimal = {
    extractTotalValue(details){ tax => tax.inYearAdjustmentIntoCYPlusOne }
  }

  def extractTotalValue(details:TaxSummaryDetails)( extract: Tax => Option[BigDecimal]):BigDecimal ={
    val incomesWithUnderpayment = details.increasesTax.flatMap(_.incomes.map(incomes =>
      TaxSummaryHelper.sortedTaxableIncomes(incomes.taxCodeIncomes).filter(income => extract(income.tax).isDefined))).getOrElse(Nil)

    incomesWithUnderpayment.foldLeft(BigDecimal(0))((total,income) =>
      extract(income.tax).getOrElse(BigDecimal(0))  + total)
  }

}
