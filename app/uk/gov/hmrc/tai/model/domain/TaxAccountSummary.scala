/*
 * Copyright 2026 HM Revenue & Customs
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

import java.time.LocalDate

case class TaxAccountSummary(
  totalEstimatedTax: BigDecimal,
  taxFreeAmount: BigDecimal,
  totalInYearAdjustmentIntoCY: BigDecimal,
  totalInYearAdjustment: BigDecimal,
  totalInYearAdjustmentIntoCYPlusOne: BigDecimal,
  totalEstimatedIncome: BigDecimal = 0,
  taxFreeAllowance: BigDecimal = 0,
  date: Option[LocalDate] = None
)

object TaxAccountSummary {

  implicit val reads: Reads[TaxAccountSummary] = (
    (JsPath \ "totalEstimatedTax").readNullable[BigDecimal].map(_.getOrElse(BigDecimal(0))) and
      (JsPath \ "taxFreeAmount").readNullable[BigDecimal].map(_.getOrElse(BigDecimal(0))) and
      (JsPath \ "totalInYearAdjustmentIntoCY").read[BigDecimal] and
      (JsPath \ "totalInYearAdjustment").read[BigDecimal] and
      (JsPath \ "totalInYearAdjustmentIntoCYPlusOne").read[BigDecimal] and
      (JsPath \ "totalEstimatedIncome").readNullable[BigDecimal].map(_.getOrElse(BigDecimal(0))) and
      (JsPath \ "taxFreeAllowance").readNullable[BigDecimal].map(_.getOrElse(BigDecimal(0))) and
      (JsPath \ "date").readNullable[LocalDate]
  )(TaxAccountSummary.apply _)

  implicit val writes: Writes[TaxAccountSummary] = Json.writes[TaxAccountSummary]
}
