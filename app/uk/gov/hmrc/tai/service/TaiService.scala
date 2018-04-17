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

package uk.gov.hmrc.tai.service

import uk.gov.hmrc.tai.connectors.TaiConnector
import controllers.auth.TaiUser
import uk.gov.hmrc.tai.model._
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.tai.util.{FormHelper, TaiConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait TaiService {

  def taiClient: TaiConnector

  type IncomeIDPage = (Int, String) => Future[Result]

  private[service] def withoutSuffix(nino: Nino): String = nino.value.take(TaiConstants.NinoWithoutSuffixLength)

  private[service] def createEmploymentAmount(income: TaxCodeIncomeSummary) = new EmploymentAmount(
    description = employmentDescriptionFromIncome(income),
    name = income.name, employmentId = income.employmentId.getOrElse(0),
    newAmount = income.income.map(_.intValue()).getOrElse(0),
    oldAmount = income.income.map(_.intValue()).getOrElse(0),
    startDate = income.startDate, endDate = income.endDate,
    isLive = income.isLive,
    isOccupationalPension = income.isOccupationalPension
  )

  private[service] def employmentDescriptionFromIncome(income: TaxCodeIncomeSummary) = {
    val employmentStatusMessage = income.incomeType match {
      case Some(TaiConstants.IncomeTypeEmployment) => Messages(s"tai.incomes.status-${income.employmentStatus.getOrElse(1)}")
      case _ => ""
    }
    val employmentTypeMessage = Messages(s"tai.incomes.type-${income.incomeType.getOrElse(TaiConstants.IncomeTypeDummy)}")
    s"$employmentStatusMessage $employmentTypeMessage"
  }


  def calculateEstimatedPay(incomeCalculation: IncomeCalculation, startDate: Option[LocalDate])(implicit hc: HeaderCarrier): Future[CalculatedPay] = {

    val paymentFrequency = incomeCalculation.payPeriodForm.map(_.payPeriod.getOrElse("")).getOrElse("")
    val pay = FormHelper.convertCurrencyToInt(incomeCalculation.payslipForm.flatMap(_.totalSalary))
    val taxablePay = incomeCalculation.taxablePayslipForm.map { pay => BigDecimal(FormHelper.convertCurrencyToInt(pay.taxablePay)) }
    val days = incomeCalculation.payPeriodForm.flatMap(_.otherInDays).getOrElse(0)
    val bonus = incomeCalculation.bonusOvertimeAmountForm.map { bonus => BigDecimal(FormHelper.convertCurrencyToInt(bonus.amount)) }

    val payDetails = PayDetails(
      paymentFrequency = paymentFrequency,
      pay = Some(pay),
      taxablePay = taxablePay,
      days = Some(days),
      bonus = bonus,
      startDate = startDate
    )

    taiClient.calculateEstimatedPay(payDetails)
  }

  def personDetails(rootUri: String)(implicit hc: HeaderCarrier): Future[TaiRoot] = taiClient.root(rootUri)
}


object TaiService extends TaiService {
  val taiClient = TaiConnector
}
