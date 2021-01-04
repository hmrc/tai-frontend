/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.forms.constaints

import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

object TelephoneNumberConstraint {

  val telephoneRegex = """^(\(?\+?[0-9]*\)?)?[0-9_\- \(\)]*$""".r

  def telephoneNumberSizeConstraint(implicit messages: Messages): Constraint[String] =
    Constraint[String]((textContent: String) =>
      textContent match {
        case txt if txt.length < 8 || txt.length > 30 || !telephoneRegex.findAllMatchIn(txt).exists(_ => true) => {
          Invalid(Messages("tai.canWeContactByPhone.telephone.invalid"))
        }
        case _ => Valid
    })

}
