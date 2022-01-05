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

import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome, PensionIncome}
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome}
import play.api.i18n.Messages

case class EmploymentAmount(
  name: String,
  description: String,
  employmentId: Int,
  newAmount: Int,
  oldAmount: Int,
  worksNumber: Option[String] = None,
  jobTitle: Option[String] = None,
  startDate: Option[LocalDate] = None,
  endDate: Option[LocalDate] = None,
  isLive: Boolean = true,
  isOccupationalPension: Boolean = false)

object EmploymentAmount {
  implicit val formats = Json.format[EmploymentAmount]
  def apply(taxCodeIncome: TaxCodeIncome, employment: Employment)(implicit messages: Messages): EmploymentAmount = {
    val description = taxCodeIncome.componentType match {
      case EmploymentIncome if employment.employmentStatus == Live =>
        s"${Messages("tai.incomes.status-1")} ${Messages(s"tai.incomes.type-0")}"
      case EmploymentIncome => s"${Messages("tai.incomes.status-2")} ${Messages(s"tai.incomes.type-0")}"
      case _                => Messages(s"tai.incomes.type-1")
    }

    EmploymentAmount(
      taxCodeIncome.name,
      description,
      taxCodeIncome.employmentId.getOrElse(0),
      taxCodeIncome.amount.toInt,
      taxCodeIncome.amount.toInt,
      None,
      None,
      Some(employment.startDate),
      employment.endDate,
      employment.employmentStatus == Live,
      taxCodeIncome.componentType == PensionIncome
    )
  }
}
