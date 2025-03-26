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

package uk.gov.hmrc.tai.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.{IncomeSources, TaxYear}
import uk.gov.hmrc.tai.service.TimeToProcess
import uk.gov.hmrc.tai.util.{MoneyPounds, TaxYearRangeUtil => Dates, ViewModelHelper}

case class TaxAccountSummaryViewModel(
  header: String,
  title: String,
  taxFreeAmount: Option[String],
  estimatedIncomeTaxAmount: Option[String],
  lastTaxYearEnd: String,
  employments: Seq[IncomeSourceViewModel],
  pensions: Seq[IncomeSourceViewModel],
  ceasedEmployments: Seq[IncomeSourceViewModel],
  displayIyaBanner: Boolean,
  isAnyFormInProgress: TimeToProcess,
  otherIncomeSources: Seq[IncomeSourceViewModel],
  rtiAvailable: Boolean,
  totalEstimatedIncome: Option[String]
)

object TaxAccountSummaryViewModel extends ViewModelHelper {

  def apply(
    taxAccountSummary: TaxAccountSummary,
    isAnyFormInProgress: TimeToProcess,
    nonTaxCodeIncome: NonTaxCodeIncome,
    incomesSources: IncomeSources,
    nonMatchingCeasedEmployments: Seq[Employment]
  )(implicit messages: Messages): TaxAccountSummaryViewModel = {

    val header = messages("tai.incomeTaxSummary.heading.part1", Dates.currentTaxYearRange)
    val title = messages("tai.incomeTaxSummary.heading.part1", Dates.currentTaxYearRange)

    val taxFreeAmount = withPoundPrefixAndSign(MoneyPounds(taxAccountSummary.taxFreeAmount, 0))
    val estimatedIncomeTaxAmount = withPoundPrefixAndSign(MoneyPounds(taxAccountSummary.totalEstimatedTax, 0))

    val employmentViewModels =
      incomesSources.liveEmploymentIncomeSources.map(IncomeSourceViewModel.createFromTaxedIncome(_))

    val pensionsViewModels = incomesSources.livePensionIncomeSources.map(IncomeSourceViewModel.createFromTaxedIncome(_))

    def employmentCeasedThisYear(employment: Employment): Boolean = {
      val currentYear = TaxYear()
      // Default to true as if there is no endDate it's potentially ceased
      employment.endDate.fold(true) { endDate =>
        !(endDate isBefore currentYear.start)
      }
    }

    val ceasedEmploymentViewModels = incomesSources.ceasedEmploymentIncomeSources.collect {
      case ti @ TaxedIncome(_, employment) if employmentCeasedThisYear(employment) =>
        IncomeSourceViewModel.createFromTaxedIncome(ti)
    } ++ nonMatchingCeasedEmployments.collect {
      case employment if employmentCeasedThisYear(employment) =>
        IncomeSourceViewModel.createFromEmployment(employment)
    }

    val lastTaxYearEnd: String = Dates.formatDate(TaxYear().prev.end)

    val totalEstimatedIncome = withPoundPrefixAndSign(MoneyPounds(taxAccountSummary.totalEstimatedIncome, 0))

    TaxAccountSummaryViewModel(
      header = header,
      title = title,
      taxFreeAmount = Some(taxFreeAmount),
      estimatedIncomeTaxAmount = Some(estimatedIncomeTaxAmount),
      lastTaxYearEnd = lastTaxYearEnd,
      employments = employmentViewModels,
      pensions = pensionsViewModels,
      ceasedEmployments = ceasedEmploymentViewModels,
      displayIyaBanner = taxAccountSummary.totalInYearAdjustmentIntoCY > 0,
      isAnyFormInProgress = isAnyFormInProgress,
      otherIncomeSources = IncomeSourceViewModel(nonTaxCodeIncome),
      rtiAvailable = incomesSources.isRtiAvailable,
      totalEstimatedIncome = Some(totalEstimatedIncome)
    )
  }

  def apply(
    taxAccountSummary: Option[TaxAccountSummary],
    isAnyFormInProgress: TimeToProcess,
    nonTaxCodeIncome: Option[NonTaxCodeIncome],
    incomesSources: IncomeSources
  )(implicit messages: Messages): TaxAccountSummaryViewModel = {

    val header = messages("tai.incomeTaxSummary.heading.part1", Dates.currentTaxYearRange)
    val title = messages("tai.incomeTaxSummary.heading.part1", Dates.currentTaxYearRange)

    val taxFreeAmount = taxAccountSummary.map(account => withPoundPrefixAndSign(MoneyPounds(account.taxFreeAmount, 0)))
    val estimatedIncomeTaxAmount =
      taxAccountSummary.map(account => withPoundPrefixAndSign(MoneyPounds(account.totalEstimatedTax, 0)))

    val employmentViewModels =
      incomesSources.liveEmploymentIncomeSources.map(IncomeSourceViewModel.createFromTaxedIncome(_))

    val pensionsViewModels = incomesSources.livePensionIncomeSources.map(IncomeSourceViewModel.createFromTaxedIncome(_))

    def employmentCeasedThisYear(employment: Employment): Boolean = {
      val currentYear = TaxYear()
      // Default to true as if there is no endDate it's potentially ceased
      employment.endDate.fold(true) { endDate =>
        !(endDate isBefore currentYear.start)
      }
    }

    val ceasedEmploymentViewModels = incomesSources.ceasedEmploymentIncomeSources.collect {
      case ti @ TaxedIncome(_, employment) if employmentCeasedThisYear(employment) =>
        IncomeSourceViewModel.createFromTaxedIncome(ti)
    }

    val lastTaxYearEnd: String = Dates.formatDate(TaxYear().prev.end)

    val totalEstimatedIncome =
      taxAccountSummary.map(account => withPoundPrefixAndSign(MoneyPounds(account.totalEstimatedIncome, 0)))

    TaxAccountSummaryViewModel(
      header = header,
      title = title,
      taxFreeAmount = taxFreeAmount,
      estimatedIncomeTaxAmount = estimatedIncomeTaxAmount,
      lastTaxYearEnd = lastTaxYearEnd,
      employments = employmentViewModels,
      pensions = pensionsViewModels,
      ceasedEmployments = ceasedEmploymentViewModels,
      displayIyaBanner = taxAccountSummary.fold(BigDecimal(0))(_.totalInYearAdjustmentIntoCY) > 0,
      isAnyFormInProgress = isAnyFormInProgress,
      otherIncomeSources = nonTaxCodeIncome.fold(Seq.empty[IncomeSourceViewModel])(IncomeSourceViewModel(_)),
      rtiAvailable = incomesSources.isRtiAvailable,
      totalEstimatedIncome = totalEstimatedIncome
    )
  }

}
