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

package uk.gov.hmrc.tai.forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.tai.forms.formValidator.TaiValidator
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.{EditIncomeIrregularPayConstants, EditIncomePayPeriodConstants}

case class HowToUpdateForm(howToUpdate: Option[String])

object HowToUpdateForm{
  implicit val formats = Json.format[HowToUpdateForm]

  def createForm()(implicit messages: Messages): Form[HowToUpdateForm] = {

    val howToUpdateValidation = Constraint[Option[String]]("Choose how to update"){
      case Some(txt) => Valid
      case _ => Invalid(messages("tai.howToUpdate.error.form.incomes.radioButton.mandatory"))
    }

    Form[HowToUpdateForm](
      mapping(
        "howToUpdate" -> optional(text).verifying(howToUpdateValidation)
      )(HowToUpdateForm.apply)(HowToUpdateForm.unapply)
    )
  }
}

case class HoursWorkedForm(workingHours: Option[String])

object HoursWorkedForm extends EditIncomeIrregularPayConstants {
  implicit val formats = Json.format[HoursWorkedForm]

  def createForm()(implicit messages: Messages): Form[HoursWorkedForm] = {

    val hoursWorkedValidation = Constraint[Option[String]]("Your working hours"){
      case Some(REGULAR_HOURS) => Valid
      case Some(IRREGULAR_HOURS) => Valid
      case _ => Invalid(messages("tai.workingHours.error.form.incomes.radioButton.mandatory"))
    }

    Form[HoursWorkedForm](
      mapping(
        "workingHours" -> optional(text).verifying(hoursWorkedValidation)
      )(HoursWorkedForm.apply)(HoursWorkedForm.unapply)
    )
  }
}

case class PayPeriodForm(payPeriod: Option[String],
                         otherInDays: Option[String] = None) {
}

object PayPeriodForm extends EditIncomePayPeriodConstants{
  implicit val formats = Json.format[PayPeriodForm]

  def createForm(howOftenError: Option[String], payPeriod : Option[String] = None)(implicit messages: Messages): Form[PayPeriodForm] = {

    val payPeriodValidation = Constraint[Option[String]]("Please select a period"){
      case Some(txt) => txt match {
        case OTHER | MONTHLY | WEEKLY | FORTNIGHTLY => Valid
        case _ => Invalid(messages("tai.payPeriod.error.form.incomes.radioButton.mandatory"))
      }
      case _ => Invalid(messages("tai.payPeriod.error.form.incomes.radioButton.mandatory"))
    }

    def otherInDaysValidation(payPeriod : Option[String]) : Constraint[Option[String]] = {
      val digitsOnly = """^\d*$""".r

      Constraint[Option[String]]("days") {
        days: Option[String] => {
          (payPeriod, days) match {
            case (Some(OTHER), Some(digitsOnly())) => Valid
            case (Some(OTHER), None) => Invalid(messages("tai.payPeriod.error.form.incomes.other.mandatory"))
            case (Some(OTHER), _) => Invalid(messages("tai.payPeriod.error.form.incomes.other.invalid"))
            case _ => Valid
          }
        }
      }
    }

    Form[PayPeriodForm](
      mapping(
        PAY_PERIOD_KEY -> optional(text).verifying(payPeriodValidation),
        OTHER_IN_DAYS_KEY -> optional(text).verifying(otherInDaysValidation(payPeriod))
      )(PayPeriodForm.apply)(PayPeriodForm.unapply)
    )
  }
}

case class PayslipForm(totalSalary: Option[String] = None)

object PayslipForm{
  implicit val formats = Json.format[PayslipForm]

  def createForm()(implicit messages: Messages): Form[PayslipForm] = {
    Form[PayslipForm](
      mapping("totalSalary" -> TaiValidator.validateNewAmounts(messages("tai.payslip.error.form.totalPay.mandatory"),
                                                               messages("error.invalid.monetaryAmount.format.invalid"),
                                                               messages("error.tai.updateDataEmployment.maxLength")))(PayslipForm.apply)(PayslipForm.unapply)
    )
  }
}



case class PayslipDeductionsForm(payslipDeductions: Option[String])

object PayslipDeductionsForm{
  implicit val formats = Json.format[PayslipDeductionsForm]

  def createForm()(implicit messages: Messages): Form[PayslipDeductionsForm] = {

    val payslipDeductionsValidation = Constraint[Option[String]]("Your working hours"){
      case Some(txt) => Valid
      case _ => Invalid(messages("tai.payslipDeductions.error.form.incomes.radioButton.mandatory"))
    }

    Form[PayslipDeductionsForm](
      mapping("payslipDeductions" -> optional(text).verifying(payslipDeductionsValidation))
        (PayslipDeductionsForm.apply)(PayslipDeductionsForm.unapply)
    )
  }
}


case class TaxablePayslipForm(taxablePay: Option[String] = None)

object TaxablePayslipForm{
  implicit val formats = Json.format[TaxablePayslipForm]

  def createForm(netSalary: Option[String] = None)(implicit messages: Messages): Form[TaxablePayslipForm] = {
    Form[TaxablePayslipForm](
      mapping("taxablePay" -> TaiValidator.validateNewAmounts(
        messages("tai.taxablePayslip.error.form.incomes.radioButton.mandatory"),
        messages("error.invalid.monetaryAmount.format.invalid"),
        messages("error.tai.updateDataEmployment.maxLength"),
        netSalary
      ))(TaxablePayslipForm.apply)(TaxablePayslipForm.unapply)
    )
  }
}

object BonusPaymentsForm{
  def createForm(implicit messages: Messages): Form[YesNoForm] = {
    YesNoForm.form(messages("tai.bonusPayments.error.form.incomes.radioButton.mandatory",
      TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited))
  }
}


case class BonusOvertimeAmountForm(amount: Option[String] = None)

object BonusOvertimeAmountForm{
  implicit val formats = Json.format[BonusOvertimeAmountForm]

  def createForm(nonEmptyMessage: Option[String]=None, notAnAmountMessage: Option[String]=None)(implicit messages: Messages): Form[BonusOvertimeAmountForm] = {
    Form[BonusOvertimeAmountForm](
      mapping("amount" -> TaiValidator.validateNewAmounts(messages("tai.bonusPaymentsAmount.error.form.mandatory",
                                                            TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited),
                                                          messages("error.invalid.monetaryAmount.format.invalid"),
                                                          messages("error.tai.updateDataEmployment.maxLength")))(
                                                          BonusOvertimeAmountForm.apply)(BonusOvertimeAmountForm.unapply)
    )
  }

}
