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

package uk.gov.hmrc.tai.service

import pages.income.{UpdateIncomePayToDatePage, UpdatedIncomeDatePage}
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaiConnector
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.income.{Ceased, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, Payment, PensionIncome}
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.journeyCache._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeService @Inject() (
  employmentService: EmploymentService,
  taiConnector: TaiConnector
) {

  def employmentAmount(nino: Nino, id: Int)(implicit
    hc: HeaderCarrier,
    messages: Messages,
    ec: ExecutionContext
  ): Future[EmploymentAmount] =
    employmentService.employment(nino, id).map {
      case Some(employment) => EmploymentAmount(taxCodeIncome = None, employment = employment)
      case None             => throw new RuntimeException(s"Not able to found employment with id $id")
    }

  def latestPayment(nino: Nino, id: Int)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[Payment]] =
    employmentService
      .employment(nino, id)
      .map(_.flatMap(_.latestAnnualAccount.flatMap(_.latestPayment))) // TODO Use the ATI getPaymentsForYear

  def calculateEstimatedPay(cache: Map[String, String], startDate: Option[LocalDate])(implicit
    hc: HeaderCarrier
  ): Future[CalculatedPay] = {

    def isCacheAvailable(key: String): Option[BigDecimal] =
      if (cache.contains(key)) Some(BigDecimal(FormHelper.convertCurrencyToInt(cache.get(key)))) else None

    val paymentFrequency = cache.getOrElse(UpdateIncomeConstants.PayPeriodKey, "")
    val pay = FormHelper.convertCurrencyToInt(cache.get(UpdateIncomeConstants.TotalSalaryKey))
    val taxablePay = isCacheAvailable(UpdateIncomeConstants.TaxablePayKey)
    val days = cache.getOrElse(UpdateIncomeConstants.OtherInDaysKey, "0").toInt
    val bonus = isCacheAvailable(UpdateIncomeConstants.BonusOvertimeAmountKey)

    val payDetails = PayDetails(
      paymentFrequency = paymentFrequency,
      pay = Some(pay),
      taxablePay = taxablePay,
      days = Some(days),
      bonus = bonus,
      startDate = startDate
    )

    taiConnector.calculateEstimatedPay(payDetails)
  }

  def editableIncomes(taxCodeIncomes: Seq[TaxCodeIncome]): Seq[TaxCodeIncome] =
    taxCodeIncomes.filter(income =>
      (income.componentType == EmploymentIncome || income.componentType == PensionIncome) && income.status != Ceased
    )

  def singularIncomeId(taxCodeIncomes: Seq[TaxCodeIncome]): Option[Int] =
    editableIncomes(taxCodeIncomes) match {
      case Seq(singleIncome) => singleIncome.employmentId
      case _                 => None
    }

  def cachePaymentForRegularIncome(latestPayment: Option[Payment], userAnswers: UserAnswers): UserAnswers =
    latestPayment match {
      case Some(payment) =>
        userAnswers
          .setOrException(UpdateIncomePayToDatePage, payment.amountYearToDate.toString)
          .setOrException(UpdatedIncomeDatePage, payment.date.toString)
      case None =>
        userAnswers.setOrException(UpdateIncomePayToDatePage, "0")
    }
}
