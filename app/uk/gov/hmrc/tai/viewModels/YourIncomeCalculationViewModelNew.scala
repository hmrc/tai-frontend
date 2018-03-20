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

import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.time.TaxYearResolver

case class PaymentDetailsViewModel(date: LocalDate,
                                   taxableIncome: BigDecimal,
                                   taxAmount: BigDecimal,
                                   nationalInsuranceAmount: BigDecimal)

case class LatestPayment(date: LocalDate,
                         amountYearToDate: BigDecimal,
                         taxAmountYearToDate: BigDecimal,
                         nationalInsuranceAmountYearToDate: BigDecimal,
                         paymentFrequency: PaymentFrequency)

case class YourIncomeCalculationViewModelNew(
                                              empId: Int,
                                              employerName: String,
                                              payments: Seq[PaymentDetailsViewModel],
                                              employmentStatus: TaxCodeIncomeSourceStatus,
                                              rtiStatus: RealTimeStatus,
                                              latestPayment: Option[LatestPayment],
                                              endDate: Option[LocalDate],
                                              isPension: Boolean,
                                              messageWhenTotalNotEqual: Option[String],
                                              incomeCalculationMessage: String,
                                              incomeCalculationEstimateMessage: Option[String],
                                              hasPayrolledBenefit: Boolean
                                            )

object YourIncomeCalculationViewModelNew {
  def apply(taxCodeIncome: TaxCodeIncome, employment: Employment): YourIncomeCalculationViewModelNew = {

    val payments = employment.latestAnnualAccount.map(_.payments).getOrElse(Seq.empty[Payment])
    val paymentDetails = payments.map(payment => PaymentDetailsViewModel(
      payment.date, payment.amount, payment.taxAmount, payment.nationalInsuranceAmount))

    val realTimeStatus = employment.latestAnnualAccount.map(_.realTimeStatus).getOrElse(TemporarilyUnavailable)

    val latestPayment = latestPaymentDetails(employment)
    val isPension = taxCodeIncome.componentType == PensionIncome
    val (incomeCalculationMessage, incomeCalculationEstimateMessage) = incomeExplanationMessage(
      taxCodeIncome.status,
      employment,
      isPension,
      taxCodeIncome,
      latestPayment.map(_.paymentFrequency),
      latestPayment.map(_.amountYearToDate).getOrElse(BigDecimal(0)),
      latestPayment.map(_.date))

    YourIncomeCalculationViewModelNew(
      employment.sequenceNumber,
      employment.name,
      paymentDetails,
      taxCodeIncome.status,
      realTimeStatus,
      latestPayment,
      employment.endDate,
      isPension,
      totalNotEqualMessage(taxCodeIncome.status == Live, paymentDetails, latestPayment, isPension),
      incomeCalculationMessage.getOrElse(""),
      if(taxCodeIncome.status == Ceased) None else incomeCalculationEstimateMessage,
      employment.hasPayrolledBenefit
    )
  }

  private def totalNotEqualMessage(isLive: Boolean,
                                   payments: Seq[PaymentDetailsViewModel],
                                   latestPayment: Option[LatestPayment],
                                   isPension: Boolean) = {
    val isTotalEqual = payments.map(_.taxAmount).sum == latestPayment.map(_.taxAmountYearToDate).getOrElse(0) &&
      payments.map(_.taxableIncome).sum == latestPayment.map(_.amountYearToDate).getOrElse(0) &&
      payments.map(_.nationalInsuranceAmount).sum == latestPayment.map(_.nationalInsuranceAmountYearToDate).getOrElse(0)

    if(isLive && !isTotalEqual && isPension) {
      Some(Messages("tai.income.calculation.totalNotMatching.pension.message"))
    } else if (isLive && !isTotalEqual && !isPension) {
      Some(Messages("tai.income.calculation.totalNotMatching.emp.message"))
    } else {
      None
    }

  }

  private def latestPaymentDetails(employment: Employment) = {
    for {
      latestAnnualAccount <- employment.latestAnnualAccount
      latestPayment <- latestAnnualAccount.latestPayment
    } yield LatestPayment(latestPayment.date,
      latestPayment.amountYearToDate,
      latestPayment.taxAmountYearToDate,
      latestPayment.nationalInsuranceAmountYearToDate,
      latestPayment.payFrequency)
  }

  def incomeExplanationMessage(employmentStatus: TaxCodeIncomeSourceStatus,
                               employment: Employment,
                               isPension: Boolean,
                               taxCodeIncome: TaxCodeIncome,
                               paymentFrequency: Option[PaymentFrequency],
                               amountYearToDate: BigDecimal,
                               paymentDate: Option[LocalDate]): (Option[String], Option[String]) = {

    (getCeasedMsg(employmentStatus, employment, isPension, taxCodeIncome.amount),
      getManualUpdateMsg(taxCodeIncome),
      getSameMsg(employment, taxCodeIncome.amount, amountYearToDate, isPension, paymentDate),
      getPayFreqMsg(employment, isPension, paymentFrequency, amountYearToDate, paymentDate, taxCodeIncome.amount)) match {

      case ((Some(ceasedMsg), Some(ceasedMsgEstimate)), _, _, _) => (Some(ceasedMsg), Some(ceasedMsgEstimate))
      case ((None, Some(ceasedMsgEstimate)), _, _, _) => (None, Some(ceasedMsgEstimate))
      case ((Some(ceasedMsg), None), _, _, _) => (Some(ceasedMsg), None)
      case (_, (Some(manualUpdateMsg), Some(manualUpdateMsgEstimate)), _, _) => (Some(manualUpdateMsg), Some(manualUpdateMsgEstimate))
      case (_, (None, Some(manualUpdateMsgEstimate)), _, _) => (None, Some(manualUpdateMsgEstimate))
      case (_, (Some(manualUpdateMsg), None), _, _) => (Some(manualUpdateMsg), None)
      case (_, _, (Some(sameIncomeMsg), Some(sameIncomeEstimateMsg)), _) => (Some(sameIncomeMsg), Some(sameIncomeEstimateMsg))
      case (_, _, (Some(sameIncomeMsg), None), _) => (Some(sameIncomeMsg), None)
      case (_, _, _, (Some(payFreqMsg), Some(payFreqMsgEstimate))) => (Some(payFreqMsg), Some(payFreqMsgEstimate))
      case (_, _, _, (None, Some(payFreqMsgEstimate))) => (None, Some(payFreqMsgEstimate))
      case (_, _, _, (Some(payFreqMsg), None)) => (Some(payFreqMsg), None)

      case _ => (Some(Messages("tai.income.calculation.default." + (if (isPension) "pension" else "emp"),
        TaxYear().end.toString("d MMMM yyyy"))),
        Some(Messages("tai.income.calculation.default.estimate." + (if (isPension) "pension" else "emp"),
          taxCodeIncome.amount)))
    }
  }


  def getCeasedMsg(employmentStatus: TaxCodeIncomeSourceStatus, employment: Employment, isPension: Boolean,
                   amount: BigDecimal): (Option[String], Option[String]) = {

    (employmentStatus, employment.endDate.isDefined,
      employment.cessationPay.isDefined) match {

      case (Ceased, true, true) =>
        (Some(Messages("tai.income.calculation.rti.ceased." + (if (isPension) "pension" else "emp"),
          employment.endDate.map(_.toString("d MMMM yyyy")).getOrElse(""))), None)
      case (Ceased, true, false) =>
        (Some(Messages("tai.income.calculation.rti.ceased." + (if (isPension) "pension" else "emp") + ".noFinalPay")),
          Some(Messages("tai.income.calculation.rti.ceased.noFinalPay.estimate",
            MoneyPounds(amount, 0).quantity)))
      case (PotentiallyCeased, false, _) => (Some(Messages("tai.income.calculation.rti.ceased." +
        (if (isPension) "pension" else "emp") + ".noFinalPay")),
        Some(Messages("tai.income.calculation.rti.ceased.noFinalPay.estimate",
          MoneyPounds(amount, 0).quantity)))

      case _ => (None, None)
    }
  }

  def getManualUpdateMsg(taxCodeIncome: TaxCodeIncome): (Option[String], Option[String]) = {

    (taxCodeIncome.iabdUpdateSource, taxCodeIncome.updateNotificationDate.isDefined, taxCodeIncome.updateActionDate.isDefined) match {
      case (Some(ManualTelephone), true, true) => (Some(Messages("tai.income.calculation.manual.update.phone",
        taxCodeIncome.updateActionDate.get.toString("d MMMM yyyy"), taxCodeIncome.updateNotificationDate.get.toString("d MMMM yyyy"))),
        Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(ManualTelephone), false, false) | (Some(ManualTelephone), false, true) |
           (Some(ManualTelephone), true, false) => (Some(Messages("tai.income.calculation.manual.update.phone.withoutDate")),
        Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(Letter), true, true) => (Some(Messages("tai.income.calculation.manual.update.letter",
        taxCodeIncome.updateActionDate.get.toString("d MMMM yyyy"), taxCodeIncome.updateNotificationDate.get.toString("d MMMM yyyy"))),
        Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(Letter), false, false) | (Some(Letter), false, true) |
           (Some(Letter), true, false) => (Some(Messages("tai.income.calculation.manual.update.letter.withoutDate")),
        Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(Email), true, true) => (Some(Messages("tai.income.calculation.manual.update.email",
        taxCodeIncome.updateActionDate.get.toString("d MMMM yyyy"), taxCodeIncome.updateNotificationDate.get.toString("d MMMM yyyy"))),
        Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(Email), false, false) | (Some(Email), false, true) |
           (Some(Email), true, false) => (Some(Messages("tai.income.calculation.manual.update.email.withoutDate")),
        Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(AgentContact), _, _) => (Some(Messages("tai.income.calculation.agent")),
        Some(Messages("tai.income.calculation.agent.estimate", taxCodeIncome.amount)))

      case (Some(OtherForm) | Some(InformationLetter), true, true) =>
        (Some(Messages("tai.income.calculation.manual.update.informationLetter", taxCodeIncome.updateActionDate.get.toString("d MMMM yyyy"),
          taxCodeIncome.updateNotificationDate.get.toString("d MMMM yyyy"))),
          Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(OtherForm) | Some(InformationLetter), false, false)
           | (Some(OtherForm) | Some(InformationLetter), false, true)
           | (Some(OtherForm) | Some(InformationLetter), true, false) =>
        (Some(Messages("tai.income.calculation.manual.update.informationLetter.withoutDate")),
          Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(Internet), true, _) => (Some(Messages("tai.income.calculation.manual.update.internet",
        taxCodeIncome.updateNotificationDate.get.toString("d MMMM yyyy"))),
        Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case (Some(Internet), false, _) => (Some(Messages("tai.income.calculation.manual.update.internet.withoutDate")),
        Some(Messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount)))

      case _ => (None, None)
    }
  }

  def getPayFreqMsg(employment: Employment, isPension: Boolean, paymentFrequency: Option[PaymentFrequency],
                    amountYearToDate: BigDecimal, paymentDate: Option[LocalDate],
                    amount: BigDecimal): (Option[String], Option[String]) = {

    val isMidYear = employment.startDate.isAfter(TaxYear().start)
    val pensionOrEmp = if (isPension) "pension" else "emp"

    (paymentFrequency, isMidYear) match {
      case ((Some(Weekly) | Some(FortNightly) | Some(FourWeekly) | Some(Monthly) | Some(Quarterly) | Some(BiAnnually)), false) =>
        (Some(Messages("tai.income.calculation.rti.continuous.weekly." + pensionOrEmp,
          MoneyPounds(amountYearToDate, 2).quantity, paymentDate.map(_.toString("d MMMM yyyy")).getOrElse(""))),
          Some(Messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(amount, 0).quantity)))
      case (Some(Annually), false) =>
        (Some(Messages("tai.income.calculation.rti.continuous.annually." + pensionOrEmp,
          MoneyPounds(amountYearToDate, 2).quantity)),
          Some(Messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(amount, 0).quantity)))
      case (Some(OneOff), false) =>
        (Some(Messages("tai.income.calculation.rti.oneOff." + pensionOrEmp,
          MoneyPounds(amountYearToDate, 2).quantity)),
          Some(Messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(amount, 0).quantity)))
      case (Some(Irregular), false) =>
        val estPay = amount
        (None, Some(Messages("tai.income.calculation.rti.irregular." + pensionOrEmp, MoneyPounds(estPay, 0).quantity)))
      case ((Some(Weekly) | Some(FortNightly) | Some(FourWeekly) | Some(Monthly) | Some(Quarterly) | Some(BiAnnually) | Some(Annually)), true) =>
        (Some(Messages("tai.income.calculation.rti.midYear.weekly", employment.startDate.toString("d MMMM yyyy"),
          paymentDate.map(_.toString("d MMMM yyyy")).getOrElse(""), MoneyPounds(amountYearToDate, 2).quantity)),
          Some(Messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(amount, 0).quantity)))
      case (Some(OneOff), true) =>
        (Some(Messages("tai.income.calculation.rti.oneOff." + pensionOrEmp, MoneyPounds(amountYearToDate, 2).quantity)),
          Some(Messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(amount, 0).quantity)))
      case (Some(Irregular), true) =>
        val estPay =  amount
        (None, Some(Messages("tai.income.calculation.rti.irregular." + pensionOrEmp, MoneyPounds(estPay, 0).quantity)))
      case _ => (None, None)
    }
  }

  def getSameMsg(employment: Employment, amount: BigDecimal, amountYearToDate: BigDecimal,
                 isPension: Boolean, paymentDate: Option[LocalDate]): (Option[String], Option[String]) = {

    val startDate = if (TaxYearResolver.fallsInThisTaxYear(employment.startDate)) employment.startDate else TaxYear().start

    if (amount == amountYearToDate) {
      (Some(Messages("tai.income.calculation.rti." + (if (isPension) "pension" else "emp") +
        ".same", startDate.toString("d MMMM yyyy"),
        paymentDate.map(_.toString("d MMMM yyyy")).getOrElse(""), MoneyPounds(amountYearToDate, 0).quantity)), None)
    } else { (None, None) }
  }
}