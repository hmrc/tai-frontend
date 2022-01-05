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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.service.YourTaxFreeAmountComparison
import uk.gov.hmrc.tai.util.{TaxAccountCalculator, TaxAccountCalculatorImpl, TaxYearRangeUtil}

trait YourTaxFreeAmount {

  def buildTaxFreeAmount(changeDate: LocalDate, previous: Option[Seq[CodingComponent]], current: Seq[CodingComponent])(
    implicit messages: Messages): YourTaxFreeAmountComparison = {

    val taxAccountCalculator: TaxAccountCalculator = new TaxAccountCalculatorImpl

    val previousTaxFreeInfo: Option[TaxFreeInfo] = previous.map(codingComponents => {
      val previousTaxCodeDateRange = Dates.formatDate(changeDate)
      TaxFreeInfo(previousTaxCodeDateRange, codingComponents, taxAccountCalculator)
    })

    val currentTaxFreeInfo = {
      val currentTaxCodeDateRange = TaxYearRangeUtil.dynamicDateRange(changeDate, TaxYear().end)
      TaxFreeInfo(currentTaxCodeDateRange, current, taxAccountCalculator)
    }

    val allowancesAndDeductions = buildAllowancesAndDeductionPairs(previous.getOrElse(Seq.empty), current)

    YourTaxFreeAmountComparison(
      previousTaxFreeInfo,
      currentTaxFreeInfo,
      allowancesAndDeductions
    )
  }

  def buildAllowancesAndDeductionPairs(
    previous: Seq[CodingComponent],
    current: Seq[CodingComponent]): AllowancesAndDeductionPairs =
    AllowancesAndDeductionPairs.fromCodingComponents(previous, current)
}
