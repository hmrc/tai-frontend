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
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{TaxCodeIncome, TaxCodeIncomeSourceStatus}

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
                                              isPension: Boolean
                                            )

object YourIncomeCalculationViewModelNew {
  def apply(taxCodeIncome: TaxCodeIncome, employment: Employment): YourIncomeCalculationViewModelNew = {

    val payments = employment.latestAnnualAccount.map(_.payments).getOrElse(Seq.empty[Payment])
    val paymentDetails = payments.map(payment => PaymentDetailsViewModel(
      payment.date, payment.amount, payment.taxAmount, payment.nationalInsuranceAmount))

    val realTimeStatus = employment.latestAnnualAccount.map(_.realTimeStatus).getOrElse(TemporarilyUnavailable)

    YourIncomeCalculationViewModelNew(
      employment.sequenceNumber,
      employment.name,
      paymentDetails,
      taxCodeIncome.status,
      realTimeStatus,
      latestPaymentDetails(employment),
      employment.endDate,
      taxCodeIncome.componentType == PensionIncome)
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