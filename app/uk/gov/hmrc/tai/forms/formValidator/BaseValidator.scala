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

package uk.gov.hmrc.tai.forms.formValidator

import play.api.Play.current
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation._
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.util.FormHelper

import scala.util.{Success, Try}


trait BaseValidator extends DateValidator {
  protected val MIN_LENGTH: Int = 0
  protected val MAX_LENGTH: Int = 100

  def nonEmptyText(requiredErrMsg : String): Constraint[Option[String]] = {
    Constraint[Option[String]]("required") {
      case Some(textValue:String) if notBlank(textValue) => Valid
      case _ => Invalid(requiredErrMsg)
    }
  }

  def validateCurrency(currencyErrorMsg : String): Constraint[Option[String]] = {
    Constraint[Option[String]]("invalidCurrency") {
      case textValue if FormHelper.isValidCurrency(textValue) => Valid
      case _ => Invalid(currencyErrorMsg)
    }
  }

  def validateCurrencyWhole(currencyErrorMsg : String): Constraint[Option[String]] = {
    Constraint[Option[String]]("invalidCurrency") {
      case textValue if FormHelper.isValidCurrency(textValue, isWholeNumRequired = true) => Valid
      case _ => Invalid(currencyErrorMsg)
    }
  }

  def validateCurrencyLength(maxLength: Int, currencyErrorMsg : String): Constraint[Option[String]] = {
    Constraint[Option[String]]("invalidCurrency") {
      case textValue if isValidMaxLength(maxLength)(FormHelper.stripNumber(textValue)) => Valid
      case _ => Invalid(currencyErrorMsg)
    }
  }

  def validateInputAmountComparisonWithTaxablePay(taxablePayYTD : BigDecimal, validateTaxablePayYTDError: String): Constraint[Option[String]] = {
    Constraint[Option[String]]("invalidAmount") {
      case Some(textValue) => Try(BigDecimal(textValue.replace(",",""))) match {
        case Success(value) if value >= taxablePayYTD => Valid
        case _ => Invalid(validateTaxablePayYTDError, "validateInputAmount")
      }
      case _ => Invalid(validateTaxablePayYTDError, "validateInputAmount")
    }
  }

  def notBlank(value: String): Boolean = !value.trim.isEmpty

  def isValidMaxLength(maxLength: Int)(value: Option[String]): Boolean = value.getOrElse("").length <= maxLength

  def validateNewAmounts(nonEmptyError: String, validateCurrencyError: String, validateCurrencyLengthError: String,
                         netSalary: Option[String] = None)(implicit messages: Messages) : Mapping[Option[String]] = {
    val INCOME_MAX_LENGTH = 9

    optional(text).verifying(StopOnFirstFail(
      nonEmptyText(nonEmptyError),
      validateCurrency(validateCurrencyError),
      validateCurrencyLength(INCOME_MAX_LENGTH, validateCurrencyLengthError),
      validateNetGrossSalary(netSalary)
    ))
  }

  def validateNetGrossSalary( netSalary: Option[String] = None)(implicit messages: Messages):  Constraint[Option[String]] = {
      val netSalaryValue = BigDecimal(netSalary.getOrElse("0"))

      val displayNetSalary = MoneyPounds(netSalaryValue, 0, roundUp = true).quantity

      Constraint[Option[String]]("taxablePay") {
        case taxablePay if BigDecimal(FormHelper.stripNumber(taxablePay).getOrElse("0")) <= netSalaryValue => Valid
        case _ if netSalary.isDefined =>
          Invalid(messages("tai.taxablePayslip.error.form.payTooHigh", displayNetSalary))
        case _ => Valid
      }
  }

  def validateTaxAmounts(nonEmptyError: String, validateCurrencyError: String, validateCurrencyLengthError: String,
                         validateTaxablePayYTDError : String, taxablePayYTD: BigDecimal) : Mapping[Option[String]] = {
    val INCOME_MAX_LENGTH = 9
    optional(text).verifying(StopOnFirstFail(
      nonEmptyText(nonEmptyError),
      validateCurrencyWhole(validateCurrencyError),
      validateCurrencyLength(INCOME_MAX_LENGTH, validateCurrencyLengthError),
      validateInputAmountComparisonWithTaxablePay(taxablePayYTD, validateTaxablePayYTDError)
    ))
  }
}

object BaseValidator extends BaseValidator

object StopOnFirstFail {
  def apply[T](constraints: Constraint[T]*): Constraint[T] = Constraint {
    field: T =>
      constraints.toList dropWhile (_(field) == Valid) match {
        case Nil => Valid
        case constraint :: _ => constraint(field)
      }
  }

  def constraint[T](message: String, validator: (T) => Boolean): Constraint[T] = {
    Constraint((data: T) => if (validator(data)) Valid else Invalid(Seq(ValidationError(message))))
  }
}
