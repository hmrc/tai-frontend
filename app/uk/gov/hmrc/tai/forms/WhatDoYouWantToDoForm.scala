/*
 * Copyright 2025 HM Revenue & Customs
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

case class WhatDoYouWantToDoFormData(whatDoYouWantToDo: Option[String])

object WhatDoYouWantToDoForm {
  def whatDoYouWantToDoValidation(implicit messages: Messages): Constraint[Option[String]] =
    Constraint[Option[String]]("Choose an option") {
      case Some(_) => Valid
      case _       => Invalid(Messages("tai.whatDoYouWantToDo.error.selectOption"))
    }

  def createForm(implicit messages: Messages): Form[WhatDoYouWantToDoFormData] =
    Form[WhatDoYouWantToDoFormData](
      mapping(
        "taxYears" -> optional(text).verifying(whatDoYouWantToDoValidation)
      )(WhatDoYouWantToDoFormData.apply)(form => Some(form.whatDoYouWantToDo))
    )
}
