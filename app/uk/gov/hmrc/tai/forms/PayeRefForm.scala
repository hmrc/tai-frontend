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
import play.api.data.Forms.*
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

object PayeRefForm {

  def form(companyName: String)(implicit messages: Messages): Form[String] = Form(
    single(
      "payeReference" -> text.verifying(payeRefCheck(companyName))
    )
  )

  def payeRefCheck(companyName: String)(implicit messages: Messages): Constraint[String] =
    Constraint("constraints.payeRefCheck") { value =>
      val regex = """\d{3}/[A-Za-z0-9]{1,10}""".r
      value.trim match {
        case str if str.isEmpty        => Invalid(messages("tai.payeRefForm.required", companyName))
        case str if regex.matches(str) => Valid
        case _                         => Invalid(messages("tai.payeRefForm.format", companyName))
      }
    }
}
