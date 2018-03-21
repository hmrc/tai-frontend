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

package uk.gov.hmrc.tai.viewModels.incomeTaxComparison

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.viewModels.IncomeSourceComparisonViewModel

class IncomeSourceComparisonViewModelSpec extends PlaySpec {

  "IncomeSourceComparisonViewModel" should {
    "return an employment and its amounts" when {
      "CY and CY+1 items are supplied to the view model" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(EmploymentIncome, Some(1), 2222, "employment1", "1150L", "employment", OtherBasisOperation, Live)

        val employmentCY = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1)
        val employmentCYPlusOne = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1)

        val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(
          Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne), Seq(employmentCYPlusOne))

        val incomeSourceComparisonDetail = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(0)

        incomeSourceComparisonDetail.name mustBe employmentCY.name
        incomeSourceComparisonDetail.amountCY mustBe "£1,111"
        incomeSourceComparisonDetail.amountCYPlusOne mustBe "£2,222"

      }
    }

    "return not applicable for CY and CY+1" when {
      "CY and CY+1 do not have a matching employment ids" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment2", "1150L", "employment", OtherBasisOperation, Live)

        val employmentCY = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1)
        val employmentCYPlusOne = Employment("employment2", None, new LocalDate(), None, Nil, "", "", 2)

        val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(
          Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne), Seq(employmentCYPlusOne))

        println(incomeSourceComparisonViewModel)

        val incomeSourceComparisonDetailCY = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(0)
        val incomeSourceComparisonDetailCYPlusOne = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(1)

        incomeSourceComparisonDetailCY.name mustBe employmentCY.name
        incomeSourceComparisonDetailCY.amountCY mustBe "£1,111"
        incomeSourceComparisonDetailCY.amountCYPlusOne mustBe "not applicable"

        incomeSourceComparisonDetailCYPlusOne.name mustBe employmentCYPlusOne.name
        incomeSourceComparisonDetailCYPlusOne.amountCY mustBe "not applicable"
        incomeSourceComparisonDetailCYPlusOne.amountCYPlusOne mustBe "£2,222"
      }
    }
  }
}