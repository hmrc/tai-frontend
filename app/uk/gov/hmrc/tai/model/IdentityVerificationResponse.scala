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

package uk.gov.hmrc.tai.model

import play.api.libs.json._

trait IdentityVerificationResponse

object IdentityVerificationResponse {
  implicit val writes: Writes[IdentityVerificationResponse] = new Writes[IdentityVerificationResponse] {
    override def writes(o: IdentityVerificationResponse): JsValue = JsString(o.toString)
  }

  implicit val reads: Reads[IdentityVerificationResponse] = new Reads[IdentityVerificationResponse] {
    override def reads(json: JsValue): JsResult[IdentityVerificationResponse] =
      if ((json \ "result").isDefined) {
        (json \ "result").as[String] match {
          case "Incomplete"           => JsSuccess(Incomplete)
          case "InsufficientEvidence" => JsSuccess(InsufficientEvidence)
          case "TechnicalIssue"       => JsSuccess(TechnicalIssue)
          case "PreconditionFailed"   => JsSuccess(PrecondFailed)
          case "InvalidResponse"      => JsSuccess(InvalidResponse)
          case _                      => JsError("Invalid IdentityVerificationResponse")
        }
      } else {
        JsError("Result is missing or incorrect")
      }
  }

  implicit val formats: Format[IdentityVerificationResponse] =
    Format(reads, writes)
}

case object Incomplete extends IdentityVerificationResponse {
  override def toString: String = "Incomplete"
}

case object InsufficientEvidence extends IdentityVerificationResponse {
  override def toString: String = "InsufficientEvidence"
}

case object TechnicalIssue extends IdentityVerificationResponse {
  override def toString: String = "TechnicalIssue"
}

case object PrecondFailed extends IdentityVerificationResponse {
  override def toString: String = "PreconditionFailed"
}

case object InvalidResponse extends IdentityVerificationResponse {
  override def toString: String = "InvalidResponse"
}
