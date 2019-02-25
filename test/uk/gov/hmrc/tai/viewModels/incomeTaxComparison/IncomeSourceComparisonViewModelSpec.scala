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

package uk.gov.hmrc.tai.viewModels.incomeTaxComparison

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome, PensionIncome}
import uk.gov.hmrc.tai.viewModels.IncomeSourceComparisonViewModel

class IncomeSourceComparisonViewModelSpec extends PlaySpec {

  "IncomeSourceComparisonViewModel" should {
    "return an employment sequence" when {
      "CY and CY+1 items are supplied to the view model" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(EmploymentIncome, Some(1), 2222, "employment1", "1150L", "employment", OtherBasisOfOperation, Live)

        val employmentCY = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1, None,false, false)

        val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(
          Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetail = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(0)

        incomeSourceComparisonDetail.name mustBe employmentCY.name
        incomeSourceComparisonDetail.amountCY mustBe "£1,111"
        incomeSourceComparisonDetail.amountCYPlusOne mustBe "£2,222"
        incomeSourceComparisonDetail.empId mustBe 1
      }
    }

    "return not a applicable vale for CY and CY+1" when {
      "CY and CY+1 do not have a matching employment ids" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment2", "1150L", "employment", OtherBasisOfOperation, Live)

        val employmentCY = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1, None, false, false)

        val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(
          Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))


        val incomeSourceComparisonDetailCY = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(0)
        val incomeSourceComparisonDetailCYPlusOne = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(1)

        incomeSourceComparisonDetailCY.name mustBe employmentCY.name
        incomeSourceComparisonDetailCY.amountCY mustBe "£1,111"
        incomeSourceComparisonDetailCY.amountCYPlusOne mustBe NA
        incomeSourceComparisonDetailCY.empId mustBe 1

        incomeSourceComparisonDetailCYPlusOne.amountCY mustBe NA
        incomeSourceComparisonDetailCYPlusOne.amountCYPlusOne mustBe "£2,222"
        incomeSourceComparisonDetailCYPlusOne.empId mustBe 2

      }
    }

    "return a pension sequence" when {
      "CY and CY+1 items are supplied to the view model" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(PensionIncome, Some(3), 4444, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val employmentCY = Employment("Pension1", Some("3ABC"), new LocalDate(2017, 3, 1), None, Nil, "DIST3", "PAYE3", 3, None, false, false)

        val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(
          Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetail = incomeSourceComparisonViewModel.pensionIncomeSourceDetail.head

        incomeSourceComparisonDetail.name mustBe employmentCY.name
        incomeSourceComparisonDetail.amountCY mustBe "£3,333"
        incomeSourceComparisonDetail.amountCYPlusOne mustBe "£4,444"
        incomeSourceComparisonDetail.empId mustBe 3
      }
    }

    "return a not applicable value for CY and CY+1" when {
      "CY and CY+1 do not have a matching pension ids" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(PensionIncome, Some(4), 4444, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val employmentCY = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 3, None, false, false)

        val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(
          Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetailCY = incomeSourceComparisonViewModel.pensionIncomeSourceDetail(0)
        val incomeSourceComparisonDetailCYPlusOne = incomeSourceComparisonViewModel.pensionIncomeSourceDetail(1)

        incomeSourceComparisonDetailCY.name mustBe employmentCY.name
        incomeSourceComparisonDetailCY.amountCY mustBe "£3,333"
        incomeSourceComparisonDetailCY.amountCYPlusOne mustBe NA
        incomeSourceComparisonDetailCY.empId mustBe 3

        incomeSourceComparisonDetailCYPlusOne.amountCY mustBe NA
        incomeSourceComparisonDetailCYPlusOne.amountCYPlusOne mustBe "£4,444"
        incomeSourceComparisonDetailCYPlusOne.empId mustBe 4

      }
    }

    "return an empty sequence" when{
      "no CY or CY+1 values are avialable" in{

        val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(
          Seq(), Seq(), Seq())

        incomeSourceComparisonViewModel.employmentIncomeSourceDetail.size mustBe 0
        incomeSourceComparisonViewModel.pensionIncomeSourceDetail.size mustBe 0

      }
    }

    "throw an exception" when {
      "no employment id is present in tax code income" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(PensionIncome, None, 4444, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val employmentCY = Employment("Pension1", Some("3ABC"), new LocalDate(2017, 3, 1), None, Nil, "DIST3", "PAYE3", 3, None, false, false)

        intercept[RuntimeException]{
          IncomeSourceComparisonViewModel(
            Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))
        }.getMessage must equal("Employment id is missing")
        
      }
    }
  }

  private lazy val NA = "not applicable"

}