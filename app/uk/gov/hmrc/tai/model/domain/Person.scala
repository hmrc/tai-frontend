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

package uk.gov.hmrc.tai.model.domain

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino

case class Person(
  nino: Nino,
  firstName: String,
  surname: String,
  isDeceased: Boolean,
  address: Address
) {
  lazy val name: String = s"$firstName $surname"
}

object PersonFormatter {

  implicit val addressFormat: OFormat[Address] = Json.format[Address]

  implicit val personReads: Reads[Person] = (
    (JsPath \ "person" \ "nino").read[Nino] and
      ((JsPath \ "person" \ "firstName").read[String] or Reads.pure("")) and
      ((JsPath \ "person" \ "lastName").read[String] or Reads.pure("")) and
      ((JsPath \ "person" \ "deceased").read[Boolean] or Reads.pure(false)) and
      ((JsPath \ "address").read[Address] or Reads.pure(Address.emptyAddress))
  )(Person.apply _)

}
