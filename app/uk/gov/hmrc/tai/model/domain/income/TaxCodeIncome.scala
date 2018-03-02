/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json._
import uk.gov.hmrc.tai.model.domain.TaxComponentType
import uk.gov.hmrc.tai.util.TaiConstants

sealed trait BasisOperation
case object Week1Month1BasisOperation extends BasisOperation
case object OtherBasisOperation extends BasisOperation

object BasisOperation{
  implicit val formatBasisOperation = new Format[BasisOperation] {
    override def reads(json: JsValue): JsSuccess[BasisOperation] = json.as[String] match {
      case "Week1Month1BasisOperation" => JsSuccess(Week1Month1BasisOperation)
      case "OtherBasisOperation" => JsSuccess(OtherBasisOperation)
      case _ => throw new IllegalArgumentException("Invalid adjustment type")
    }

    override def writes(adjustmentType: BasisOperation) = JsString(adjustmentType.toString)
  }
}

sealed trait TaxCodeIncomeSourceStatus
case object Live extends TaxCodeIncomeSourceStatus
case object PotentiallyCeased extends TaxCodeIncomeSourceStatus
case object Ceased extends TaxCodeIncomeSourceStatus

object TaxCodeIncomeSourceStatus{
  implicit val formatTaxCodeIncomeSourceStatus = new Format[TaxCodeIncomeSourceStatus] {
    override def reads(json: JsValue): JsSuccess[TaxCodeIncomeSourceStatus] = ???
    override def writes(taxCodeIncomeSourceStatus: TaxCodeIncomeSourceStatus) = JsString(taxCodeIncomeSourceStatus.toString)
  }
}

case class TaxCodeIncome(componentType:TaxComponentType, employmentId:Option[Int], amount:BigDecimal, description:String, taxCode:String,
                         name: String, basisOperation: BasisOperation, status: TaxCodeIncomeSourceStatus){
  lazy val taxCodeWithEmergencySuffix: String = basisOperation match {
    case Week1Month1BasisOperation => taxCode + TaiConstants.EmergencyTaxCodeSuffix
    case _ => taxCode
  }
}

object TaxCodeIncome {
  implicit val format: Format[TaxCodeIncome] = Json.format[TaxCodeIncome]
}