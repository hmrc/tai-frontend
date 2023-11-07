/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.tai.model

import play.api.libs.json.Json

case class ErrorView(url: String, statusCode: Int)

object ErrorView {
  implicit val format = Json.format[ErrorView]
}
