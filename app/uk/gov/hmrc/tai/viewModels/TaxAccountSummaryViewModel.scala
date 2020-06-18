/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.service.TimeToProcess
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil, ViewModelHelper}

case class TaxAccountSummaryViewModel(
  header: String,
  title: String,
  taxFreeAmount: String,
  estimatedIncomeTaxAmount: String,
  lastTaxYearEnd: String,
  employments: Seq[IncomeSourceViewModel],
  pensions: Seq[IncomeSourceViewModel],
  ceasedEmployments: Seq[IncomeSourceViewModel],
  displayIyaBanner: Boolean,
  isAnyFormInProgress: TimeToProcess,
  otherIncomeSources: Seq[IncomeSourceViewModel])

case class IncomesSources(
  livePensionIncomeSources: Seq[TaxedIncome],
  liveEmploymentIncomeSources: Seq[TaxedIncome],
  ceasedEmploymentIncomeSources: Seq[TaxedIncome])

object TaxAccountSummaryViewModel extends ViewModelHelper {

  def apply(
    taxAccountSummary: TaxAccountSummary,
    isAnyFormInProgress: TimeToProcess,
    nonTaxCodeIncome: NonTaxCodeIncome,
    incomesSources: IncomesSources,
    nonMatchingCeasedEmployments: Seq[Employment])(implicit messages: Messages): TaxAccountSummaryViewModel = {

    val header = messages("tai.incomeTaxSummary.heading.part1", TaxYearRangeUtil.currentTaxYearRangeSingleLine)
    val title = messages("tai.incomeTaxSummary.heading.part1", TaxYearRangeUtil.currentTaxYearRangeSingleLine)

    val taxFreeAmount = withPoundPrefixAndSign(MoneyPounds(taxAccountSummary.taxFreeAmount, 0))
    val estimatedIncomeTaxAmount = withPoundPrefixAndSign(MoneyPounds(taxAccountSummary.totalEstimatedTax, 0))

    val employmentViewModels =
      incomesSources.liveEmploymentIncomeSources.map(IncomeSourceViewModel.createFromTaxedIncome(_))

    val pensionsViewModels = incomesSources.livePensionIncomeSources.map(IncomeSourceViewModel.createFromTaxedIncome(_))

    val ceasedEmploymentViewModels = incomesSources.ceasedEmploymentIncomeSources.map(
      IncomeSourceViewModel.createFromTaxedIncome(_)) ++
      nonMatchingCeasedEmployments.map(IncomeSourceViewModel.createFromEmployment(_))

    val lastTaxYearEnd: String = Dates.formatDate(TaxYear().prev.end)

    TaxAccountSummaryViewModel(
      header = header,
      title = title,
      taxFreeAmount = taxFreeAmount,
      estimatedIncomeTaxAmount = estimatedIncomeTaxAmount,
      lastTaxYearEnd = lastTaxYearEnd,
      employments = employmentViewModels,
      pensions = pensionsViewModels,
      ceasedEmployments = ceasedEmploymentViewModels,
      displayIyaBanner = taxAccountSummary.totalInYearAdjustmentIntoCY > 0,
      isAnyFormInProgress = isAnyFormInProgress,
      otherIncomeSources = IncomeSourceViewModel(nonTaxCodeIncome)
    )
  }
}
