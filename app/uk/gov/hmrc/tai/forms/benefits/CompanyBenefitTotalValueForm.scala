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

package uk.gov.hmrc.tai.forms.benefits

import play.api.data.Form
import play.api.data.Forms.{single, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import uk.gov.hmrc.play.mappers.StopOnFirstFail
import uk.gov.hmrc.tai.util.FormHelper.isValidCurrency

object CompanyBenefitTotalValueForm {

  def form(implicit messages: Messages): Form[String] = Form(
    single(
      "totalValue" ->
        text.verifying(StopOnFirstFail(
          nonEmptyText(Messages("tai.bbsi.update.form.interest.blank")),
          isNumber(Messages("tai.bbsi.update.form.interest.isCurrency"))
          ))
  ))


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

  def notBlank(value: String): Boolean = !value.trim.isEmpty

}
