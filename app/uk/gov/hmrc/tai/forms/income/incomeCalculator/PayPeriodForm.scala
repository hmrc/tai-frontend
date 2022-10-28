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

package uk.gov.hmrc.tai.forms.income.incomeCalculator

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tai.forms.income.incomeCalculator.PayPeriodForm.{FORTNIGHTLY, MONTHLY, OTHER, OTHER_IN_DAYS_KEY, PAY_PERIOD_KEY, WEEKLY}
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants

case class PayPeriodForm(payPeriod: Option[String], otherInDays: Option[String] = None) {}

object PayPeriodForm extends EditIncomePayPeriodConstants {
  implicit val formats: OFormat[PayPeriodForm] = Json.format[PayPeriodForm]

  def createForm(howOftenError: Option[String], payPeriod: Option[String] = None)(
    implicit messages: Messages): Form[PayPeriodForm] = {

    val payPeriodValidation = Constraint[Option[String]]("Please select a period") {
      case Some(txt) =>
        txt match {
          case OTHER | MONTHLY | WEEKLY | FORTNIGHTLY => Valid
          case _                                      => Invalid(messages("tai.payPeriod.error.form.incomes.radioButton.mandatory"))
        }
      case _ => Invalid(messages("tai.payPeriod.error.form.incomes.radioButton.mandatory"))
    }

    def otherInDaysValidation(payPeriod: Option[String]): Constraint[Option[String]] = {
      val digitsOnly = """^\d*$""".r

      Constraint[Option[String]]("days") { days: Option[String] =>
        {
          (payPeriod, days) match {
            case (Some(OTHER), Some(digitsOnly())) => Valid
            case (Some(OTHER), None)               => Invalid(messages("tai.payPeriod.error.form.incomes.other.mandatory"))
            case (Some(OTHER), _)                  => Invalid(messages("tai.payPeriod.error.form.incomes.other.invalid"))
            case _                                 => Valid
          }
        }
      }
    }

    Form[PayPeriodForm](
      mapping(
        PAY_PERIOD_KEY    -> optional(text).verifying(payPeriodValidation),
        OTHER_IN_DAYS_KEY -> optional(text).verifying(otherInDaysValidation(payPeriod))
      )(PayPeriodForm.apply)(PayPeriodForm.unapply)
    )
  }
}
