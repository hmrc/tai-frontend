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
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear

case class PaymentDetailsViewModel(date: LocalDate,
                                   taxableIncome: BigDecimal,
                                   taxAmount: BigDecimal,
                                   nationalInsuranceAmount: BigDecimal)

case class LatestPayment(date: LocalDate,
                         amountYearToDate: BigDecimal,
                         taxAmountYearToDate: BigDecimal,
                         nationalInsuranceAmountYearToDate: BigDecimal,
                         paymentFrequency: PaymentFrequency)

case class YourIncomeCalculationViewModel(
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

object YourIncomeCalculationViewModel {
  def apply(taxCodeIncome: Option[TaxCodeIncome], employment: Employment)(implicit messages: Messages): YourIncomeCalculationViewModel = {

    val payments = employment.latestAnnualAccount.map(_.payments).getOrElse(Seq.empty[Payment])
    val paymentDetails = payments.map(payment => PaymentDetailsViewModel(
      payment.date, payment.amount, payment.taxAmount, payment.nationalInsuranceAmount))

    val realTimeStatus = employment.latestAnnualAccount.map(_.realTimeStatus).getOrElse(TemporarilyUnavailable)

    val latestPayment = latestPaymentDetails(employment)
    val isPension = taxCodeIncome.exists(_.componentType == PensionIncome)
    val status = taxCodeIncome.map(_.status).getOrElse(Ceased)

    val (incomeCalculationMessage, incomeCalculationEstimateMessage) = taxCodeIncome map { income =>
      incomeExplanationMessage(
        income.status,
        employment,
        pensionOrEmpMessage(isPension),
        income,
        latestPayment.map(_.paymentFrequency),
        latestPayment.map(_.amountYearToDate).getOrElse(BigDecimal(0)),
        latestPayment.map(_.date))
    } getOrElse(None, None)

    YourIncomeCalculationViewModel(
      employment.sequenceNumber,
      employment.name,
      paymentDetails,
      status,
      realTimeStatus,
      latestPayment,
      employment.endDate,
      isPension,
      totalNotEqualMessage(status == Live, paymentDetails, latestPayment, isPension),
      incomeCalculationMessage.getOrElse(""),
      if (status == Ceased) None else incomeCalculationEstimateMessage,
      employment.hasPayrolledBenefit
    )
  }

  private def pensionOrEmpMessage(isPension: Boolean): String = if (isPension) "pension" else "emp"

  private def totalNotEqualMessage(isLive: Boolean,
                                   payments: Seq[PaymentDetailsViewModel],
                                   latestPayment: Option[LatestPayment],
                                   isPension: Boolean)(implicit messages: Messages) = {
    val isTotalEqual = payments.map(_.taxAmount).sum == latestPayment.map(_.taxAmountYearToDate).getOrElse(0) &&
      payments.map(_.taxableIncome).sum == latestPayment.map(_.amountYearToDate).getOrElse(0) &&
      payments.map(_.nationalInsuranceAmount).sum == latestPayment.map(_.nationalInsuranceAmountYearToDate).getOrElse(0)

    if (isLive && !isTotalEqual && isPension) {
      Some(messages("tai.income.calculation.totalNotMatching.pension.message"))
    } else if (isLive && !isTotalEqual && !isPension) {
      Some(messages("tai.income.calculation.totalNotMatching.emp.message"))
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
                                pensionOrEmployment: String,
                                taxCodeIncome: TaxCodeIncome,
                                paymentFrequency: Option[PaymentFrequency],
                                amountYearToDate: BigDecimal,
                                paymentDate: Option[LocalDate])(implicit messages: Messages): (Option[String], Option[String]) = {


    lazy val ceasedIncomeMessages = (
      ceasedIncomeCalculationMessage(employmentStatus, employment, pensionOrEmployment),
      ceasedIncomeCalculationEstimateMessage(employmentStatus, employment, taxCodeIncome.amount)
    )

    lazy val manualIncomeMessages = {
      val msg = manualUpdateIncomeCalculationMessage(taxCodeIncome)
      (msg, if(msg.isDefined) manualUpdateIncomeCalculationEstimateMessage(taxCodeIncome) else None)
    }

    lazy val sameMessages = (sameIncomeCalculationMessage(employment, taxCodeIncome.amount, amountYearToDate, pensionOrEmployment, paymentDate), None)

    lazy val payFrequencyMessages = (
      payFreqIncomeCalculationMessage(employment, pensionOrEmployment, paymentFrequency, amountYearToDate, paymentDate),
      payFreqIncomeCalculationEstimateMessage(pensionOrEmployment, paymentFrequency, paymentDate, taxCodeIncome.amount)
    )

    lazy val defaultMessages =
      (Some(messages(s"tai.income.calculation.default.$pensionOrEmployment", Dates.formatDate(TaxYear().end))),
        Some(messages(s"tai.income.calculation.default.estimate.$pensionOrEmployment", taxCodeIncome.amount)))


    if(fetchMessages.isDefinedAt(ceasedIncomeMessages)) ceasedIncomeMessages
    else if (fetchMessages.isDefinedAt(manualIncomeMessages)) manualIncomeMessages
    else if (fetchMessages.isDefinedAt(sameMessages)) sameMessages
    else if (fetchMessages.isDefinedAt(payFrequencyMessages)) payFrequencyMessages
    else defaultMessages

  }

  def fetchMessages: PartialFunction[(Option[String], Option[String]), (Option[String], Option[String])] = {
    case (incomeMessage@Some(_), incomeEstimateMessage@Some(_)) => (incomeMessage, incomeEstimateMessage)
    case (None, incomeEstimateMessage@Some(_)) => (None, incomeEstimateMessage)
    case (incomeMessage@Some(_), None) => (incomeMessage, None)
  }

  def ceasedIncomeCalculationMessage(employmentStatus: TaxCodeIncomeSourceStatus, employment: Employment, pensionOrEmpMessage: String
                                    )(implicit messages: Messages): Option[String] = {
    (employmentStatus, employment.endDate, employment.cessationPay) match {
      case (Ceased, Some(endDate), Some(_)) =>
        Some(messages(s"tai.income.calculation.rti.ceased.$pensionOrEmpMessage", Dates.formatDate(endDate)))

      case (Ceased, Some(_), None) =>
        Some(messages(s"tai.income.calculation.rti.ceased.$pensionOrEmpMessage.noFinalPay"))

      case (PotentiallyCeased, None, _) =>
        Some(messages(s"tai.income.calculation.rti.ceased.$pensionOrEmpMessage.noFinalPay"))

      case _ => None
    }
  }

  def ceasedIncomeCalculationEstimateMessage(employmentStatus: TaxCodeIncomeSourceStatus, employment: Employment,
                                             amount: BigDecimal)(implicit messages: Messages): Option[String] = {
    (employmentStatus, employment.endDate, employment.cessationPay) match {
      case (Ceased, Some(_), None) =>
        Some(messages("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(amount, 0).quantity))

      case (PotentiallyCeased, None, _) =>
        Some(messages("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(amount, 0).quantity))

      case _ => None
    }
  }

  def manualUpdateIncomeCalculationMessage(taxCodeIncome: TaxCodeIncome)(implicit messages: Messages): Option[String] = {
    (taxCodeIncome.iabdUpdateSource, taxCodeIncome.updateNotificationDate, taxCodeIncome.updateActionDate) match {
      case (Some(ManualTelephone), Some(notificationDate), Some(actionDate)) =>
        Some(messages("tai.income.calculation.manual.update.phone", Dates.formatDate(actionDate), Dates.formatDate(notificationDate)))

      case (Some(ManualTelephone), _, _) =>
        Some(messages("tai.income.calculation.manual.update.phone.withoutDate"))

      case (Some(Letter), Some(notificationDate), Some(actionDate)) =>
        Some(messages("tai.income.calculation.manual.update.letter", Dates.formatDate(actionDate), Dates.formatDate(notificationDate)))

      case (Some(Letter), _, _) =>
        Some(messages("tai.income.calculation.manual.update.letter.withoutDate"))

      case (Some(Email), Some(notificationDate), Some(actionDate)) =>
        Some(messages("tai.income.calculation.manual.update.email", Dates.formatDate(actionDate), Dates.formatDate(notificationDate)))

      case (Some(Email), _, _) =>
        Some(messages("tai.income.calculation.manual.update.email.withoutDate"))

      case (Some(AgentContact), _, _) => Some(messages("tai.income.calculation.agent"))

      case (Some(OtherForm) | Some(InformationLetter), Some(notificationDate), Some(actionDate)) =>
        Some(messages("tai.income.calculation.manual.update.informationLetter", Dates.formatDate(actionDate), Dates.formatDate(notificationDate)))

      case (Some(OtherForm) | Some(InformationLetter), _, _) =>
        Some(messages("tai.income.calculation.manual.update.informationLetter.withoutDate"))

      case (Some(Internet), Some(notificationDate), _) =>
        Some(messages("tai.income.calculation.manual.update.internet", Dates.formatDate(notificationDate)))

      case (Some(Internet), None, _) =>
        Some(messages("tai.income.calculation.manual.update.internet.withoutDate"))

      case _ => None
    }
  }

  def manualUpdateIncomeCalculationEstimateMessage(taxCodeIncome: TaxCodeIncome)(implicit messages: Messages): Option[String] = {
    (taxCodeIncome.iabdUpdateSource, taxCodeIncome.updateNotificationDate, taxCodeIncome.updateActionDate) match {
      case (Some(AgentContact), _, _) => Some(messages("tai.income.calculation.agent.estimate", taxCodeIncome.amount))
      case _ => Some(messages("tai.income.calculation.rti.manual.update.estimate", taxCodeIncome.amount))
    }
  }

  def payFreqIncomeCalculationMessage(employment: Employment, pensionOrEmployment: String, paymentFrequency: Option[PaymentFrequency],
                                      amountYearToDate: BigDecimal, paymentDate: Option[LocalDate])(implicit messages: Messages): Option[String] = {

    val isMidYear = employment.startDate.isAfter(TaxYear().start)
    val paymentDt = paymentDate.map(Dates.formatDate).getOrElse("")
    print("------------Epic fail I am calling this guy")
    paymentFrequency match {
      case ((Some(Weekly) | Some(FortNightly) | Some(FourWeekly) | Some(Monthly) | Some(Quarterly) | Some(BiAnnually))) =>
        if (isMidYear)
          Some(messages(s"tai.income.calculation.rti.midYear.weekly", Dates.formatDate(employment.startDate), paymentDt, MoneyPounds(amountYearToDate, 2).quantity))
        else
          Some(messages(s"tai.income.calculation.rti.continuous.weekly.$pensionOrEmployment", MoneyPounds(amountYearToDate, 2).quantity, paymentDt))

      case Some(Annually) =>
        if (isMidYear)
          Some(messages("tai.income.calculation.rti.midYear.weekly", Dates.formatDate(employment.startDate), paymentDt, MoneyPounds(amountYearToDate, 2).quantity))
        else
          Some(messages(s"tai.income.calculation.rti.continuous.annually.$pensionOrEmployment", MoneyPounds(amountYearToDate, 2).quantity))

      case Some(OneOff) =>
        Some(messages(s"tai.income.calculation.rti.oneOff.$pensionOrEmployment", MoneyPounds(amountYearToDate, 2).quantity))

      case _ => None
    }
  }

  def payFreqIncomeCalculationEstimateMessage(pensionOrEmployment: String, paymentFrequency: Option[PaymentFrequency],
                                              paymentDate: Option[LocalDate], amount: BigDecimal)(implicit messages: Messages): Option[String] = {
    paymentFrequency match {
      case (Some(Weekly) | Some(FortNightly) | Some(FourWeekly) | Some(Monthly) |
            Some(Quarterly) | Some(BiAnnually) | Some(Annually) | Some(OneOff)) =>
        Some(messages(s"tai.income.calculation.rti.$pensionOrEmployment.estimate", MoneyPounds(amount, 0).quantity))

      case Some(Irregular) =>
        Some(messages(s"tai.income.calculation.rti.irregular.$pensionOrEmployment", MoneyPounds(amount, 0).quantity))

      case _ => None
    }
  }

  def sameIncomeCalculationMessage(employment: Employment, amount: BigDecimal, amountYearToDate: BigDecimal,
                                   pensionOrEmployment: String, paymentDate: Option[LocalDate])(implicit messages: Messages): Option[String] = {

    val startDate = if (TaxYearResolver.fallsInThisTaxYear(employment.startDate)) employment.startDate else TaxYear().start

    if (amount == amountYearToDate) {
      Some(messages(s"tai.income.calculation.rti.$pensionOrEmployment.same", Dates.formatDate(startDate),
        paymentDate.map(Dates.formatDate).getOrElse(""), MoneyPounds(amountYearToDate, 0).quantity))
    } else {
      None
    }
  }
}
