/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import utils.BaseSpec

import java.time.LocalDate

class IncomeSourceSummaryViewModelSpec extends BaseSpec {

  private val firstPayment  = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  private val secondPayment = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  private val thirdPayment  = Payment(LocalDate.now.minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  private val latestPayment = Payment(LocalDate.now.minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)

  private val annualAccount = AnnualAccount(
    7,
    uk.gov.hmrc.tai.model.TaxYear(),
    Available,
    Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
    Nil
  )

  private val expectedPenisonViewModel = IncomeSourceSummaryViewModel(
    empId = 1,
    displayName = "User Name",
    empOrPensionName = "Pension",
    estimatedTaxableIncome = Some(100),
    incomeReceivedToDate = 400,
    taxCode = Some("1100L"),
    pensionOrPayrollNumber = "PENSION-1122",
    isPension = true,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "475",
    payeNumber = "GA82452"
  )

  private val expectedPensionVmUpdateInProgress = IncomeSourceSummaryViewModel(
    empId = 1,
    displayName = "User Name",
    empOrPensionName = "test employment",
    estimatedTaxableIncome = Some(100),
    incomeReceivedToDate = 400,
    taxCode = Some("100L"),
    pensionOrPayrollNumber = "PENSION-1122",
    isPension = true,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "475",
    payeNumber = "GA82452",
    isUpdateInProgress = true
  )

  private val expectedEmploymentViewModel = IncomeSourceSummaryViewModel(
    empId = 1,
    displayName = "User Name",
    empOrPensionName = "test employment",
    estimatedTaxableIncome = Some(100),
    incomeReceivedToDate = 400,
    taxCode = Some("1100L"),
    pensionOrPayrollNumber = "EMPLOYER-1122",
    isPension = false,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "123",
    payeNumber = "AB12345"
  )

  private val expectedEmploymentVmUpdateInProgress = IncomeSourceSummaryViewModel(
    empId = 1,
    displayName = "User Name",
    empOrPensionName = "test employment",
    estimatedTaxableIncome = Some(100),
    incomeReceivedToDate = 400,
    taxCode = Some("100L"),
    pensionOrPayrollNumber = "EMPLOYER-1122",
    isPension = false,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrictNumber = "123",
    payeNumber = "AB12345",
    isUpdateInProgress = true
  )

  private def createViewModel(
    taxCodeIncomeSources: Seq[TaxCodeIncome],
    employment: Employment,
    payments: Option[AnnualAccount],
    empId: Int = 1,
    overrides: Map[Int, BigDecimal] = Map.empty
  ): IncomeSourceSummaryViewModel =
    IncomeSourceSummaryViewModel(
      empId = empId,
      displayName = "User Name",
      optTaxCodeIncome = taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
      employment = employment,
      payments = payments,
      estimatedPayJourneyCompleted = false,
      rtiAvailable = true,
      cacheUpdatedIncomeAmount = None,
      estimatedPayOverrides = overrides
    )

  "IncomeSourceSummaryViewModel apply method" must {

    "return pension details" when {
      "employment receivingOccupationalPension is true" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "1100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "Pension",
          employmentStatus = Live,
          payrollNumber = Some("PENSION-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          taxDistrictNumber = "475",
          payeNumber = "GA82452",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = true,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount))
        model mustBe expectedPenisonViewModel
      }
    }

    "return income details" when {
      "component type is employment" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "100L", "Employer", OtherBasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "1100L", "Employer2", OtherBasisOfOperation, Live)
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("EMPLOYER-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          taxDistrictNumber = "123",
          payeNumber = "AB12345",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount))
        model mustBe expectedEmploymentViewModel
      }
    }

    "generate a view model with isUpdateInProgress set to true" when {
      "cache amount differs from HOD amount for employment" in {

        def createVM(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          payments: Option[AnnualAccount],
          empId: Int = 1,
          overrides: Map[Int, BigDecimal] = Map.empty
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel(
            empId = empId,
            displayName = "User Name",
            optTaxCodeIncome = taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
            employment = employment,
            payments = payments,
            estimatedPayJourneyCompleted = false,
            rtiAvailable = true,
            cacheUpdatedIncomeAmount = Some(300),
            estimatedPayOverrides = overrides
          )

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", OtherBasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2", OtherBasisOfOperation, Live)
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("EMPLOYER-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          taxDistrictNumber = "123",
          payeNumber = "AB12345",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val model = createVM(taxCodeIncomeSources, employment, Some(annualAccount))
        model mustBe expectedEmploymentVmUpdateInProgress
      }

      "cache amount differs from HOD amount for pension" in {

        def createVM(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          payments: Option[AnnualAccount],
          empId: Int = 1,
          overrides: Map[Int, BigDecimal] = Map.empty
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel(
            empId = empId,
            displayName = "User Name",
            optTaxCodeIncome = taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
            employment = employment,
            payments = payments,
            estimatedPayJourneyCompleted = false,
            rtiAvailable = true,
            cacheUpdatedIncomeAmount = Some(300),
            estimatedPayOverrides = overrides
          )

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "1100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("PENSION-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          taxDistrictNumber = "475",
          payeNumber = "GA82452",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = true,
          employmentType = EmploymentIncome
        )

        val model = createVM(taxCodeIncomeSources, employment, Some(annualAccount))
        model mustBe expectedPensionVmUpdateInProgress
      }
    }

    "generate a view model with isUpdateInProgress set to false" when {
      "cache amount equals HOD amount for employment" in {

        def createVM(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          payments: Option[AnnualAccount],
          empId: Int = 1,
          overrides: Map[Int, BigDecimal] = Map.empty
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel(
            empId = empId,
            displayName = "User Name",
            optTaxCodeIncome = taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
            employment = employment,
            payments = payments,
            estimatedPayJourneyCompleted = false,
            rtiAvailable = true,
            cacheUpdatedIncomeAmount = Some(100),
            estimatedPayOverrides = overrides
          )

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "100L", "Employer", OtherBasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "1100L", "Employer2", OtherBasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          Some(LocalDate.now()),
          None,
          "123",
          "AB12345",
          2,
          None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val model = createVM(taxCodeIncomeSources, employment, Some(annualAccount))
        model mustBe expectedEmploymentViewModel
      }

      "cache amount equals HOD amount for pension" in {

        def createVM(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          payments: Option[AnnualAccount],
          empId: Int = 1,
          overrides: Map[Int, BigDecimal] = Map.empty
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel(
            empId = empId,
            displayName = "User Name",
            optTaxCodeIncome = taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
            employment = employment,
            payments = payments,
            estimatedPayJourneyCompleted = false,
            rtiAvailable = true,
            cacheUpdatedIncomeAmount = Some(100),
            estimatedPayOverrides = overrides
          )

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "1100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "Pension",
          employmentStatus = Live,
          payrollNumber = Some("PENSION-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          taxDistrictNumber = "475",
          payeNumber = "GA82452",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = true,
          employmentType = EmploymentIncome
        )

        val model = createVM(taxCodeIncomeSources, employment, Some(annualAccount))
        model mustBe expectedPenisonViewModel
      }

      "cacheUpdatedIncomeAmount is None" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "1100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "Pension",
          employmentStatus = Live,
          payrollNumber = Some("PENSION-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          taxDistrictNumber = "475",
          payeNumber = "GA82452",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = true,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount))
        model mustBe expectedPenisonViewModel
      }
    }

    "use the override amount when present for the empId" in {
      val taxCodeIncomeSources = Seq(
        TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "123L", "Employer", OtherBasisOfOperation, Live)
      )

      val employment = Employment(
        name = "test employment",
        employmentStatus = Live,
        payrollNumber = Some("EMPLOYER-1122"),
        startDate = Some(LocalDate.now()),
        endDate = None,
        taxDistrictNumber = "123",
        payeNumber = "AB12345",
        sequenceNumber = 1,
        cessationPay = None,
        hasPayrolledBenefit = false,
        receivingOccupationalPension = false,
        employmentType = EmploymentIncome
      )

      val model = createViewModel(
        taxCodeIncomeSources,
        employment,
        Some(annualAccount),
        empId = 1,
        overrides = Map(1 -> BigDecimal(9999))
      )

      model.estimatedTaxableIncome mustBe Some(BigDecimal(9999))
    }
  }
}
