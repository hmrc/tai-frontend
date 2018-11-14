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
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants

case class YesNoForm(yesNoChoice: Option[String])

object YesNoForm extends FormValuesConstants {

  private def yesNoChoiceValidation(emptySelectionMsg: String) = Constraint[Option[String]]("") {
    case Some(txt) if txt == YesValue || txt == NoValue => Valid
    case _ => Invalid(emptySelectionMsg)
  }

  def form(emptySelectionMsg: String = "") = Form[YesNoForm](
    mapping(
      YesNoChoice -> optional(text).verifying(yesNoChoiceValidation(emptySelectionMsg))
    )(YesNoForm.apply)(YesNoForm.unapply)
  )

}

