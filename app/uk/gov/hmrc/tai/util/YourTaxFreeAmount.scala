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
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.viewModels.{TaxFreeAmountSummaryViewModel}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.time.TaxYearResolver


trait YourTaxFreeAmount extends ViewModelHelper with TaxAccountCalculator {
  def buildTaxFreeAmount(recentTaxCodeChangeDate: LocalDate,
                         codingComponents: Seq[CodingComponent],
                         employmentNames: Map[Int, String],
                         companyCarBenefits: Seq[CompanyCarBenefit])
                        (implicit messages: Messages): YourTaxFreeAmountViewModel = {

    val taxCodeDateRange = TaxYearRangeUtil.dynamicDateRange(recentTaxCodeChangeDate, TaxYearResolver.endOfCurrentTaxYear)
    val taxFreeAmountTotal = taxFreeAmount(codingComponents)
    val annualTaxFreeAmount = withPoundPrefixAndSign(MoneyPounds (taxFreeAmountTotal, 0) )
    val taxFreeAmountSummary = TaxFreeAmountSummaryViewModel(codingComponents, employmentNames, companyCarBenefits, taxFreeAmountTotal)

    new YourTaxFreeAmountViewModel(
      taxCodeDateRange,
      annualTaxFreeAmount,
      taxFreeAmountSummary
    )
  }

  def buildTaxFreeAmountPairs(currentTaxCodeChangeDate: LocalDate,
                              previousTaxCodeChangeDate: LocalDate,
                              currentCodingComponents: Seq[CodingComponent],
                              previousCodingComponents: Seq[CodingComponent],
                              currentCompanyCarBenefits: Seq[CompanyCarBenefit],
                              previousCompanyCarBenefits: Seq[CompanyCarBenefit],
                              employmentNames: Map[Int, String])
                             (implicit messages: Messages): YourTaxFreeAmountViewModel = {
    ???
  }
}
