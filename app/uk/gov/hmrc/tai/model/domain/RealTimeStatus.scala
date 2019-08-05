/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{Format, JsString, JsSuccess, JsValue}

sealed trait RealTimeStatus

case object Available extends RealTimeStatus

case object TemporarilyUnavailable extends RealTimeStatus

case object Unavailable extends RealTimeStatus

object RealTimeStatus extends RealTimeStatus {

  implicit val realTimeStatusFormat = new Format[RealTimeStatus] {
    override def reads(json: JsValue): JsSuccess[RealTimeStatus] = json.as[String] match {
      case "Available"              => JsSuccess(Available)
      case "TemporarilyUnavailable" => JsSuccess(TemporarilyUnavailable)
      case "Unavailable"            => JsSuccess(Unavailable)
      case _                        => throw new IllegalArgumentException("Invalid real time status value")
    }

    override def writes(realTimeStatus: RealTimeStatus) = JsString(realTimeStatus.toString)
  }
}
