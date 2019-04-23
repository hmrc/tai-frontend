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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.IncomeSource


class EstimatedPayViewModelSpec extends PlaySpec {

  val employer = IncomeSource(1, "employer1")

  "Estimated Pay View Model " must {

    "return true if the gross annual pay equals the net annual pay" in {
      val grossAnnualPay = 20000
      val netAnnualPay = 20000

      val viewModel = EstimatedPayViewModel(Some(grossAnnualPay), Some(netAnnualPay), false, Some(20000), None, employer)

      viewModel.isGrossPayEqualsNetPay mustBe true
    }

    "return true if the gross pay is apportioned" in {

      val grossAnnualPay = 20000
      val netAnnualPay = 18000
      val annualSalary = 25000
      val employmentStartDate = TaxYear().start.plusMonths(2)

      val viewModel = EstimatedPayViewModel(Some(grossAnnualPay), Some(netAnnualPay), false, Some(annualSalary), Some(employmentStartDate), employer)

      viewModel.isGrossPayApportioned mustBe true
    }

  }

}
