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

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import utils.BaseSpec

import java.time.LocalDate

class IncomeSourceSummaryViewModelNewSpec extends BaseSpec {

  private val emptyBenefits = Benefits(Seq.empty[CompanyCarBenefit], Seq.empty[GenericBenefit])
  private val firstPayment = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  private val secondPayment = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  private val thirdPayment = Payment(LocalDate.now.minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
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
    benefits: Benefits,
    empId: Int = 1
  ): IncomeSourceSummaryViewModel =
    IncomeSourceSummaryViewModel.applyNew(
      empId = empId,
      displayName = "User Name",
      taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
      employment = employment,
      payments = payments,
      benefits = benefits,
      estimatedPayJourneyCompleted = false,
      rtiAvailable = true,
      applicationConfig = appConfig,
      cacheUpdatedIncomeAmount = None
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
          annualAccounts = Seq.empty,
          taxDistrictNumber = "475",
          payeNumber = "GA82452",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = true,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

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
          annualAccounts = Seq.empty,
          taxDistrictNumber = "123",
          payeNumber = "AB12345",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

        model mustBe expectedEmploymentViewModel
        /*
         IncomeSourceSummaryViewModel(1, "User Name", "test employment", Some(100), 400, Some("1100L"), "EMPLOYER-1122", false, List(), true, false, true, "123", "AB12345", false) was not equal to
         IncomeSourceSummaryViewModel(1, "User Name", "Employer", Some(100), 400, Some("1100L"), "EMPLOYER-1122", false, List(), true, false, true, "123", "AB12345", false) (IncomeSourceSummaryViewModelNewSpec.scala:178)

         */
      }
    }

    "throws exception" when {
//      "employment income sources are not present" in {
//        val taxCodeIncomeSources = Seq(
//          TaxCodeIncome(EmploymentIncome, None, 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live),
//          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2", Week1Month1BasisOfOperation, Live)
//        )
//
//        val employment = Employment(
//          name = "test employment",
//          employmentStatus = Live,
//          payrollNumber = Some("EMPLOYER-1122"),
//          startDate = Some(LocalDate.now()),
//          endDate = None,
//          annualAccounts = Seq(annualAccount),
//          taxDistrictNumber = "",
//          payeNumber = "",
//          sequenceNumber = 2,
//          cessationPay = None,
//          hasPayrolledBenefit = false,
//          receivingOccupationalPension = false,
//          employmentType = EmploymentIncome
//        )
//
//        val exception = the[RuntimeException] thrownBy createViewModel(taxCodeIncomeSources, employment, emptyBenefits)
//
//        exception.getMessage mustBe "Income details not found for employment id 1"
//      }
    }

    "generate an empty sequence of company benefit view models" when {
      "no company benefits are present" in {

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("EMPLOYER-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          annualAccounts = Seq.empty,
          taxDistrictNumber = "",
          payeNumber = "",
          sequenceNumber = 1,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

        model.benefits mustBe Seq.empty[CompanyBenefitViewModel]
      }

      "company benefits are present, but not for the supplied employment id" in {

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(7), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("EMPLOYER-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          annualAccounts = Seq.empty,
          taxDistrictNumber = "",
          payeNumber = "",
          sequenceNumber = 7,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val companyCars = Seq(
          CompanyCarBenefit(
            2,
            BigDecimal(200.22),
            Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))
          )
        )
        val otherBenefits = Seq(
          GenericBenefit(MedicalInsurance, Some(2), BigDecimal(321.12)),
          GenericBenefit(Entertaining, None, BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)
        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), benefits, empId = 7)

        model.benefits mustBe Seq.empty[CompanyBenefitViewModel]
      }
    }

    "generate a sequence of company benefit view models with appropriate content" when {
      "company benefits are present, and associated with the supplied employment id" in {

        val employmentId = 1

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(
            EmploymentIncome,
            Some(employmentId),
            100,
            "Test",
            "1100L",
            "Employer",
            Week1Month1BasisOfOperation,
            Live
          )
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("EMPLOYER-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          annualAccounts = Seq.empty,
          taxDistrictNumber = "",
          payeNumber = "",
          sequenceNumber = employmentId,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val companyCars = Seq(
          CompanyCarBenefit(
            employmentId,
            BigDecimal(200.22),
            Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))
          )
        )
        val otherBenefits = Seq(
          GenericBenefit(MedicalInsurance, Some(employmentId), BigDecimal(321.12)),
          GenericBenefit(Entertaining, Some(employmentId), BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), benefits)

        model.benefits mustBe Seq(
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"),
            BigDecimal(200.22),
            appConfig.cocarFrontendUrl
          ),
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"),
            BigDecimal(321.12),
            controllers.routes.ExternalServiceRedirectController
              .auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform)
              .url
          ),
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.Entertaining"),
            BigDecimal(120653.99),
            controllers.benefits.routes.CompanyBenefitController
              .redirectCompanyBenefitSelection(employmentId, Entertaining)
              .url
          )
        )
      }
      "company benefits are present with car fuel benefit, and associated with the supplied employment id" in {

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("EMPLOYER-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          annualAccounts = Seq.empty,
          taxDistrictNumber = "",
          payeNumber = "",
          sequenceNumber = 1,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val companyCars = Seq(
          CompanyCarBenefit(1, BigDecimal(200.22), Seq(CompanyCar(1, "transit", true, Some(LocalDate.now), None, None)))
        )
        val otherBenefits = Seq(
          GenericBenefit(CarFuelBenefit, Some(1), BigDecimal(200.22)),
          GenericBenefit(MedicalInsurance, Some(1), BigDecimal(321.12)),
          GenericBenefit(Entertaining, Some(1), BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), benefits)

        model.benefits must contain theSameElementsAs Seq(
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"),
            BigDecimal(200.22),
            appConfig.cocarFrontendUrl
          ),
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit"),
            BigDecimal(200.22),
            appConfig.cocarFrontendUrl
          ),
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"),
            BigDecimal(321.12),
            controllers.routes.ExternalServiceRedirectController
              .auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform)
              .url
          ),
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.Entertaining"),
            BigDecimal(120653.99),
            controllers.benefits.routes.CompanyBenefitController.redirectCompanyBenefitSelection(1, Entertaining).url
          )
        )
      }
    }

    "generate a view model with the displayAddCompanyCar flag set to true" when {
      "no existing company car benefit is present" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("EMPLOYER-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          annualAccounts = Seq.empty,
          taxDistrictNumber = "",
          payeNumber = "",
          sequenceNumber = 1,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

        model.displayAddCompanyCarLink mustBe true
      }
    }

    "generate a view model with the displayAddCompanyCar flag set to false" when {
      "an existing company car benefit is present" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          name = "test employment",
          employmentStatus = Live,
          payrollNumber = Some("EMPLOYER-1122"),
          startDate = Some(LocalDate.now()),
          endDate = None,
          annualAccounts = Seq.empty,
          taxDistrictNumber = "",
          payeNumber = "",
          sequenceNumber = 1,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val companyCars = Seq(
          CompanyCarBenefit(
            1,
            BigDecimal(200.22),
            Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))
          )
        )
        val benefits = Benefits(companyCars, Seq.empty[GenericBenefit])
        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), benefits)
        model.displayAddCompanyCarLink mustBe false
      }
    }
    "generate a view model with isUpdateInProgress set to true" when {
      "update is in progress for employment as the taxCodeIncomeSource amount is different to the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          payments: Option[AnnualAccount],
          benefits: Benefits,
          empId: Int = 1
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel.applyNew(
            empId = empId,
            displayName = "User Name",
            taxCodeIncomeSources
              .find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
            employment = employment,
            payments = payments,
            benefits = benefits,
            estimatedPayJourneyCompleted = false,
            rtiAvailable = true,
            applicationConfig = appConfig,
            cacheUpdatedIncomeAmount = Some(300)
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
          annualAccounts = Seq.empty,
          taxDistrictNumber = "123",
          payeNumber = "AB12345",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = false,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

        model mustBe expectedEmploymentVmUpdateInProgress
        /*
        IncomeSourceSummaryViewModel(1, "User Name", "test employment", Some(100), 400, Some("100L"), "EMPLOYER-1122", false, List(), true, false, true, "123", "AB12345", true) was not equal to
        IncomeSourceSummaryViewModel(1, "User Name", "Employer", Some(100), 400, Some("1100L"), "EMPLOYER-1122", false, List(), true, false, true, "123", "AB12345", true) (IncomeSourceSummaryViewModelNewSpec.scala:523)

         */
      }

      "update is in progress for pension as the taxCodeIncomeSource amount is different to the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          payments: Option[AnnualAccount],
          benefits: Benefits,
          empId: Int = 1
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel.applyNew(
            empId = empId,
            displayName = "User Name",
            taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
            employment = employment,
            payments = payments,
            benefits = benefits,
            estimatedPayJourneyCompleted = false,
            rtiAvailable = true,
            applicationConfig = appConfig,
            cacheUpdatedIncomeAmount = Some(300)
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
          annualAccounts = Seq.empty,
          taxDistrictNumber = "475",
          payeNumber = "GA82452",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = true,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

        model mustBe expectedPensionVmUpdateInProgress
      }
    }

    "generate a view model with isUpdateInProgress set to false" when {
      "update is not in progress for employment as the taxCodeIncomeSource amount is the same as the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          payments: Option[AnnualAccount],
          benefits: Benefits,
          empId: Int = 1
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel.applyNew(
            empId = empId,
            displayName = "User Name",
            taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
            employment = employment,
            payments = payments,
            benefits = benefits,
            estimatedPayJourneyCompleted = false,
            rtiAvailable = true,
            applicationConfig = appConfig,
            cacheUpdatedIncomeAmount = Some(100)
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
          Seq.empty,
          "123",
          "AB12345",
          2,
          None,
          false,
          false,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

        model mustBe expectedEmploymentViewModel
      }
      "update is not in progress for pension as the taxCodeIncomeSource amount is the same as the cache amount" in {
        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          payments: Option[AnnualAccount],
          benefits: Benefits,
          empId: Int = 1
        ): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel.applyNew(
            empId = empId,
            displayName = "User Name",
            taxCodeIncomeSources.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)),
            employment = employment,
            payments = payments,
            benefits = benefits,
            estimatedPayJourneyCompleted = false,
            rtiAvailable = true,
            applicationConfig = appConfig,
            cacheUpdatedIncomeAmount = Some(100)
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
          annualAccounts = Seq.empty,
          taxDistrictNumber = "475",
          payeNumber = "GA82452",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = true,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

        model mustBe expectedPenisonViewModel

      }
      "cacheUpdatedIncomeAmount is a None" in {
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
          annualAccounts = Seq.empty,
          taxDistrictNumber = "475",
          payeNumber = "GA82452",
          sequenceNumber = 2,
          cessationPay = None,
          hasPayrolledBenefit = false,
          receivingOccupationalPension = true,
          employmentType = EmploymentIncome
        )

        val model = createViewModel(taxCodeIncomeSources, employment, Some(annualAccount), emptyBenefits)

        model mustBe expectedPenisonViewModel

      }
    }
  }
}
