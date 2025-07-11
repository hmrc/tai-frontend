/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tai.util.DateHelper

import java.time.LocalDate

case class TaxCodeChange(previous: List[TaxCodeRecord], current: List[TaxCodeRecord]) {

  require(current.nonEmpty, "No current records for Tax Code Change. Current date cannot be determined.")

  val currentPensionCount: Int                       = current.count(_.pensionIndicator)
  val currentEmploymentCount: Int                    = current.count(!_.pensionIndicator)
  val mostRecentTaxCodeChangeDate: LocalDate         = DateHelper.mostRecentDate(current.map(_.startDate))
  val mostRecentPreviousTaxCodeChangeDate: LocalDate =
    DateHelper.mostRecentDate(previous.map(_.startDate))

  lazy val uniqueTaxCodes: Seq[String] = (previous ++ current).map(_.taxCode).distinct
}

object TaxCodeChange {
  implicit val format: OFormat[TaxCodeChange] = Json.format[TaxCodeChange]
}
