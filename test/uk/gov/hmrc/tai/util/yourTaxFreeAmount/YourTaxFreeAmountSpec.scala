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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.YourTaxFreeAmountComparison
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import utils.BaseSpec

import java.time.LocalDate

class YourTaxFreeAmountSpec extends BaseSpec with YourTaxFreeAmount {

  val date = LocalDate.of(2018, 6, 5)

  def createYourTaxFreeAmountComparison(): YourTaxFreeAmountComparison = {

    val formattedDate      = Dates.formatDate(date)
    val formattedDateRange = createFormattedDate(date)

    val allowancesAndDeductionPairs = AllowancesAndDeductionPairs(Seq.empty, Seq.empty)

    YourTaxFreeAmountComparison(
      Some(TaxFreeInfo(formattedDate, 0, 0)),
      TaxFreeInfo(formattedDateRange, 0, 0),
      allowancesAndDeductionPairs
    )
  }

  def createFormattedDate(date: LocalDate): String =
    Dates.dynamicDateRange(date, TaxYear().end)

  "buildTaxFreeAmount" should {
    "have the correct date formatting" in {
      val expected = createYourTaxFreeAmountComparison()

      buildTaxFreeAmount(date, Some(Seq.empty), Seq.empty) mustBe expected
    }
  }
}
