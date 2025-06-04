/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates, ViewModelHelper}

case class IncomeSourceSummaryViewModel(
  empId: Int,
  displayName: String,
  empOrPensionName: String,
  estimatedTaxableIncome: Option[BigDecimal],
  incomeReceivedToDate: BigDecimal,
  taxCode: Option[String],
  pensionOrPayrollNumber: String,
  isPension: Boolean,
  estimatedPayJourneyCompleted: Boolean,
  rtiAvailable: Boolean,
  taxDistrictNumber: String,
  payeNumber: String,
  isUpdateInProgress: Boolean = false
) extends ViewModelHelper {
  def startOfCurrentYear(implicit messages: Messages): String = Dates.formatDate(TaxYear().start)

  def endOfCurrentYear(implicit messages: Messages): String = Dates.formatDate(TaxYear().end)
}

object IncomeSourceSummaryViewModel {
  def applyNew(
    empId: Int,
    displayName: String,
    optTaxCodeIncome: Option[TaxCodeIncome], // Tax account API response
    employment: Employment, // Employment API response
    payments: Option[AnnualAccount],
    estimatedPayJourneyCompleted: Boolean,
    rtiAvailable: Boolean,
    cacheUpdatedIncomeAmount: Option[Int]
  ): IncomeSourceSummaryViewModel = {
    val estimatedPayAmount = optTaxCodeIncome.map(_.amount)
    val taxCode = optTaxCodeIncome.map(_.taxCode)

    val amountYearToDate = payments.flatMap(_.latestPayment).map(_.amountYearToDate)

    val isUpdateInProgress = cacheUpdatedIncomeAmount match {
      case Some(cacheUpdateAMount) => cacheUpdateAMount != estimatedPayAmount.map(_.toInt).getOrElse(0)
      case None                    => false
    }

    IncomeSourceSummaryViewModel(
      empId = empId,
      displayName = displayName,
      empOrPensionName = employment.name,
      estimatedTaxableIncome = estimatedPayAmount,
      incomeReceivedToDate = amountYearToDate.getOrElse(0),
      taxCode = taxCode,
      pensionOrPayrollNumber = employment.payrollNumber.getOrElse(""),
      isPension = employment.receivingOccupationalPension,
      estimatedPayJourneyCompleted = estimatedPayJourneyCompleted,
      rtiAvailable = rtiAvailable,
      taxDistrictNumber = employment.taxDistrictNumber,
      payeNumber = employment.payeNumber,
      isUpdateInProgress = isUpdateInProgress
    )
  }

  def applyOld(
    empId: Int,
    displayName: String,
    taxCodeIncomeSources: Seq[TaxCodeIncome],
    employment: Employment,
    estimatedPayJourneyCompleted: Boolean,
    rtiAvailable: Boolean,
    cacheUpdatedIncomeAmount: Option[Int]
  ): IncomeSourceSummaryViewModel = {

    val amountYearToDate = for {
      latestAnnualAccount <- employment.latestAnnualAccount
      latestPayment       <- latestAnnualAccount.latestPayment
    } yield latestPayment.amountYearToDate

    val taxCodeIncomeSource = taxCodeIncomeSources
      .find(_.employmentId.contains(empId))
      .getOrElse(throw new RuntimeException(s"Income details not found for employment id $empId"))

    val isUpdateInProgress = cacheUpdatedIncomeAmount match {
      case Some(cacheUpdateAMount) => cacheUpdateAMount != taxCodeIncomeSource.amount.toInt
      case None                    => false
    }

    IncomeSourceSummaryViewModel(
      empId,
      displayName,
      taxCodeIncomeSource.name,
      Some(taxCodeIncomeSource.amount),
      amountYearToDate.getOrElse(0),
      Some(taxCodeIncomeSource.taxCode),
      employment.payrollNumber.getOrElse(""),
      taxCodeIncomeSource.componentType == PensionIncome,
      estimatedPayJourneyCompleted,
      rtiAvailable,
      employment.taxDistrictNumber,
      employment.payeNumber,
      isUpdateInProgress
    )
  }

}
