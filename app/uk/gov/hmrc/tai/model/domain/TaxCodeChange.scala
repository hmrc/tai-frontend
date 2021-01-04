/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json.Json
import uk.gov.hmrc.tai.util.DateHelper

case class TaxCodeChange(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]) {

  require(current.length > 0, "No current records for Tax Code Change. Current date cannot be determined.")

  val currentPensionCount: Int = current.count(_.pensionIndicator == true)
  val currentEmploymentCount: Int = current.count(_.pensionIndicator == false)
  val mostRecentTaxCodeChangeDate: LocalDate = DateHelper.mostRecentDate(current.map(_.startDate))
  val mostRecentPreviousTaxCodeChangeDate: LocalDate = DateHelper.mostRecentDate(previous.map(_.startDate))

  lazy val uniqueTaxCodes: Seq[String] = (previous ++ current).map(_.taxCode).distinct
}

object TaxCodeChange {
  implicit val format = Json.format[TaxCodeChange]
}
