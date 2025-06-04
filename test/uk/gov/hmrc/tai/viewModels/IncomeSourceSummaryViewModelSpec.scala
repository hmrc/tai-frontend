/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.domain.benefits._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import utils.BaseSpec

import java.time.LocalDate

class IncomeSourceSummaryViewModelSpec extends BaseSpec {

  val emptyBenefits = Benefits(Seq.empty[CompanyCarBenefit], Seq.empty[GenericBenefit])
  val firstPayment = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  val secondPayment = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  val thirdPayment = Payment(LocalDate.now.minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  val latestPayment = Payment(LocalDate.now.minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)
  val annualAccount = AnnualAccount(
    7,
    uk.gov.hmrc.tai.model.TaxYear(),
    Available,
    Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
    Nil
  )
  val estimatedPayJourneyCompleted = false

  val expectedPenisonViewModel = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Pension",
    Some(100),
    400,
    Some("1100L"),
    "PENSION-1122",
    true,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "475",
    payeNumber = "GA82452"
  )

  val expectedPensionVmUpdateInProgress = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Pension",
    Some(100),
    400,
    Some("1100L"),
    "PENSION-1122",
    true,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "475",
    payeNumber = "GA82452",
    isUpdateInProgress = true
  )

  val expectedEmploymentViewModel = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Employer",
    Some(100),
    400,
    Some("1100L"),
    "EMPLOYER-1122",
    false,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "123",
    payeNumber = "AB12345"
  )

  val expectedEmploymentVmUpdateInProgress = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Employer",
    Some(100),
    400,
    Some("1100L"),
    "EMPLOYER-1122",
    false,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "123",
    payeNumber = "AB12345",
    isUpdateInProgress = true
  )

  def createViewModel(
    taxCodeIncomeSources: Seq[TaxCodeIncome],
    employment: Employment,
    empId: Int = 1
  ): IncomeSourceSummaryViewModel =
    IncomeSourceSummaryViewModel.applyOld(
      empId,
      "User Name",
      taxCodeIncomeSources,
      employment,
      false,
      true,
      None
    )

  "IncomeSourceSummaryViewModel apply method" must {
    "return pension details" when {
      "component type is pension" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "1100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("PENSION-1122"),
          Some(LocalDate.now()),
          None,
          Seq(annualAccount),
          "475",
          "GA82452",
          2,
          None,
          false,
          false,
          EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment)

        model mustBe expectedPenisonViewModel

      }
    }

    "return income details" when {
      "component type is employment" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", OtherBasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2", OtherBasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          Some(LocalDate.now()),
          None,
          Seq(annualAccount),
          "123",
          "AB12345",
          2,
          None,
          false,
          false,
          EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment)

        model mustBe expectedEmploymentViewModel
      }
    }

    "throws exception" when {
      "employment income sources are not present" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, None, 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          Some(LocalDate.now()),
          None,
          Seq(annualAccount),
          "",
          "",
          2,
          None,
          false,
          false,
          EmploymentIncome
        )

        val exception = the[RuntimeException] thrownBy createViewModel(taxCodeIncomeSources, employment)

        exception.getMessage mustBe "Income details not found for employment id 1"
      }
    }

    "generate a view model with isUpdateInProgress set to true" when {
      "update is in progress for employment as the taxCodeIncomeSource amount is different to the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          empId: Int = 1
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel.applyOld(
            empId,
            "User Name",
            taxCodeIncomeSources,
            employment,
            false,
            true,
            Some(300)
          )

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", OtherBasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2", OtherBasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          Some(LocalDate.now()),
          None,
          Seq(annualAccount),
          "123",
          "AB12345",
          2,
          None,
          false,
          false,
          EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment)

        model mustBe expectedEmploymentVmUpdateInProgress
      }

      "update is in progress for pension as the taxCodeIncomeSource amount is different to the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          empId: Int = 1
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel.applyOld(
            empId,
            "User Name",
            taxCodeIncomeSources,
            employment,
            false,
            true,
            Some(300)
          )

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "1100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("PENSION-1122"),
          Some(LocalDate.now()),
          None,
          Seq(annualAccount),
          "475",
          "GA82452",
          2,
          None,
          false,
          false,
          EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment)

        model mustBe expectedPensionVmUpdateInProgress
      }
    }

    "generate a view model with isUpdateInProgress set to false" when {
      "update is not in progress for employment as the taxCodeIncomeSource amount is the same as the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          empId: Int = 1
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel.applyOld(
            empId,
            "User Name",
            taxCodeIncomeSources,
            employment,
            false,
            true,
            Some(100)
          )

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", OtherBasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2", OtherBasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          Some(LocalDate.now()),
          None,
          Seq(annualAccount),
          "123",
          "AB12345",
          2,
          None,
          false,
          false,
          EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment)

        model mustBe expectedEmploymentViewModel
      }
      "update is not in progress for pension as the taxCodeIncomeSource amount is the same as the cache amount" in {
        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          empId: Int = 1
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel.applyOld(
            empId,
            "User Name",
            taxCodeIncomeSources,
            employment,
            false,
            true,
            Some(100)
          )

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "1100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("PENSION-1122"),
          Some(LocalDate.now()),
          None,
          Seq(annualAccount),
          "475",
          "GA82452",
          2,
          None,
          false,
          false,
          EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment)

        model mustBe expectedPenisonViewModel

      }
      "cacheUpdatedIncomeAmount is a None" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "1100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("PENSION-1122"),
          Some(LocalDate.now()),
          None,
          Seq(annualAccount),
          "475",
          "GA82452",
          2,
          None,
          false,
          false,
          EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment)

        model mustBe expectedPenisonViewModel
      }
    }
  }
}
