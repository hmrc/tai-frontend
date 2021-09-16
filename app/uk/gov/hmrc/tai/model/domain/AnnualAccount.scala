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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.tai.model.TaxYear

case class AnnualAccount(
  taxYear: TaxYear,
  realTimeStatus: RealTimeStatus,
  payments: Seq[Payment],
  endOfTaxYearUpdates: Seq[EndOfTaxYearUpdate]) {

  lazy val totalIncomeYearToDate: BigDecimal = maxPayment(payments.max.amountYearToDate)

  lazy val latestPayment: Option[Payment] = if (payments.isEmpty) None else Some(payments.max)

  lazy val isIrregularPayment: Boolean = latestPayment.exists(latestPayment => {
    latestPayment.payFrequency == Irregular || latestPayment.payFrequency == Annually ||
    latestPayment.payFrequency == BiAnnually
  })

  private def maxPayment(maximumPayment: => BigDecimal): BigDecimal =
    if (payments.isEmpty) 0 else maximumPayment

}

object AnnualAccount {
  implicit val annualAccountOrdering: Ordering[AnnualAccount] = Ordering.by(_.taxYear.year)
  implicit val annualAccountFormat: Format[AnnualAccount] = Json.format[AnnualAccount]
}
