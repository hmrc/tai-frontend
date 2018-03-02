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

import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.viewModels.HistoricPayAsYouEarnViewModel.EmploymentViewModel
import uk.gov.hmrc.tai.model.tai

case class HistoricPayAsYouEarnViewModel(taxYear: tai.TaxYear, employments: Seq[EmploymentViewModel], hasEmployments: Boolean) {
  val p800ServiceIsAvailable: Boolean = taxYear == tai.TaxYear().prev
}

object HistoricPayAsYouEarnViewModel {

  def apply(taxYear: tai.TaxYear, employments: Seq[Employment]): HistoricPayAsYouEarnViewModel = {
    val employmentVMs: Seq[EmploymentViewModel] = filterEmployments(taxYear, employments) sortBy(_.id)
    HistoricPayAsYouEarnViewModel(taxYear, employmentVMs, employments.nonEmpty)
  }

  private def filterEmployments(taxYear: tai.TaxYear, employments: Seq[Employment]): Seq[EmploymentViewModel] = {
    for {
      emp <- employments
      account <- emp.annualAccounts.find(_.taxYear.year == taxYear.year)
    } yield EmploymentViewModel(emp.name, account.totalIncomeYearToDate, emp.sequenceNumber)
  }

  case class EmploymentViewModel(name: String, taxablePayYTD: BigDecimal, id: Int)
}
