/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.{TaxAccountCalculator, TaxAccountCalculatorImpl, TaxYearRangeUtil}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountSummaryViewModel
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.time.TaxYearResolver

case class CodingComponentsWithCarBenefits(date: LocalDate, codingComponents: Seq[CodingComponent], companyCarBenefits: Seq[CompanyCarBenefit])

trait YourTaxFreeAmount {
  def buildTaxFreeAmount(previous: CodingComponentsWithCarBenefits,
                         current: CodingComponentsWithCarBenefits,
                         employmentIds: Map[Int, String])
                        (implicit messages: Messages): YourTaxFreeAmountViewModel = {
    val taxAccountCalculator: TaxAccountCalculator = new TaxAccountCalculatorImpl

    val removeMeTaxFreeAmountSummary =
      TaxFreeAmountSummaryViewModel(current.codingComponents, employmentIds, current.companyCarBenefits, taxAccountCalculator.taxFreeAmount(current.codingComponents))

    val previousTaxFreeInfo = {
      val previousTaxCodeDateRange = Dates.formatDate(previous.date)
      TaxFreeInfo(previousTaxCodeDateRange, previous.codingComponents, taxAccountCalculator)
    }

    val currentTaxFreeInfo = {
      val currentTaxCodeDateRange = TaxYearRangeUtil.dynamicDateRange(current.date, TaxYearResolver.endOfCurrentTaxYear)
      TaxFreeInfo(currentTaxCodeDateRange, current.codingComponents, taxAccountCalculator)
    }

    val allowancesAndDeductions = AllowancesAndDeductions.fromCodingComponents(previous.codingComponents, current.codingComponents)
    val allowancesDescription = for (
      allowance <- allowancesAndDeductions.allowances
    ) yield CodingComponentPairDescription(allowance, employmentIds, previous.companyCarBenefits ++ current.companyCarBenefits)

    val deductionsDescription = for (
      deduction <- allowancesAndDeductions.deductions
    ) yield CodingComponentPairDescription(deduction, employmentIds, previous.companyCarBenefits ++ current.companyCarBenefits)

    YourTaxFreeAmountViewModel(
      previousTaxFreeInfo,
      currentTaxFreeInfo,
      removeMeTaxFreeAmountSummary,
      allowancesDescription,
      deductionsDescription)
  }
}
