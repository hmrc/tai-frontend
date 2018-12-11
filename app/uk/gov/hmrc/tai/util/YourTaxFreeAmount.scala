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

package uk.gov.hmrc.tai.util

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountSummaryViewModel
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.time.TaxYearResolver

case class MungedCodingComponents(previousDeductions: Seq[CodingComponent] = Seq.empty,
                                  previousAdditions: Seq[CodingComponent] = Seq.empty,
                                  currentDeductions: Seq[CodingComponent] = Seq.empty,
                                  currentAdditions: Seq[CodingComponent] = Seq.empty)


trait YourTaxFreeAmount extends TaxAccountCalculator {
  def buildTaxFreeAmount(previousTaxCodeChangeDate: LocalDate,
                         currentTaxCodeChangeDate: LocalDate,
                         previousCodingComponents: Seq[CodingComponent],
                         currentCodingComponents: Seq[CodingComponent],
                         currentCompanyCarBenefits: Seq[CompanyCarBenefit],
                         employmentNames: Map[Int, String])
                        (implicit messages: Messages): YourTaxFreeAmountViewModel = {

    val taxCodeDateRange = TaxYearRangeUtil.dynamicDateRange(currentTaxCodeChangeDate, TaxYearResolver.endOfCurrentTaxYear)

    val previousAnnualTaxFreeAmount = taxFreeAmount(previousCodingComponents)
    val currentAnnualTaxFreeAmount = taxFreeAmount(currentCodingComponents)

    val removeMeTaxFreeAmountSummary =
      TaxFreeAmountSummaryViewModel(currentCodingComponents, employmentNames, currentCompanyCarBenefits, taxFreeAmount(currentCodingComponents))

    val currentDeductions = getDeduction(currentCodingComponents)
    val currentAdditions = getAllowances(currentCodingComponents)
    val previousDeductions = getDeduction(previousCodingComponents)
    val previousAdditions = getAllowances(previousCodingComponents)

    val mungedCodingComponents = MungedCodingComponents(
      previousDeductions,
      previousAdditions,
      currentDeductions,
      currentAdditions)

    val previousPersonalAllowance = sumOfPersonalAllowances(previousCodingComponents)
    val currentPersonalAllowance = sumOfPersonalAllowances(currentCodingComponents)

    new YourTaxFreeAmountViewModel(
      Dates.formatDate(previousTaxCodeChangeDate),
      taxCodeDateRange,
      previousAnnualTaxFreeAmount,
      currentAnnualTaxFreeAmount,
      removeMeTaxFreeAmountSummary,
      mungedCodingComponents,
      previousPersonalAllowance,
      currentPersonalAllowance
    )
  }

  private def getDeduction(codingComponents: Seq[CodingComponent]): Seq[CodingComponent] = {
    codingComponents.filter({
      _.componentType match {
        case _: AllowanceComponentType => false
        case _ => true
      }
    })
  }

  private def getAllowances(codingComponents: Seq[CodingComponent]): Seq[CodingComponent] = {
    codingComponents.filterNot(isPersonalAllowanceComponent).filter({
      _.componentType match {
        case _: DeductionComponentType => false
        case _ => true
      }
    })
  }

  private def isPersonalAllowanceComponent(codingComponent: CodingComponent): Boolean = codingComponent.componentType match {
    case PersonalAllowancePA | PersonalAllowanceAgedPAA | PersonalAllowanceElderlyPAE => true
    case _ => false
  }

  private def sumOfPersonalAllowances(codingComponents: Seq[CodingComponent]): BigDecimal = {
    codingComponents.filter(isPersonalAllowanceComponent).map(_.amount).sum
  }
}
