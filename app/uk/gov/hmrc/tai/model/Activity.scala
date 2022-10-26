/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDateTime
import play.api.libs.json.{Format, Json}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.domain.TaxIds
import play.api.libs.json.OFormat

// This is what we send to activity-logger
case class Activity(
  applicationName: String,
  eventTime: LocalDateTime,
  eventType: String,
  eventDescriptionId: String,
  principalTaxIds: TaxIds)

object Activity {
  implicit val taxIdsFormat: Format[TaxIds] = TaxIds.format(TaxIds.defaultSerialisableIds: _*)
  implicit val formats: OFormat[Activity] = Json.format[Activity]
}

// This is what the activity-logger returns
case class LogActivityEntry(
  applicationName: String,
  eventTime: LocalDateTime,
  eventType: String,
  eventDescription: String,
  principal: PersonDetails,
  attorney: PersonDetails)

object LogActivityEntry {
  implicit val formats: OFormat[LogActivityEntry] = Json.format[LogActivityEntry]
}

case class LogActivityResponse(
  pageNumber: Int,
  pageSize: Int,
  totalNumberOfRecords: Int,
  activityList: Seq[LogActivityEntry])

object LogActivityResponse {
  implicit val formats: OFormat[LogActivityResponse] = Json.format[LogActivityResponse]
}

case class PersonDetails(taxIds: TaxIds, name: String)

object PersonDetails {
  implicit val taxIdsFormat: Format[TaxIds] = TaxIds.format(TaxIds.defaultSerialisableIds: _*)
  implicit val formats: Format[PersonDetails] = Json.format[PersonDetails]
}
