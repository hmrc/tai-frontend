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

package uk.gov.hmrc.tai.model

import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome, TaxCodeIncomeSourceStatus}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}

import java.time.LocalDate

case class EmploymentAmount(
  name: String,
  description: String,
  employmentId: Int,
  oldAmount: Option[Int],
  worksNumber: Option[String] = None,
  jobTitle: Option[String] = None,
  startDate: Option[LocalDate] = None,
  endDate: Option[LocalDate] = None,
  isLive: Boolean = true,
  isOccupationalPension: Boolean = false
)

object EmploymentAmount {
  implicit val formats: OFormat[EmploymentAmount] = Json.format[EmploymentAmount]

  def apply(taxCodeIncome: Option[TaxCodeIncome], employment: Employment)(implicit
    messages: Messages
  ): EmploymentAmount =
    EmploymentAmount(
      name = employment.name,
      description = descriptionFrom(employment.employmentType, employment.employmentStatus),
      employmentId = employment.sequenceNumber,
      oldAmount = taxCodeIncome.map(_.amount.intValue),
      startDate = employment.startDate,
      endDate = employment.endDate,
      isLive = employment.employmentStatus == Live,
      isOccupationalPension = employment.receivingOccupationalPension
    )

  private def descriptionFrom(componentType: Any, employmentStatus: TaxCodeIncomeSourceStatus)(implicit
    messages: Messages
  ): String =
    componentType match {
      case EmploymentIncome if employmentStatus == Live =>
        s"${Messages("tai.incomes.status-1")} ${Messages("tai.incomes.type-0")}"
      case EmploymentIncome =>
        s"${Messages("tai.incomes.status-2")} ${Messages("tai.incomes.type-0")}"
      case _ =>
        Messages("tai.incomes.type-1")
    }
}
