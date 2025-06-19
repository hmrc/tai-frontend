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

import play.api.libs.json.{Format, Json}

case class Address(
  line1: Option[String],
  line2: Option[String],
  line3: Option[String],
  postcode: Option[String],
  country: Option[String]
)

object Address {
  implicit val formats: Format[Address] = Json.format[Address]
  val emptyAddress: Address             = Address(None, None, None, None, None)

  def apply(
    line1: String,
    line2: String,
    line3: String,
    postcode: String,
    country: String
  ): Address =
    Address(
      line1 = Some(line1),
      line2 = Some(line2),
      line3 = Some(line3),
      postcode = Some(postcode),
      country = Some(country)
    )
}
