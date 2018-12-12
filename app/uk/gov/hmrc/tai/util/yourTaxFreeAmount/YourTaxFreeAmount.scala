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
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.{TaxAccountCalculator, TaxYearRangeUtil}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountSummaryViewModel
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.time.TaxYearResolver

object IsPersonalAllowance {
  def isPersonalAllowanceComponent(codingComponentType: TaxComponentType): Boolean = {
    codingComponentType match {
      case PersonalAllowancePA | PersonalAllowanceAgedPAA | PersonalAllowanceElderlyPAE => true
      case _ => false
    }
  }
}

trait YourTaxFreeAmount extends TaxAccountCalculator {
  def buildTaxFreeAmount(previousTaxCodeChangeDate: LocalDate,
                         currentTaxCodeChangeDate: LocalDate,
                         previousCodingComponents: Seq[CodingComponent],
                         currentCodingComponents: Seq[CodingComponent],
                         currentCompanyCarBenefits: Seq[CompanyCarBenefit],
                         employmentNames: Map[Int, String])
                        (implicit messages: Messages): YourTaxFreeAmountViewModel = {


    val removeMeTaxFreeAmountSummary =
      TaxFreeAmountSummaryViewModel(currentCodingComponents, employmentNames, currentCompanyCarBenefits, taxFreeAmount(currentCodingComponents))

    val allowancesAndDeductions = AllowancesAndDeductions.fromCodingComponents(previousCodingComponents, currentCodingComponents)

    val previousTaxCodeDateRange = Dates.formatDate(previousTaxCodeChangeDate)
    val previousTaxFreeInfo = TaxFreeInfo(previousTaxCodeDateRange, previousCodingComponents)

    val currentTaxCodeDateRange = TaxYearRangeUtil.dynamicDateRange(currentTaxCodeChangeDate, TaxYearResolver.endOfCurrentTaxYear)
    val currentTaxFreeInfo = TaxFreeInfo(currentTaxCodeDateRange, currentCodingComponents)

    new YourTaxFreeAmountViewModel(
      previousTaxFreeInfo,
      currentTaxFreeInfo,
      removeMeTaxFreeAmountSummary,
      allowancesAndDeductions
    )
  }
}
