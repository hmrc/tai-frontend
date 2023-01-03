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

package uk.gov.hmrc.tai.forms.income.incomeCalculator

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}

case class HowToUpdateForm(howToUpdate: Option[String])

object HowToUpdateForm {
  implicit val formats: OFormat[HowToUpdateForm] = Json.format[HowToUpdateForm]

  def createForm()(implicit messages: Messages): Form[HowToUpdateForm] = {

    val howToUpdateValidation = Constraint[Option[String]]("Choose how to update") {
      case Some(txt) => Valid
      case _         => Invalid(messages("tai.howToUpdate.error.form.incomes.radioButton.mandatory"))
    }

    Form[HowToUpdateForm](
      mapping(
        "howToUpdate" -> optional(text).verifying(howToUpdateValidation)
      )(HowToUpdateForm.apply)(HowToUpdateForm.unapply)
    )
  }
}
