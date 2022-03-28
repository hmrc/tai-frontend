/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.income.{Ceased, Live, NotLive, OtherBasisOfOperation, PotentiallyCeased, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome, PensionIncome}
import uk.gov.hmrc.tai.viewModels.IncomeSourceComparisonViewModel

class IncomeSourceComparisonViewModelSpec extends PlaySpec {

  "IncomeSourceComparisonViewModel" should {
    "return an employment sequence" when {
      "CY and CY+1 items are supplied to the view model" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment1",
            "1150L",
            "employment",
            OtherBasisOfOperation,
            Live)

        val taxCodeIncomesCYPlusOne = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1234,
            "employment1",
            "1150L",
            "employment",
            OtherBasisOfOperation,
            Live),
          TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            5678,
            "employment2",
            "1150L",
            "employment",
            OtherBasisOfOperation,
            Live)
        )

        val employmentCY = Seq(
          Employment("employment1", Live, None, LocalDate.now, None, Nil, "", "", 1, None, false, false),
          Employment("employment2", Live, None, LocalDate.now, None, Nil, "", "", 2, None, false, false)
        )

        val incomeSourceComparisonViewModel =
          IncomeSourceComparisonViewModel(Seq(taxCodeIncomesCY), employmentCY, taxCodeIncomesCYPlusOne)

        val incomeSourceComparisonDetail = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(0)

        incomeSourceComparisonDetail.name mustBe "employment1"
        incomeSourceComparisonDetail.amountCY mustBe "£1,111"
        incomeSourceComparisonDetail.amountCYPlusOne mustBe "£1,234"
        incomeSourceComparisonDetail.empId mustBe 1
        incomeSourceComparisonDetail.isLive mustBe true
      }
    }

    "return not a applicable vale for CY and CY+1" when {
      "CY and CY+1 do not have a matching employment ids" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment1",
            "1150L",
            "employment",
            OtherBasisOfOperation,
            Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            2222,
            "employment2",
            "1150L",
            "employment",
            OtherBasisOfOperation,
            Live)

        val employmentCY = Seq(
          Employment("employment1", Live, None, LocalDate.now, None, Nil, "", "", 1, None, false, false),
          Employment("employment2", Live, None, LocalDate.now, None, Nil, "", "", 2, None, false, false)
        )

        val incomeSourceComparisonViewModel =
          IncomeSourceComparisonViewModel(Seq(taxCodeIncomesCY), employmentCY, Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetailCY = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(0)
        val incomeSourceComparisonDetailCYPlusOne = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(1)

        incomeSourceComparisonDetailCY.name mustBe "employment1"
        incomeSourceComparisonDetailCY.amountCY mustBe "£1,111"
        incomeSourceComparisonDetailCY.amountCYPlusOne mustBe NA
        incomeSourceComparisonDetailCY.empId mustBe 1
        incomeSourceComparisonDetailCY.isLive mustBe true

        incomeSourceComparisonDetailCYPlusOne.amountCY mustBe NA
        incomeSourceComparisonDetailCYPlusOne.amountCYPlusOne mustBe "£2,222"
        incomeSourceComparisonDetailCYPlusOne.empId mustBe 2
        incomeSourceComparisonDetailCYPlusOne.isLive mustBe false

      }
    }

    "return a pension sequence" when {
      "CY and CY+1 items are supplied to the view model" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(PensionIncome, Some(3), 4444, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val employmentCY = Employment(
          "Pension1",
          Live,
          Some("3ABC"),
          LocalDate.of(2017, 3, 1),
          None,
          Nil,
          "DIST3",
          "PAYE3",
          3,
          None,
          false,
          false)

        val incomeSourceComparisonViewModel =
          IncomeSourceComparisonViewModel(Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetail = incomeSourceComparisonViewModel.pensionIncomeSourceDetail.head

        incomeSourceComparisonDetail.name mustBe employmentCY.name
        incomeSourceComparisonDetail.amountCY mustBe "£3,333"
        incomeSourceComparisonDetail.amountCYPlusOne mustBe "£4,444"
        incomeSourceComparisonDetail.empId mustBe 3
        incomeSourceComparisonDetail.isLive mustBe true
      }
    }

    "return a not applicable value for CY and CY+1" when {
      "CY and CY+1 do not have a matching pension ids" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(PensionIncome, Some(4), 4444, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val employmentCY =
          Employment("employment1", Live, None, LocalDate.now, None, Nil, "", "", 3, None, false, false)

        val incomeSourceComparisonViewModel =
          IncomeSourceComparisonViewModel(Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetailCY = incomeSourceComparisonViewModel.pensionIncomeSourceDetail(0)
        val incomeSourceComparisonDetailCYPlusOne = incomeSourceComparisonViewModel.pensionIncomeSourceDetail(1)

        incomeSourceComparisonDetailCY.name mustBe employmentCY.name
        incomeSourceComparisonDetailCY.amountCY mustBe "£3,333"
        incomeSourceComparisonDetailCY.amountCYPlusOne mustBe NA
        incomeSourceComparisonDetailCY.empId mustBe 3
        incomeSourceComparisonDetailCY.isLive mustBe true

        incomeSourceComparisonDetailCYPlusOne.amountCY mustBe NA
        incomeSourceComparisonDetailCYPlusOne.amountCYPlusOne mustBe "£4,444"
        incomeSourceComparisonDetailCYPlusOne.empId mustBe 4
        incomeSourceComparisonDetailCYPlusOne.isLive mustBe false

      }
    }

    "return an empty sequence" when {
      "no CY or CY+1 values are available" in {

        val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Seq(), Seq(), Seq())

        incomeSourceComparisonViewModel.employmentIncomeSourceDetail.size mustBe 0
        incomeSourceComparisonViewModel.pensionIncomeSourceDetail.size mustBe 0

      }
    }

    "show not applicable amount" when {
      "no employment id is present in tax code income for CY+1" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(PensionIncome, None, 4444, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)

        val employmentCY = Employment(
          "Pension1",
          Live,
          Some("3ABC"),
          LocalDate.of(2017, 3, 1),
          None,
          Nil,
          "DIST3",
          "PAYE3",
          3,
          None,
          false,
          false)

        val incomeSourceComparisonViewModel =
          IncomeSourceComparisonViewModel(Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetailCY = incomeSourceComparisonViewModel.pensionIncomeSourceDetail(0)

        incomeSourceComparisonDetailCY.amountCYPlusOne mustBe NA
      }
    }

    Seq(Ceased, NotLive, PotentiallyCeased).foreach { taxCodeIncomeSourceStatus =>
      s"set isLive to false if employment is $taxCodeIncomeSourceStatus" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            1111,
            "employment1",
            "1150L",
            "employment",
            OtherBasisOfOperation,
            Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            2222,
            "employment1",
            "1150L",
            "employment",
            OtherBasisOfOperation,
            Live)

        val employmentCY =
          Employment(
            "employment1",
            taxCodeIncomeSourceStatus,
            None,
            LocalDate.now,
            None,
            Nil,
            "",
            "",
            1,
            None,
            false,
            false)

        val incomeSourceComparisonViewModel =
          IncomeSourceComparisonViewModel(Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetail = incomeSourceComparisonViewModel.employmentIncomeSourceDetail(0)

        incomeSourceComparisonDetail.name mustBe employmentCY.name
        incomeSourceComparisonDetail.amountCY mustBe "£1,111"
        incomeSourceComparisonDetail.amountCYPlusOne mustBe "£2,222"
        incomeSourceComparisonDetail.empId mustBe 1
        incomeSourceComparisonDetail.isLive mustBe false
      }
    }

    Seq(Ceased, NotLive, PotentiallyCeased).foreach { taxCodeIncomeSourceStatus =>
      s"set isLive to false if pension is $taxCodeIncomeSourceStatus" in {

        val taxCodeIncomesCY =
          TaxCodeIncome(PensionIncome, Some(1), 1111, "pension1", "1150L", "employment", OtherBasisOfOperation, Live)

        val taxCodeIncomesCYPlusOne =
          TaxCodeIncome(PensionIncome, Some(1), 2222, "pension1", "1150L", "employment", OtherBasisOfOperation, Live)

        val employmentCY =
          Employment(
            "pension1",
            taxCodeIncomeSourceStatus,
            None,
            LocalDate.now,
            None,
            Nil,
            "",
            "",
            1,
            None,
            false,
            false)

        val incomeSourceComparisonViewModel =
          IncomeSourceComparisonViewModel(Seq(taxCodeIncomesCY), Seq(employmentCY), Seq(taxCodeIncomesCYPlusOne))

        val incomeSourceComparisonDetail = incomeSourceComparisonViewModel.pensionIncomeSourceDetail(0)

        incomeSourceComparisonDetail.name mustBe "pension1"
        incomeSourceComparisonDetail.amountCY mustBe "£1,111"
        incomeSourceComparisonDetail.amountCYPlusOne mustBe "£2,222"
        incomeSourceComparisonDetail.empId mustBe 1
        incomeSourceComparisonDetail.isLive mustBe false
      }
    }
  }

  private lazy val NA = "not applicable"

}
