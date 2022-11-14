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

package uk.gov.hmrc.tai.model

import play.api.libs.json.Json
import play.api.libs.json.OFormat

object TaxCalculation {
  implicit val formats: OFormat[TaxCalculation] = Json.format[TaxCalculation]
}

case class TaxCalculation(
  p800_status: String,
  amount: BigDecimal,
  taxYear: Int,
  paymentStatus: Option[String]
)
