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
import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome, TaxCodeIncomeSourceStatus}

case class PaymentDetailsViewModel(date: LocalDate,
                                   taxableIncome: BigDecimal,
                                   taxAmount: BigDecimal,
                                   nationalInsuranceAmount: BigDecimal)

case class LatestPayment(date: LocalDate,
                         amountYearToDate: BigDecimal,
                         taxAmountYearToDate: BigDecimal,
                         nationalInsuranceAmountYearToDate: BigDecimal)

case class YourIncomeCalculationViewModelNew(
                                              empId: Int,
                                              employerName: String,
                                              payments: Seq[PaymentDetailsViewModel],
                                              employmentStatus: TaxCodeIncomeSourceStatus,
                                              rtiStatus: RealTimeStatus,
                                              latestPayment: Option[LatestPayment],
                                              endDate: Option[LocalDate],
                                              isPension: Boolean,
                                              messageWhenTotalNotEqual: Option[String]
                                            )

object YourIncomeCalculationViewModelNew {
  def apply(taxCodeIncome: TaxCodeIncome, employment: Employment): YourIncomeCalculationViewModelNew = {

    val payments = employment.latestAnnualAccount.map(_.payments).getOrElse(Seq.empty[Payment])
    val paymentDetails = payments.map(payment => PaymentDetailsViewModel(
      payment.date, payment.amount, payment.taxAmount, payment.nationalInsuranceAmount))

    val realTimeStatus = employment.latestAnnualAccount.map(_.realTimeStatus).getOrElse(TemporarilyUnavailable)

    val latestPayment = latestPaymentDetails(employment)
    val isPension = taxCodeIncome.componentType == PensionIncome
    YourIncomeCalculationViewModelNew(
      employment.sequenceNumber,
      employment.name,
      paymentDetails,
      taxCodeIncome.status,
      realTimeStatus,
      latestPayment,
      employment.endDate,
      isPension,
      totalNotEqualMessage(taxCodeIncome.status == Live, paymentDetails, latestPayment, isPension)
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
      latestPayment.nationalInsuranceAmountYearToDate)
  }
}