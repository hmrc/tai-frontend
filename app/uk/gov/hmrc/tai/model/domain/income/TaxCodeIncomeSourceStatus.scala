/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.domain.income

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

sealed trait TaxCodeIncomeSourceStatus
case object Live extends TaxCodeIncomeSourceStatus
case object NotLive extends TaxCodeIncomeSourceStatus
case object PotentiallyCeased extends TaxCodeIncomeSourceStatus
case object Ceased extends TaxCodeIncomeSourceStatus

object TaxCodeIncomeSourceStatus {
  implicit val formatTaxCodeIncomeSourceStatus: Format[TaxCodeIncomeSourceStatus] =
    new Format[TaxCodeIncomeSourceStatus] {
      override def reads(json: JsValue): JsResult[TaxCodeIncomeSourceStatus] = json.as[String] match {
        case "Live"              => JsSuccess(Live)
        case "NotLive"           => JsSuccess(NotLive)
        case "PotentiallyCeased" => JsSuccess(PotentiallyCeased)
        case "Ceased"            => JsSuccess(Ceased)
        case _                   => JsError("Invalid Tax component type")
      }

      override def writes(taxCodeIncomeSourceStatus: TaxCodeIncomeSourceStatus) =
        JsString(taxCodeIncomeSourceStatus.toString)
    }
}
