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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.constants.PayPeriodConstants._

trait DynamicPayPeriodTitle {

  def dynamicTitle(payPeriod: Option[String], payPeriodInDays: Option[String], periodMessages: Map[String, String])(
    implicit message: Messages): String =
    payPeriod match {
      case Some(Monthly)     => message(periodMessages(Monthly))
      case Some(Weekly)      => message(periodMessages(Weekly))
      case Some(Fortnightly) => message(periodMessages(Fortnightly))
      case Some(Other)       => dayPeriodTitle(payPeriodInDays, periodMessages(Other))
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
      Monthly     -> "tai.payslip.title.month",
      Weekly      -> "tai.payslip.title.week",
      Fortnightly -> "tai.payslip.title.2week",
      Other       -> "tai.payslip.title.days")

    dynamicTitle(payPeriod, payPeriodInDays, messages)
  }
}

object TaxablePayPeriod extends DynamicPayPeriodTitle {
  def errorMessage(payPeriod: Option[String], payPeriodInDays: Option[String])(implicit message: Messages): String = {

    val taxableMessages = Map(
      Monthly     -> "tai.taxablePayslip.title.month",
      Weekly      -> "tai.taxablePayslip.title.week",
      Fortnightly -> "tai.taxablePayslip.title.2week",
      Other       -> "tai.taxablePayslip.title.days"
    )

    dynamicTitle(payPeriod, payPeriodInDays, taxableMessages)
  }
}
