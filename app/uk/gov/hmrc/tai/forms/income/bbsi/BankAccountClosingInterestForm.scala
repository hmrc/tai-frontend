/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.tai.forms.income.bbsi

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import uk.gov.hmrc.play.mappers.StopOnFirstFail
import uk.gov.hmrc.tai.util.FormHelper.isValidCurrency
import uk.gov.voa.play.form.ConditionalMappings._
import uk.gov.hmrc.tai.util.constants.{BankAccountClosingInterestConstants, FormValuesConstants}


case class BankAccountClosingInterestForm(closingBankAccountInterestChoice: Option[String], closingInterestEntry: Option[String])

object BankAccountClosingInterestForm extends BankAccountClosingInterestConstants with FormValuesConstants {

  private def yesNoChoiceValidation(implicit messages: Messages) = Constraint[Option[String]]("") {
    case Some(txt) if txt == YesValue || txt == NoValue => Valid
    case _ => Invalid(Messages("tai.closeBankAccount.closingInterest.error.selectOption"))
  }

  def form(implicit messages: Messages) = Form[BankAccountClosingInterestForm](
    mapping(
      ClosingInterestChoice -> optional(text).verifying(yesNoChoiceValidation),
      ClosingInterestEntry -> mandatoryIfEqual(ClosingInterestChoice,
        YesValue,
        text.verifying(StopOnFirstFail(
            nonEmptyText(Messages("tai.closeBankAccount.closingInterest.error.blank")),
            isNumber(Messages("tai.bbsi.update.form.interest.isCurrency")),
            validateWholeNumber(Messages("tai.bbsi.update.form.interest.wholeNumber"))
        ))
      )
    )(BankAccountClosingInterestForm.apply)(BankAccountClosingInterestForm.unapply)
  )


  def nonEmptyText(requiredErrMsg : String): Constraint[String] = {
    Constraint[String]("required") {
      case textValue:String if notBlank(textValue) => Valid
      case _ => Invalid(requiredErrMsg)
    }
  }

  def isNumber(currencyErrorMsg : String): Constraint[String] = {
    Constraint[String]("invalidCurrency") {
      case textValue if isValidCurrency(Some(textValue)) => Valid
      case _ => Invalid(currencyErrorMsg)
    }
  }

  def validateWholeNumber(currencyErrorMsg : String): Constraint[String] = {
    Constraint[String]("invalidCurrency") {
      case textValue if isValidCurrency(Some(textValue), isWholeNumRequired = true) => Valid
      case _ => Invalid(currencyErrorMsg)
    }
  }

  def notBlank(value: String): Boolean = !value.trim.isEmpty

}
