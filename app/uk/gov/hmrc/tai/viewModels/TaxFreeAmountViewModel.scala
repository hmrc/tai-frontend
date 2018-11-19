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

package uk.gov.hmrc.tai.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.{TaxAccountCalculator, ViewModelHelper}

case class TaxFreeAmountViewModel(header: String,
                                     title: String,
                                     annualTaxFreeAmount: String,
                                     taxFreeAmountSummary: TaxFreeAmountSummaryViewModel)

object TaxFreeAmountViewModel extends TaxAccountCalculator with ViewModelHelper {

  def apply(codingComponents: Seq[CodingComponent],
            employmentName: Map[Int, String],
            companyCarBenefits: Seq[CompanyCarBenefit])
           (implicit messages: Messages): TaxFreeAmountViewModel = {

    val taxFreeAmountMsg = Messages("tai.taxFreeAmount.heading.pt1")

    val headerWithAdditionalMarkup = s"""$taxFreeAmountMsg $currentTaxYearRangeHtmlNonBreak"""
    val title = s"$taxFreeAmountMsg $currentTaxYearRangeHtmlNonBreak"

    val taxFreeAmountTotal: BigDecimal = taxFreeAmount(codingComponents)

    val taxFreeAmountSummary  = TaxFreeAmountSummaryViewModel(codingComponents, employmentName, companyCarBenefits, taxFreeAmountTotal)

    TaxFreeAmountViewModel(headerWithAdditionalMarkup, title, withPoundPrefixAndSign(MoneyPounds(taxFreeAmountTotal, 0)), taxFreeAmountSummary)
  }

}
