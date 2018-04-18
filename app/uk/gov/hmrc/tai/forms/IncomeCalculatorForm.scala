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

import uk.gov.hmrc.tai.forms.formValidator.TaiValidator
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.Request

case class HowToUpdateForm(howToUpdate: Option[String])

object HowToUpdateForm{
  implicit val formats = Json.format[HowToUpdateForm]

  def bind(implicit request: Request[_]) = createForm().bindFromRequest

//  def create(howToUpdateForm: HowToUpdateForm) = {
//    createForm.fill(howToUpdateForm)
//  }

//  def howToUpdateValidation = Constraint[Option[String]]("Choose how to update"){
//    case Some(txt) => Valid
//    case _ => Invalid(Messages("tai.howToUpdate.error.form.incomes.radioButton.mandatory"))
//  }

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

object HoursWorkedForm{
  implicit val formats = Json.format[HoursWorkedForm]

  def bind(implicit request: Request[_]) = createForm().bindFromRequest

//  def create(hoursWorkedForm: HoursWorkedForm) = {
//    createForm.fill(hoursWorkedForm)
//  }

  def hoursWorkedValidation = Constraint[Option[String]]("Your working hours"){
    case Some(txt) => Valid
    case _ => Invalid(Messages("tai.workingHours.error.form.incomes.radioButton.mandatory"))
  }

  def createForm(): Form[HoursWorkedForm] = {
    Form[HoursWorkedForm](
      mapping(
        "workingHours" -> optional(text).verifying(hoursWorkedValidation)
      )(HoursWorkedForm.apply)(HoursWorkedForm.unapply)
    )
  }
}

case class PayPeriodForm(payPeriod: Option[String],
                         otherInDays: Option[Int] = None)

object PayPeriodForm{
  implicit val formats = Json.format[PayPeriodForm]

  def bind(implicit request: Request[_]) = createForm(None).bindFromRequest

//  def create(payPeriodForm: PayPeriodForm) = {
//
//    createForm(None).fill(payPeriodForm)
//  }


  def payPeriodValidation = Constraint[Option[String]]("Please select a period"){

    case Some(txt) => txt match {case "other" | "monthly" | "weekly" | "fortnightly" => Valid
                                 case _ => Invalid(Messages("tai.payPeriod.error.form.incomes.radioButton.mandatory"))
                          }
    case _ => Invalid(Messages("tai.payPeriod.error.form.incomes.radioButton.mandatory"))
  }

  def otherInDaysValidation(payPeriod : Option[String]) : Constraint[Option[Int]] = {
    Constraint[Option[Int]]("days") {
      days => {
        if(payPeriod.getOrElse("") == "other" && !days.isDefined) {
          Invalid(Messages("tai.payPeriod.error.form.incomes.other.mandatory"))
        } else {
          Valid
        }
      }
    }
  }

  def createForm(howOftenError: Option[String], payPeriod : Option[String] = None): Form[PayPeriodForm] = {
    Form[PayPeriodForm](
      mapping(
        "payPeriod" -> optional(text).verifying(payPeriodValidation),
        "otherInDays" -> optional(number).verifying(otherInDaysValidation(payPeriod))
      )(PayPeriodForm.apply)(PayPeriodForm.unapply)
    )
  }
}

case class PayslipForm(totalSalary: Option[String] = None)

object PayslipForm{
  implicit val formats = Json.format[PayslipForm]

  def bind(implicit request: Request[_]) = createForm().bindFromRequest

//  def create(payslipForm: PayslipForm) = {
//    createForm.fill(payslipForm)
//  }

  def createForm(): Form[PayslipForm] = {
    Form[PayslipForm](
      mapping("totalSalary" -> TaiValidator.validateNewAmounts(Messages("tai.payslip.error.form.incomes.radioButton.mandatory"),
                                                               Messages("tai.payslip.error.form.notAnAmount"),
                                                               Messages("error.tai.updateDataEmployment.maxLength")))(PayslipForm.apply)(PayslipForm.unapply)
    )
  }
}



case class PayslipDeductionsForm(payslipDeductions: Option[String])

object PayslipDeductionsForm{
  implicit val formats = Json.format[PayslipDeductionsForm]

  def bind(implicit request: Request[_]) = createForm().bindFromRequest

//  def create(payslipDeductions: PayslipDeductionsForm) = {
//    createForm.fill(payslipDeductions)
//  }

  def payslipDeductionsValidation = Constraint[Option[String]]("Your working hours"){
    case Some(txt) => Valid
    case _ => Invalid(Messages("tai.payslipDeductions.error.form.incomes.radioButton.mandatory"))
  }

  def createForm(): Form[PayslipDeductionsForm] = {
    Form[PayslipDeductionsForm](
      mapping("payslipDeductions" -> optional(text).verifying(payslipDeductionsValidation))
        (PayslipDeductionsForm.apply)(PayslipDeductionsForm.unapply)
    )
  }
}


case class TaxablePayslipForm(taxablePay: Option[String] = None)

object TaxablePayslipForm{
  implicit val formats = Json.format[TaxablePayslipForm]

  def bind(implicit request: Request[_]) = createForm().bindFromRequest

//  def create(taxablePayslipForm: TaxablePayslipForm) = {
//    createForm().fill(taxablePayslipForm)
//  }

  def createForm(netSalary: Option[String] = None): Form[TaxablePayslipForm] = {
    Form[TaxablePayslipForm](
      mapping("taxablePay" -> TaiValidator.validateNewAmounts(
        Messages("tai.taxablePayslip.error.form.incomes.radioButton.mandatory"),
        Messages("tai.taxablePayslip.error.form.invalid"),
        Messages("error.tai.updateDataEmployment.maxLength"),
        netSalary
      ))(TaxablePayslipForm.apply)(TaxablePayslipForm.unapply)
    )
  }
}


case class BonusPaymentsForm(bonusPayments: Option[String], bonusPaymentsMoreThisYear: Option[String])

object BonusPaymentsForm{
  implicit val formats = Json.format[BonusPaymentsForm]

  def bind(implicit request: Request[_]) = createForm().bindFromRequest

  def bonusPaymentsValidation = Constraint[Option[String]]("Does this payslip include bonus or overtime"){

    case Some(txt) => txt match {case "Yes" | "No" => Valid
    case _ => Invalid(Messages("tai.bonusPayments.error.form.incomes.radioButton.mandatory"))
    }
    case _ => Invalid(Messages("tai.bonusPayments.error.form.incomes.radioButton.mandatory"))
  }

  def moreThisYearValidation(bonusPayments : Option[String]) : Constraint[Option[String]] = {
    Constraint[Option[String]]("moreThisYear") {
      moreThisYear => {
        if(bonusPayments.getOrElse("") == "Yes" && !moreThisYear.isDefined) {
          Invalid(Messages("tai.bonusPayments.error.form.likely"))
        } else {
          Valid
        }
      }
    }
  }

//  def create(bonusPayments: BonusPaymentsForm) = {
//    createForm().fill(bonusPayments)
//  }
  def createForm(bonusPayments : Option[String]= None): Form[BonusPaymentsForm] = {
    Form[BonusPaymentsForm](
      mapping("bonusPayments" -> optional(text).verifying(bonusPaymentsValidation),
        "bonusPaymentsMoreThisYear" -> optional(text).verifying(moreThisYearValidation(bonusPayments))
      )
        (BonusPaymentsForm.apply)(BonusPaymentsForm.unapply)
    )
  }
}


case class BonusOvertimeAmountForm(amount: Option[String] = None)

object BonusOvertimeAmountForm{
  implicit val formats = Json.format[BonusOvertimeAmountForm]

  def bind(period:Option[String])(implicit request: Request[_]) = createForm(nonEmptyMessage=None, notAnAmountMessage=None).bindFromRequest

//  def create(bonusOvertimeAmountForm: BonusOvertimeAmountForm, nonEmptyMessage:Option[String]=None, notAnAmountMessage:Option[String]=None) = {
//    createForm(nonEmptyMessage=nonEmptyMessage, notAnAmountMessage=notAnAmountMessage).fill(bonusOvertimeAmountForm)
//  }

  def createForm(nonEmptyMessage: Option[String]=None, notAnAmountMessage: Option[String]=None): Form[BonusOvertimeAmountForm] = {
    Form[BonusOvertimeAmountForm](
      mapping("amount" -> TaiValidator.validateNewAmounts(nonEmptyMessage.getOrElse(""),
                                                          notAnAmountMessage.getOrElse(""),
                                                          Messages("error.tai.updateDataEmployment.maxLength")))(
                                                          BonusOvertimeAmountForm.apply)(BonusOvertimeAmountForm.unapply)
    )
  }

  def bonusPaymentsAmountErrorMessage(moreThisYear: Option[String], payPeriod: Option[String]) = {
    moreThisYear match {
      case Some("Yes") => "tai.bonusPaymentsAmount.year.error"
      case _ =>
        payPeriod match {
          case Some("monthly") => "tai.bonusPaymentsAmount.month.error"
          case Some("fortnightly") => "tai.bonusPaymentsAmount.fortnightly.error"
          case Some("weekly") => "tai.bonusPaymentsAmount.week.error"
          case _ => "tai.bonusPaymentsAmount.period.error"
        }
    }
  }

  def notAmountMessage(moreThisYear: Option[String]) = {
    moreThisYear match {
      case Some("Yes") => "tai.bonusPaymentsAmount.error.form.notAnAmountAnnual"
      case _ => "tai.bonusPaymentsAmount.error.form.notAnAmount"
    }
  }
}
