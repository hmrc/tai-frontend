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

package uk.gov.hmrc.tai.model.domain

import play.api.libs.json.{Format, JsString, JsSuccess, JsValue}

sealed trait IabdUpdateSource

case object ManualTelephone extends IabdUpdateSource

case object Letter extends IabdUpdateSource

case object Email extends IabdUpdateSource

case object AgentContact extends IabdUpdateSource

case object OtherForm extends IabdUpdateSource

case object Internet extends IabdUpdateSource

case object InformationLetter extends IabdUpdateSource

object IabdUpdateSource extends IabdUpdateSource {
  implicit val formatIabdUpdateSource: Format[IabdUpdateSource] = new Format[IabdUpdateSource] {
    override def reads(json: JsValue): JsSuccess[IabdUpdateSource] = json.as[String] match {
      case "ManualTelephone"   => JsSuccess(ManualTelephone)
      case "Letter"            => JsSuccess(Letter)
      case "Email"             => JsSuccess(Email)
      case "AgentContact"      => JsSuccess(AgentContact)
      case "OtherForm"         => JsSuccess(OtherForm)
      case "Internet"          => JsSuccess(Internet)
      case "InformationLetter" => JsSuccess(InformationLetter)
      case _                   => throw new RuntimeException("Invalid Iabd Update Source")
    }

    override def writes(iabdUpdateSource: IabdUpdateSource) = JsString(iabdUpdateSource.toString)
  }
}
