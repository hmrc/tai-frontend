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

import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.{NonSavingsIncomeCategory, TotalTax}
import uk.gov.hmrc.tai.model.domain.{Employment, UnderPaymentFromPreviousYear}
import uk.gov.hmrc.tai.util.ViewModelHelper

case class PreviousYearUnderpaymentViewModel(
                                              shouldHavePaid: BigDecimal,
                                              actuallyPaid: BigDecimal,
                                              allowanceReducedBy: BigDecimal,
                                              amountDue: BigDecimal,
                                              previousTaxYear: TaxYear) {

}

object PreviousYearUnderpaymentViewModel extends ViewModelHelper {

  def apply(codingComponents: Seq[CodingComponent], employments: Seq[Employment], totalTax: TotalTax): PreviousYearUnderpaymentViewModel = {

    val taxYear = TaxYear().prev

    val actuallyPaid = (for {
      emp <- employments
      account <- emp.annualAccounts.find(_.taxYear.year == taxYear.year)
    } yield account.totalTaxPaidYearToDate).sum

    val allowanceReducedBy = codingComponents.collectFirst {
      case CodingComponent(UnderPaymentFromPreviousYear, _, amount, _, _) => amount
    }.getOrElse(BigDecimal(0))

    val taxRate: BigDecimal = totalTax.incomeCategories.filter(_.incomeCategoryType == NonSavingsIncomeCategory)
      .flatMap(_.taxBands).find(_.bandType == "B").map(_.rate / 100).getOrElse(0.2)

    val amountDue = allowanceReducedBy * taxRate

    val shouldHavePaid = actuallyPaid + amountDue

    PreviousYearUnderpaymentViewModel(shouldHavePaid, actuallyPaid, allowanceReducedBy, amountDue, taxYear)
  }
}