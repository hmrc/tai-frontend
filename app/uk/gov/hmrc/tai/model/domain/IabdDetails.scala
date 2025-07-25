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

import play.api.libs.json.Reads.localDateReads
import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.domain.Nino
import play.api.libs.functional.syntax.toFunctionalBuilderOps

import java.time.LocalDate

case class IabdDetails(
  employmentSequenceNumber: Option[Int],
  source: Option[IabdUpdateSource], // iabdUpdateSource source.flatMap(code => IabdUpdateSource.fromCode(code)
  `type`: Option[Int],
  receiptDate: Option[LocalDate], // updateNotificationDate
  captureDate: Option[LocalDate], // updateActionDate
  grossAmount: Option[BigDecimal] = None
)

object IabdDetails {
  private val dateReads: Reads[LocalDate] = localDateReads("yyyy-MM-dd")

  implicit val readsIabds: Reads[IabdDetails] = (
    (JsPath \ "employmentSequenceNumber").readNullable[Int] and
      (JsPath \ "source").readNullable[IabdUpdateSource] and
      (JsPath \ "type").readNullable[Int] and
      (JsPath \ "receiptDate").readNullable[LocalDate](dateReads) and
      (JsPath \ "captureDate").readNullable[LocalDate](dateReads) and
      (JsPath \ "grossAmount").readNullable[BigDecimal]
  )(IabdDetails.apply _)
}
