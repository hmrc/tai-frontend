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

import hmrc.nps2.IabdType
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.TaxSummaryDetails
import uk.gov.hmrc.tai.model.{IabdSummary, TaxSummaryDetails}
import uk.gov.hmrc.tai.model.domain.BankAccount

case class YourTaxableIncomeViewModelV2(hasCompanyCar: Boolean, hasBankAccount: Boolean)

object YourTaxableIncomeViewModelV2 {
  def apply(taxSummaryDetails: TaxSummaryDetails, bankAccounts: Seq[BankAccount])(implicit messages: Messages): YourTaxableIncomeViewModelV2 = {
    val carBenefitsList: Option[List[IabdSummary]] = taxSummaryDetails
      .increasesTax
      .flatMap(_
        .benefitsFromEmployment
        .map(_
          .iabdSummaries
            .filter(_.iabdType == IabdType.CarBenefit.code)
        )
      )
    val hasCompanyCar = carBenefitsList.getOrElse(Nil).nonEmpty
    YourTaxableIncomeViewModelV2(hasCompanyCar, bankAccounts.nonEmpty)
  }
}
