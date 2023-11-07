/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.tai.model

import play.api.libs.json.{Json, OFormat}

case class PertaxRequestDetails(isBackendService: Boolean = false)

object PertaxRequestDetails {
  implicit val formats: OFormat[PertaxRequestDetails] = Json.format[PertaxRequestDetails]
}
