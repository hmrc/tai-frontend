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

package uk.gov.hmrc.tai.model.domain

import play.api.libs.json._

sealed trait PaymentFrequency

case object Weekly extends PaymentFrequency

case object FortNightly extends PaymentFrequency

case object FourWeekly extends PaymentFrequency

case object Monthly extends PaymentFrequency

case object Quarterly extends PaymentFrequency

case object BiAnnually extends PaymentFrequency

case object Annually extends PaymentFrequency

case object OneOff extends PaymentFrequency

case object Irregular extends PaymentFrequency

object PaymentFrequency {
  implicit val paymentFrequencyFormat = new Format[PaymentFrequency] {
    override def reads(json: JsValue): JsResult[PaymentFrequency] = json.as[String] match {
      case "Weekly"      => JsSuccess(Weekly)
      case "FortNightly" => JsSuccess(FortNightly)
      case "FourWeekly"  => JsSuccess(FourWeekly)
      case "Monthly"     => JsSuccess(Monthly)
      case "Quarterly"   => JsSuccess(Quarterly)
      case "BiAnnually"  => JsSuccess(BiAnnually)
      case "Annually"    => JsSuccess(Annually)
      case "OneOff"      => JsSuccess(OneOff)
      case "Irregular"   => JsSuccess(Irregular)
      case _             => throw new IllegalArgumentException("Invalid payment frequency")
    }

    override def writes(paymentFrequency: PaymentFrequency): JsValue = JsString(paymentFrequency.toString)
  }
}
