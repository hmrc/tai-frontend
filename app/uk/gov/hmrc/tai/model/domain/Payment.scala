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

import play.api.libs.json.{Format, Json}

import java.time.LocalDate

//import uk.gov.hmrc.tai.util.DateHelper.dateTimeOrdering

case class Payment(
  date: LocalDate,
  amountYearToDate: BigDecimal,
  taxAmountYearToDate: BigDecimal,
  nationalInsuranceAmountYearToDate: BigDecimal,
  amount: BigDecimal,
  taxAmount: BigDecimal,
  nationalInsuranceAmount: BigDecimal,
  payFrequency: PaymentFrequency,
  duplicate: Option[Boolean] = None
)

object Payment {

  def toDate(date: LocalDate): java.util.Date = java.sql.Date.valueOf(date)

//  private implicit val ord: Ordering[LocalDate] = (x: LocalDate, y: LocalDate) => x.compareTo(y)

  private implicit val asdf: Ordering[LocalDate] = (x: LocalDate, y: LocalDate) => x.compareTo(y)

  implicit val dateOrdering: Ordering[Payment] = Ordering.by(_.date)
  implicit val paymentFormat: Format[Payment]  = Json.format[Payment]
}
