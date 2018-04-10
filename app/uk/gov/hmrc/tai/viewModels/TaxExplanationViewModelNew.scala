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

import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.util.{BandTypesConstants, ViewModelHelper}

case class TaxExplanationViewModelNew(
                                       nonSavings: Seq[TaxBand],
                                       savings: Seq[TaxBand],
                                       dividends: Seq[TaxBand],
                                       bandType: String
                                     ) extends ViewModelHelper {
  val totalTax: BigDecimal = List(nonSavings, savings, dividends).flatten.map(_.tax).sum
}

object TaxExplanationViewModelNew extends BandTypesConstants {
  def apply(totalTax: TotalTax, taxCodeIncomes: Seq[TaxCodeIncome]): TaxExplanationViewModelNew = {
    val nonSavings = totalTax.incomeCategories.filter(_.incomeCategoryType == NonSavingsIncomeCategory).
      flatMap(_.taxBands).filter(_.income > 0).filterNot(_.rate == 0)

    val savings = totalTax.incomeCategories.filter {
      category => category.incomeCategoryType == UntaxedInterestIncomeCategory ||
        category.incomeCategoryType == BankInterestIncomeCategory || category.incomeCategoryType == ForeignInterestIncomeCategory
    }.flatMap(_.taxBands).filter(_.income > 0).filterNot(_.rate == 0)

    val dividends = totalTax.incomeCategories.filter {
      category => category.incomeCategoryType == UkDividendsIncomeCategory ||
        category.incomeCategoryType == ForeignDividendsIncomeCategory
    }.flatMap(_.taxBands).filter(_.income > 0).filterNot(_.rate == 0)

    val bandType = if(taxCodeIncomes.exists(_.taxCode.startsWith("S"))) ScottishBands else UkBands

    TaxExplanationViewModelNew(
      nonSavings,
      savings,
      dividends,
      bandType
    )

  }
}
