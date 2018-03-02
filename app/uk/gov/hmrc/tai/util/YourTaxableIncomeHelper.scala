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

import hmrc.nps2.IabdType.{CarBenefit, MedicalInsurance}
import uk.gov.hmrc.tai.model.{NoneTaxCodeIncomes, TaxCodeIncomes, TaxComponent}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.config
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.{IabdSummary, NoneTaxCodeIncomes, TaxCodeIncomes, TaxComponent}
import uk.gov.hmrc.urls.Link


object YourTaxableIncomeHelper {

  def extractFromTaxCodeIncomes(codingComponent: Option[TaxComponent]): TaxComponent = {
    if (codingComponent.isDefined) {
      codingComponent.get
    } else {
      TaxComponent(BigDecimal(0), 0, "", List())
    }
  }

  def iterateCodingComponent(iabdSummaryList:List[IabdSummary]): List[Option[(String, String, String)]] = {
    for {
      iabdSummary <- iabdSummaryList

      message: (String, String) = {
        if (!Messages(s"tai.iabdSummary.description-${iabdSummary.iabdType}").isEmpty) {
          (Messages(s"tai.iabdSummary.description-${iabdSummary.iabdType}"),
            Messages(s"tai.iabdSummary.type-${iabdSummary.iabdType}"))
        }
        else {
          (Messages(s"tai.iabdSummary.description-${iabdSummary.iabdType}"), "")
        }

      }

      rawAmt = iabdSummary.amount
      amount = MoneyPounds(rawAmt, 0).quantity

      taxComponentData = if (rawAmt > 0) {
        Some((message._2, amount, message._1))
      } else {
        None
      }

    } yield taxComponentData
  }

  def createInvestmentIncomeTable(nonTaxCodeIncomes: Option[NoneTaxCodeIncomes]): (List[(String, String, String)], BigDecimal) = {

    val dividends = extractFromTaxCodeIncomes(nonTaxCodeIncomes.flatMap(_.dividends))

    val bankInterest = extractFromTaxCodeIncomes(nonTaxCodeIncomes.flatMap(_.bankBsInterest))

    val unTaxedBankInterest = extractFromTaxCodeIncomes(nonTaxCodeIncomes.flatMap(_.untaxedInterest))

    val foreignInterest = extractFromTaxCodeIncomes(nonTaxCodeIncomes.flatMap(_.foreignInterest))

    val foreignDividends = extractFromTaxCodeIncomes(nonTaxCodeIncomes.flatMap(_.foreignDividends))


    val totalInvestmentIncome: BigDecimal = dividends.amount +
      bankInterest.amount +
      unTaxedBankInterest.amount +
      foreignInterest.amount +
      foreignDividends.amount

    val dividendsRows: List[Option[(String, String, String)]] =
      iterateCodingComponent(dividends.iabdSummaries)

    val bankInterestRows: List[Option[(String, String, String)]] =
      iterateCodingComponent(bankInterest.iabdSummaries)

    val unTaxedInterestRows: List[Option[(String, String, String)]] =
      iterateCodingComponent(unTaxedBankInterest.iabdSummaries)

    val foreignInterestRows: List[Option[(String, String, String)]] =
      iterateCodingComponent(foreignInterest.iabdSummaries)

    val foreignDividendsRows: List[Option[(String, String, String)]] =
      iterateCodingComponent(foreignDividends.iabdSummaries)


    val allInvestmentIncomeRows = dividendsRows ::: bankInterestRows :::
      unTaxedInterestRows ::: foreignInterestRows ::: foreignDividendsRows
    (allInvestmentIncomeRows.flatten, totalInvestmentIncome)
  }

  //temp fix to move Bereavement allowance into taxable state benefit table remove/refactor after data-structure change
  def createTaxableBenefitTable(nonTaxCodeIncomes: Option[NoneTaxCodeIncomes],
                                taxCodeIncomes: Option[TaxCodeIncomes],
                                bevAllowance: Option[IabdSummary] = None): (List[(String, String, String)], BigDecimal) = {

    val taxableStateBenefit = extractFromTaxCodeIncomes(nonTaxCodeIncomes.flatMap(_.taxableStateBenefit))
    val taxableStateBenefitIncomes = taxCodeIncomes.flatMap(_.taxableStateBenefitIncomes)
    val taxCodeIncome = taxableStateBenefitIncomes.map(_.taxCodeIncomes).getOrElse(List())
    val taxableStateBenefitEmploymentRows: List[(Option[(String, String, String)], scala.BigDecimal)] =
      for {
        payeIncome <- taxCodeIncome
        message: (String) = payeIncome.name
        rawAmt = payeIncome.income.getOrElse(BigDecimal(0))
        amount = MoneyPounds(rawAmt, 0).quantity
        taxableStateBenefitData = if (rawAmt > 0) Some((message, amount, "")) else None
      } yield ((taxableStateBenefitData, rawAmt))

    val unzipTaxableStateBenEmpRows = taxableStateBenefitEmploymentRows.unzip
    val statePension = nonTaxCodeIncomes.flatMap(_.statePension).getOrElse(BigDecimal(0))
    val statePensionLumpSum = nonTaxCodeIncomes.flatMap(_.statePensionLumpSum).getOrElse(BigDecimal(0))
    val statePensionData = if (statePension > 0) {
      Some((Messages("tai.income.statePension.title"),
        MoneyPounds(statePension, 0).quantity,
        Messages("tai.iabdSummary.description-state-pension")))
    } else { None }

    val statePensionLumpSumData = if (statePensionLumpSum > 0) {
      Some((Messages("tai.income.statePensionLumpSum.total"),
        MoneyPounds(statePensionLumpSum, 0).quantity, ""))
    } else { None }

    val tStateBenefitRows: List[Option[(String, String, String)]] = iterateCodingComponent(taxableStateBenefit.iabdSummaries)
    val totalBenefits: BigDecimal = statePension +
      statePensionLumpSum + taxableStateBenefit.amount +
      unzipTaxableStateBenEmpRows._2.sum + bevAllowance.map(_.amount).getOrElse(0)

    //need to put bev allowance description
    val bevAllowanceRow = if (bevAllowance.isDefined) {
      Some((Messages("tai.iabdSummary.type-125"),
        MoneyPounds(bevAllowance.map(_.amount).getOrElse(0), 0).quantity,
        Messages("")))
    } else { None }

    val statePensionRows = List(statePensionData, statePensionLumpSumData, bevAllowanceRow)
    val allOtherIncomeRows = tStateBenefitRows ::: unzipTaxableStateBenEmpRows._1 ::: statePensionRows
    (allOtherIncomeRows.flatten, totalBenefits)
  }

  def createOtherIncomeTable(nonTaxCodeIncomes: Option[NoneTaxCodeIncomes], otherIncome: List[IabdSummary]): (List[(String, String, String)], BigDecimal) = {

    val otherIncomeRows: List[Option[(String, String, String)]] =
      iterateCodingComponent(otherIncome)


    val otherPension = extractFromTaxCodeIncomes(nonTaxCodeIncomes.flatMap(_.otherPensions))

    val otherPensionRows =
      iterateCodingComponent(otherPension.iabdSummaries)

    val totalOtherIncomeAmount = otherIncome.map(_.amount).sum + otherPension.amount

    val allOtherIncomeRows = otherIncomeRows ::: otherPensionRows

    (allOtherIncomeRows.flatten, totalOtherIncomeAmount)

  }

  def createBenefitsTable(benefitsFromEmployment: TaxComponent): (List[(String, String, String, String, Option[Int], Option[Int])], BigDecimal) = {

    val benefitsRows: List[Option[(String, String, String, String, Option[Int], Option[Int])]] =
      for {iabdSummary <- benefitsFromEmployment.iabdSummaries

           message: (String, String, String) = iabdSummary.iabdType match {
             case MedicalInsurance.code => {
               (Messages(s"tai.iabdSummary.employmentBenefit.type-${iabdSummary.iabdType}",
                 iabdSummary.employmentName.getOrElse("")), Messages(s"tai.iabdSummary.description-${iabdSummary.iabdType}"),
                 config.ApplicationConfig.medBenefitServiceUrl)
             }
             case CarBenefit.code => {
               (Messages(s"tai.iabdSummary.employmentBenefit.type-${iabdSummary.iabdType}",
                 iabdSummary.employmentName.getOrElse("")), Messages(s"tai.iabdSummary.description-${iabdSummary.iabdType}"),
                 config.ApplicationConfig.companyCarServiceUrl)
             }
             case _ => {
               (Messages(s"tai.iabdSummary.employmentBenefit.type-${iabdSummary.iabdType}",
                 iabdSummary.employmentName.getOrElse("")),
                 Messages(s"tai.iabdSummary.description-${iabdSummary.iabdType}"),
                 "")
             }
           }

           rawAmt = iabdSummary.amount
           amount = MoneyPounds(rawAmt, 0).quantity

           benefitsData = if (rawAmt > 0) {
             Some((message._1, amount, message._2, message._3, iabdSummary.employmentId, Some(iabdSummary.iabdType)))
           } else {
             None
           }

      } yield benefitsData

    val totalBenefitsAmount = benefitsFromEmployment.amount
    (benefitsRows.flatten, totalBenefitsAmount)
  }

}
