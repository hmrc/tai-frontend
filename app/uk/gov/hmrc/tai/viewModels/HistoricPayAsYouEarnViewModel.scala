/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.viewModels.HistoricPayAsYouEarnViewModel.EmploymentViewModel

case class HistoricPayAsYouEarnViewModel(taxYear: TaxYear,
                                         pensions: Seq[EmploymentViewModel],
                                         employments: Seq[EmploymentViewModel],
                                         hasEmploymentsOrPensions: Boolean,
                                         showTaxCodeDescriptionLink: Boolean) {

  val p800ServiceIsAvailable: Boolean = taxYear == TaxYear().prev
}

object HistoricPayAsYouEarnViewModel {

  def apply(taxYear: TaxYear, employments: Seq[Employment], showTaxCodeDescriptionLink: Boolean)(implicit messages: Messages): HistoricPayAsYouEarnViewModel = {
    val incomeSources: Seq[EmploymentViewModel] = filterIncomeSources(taxYear, employments) sortBy(_.id)
    val (pensionsVMs, employmentsVMs): (Seq[EmploymentViewModel], Seq[EmploymentViewModel]) = incomeSources.partition(_.isPension)

    HistoricPayAsYouEarnViewModel(taxYear, pensionsVMs, employmentsVMs, pensionsVMs.nonEmpty || employmentsVMs.nonEmpty, showTaxCodeDescriptionLink)
  }

  private def filterIncomeSources(taxYear: TaxYear, employments: Seq[Employment]): Seq[EmploymentViewModel] = {
    for {
      employment <- employments
      account <- employment.annualAccounts.find(_.taxYear.year == taxYear.year)
    } yield EmploymentViewModel(employment.name, account.totalIncomeYearToDate, employment.sequenceNumber, employment.receivingOccupationalPension, employment.payrollNumber)
  }

  case class EmploymentViewModel(name: String, taxablePayYTD: BigDecimal, id: Int, isPension: Boolean, payrollNumber:Option[String])
}