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
