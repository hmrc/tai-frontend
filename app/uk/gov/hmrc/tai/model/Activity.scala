/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.TaxIds

// This is what we send to activity-logger
case class Activity(
  applicationName: String,
  eventTime: DateTime,
  eventType: String,
  eventDescriptionId: String,
  principalTaxIds: TaxIds)

object Activity {
  implicit val taxIdsFormat = TaxIds.format(TaxIds.defaultSerialisableIds: _*)
  implicit val formats = Json.format[Activity]
}

// This is what the activity-logger returns
case class LogActivityEntry(
  applicationName: String,
  eventTime: DateTime,
  eventType: String,
  eventDescription: String,
  principal: PersonDetails,
  attorney: PersonDetails)

object LogActivityEntry {
  implicit val formats = Json.format[LogActivityEntry]
}

case class LogActivityResponse(
  pageNumber: Int,
  pageSize: Int,
  totalNumberOfRecords: Int,
  activityList: Seq[LogActivityEntry])

object LogActivityResponse {
  implicit val formats = Json.format[LogActivityResponse]
}

case class PersonDetails(taxIds: TaxIds, name: String)

object PersonDetails {
  implicit val taxIdsFormat = TaxIds.format(TaxIds.defaultSerialisableIds: _*)
  implicit val formats: Format[PersonDetails] = Json.format[PersonDetails]
}
