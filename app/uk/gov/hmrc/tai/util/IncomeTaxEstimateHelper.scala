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

import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.formatting.Money.pounds
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.urls.Link

trait IncomeTaxEstimateHelper {

  def getTaxOnIncomeTypeHeading(taxCodeIncomes: Seq[TaxCodeIncome])(implicit messages:Messages): String = {

    val employments: Boolean = taxCodeIncomes.exists(taxCodeIncome => taxCodeIncome.componentType == EmploymentIncome)
    val pensions: Boolean = taxCodeIncomes.exists(taxCodeIncome => taxCodeIncome.componentType == PensionIncome)
    val other: Boolean = taxCodeIncomes.exists(taxCodeIncome => taxCodeIncome.componentType == JobSeekerAllowanceIncome ||
                                                                taxCodeIncome.componentType == OtherIncome)

    (employments, pensions, other) match {
      case (true, false, false) => Messages(s"tax.on.your.employment.income")
      case (false, true, false) => Messages(s"tax.on.your.private.pension.income")
      case (_, _, _)            => Messages(s"tax.on.your.paye.income")
    }
  }

  def getTaxOnIncomeTypeDescription(taxCodeIncomes: Seq[TaxCodeIncome], taxAccountSummary: TaxAccountSummary)(implicit messages:Messages): String = {

    val employments: Boolean = taxCodeIncomes.exists(taxCodeIncome => taxCodeIncome.componentType == EmploymentIncome)
    val pensions: Boolean = taxCodeIncomes.exists(taxCodeIncome => taxCodeIncome.componentType == PensionIncome)
    val other: Boolean = taxCodeIncomes.exists(taxCodeIncome => taxCodeIncome.componentType == JobSeekerAllowanceIncome ||
      taxCodeIncome.componentType == OtherIncome)

    val incomeType = (employments, pensions, other) match {
      case (true, false, false) => "employment"
      case (false, true, false) => "private.pension"
      case (_, _, _)            => "paye"
    }

    Messages(s"your.total.income.from.$incomeType.desc", pounds(taxAccountSummary.totalEstimatedIncome),
              Link.toInternalPage(id=Some("taxFreeAmountLink"),
                                  url=routes.TaxFreeAmountController.taxFreeAmount.url,
                                  value=Some(Messages("tai.estimatedIncome.taxFree.link"))).toHtml,
              pounds(taxAccountSummary.taxFreeAllowance))

  }
}
