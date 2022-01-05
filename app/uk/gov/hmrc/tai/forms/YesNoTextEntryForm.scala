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

package uk.gov.hmrc.tai.forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.voa.play.form.ConditionalMappings._

case class YesNoTextEntryForm(yesNoChoice: Option[String], yesNoTextEntry: Option[String])

object YesNoTextEntryForm extends FormValuesConstants {

  private def yesNoChoiceValidation(emptySelectionMsg: String) = Constraint[Option[String]]("") {
    case Some(txt) if txt == YesValue || txt == NoValue => Valid
    case _                                              => Invalid(emptySelectionMsg)
  }

  def form(
    emptySelectionMsg: String = "",
    emptyTextFieldMsg: String = "",
    additionalTextConstraint: Option[Constraint[String]] = None) = Form[YesNoTextEntryForm](
    mapping(
      YesNoChoice -> optional(text).verifying(yesNoChoiceValidation(emptySelectionMsg)),
      YesNoTextEntry -> mandatoryIfEqual(
        YesNoChoice,
        YesValue,
        text
          .verifying(
            emptyTextFieldMsg,
            _.nonEmpty
          )
          .verifying(additionalTextConstraint.getOrElse(Constraint[String] { _: String =>
            Valid
          })))
    )(YesNoTextEntryForm.apply)(YesNoTextEntryForm.unapply)
  )

}
