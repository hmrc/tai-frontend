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
import play.api.libs.json.{JsPath, Reads}

case class TaxCodeHistory(nino: String, taxCodeRecords: Seq[TaxCodeRecord]){
  import TaxCodeHistory._
  def latestP2Date: String = getLatestP2Date(taxCodeRecords)
}

object TaxCodeHistory {

  implicit val reads: Reads[TaxCodeHistory] = (
    (JsPath \ "nino").read[String] and
      (JsPath \ "taxCodeRecord").read[Seq[TaxCodeRecord]](minLength[Seq[TaxCodeRecord]](1))
    )(TaxCodeHistory.apply _)

  implicit def dateTimeOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  private def getLatestP2Date(taxCodeRecords: Seq[TaxCodeRecord]): String =
    taxCodeRecords.map(_.p2Date).max
}


