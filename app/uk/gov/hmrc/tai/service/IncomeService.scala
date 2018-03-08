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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.forms.{BonusPaymentsForm, PayPeriodForm}
import uk.gov.hmrc.tai.model.EmploymentAmount
import uk.gov.hmrc.tai.model.domain.Payment
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.util.JourneyCacheConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait IncomeService extends JourneyCacheConstants {

  def taxAccountService: TaxAccountService

  def employmentService: EmploymentService

  def employmentAmount(nino: Nino, id: Int)(implicit hc: HeaderCarrier): Future[EmploymentAmount] = {
    for {
      taxCodeIncomeDetails <- taxAccountService.taxCodeIncomes(nino, TaxYear())
      employmentDetails <- employmentService.employment(nino, id)
    } yield {
      (taxCodeIncomeDetails, employmentDetails) match {
        case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
          taxCodeIncomes.find(_.employmentId.contains(id)) match {
            case Some(taxCodeIncome) =>
              EmploymentAmount(taxCodeIncome, employment)
            case _ => throw new RuntimeException(s"Not able to found employment with id $id")
          }
        case _ => throw new RuntimeException("Exception while reading employment and tax code details")
      }
    }
  }

  def latestPayment(nino: Nino, id: Int)(implicit hc: HeaderCarrier): Future[Option[Payment]] = {
    employmentService.employment(nino, id) map {
      case Some(employment) =>
        for {
          latestAnnualAccount <- employment.latestAnnualAccount
          latestPayment <- latestAnnualAccount.latestPayment
        } yield latestPayment

      case _ => None
    }
  }

  def cachePaymentForRegularIncome(latestPayment: Option[Payment])(implicit hc: HeaderCarrier): Map[String, String] = {
    latestPayment match {
      case Some(payment) => Map(UpdateIncome_PayToDateKey -> payment.amountYearToDate.toString, UpdateIncome_DateKey -> payment.date.toString)
      case None => Map(UpdateIncome_PayToDateKey -> "0")
    }
  }

  def cachePayPeriod(form: PayPeriodForm)(implicit hc: HeaderCarrier): Map[String, String] =
    form.otherInDays match {
      case Some(days) => Map(UpdateIncome_PayPeriodKey -> form.payPeriod.getOrElse(""), UpdateIncome_OtherInDaysKey -> days.toString)
      case _ => Map(UpdateIncome_PayPeriodKey -> form.payPeriod.getOrElse(""))
    }

  def cacheBonusPayments(bonusPaymentsForm: BonusPaymentsForm)(implicit hc: HeaderCarrier): Map[String, String] = {
    bonusPaymentsForm.bonusPayments.fold(Map.empty[String, String])(bonusPayments => Map(UpdateIncome_BonusPaymentsKey -> bonusPayments)) ++
      bonusPaymentsForm.bonusPaymentsMoreThisYear.fold(Map.empty[String, String])(bonusPayments => Map(UpdateIncome_BonusPaymentsThisYearKey -> bonusPayments))
  }
}

object IncomeService extends IncomeService {
  override val taxAccountService: TaxAccountService = TaxAccountService
  override val employmentService: EmploymentService = EmploymentService
}
