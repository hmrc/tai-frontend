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

package uk.gov.hmrc.tai.model.domain

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Json, Reads}

case class TaxCodeChange(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]){

  case class TaxCodePair(previous: Option[TaxCodeRecord], current: Option[TaxCodeRecord])

  implicit val dateTimeOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isAfter _)

  val mostRecentTaxCodeChangeDate: LocalDate = current.map(_.startDate).min

  val taxCodePairs: Seq[TaxCodePair] = {
    for {
      p <- previous
      c <- current
      if p.employmentId == c.employmentId
    } yield TaxCodePair(Some(p), Some(c))
  }
  
  val primaryPairs: Seq[TaxCodePair] = {
    taxCodePairs.filter(taxCodeRecordPair => taxCodeRecordPair.current.exists(_.primary))
  }

  val secondaryPairs: Seq[TaxCodePair] = {
    taxCodePairs.filterNot(taxCodeRecordPair => taxCodeRecordPair.current.exists(_.primary))
  }

  val unpairedCurrentCodes: Seq[TaxCodePair] = {
    val unpairedRecords = current.filterNot(record => taxCodePairs.map(_.current).contains(Some(record)))

    unpairedRecords.map(record => TaxCodePair(None, Some(record)))
  }

  val unpairedPreviousCodes: Seq[TaxCodePair] = {
    val unpairedRecords = previous.filterNot(record => taxCodePairs.map(_.previous).contains(Some(record)))

    unpairedRecords.map(record => TaxCodePair(Some(record), None))
  }
}

object TaxCodeChange {
  implicit val format = Json.format[TaxCodeChange]
}