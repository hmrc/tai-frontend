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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.util.BandTypesConstants

import scala.math.BigDecimal

trait Dividends extends BandTypesConstants{

  def hasDividends(incomeCategories:Seq[IncomeCategory]): Boolean = {
    totalDividendIncome(incomeCategories) > 0
  }

  def totalDividendIncome(incomeCategories: Seq[IncomeCategory]): BigDecimal = {
    incomeCategories.filter {
      category => category.incomeCategoryType == UkDividendsIncomeCategory ||
        category.incomeCategoryType == ForeignDividendsIncomeCategory
    }.map(_.totalIncome).sum
  }

  def retrieveDividends(incomeCategories: Seq[IncomeCategory]):List[TaxBand] = {
    incomeCategories.filter {
      category => category.incomeCategoryType == UkDividendsIncomeCategory ||
        category.incomeCategoryType == ForeignDividendsIncomeCategory
    }.flatMap(_.taxBands).filter(_.income > 0).toList
  }

  def taxFreeDividendAllowance(incomeCategories: Seq[IncomeCategory]): BigDecimal = {
    val taxBands = incomeCategories.flatMap(_.taxBands)

    taxBands.find(_.bandType == DividendZeroRate).flatMap(_.upperBand).getOrElse(BigDecimal(0))

  }


}
