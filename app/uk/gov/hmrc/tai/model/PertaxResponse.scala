/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.tai.model

import play.api.libs.json.{Json, OFormat}

case class PertaxResponse(
                           code: String,
                           message: String,
                           errorView: Option[ErrorView] = None,
                           redirect: Option[String] = None
                         )

object PertaxResponse {
  implicit val formats: OFormat[PertaxResponse] = Json.format[PertaxResponse]
}
