/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants

trait DynamicPayPeriodTitle extends EditIncomePayPeriodConstants {

  def dynamicTitle(payPeriod: Option[String], payPeriodInDays: Option[String], periodMessages: Map[String, String])(
    implicit message: Messages) =
    payPeriod match {
      case Some(MONTHLY)     => message(periodMessages(MONTHLY))
      case Some(WEEKLY)      => message(periodMessages(WEEKLY))
      case Some(FORTNIGHTLY) => message(periodMessages(FORTNIGHTLY))
      case Some(OTHER)       => dayPeriodTitle(payPeriodInDays, periodMessages(OTHER))
      case _                 => throw new RuntimeException("No pay period found")
    }

  private def dayPeriodTitle(payPeriodInDays: Option[String], messageKey: String)(implicit message: Messages): String =
    payPeriodInDays match {
      case Some(days) => message(messageKey, days)
      case _          => throw new RuntimeException("No days found for pay period")
    }
}

object GrossPayPeriodTitle extends DynamicPayPeriodTitle {
  def title(payPeriod: Option[String], payPeriodInDays: Option[String])(implicit message: Messages): String = {

    val messages = Map(
      MONTHLY     -> "tai.payslip.title.month",
      WEEKLY      -> "tai.payslip.title.week",
      FORTNIGHTLY -> "tai.payslip.title.2week",
      OTHER       -> "tai.payslip.title.days")

    dynamicTitle(payPeriod, payPeriodInDays, messages)
  }
}

object TaxablePayPeriod extends DynamicPayPeriodTitle {
  def errorMessage(payPeriod: Option[String], payPeriodInDays: Option[String])(implicit message: Messages): String = {

    val taxableMessages = Map(
      MONTHLY     -> "tai.taxablePayslip.title.month",
      WEEKLY      -> "tai.taxablePayslip.title.week",
      FORTNIGHTLY -> "tai.taxablePayslip.title.2week",
      OTHER       -> "tai.taxablePayslip.title.days"
    )

    dynamicTitle(payPeriod, payPeriodInDays, taxableMessages)
  }
}
