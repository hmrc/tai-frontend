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

import java.time.LocalDate
import play.api.i18n.{I18nSupport, Messages}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import utils.BaseSpec

class IncomeSourceSummaryViewModelSpec extends BaseSpec {

  val emptyBenefits = Benefits(Seq.empty[CompanyCarBenefit], Seq.empty[GenericBenefit])
  val firstPayment = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  val secondPayment = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  val thirdPayment = Payment(LocalDate.now.minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  val latestPayment = Payment(LocalDate.now.minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)
  val annualAccount = AnnualAccount(
    uk.gov.hmrc.tai.model.TaxYear(),
    Available,
    Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
    Nil)
  val estimatedPayJourneyCompleted = false

  val expectedPenisonViewModel = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Pension",
    100,
    400,
    "1100L",
    "PENSION-1122",
    true,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrctNumber = "475",
    payeNumber = "GA82452"
  )

  val expectedPensionVmUpdateInProgress = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Pension",
    100,
    400,
    "1100L",
    "PENSION-1122",
    true,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrctNumber = "475",
    payeNumber = "GA82452",
    isUpdateInProgress = true
  )

  val expectedEmploymentViewModel = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Employer",
    100,
    400,
    "1100L",
    "EMPLOYER-1122",
    false,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrctNumber = "123",
    payeNumber = "AB12345"
  )

  val expectedEmploymentVmUpdateInProgress = IncomeSourceSummaryViewModel(
    1,
    "User Name",
    "Employer",
    100,
    400,
    "1100L",
    "EMPLOYER-1122",
    false,
    estimatedPayJourneyCompleted = false,
    rtiAvailable = true,
    taxDistrctNumber = "123",
    payeNumber = "AB12345",
    isUpdateInProgress = true
  )

  def createViewModel(
    taxCodeIncomeSources: Seq[TaxCodeIncome],
    employment: Employment,
    benefits: Benefits,
    empId: Int = 1): IncomeSourceSummaryViewModel =
    IncomeSourceSummaryViewModel(
      empId,
      "User Name",
      taxCodeIncomeSources,
      employment,
      benefits,
      false,
      true,
      appConfig,
      None)

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
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "475",
          "GA82452",
          2,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

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
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "123",
          "AB12345",
          2,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

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
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "",
          "",
          2,
          None,
          false,
          false)

        val exception = the[RuntimeException] thrownBy createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

        exception.getMessage mustBe "Income details not found for employment id 1"
      }
    }

    "generate an empty sequence of company benefit view models" when {
      "no company benefits are present" in {

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live))

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "",
          "",
          1,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

        model.benefits mustBe Seq.empty[CompanyBenefitViewModel]
      }

      "company benefits are present, but not for the supplied employment id" in {

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(7), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live))

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "",
          "",
          7,
          None,
          false,
          false)

        val companyCars = Seq(
          CompanyCarBenefit(
            2,
            BigDecimal(200.22),
            Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))))
        val otherBenefits = Seq(
          GenericBenefit(MedicalInsurance, Some(2), BigDecimal(321.12)),
          GenericBenefit(Entertaining, None, BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)
        val model = createViewModel(taxCodeIncomeSources, employment, benefits, empId = 7)

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
            Live))

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "",
          "",
          employmentId,
          None,
          false,
          false)

        val companyCars = Seq(
          CompanyCarBenefit(
            employmentId,
            BigDecimal(200.22),
            Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))))
        val otherBenefits = Seq(
          GenericBenefit(MedicalInsurance, Some(employmentId), BigDecimal(321.12)),
          GenericBenefit(Entertaining, Some(employmentId), BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)

        val model = createViewModel(taxCodeIncomeSources, employment, benefits)

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
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live))

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "",
          "",
          1,
          None,
          false,
          false)

        val companyCars = Seq(
          CompanyCarBenefit(
            1,
            BigDecimal(200.22),
            Seq(CompanyCar(1, "transit", true, Some(LocalDate.now), None, None))))
        val otherBenefits = Seq(
          GenericBenefit(CarFuelBenefit, Some(1), BigDecimal(200.22)),
          GenericBenefit(MedicalInsurance, Some(1), BigDecimal(321.12)),
          GenericBenefit(Entertaining, Some(1), BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)

        val model = createViewModel(taxCodeIncomeSources, employment, benefits)

        model.benefits must contain theSameElementsAs Seq(
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"),
            BigDecimal(200.22),
            appConfig.cocarFrontendUrl
          ),
          CompanyBenefitViewModel(
            Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit"),
            BigDecimal(200.22),
            appConfig.cocarFrontendUrl),
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
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live))

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "",
          "",
          1,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

        model.displayAddCompanyCarLink mustBe true
      }
    }

    "generate a view model with the displayAddCompanyCar flag set to false" when {
      "an existing company car benefit is present" in {
        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOfOperation, Live))

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "",
          "",
          1,
          None,
          false,
          false)

        val companyCars = Seq(
          CompanyCarBenefit(
            1,
            BigDecimal(200.22),
            Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))))
        val benefits = Benefits(companyCars, Seq.empty[GenericBenefit])
        val model = createViewModel(taxCodeIncomeSources, employment, benefits)
        model.displayAddCompanyCarLink mustBe false
      }
    }
    "generate a view model with isUpdateInProgress set to true" when {
      "update is in progress for employment as the taxCodeIncomeSource amount is different to the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          benefits: Benefits,
          empId: Int = 1): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel(
            empId,
            "User Name",
            taxCodeIncomeSources,
            employment,
            benefits,
            false,
            true,
            appConfig,
            Some(300))

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", OtherBasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2", OtherBasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "123",
          "AB12345",
          2,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

        model mustBe expectedEmploymentVmUpdateInProgress
      }

      "update is in progress for pension as the taxCodeIncomeSource amount is different to the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          benefits: Benefits,
          empId: Int = 1): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel(
            empId,
            "User Name",
            taxCodeIncomeSources,
            employment,
            benefits,
            false,
            true,
            appConfig,
            Some(300))

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "1100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("PENSION-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "475",
          "GA82452",
          2,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

        model mustBe expectedPensionVmUpdateInProgress
      }
    }

    "generate a view model with isUpdateInProgress set to false" when {
      "update is not in progress for employment as the taxCodeIncomeSource amount is the same as the cache amount" in {

        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          benefits: Benefits,
          empId: Int = 1): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel(
            empId,
            "User Name",
            taxCodeIncomeSources,
            employment,
            benefits,
            false,
            true,
            appConfig,
            Some(100))

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", OtherBasisOfOperation, Live),
          TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2", OtherBasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("EMPLOYER-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "123",
          "AB12345",
          2,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

        model mustBe expectedEmploymentViewModel
      }
      "update is not in progress for pension as the taxCodeIncomeSource amount is the same as the cache amount" in {
        def createViewModel(
          taxCodeIncomeSources: Seq[TaxCodeIncome],
          employment: Employment,
          benefits: Benefits,
          empId: Int = 1): IncomeSourceSummaryViewModel =
          IncomeSourceSummaryViewModel(
            empId,
            "User Name",
            taxCodeIncomeSources,
            employment,
            benefits,
            false,
            true,
            appConfig,
            Some(100))

        val taxCodeIncomeSources = Seq(
          TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "1100L", "Pension", Week1Month1BasisOfOperation, Live),
          TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "100L", "Pension2", Week1Month1BasisOfOperation, Live)
        )

        val employment = Employment(
          "test employment",
          Live,
          Some("PENSION-1122"),
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "475",
          "GA82452",
          2,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

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
          LocalDate.now(),
          None,
          Seq(annualAccount),
          "475",
          "GA82452",
          2,
          None,
          false,
          false)

        val model = createViewModel(taxCodeIncomeSources, employment, emptyBenefits)

        model mustBe expectedPenisonViewModel
      }
    }
  }
}
