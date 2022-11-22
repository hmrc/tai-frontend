/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.Inject
import java.time.LocalDate
import play.api.i18n.Messages
import cats.implicits._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaiConnector
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.{TaxYear, _}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, Payment, PensionIncome}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.model.domain.income.Ceased
import uk.gov.hmrc.tai.util.constants.journeyCache._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IncomeService @Inject()(
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  taiConnector: TaiConnector
) {

  def employmentAmount(nino: Nino, id: Int)(implicit hc: HeaderCarrier, messages: Messages): Future[EmploymentAmount] =
    (
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      employmentService.employment(nino, id)
    ) mapN {
      case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
        taxCodeIncomes.find(_.employmentId.contains(id)) match {
          case Some(taxCodeIncome) => EmploymentAmount(taxCodeIncome, employment)
          case _                   => throw new RuntimeException(s"Not able to found employment with id $id")
        }
      case _ => throw new RuntimeException("Exception while reading employment and tax code details")
    }

  def latestPayment(nino: Nino, id: Int)(implicit hc: HeaderCarrier): Future[Option[Payment]] =
    employmentService.employment(nino, id) map {
      case Some(employment) =>
        for {
          latestAnnualAccount <- employment.latestAnnualAccount
          latestPayment       <- latestAnnualAccount.latestPayment
        } yield latestPayment

      case _ => None
    }

  def calculateEstimatedPay(cache: Map[String, String], startDate: Option[LocalDate])(
    implicit hc: HeaderCarrier
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
    taxCodeIncomes.filter { income =>
      (income.componentType == EmploymentIncome || income.componentType == PensionIncome) &&
      income.status != Ceased
    }

  def singularIncomeId(taxCodeIncomes: Seq[TaxCodeIncome]): Option[Int] = {
    val incomes = editableIncomes(taxCodeIncomes)
    if (incomes.size == 1) {
      incomes.head.employmentId
    } else {
      None
    }
  }

  def cachePaymentForRegularIncome(latestPayment: Option[Payment])(implicit hc: HeaderCarrier): Map[String, String] =
    latestPayment match {
      case Some(payment) =>
        Map(
          UpdateIncomeConstants.PayToDateKey -> payment.amountYearToDate.toString,
          UpdateIncomeConstants.DateKey      -> payment.date.toString)
      case None => Map(UpdateIncomeConstants.PayToDateKey -> "0")
    }

}
