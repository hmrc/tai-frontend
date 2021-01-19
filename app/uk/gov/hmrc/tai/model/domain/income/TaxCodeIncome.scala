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

package uk.gov.hmrc.tai.model.domain.income

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.tai.model.domain.TaxComponentType

sealed trait BasisOfOperation

case object Week1Month1BasisOfOperation extends BasisOfOperation

case object OtherBasisOfOperation extends BasisOfOperation

object BasisOfOperation {
  implicit val formatBasisOperation = new Format[BasisOfOperation] {
    override def reads(json: JsValue): JsSuccess[BasisOfOperation] = json.as[String] match {
      case "Week1Month1BasisOperation" => JsSuccess(Week1Month1BasisOfOperation)
      case "Week1/Month1"              => JsSuccess(Week1Month1BasisOfOperation)
      case "OtherBasisOperation"       => JsSuccess(OtherBasisOfOperation)
      case "Cumulative"                => JsSuccess(OtherBasisOfOperation)
      case _                           => throw new IllegalArgumentException("Invalid basis of operation")
    }

    override def writes(adjustmentType: BasisOfOperation) = JsString(adjustmentType.toString)
  }
}

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

case class TaxCodeIncome(
  componentType: TaxComponentType,
  employmentId: Option[Int],
  amount: BigDecimal,
  description: String,
  taxCode: String,
  name: String,
  basisOperation: BasisOfOperation,
  // This is not the live employment status but the batched job employment status
  status: TaxCodeIncomeSourceStatus,
  iabdUpdateSource: Option[IabdUpdateSource] = None,
  updateNotificationDate: Option[LocalDate] = None,
  updateActionDate: Option[LocalDate] = None)

object TaxCodeIncome {
  implicit val format: Format[TaxCodeIncome] = Json.format[TaxCodeIncome]
}
