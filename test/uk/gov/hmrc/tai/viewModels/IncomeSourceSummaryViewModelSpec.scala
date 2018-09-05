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

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome, Week1Month1BasisOperation}
import uk.gov.hmrc.tai.util.TaiConstants

class IncomeSourceSummaryViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "IncomeSourceSummaryViewModel apply method" must {
    "return pension details" when {
      "component type is pension" in {
        val taxCodeIncomeSources = Seq(TaxCodeIncome(PensionIncome, Some(1), 100, "Test", "1100L", "Pension",
          Week1Month1BasisOperation, Live), TaxCodeIncome(PensionIncome, Some(2), 100, "Test", "100L", "Pension2",
          Week1Month1BasisOperation, Live))

        val employment = Employment("test employment", Some("PENSION-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", 2, None, false, false)

        val model = IncomeSourceSummaryViewModel(1, "User Name", taxCodeIncomeSources, employment, emptyBenefits)

        model mustBe IncomeSourceSummaryViewModel(1, "User Name", "Pension", 100, 400, "1100LX", "PENSION-1122", true)

      }
    }

    "return income details" when {
      "component type is employment" in {
        val taxCodeIncomeSources = Seq(TaxCodeIncome(EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer",
          OtherBasisOperation, Live), TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2",
          OtherBasisOperation, Live))

        val employment = Employment("test employment", Some("EMPLOYER-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", 2, None, false, false)

        val model = IncomeSourceSummaryViewModel(1, "User Name", taxCodeIncomeSources, employment, emptyBenefits)

        model mustBe IncomeSourceSummaryViewModel(1, "User Name", "Employer", 100, 400, "1100L", "EMPLOYER-1122", false)
      }
    }

    "throws exception" when {
      "employment income sources are not present" in {
        val taxCodeIncomeSources = Seq(TaxCodeIncome(EmploymentIncome, None, 100, "Test", "1100L", "Employer",
          Week1Month1BasisOperation, Live), TaxCodeIncome(EmploymentIncome, Some(2), 100, "Test", "100L", "Employer2",
          Week1Month1BasisOperation, Live))

        val employment = Employment("test employment", Some("EMPLOYER-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", 2, None, false, false)

        val exception = the[RuntimeException] thrownBy IncomeSourceSummaryViewModel(1, "User Name", taxCodeIncomeSources, employment, emptyBenefits)

        exception.getMessage mustBe "Income details not found for employment id 1"
      }
    }

    "generate an empty sequence of company benefit view models" when {
      "no company benefits are present" in {

        val taxCodeIncomeSources = Seq(TaxCodeIncome(
          EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOperation, Live))

        val employment = Employment("test employment", Some("EMPLOYER-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", 1, None, false, false)

        val model = IncomeSourceSummaryViewModel(1, "User Name", taxCodeIncomeSources, employment, emptyBenefits)
        model.benefits mustBe Seq.empty[CompanyBenefitViewModel]
      }

      "company benefits are present, but not for the supplied employment id" in {

        val taxCodeIncomeSources = Seq(TaxCodeIncome(
          EmploymentIncome, Some(7), 100, "Test", "1100L", "Employer", Week1Month1BasisOperation, Live))

        val employment = Employment("test employment", Some("EMPLOYER-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", 7, None, false, false)

        val companyCars = Seq(CompanyCarBenefit(2, BigDecimal(200.22), Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))))
        val otherBenefits = Seq(
          GenericBenefit(MedicalInsurance, Some(2), BigDecimal(321.12)),
          GenericBenefit(Entertaining, None, BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)

        val model = IncomeSourceSummaryViewModel(7, "User Name", taxCodeIncomeSources, employment, benefits)
        model.benefits mustBe Seq.empty[CompanyBenefitViewModel]
      }
    }

    "generate a sequence of company benefit view models with appropriate content" when {
      "company benefits are present, and associated with the supplied employment id" in {

        val employmentId = 1

        val taxCodeIncomeSources = Seq(TaxCodeIncome(
          EmploymentIncome, Some(employmentId), 100, "Test", "1100L", "Employer", Week1Month1BasisOperation, Live))

        val employment = Employment("test employment", Some("EMPLOYER-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", employmentId, None, false, false)

        val companyCars = Seq(CompanyCarBenefit(employmentId, BigDecimal(200.22), Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))))
        val otherBenefits = Seq(
          GenericBenefit(MedicalInsurance, Some(employmentId), BigDecimal(321.12)),
          GenericBenefit(Entertaining, Some(employmentId), BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)

        val model = IncomeSourceSummaryViewModel(employmentId, "User Name", taxCodeIncomeSources, employment, benefits)
        model.benefits mustBe Seq(
          CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"), BigDecimal(200.22), controllers.routes.CompanyCarController.redirectCompanyCarSelection(1).url),
          CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"), BigDecimal(321.12), controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform).url),
          CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.Entertaining"), BigDecimal(120653.99), controllers.benefits.routes.CompanyBenefitController.redirectCompanyBenefitSelection(employmentId, Entertaining).url)
        )
      }
      "company benefits are present with car fuel benefit, and associated with the supplied employment id" in {

        val taxCodeIncomeSources = Seq(TaxCodeIncome(
          EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOperation, Live))

        val employment = Employment("test employment", Some("EMPLOYER-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", 1, None, false, false)

        val companyCars = Seq(CompanyCarBenefit(1, BigDecimal(200.22), Seq(CompanyCar(1, "transit", true, Some(LocalDate.now), None, None))))
        val otherBenefits = Seq(
          GenericBenefit(CarFuelBenefit, Some(1), BigDecimal(200.22)),
          GenericBenefit(MedicalInsurance, Some(1), BigDecimal(321.12)),
          GenericBenefit(Entertaining, Some(1), BigDecimal(120653.99))
        )
        val benefits = Benefits(companyCars, otherBenefits)

        val model = IncomeSourceSummaryViewModel(1, "User Name", taxCodeIncomeSources, employment, benefits)
        model.benefits must contain theSameElementsAs Seq(
          CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"), BigDecimal(200.22), controllers.routes.CompanyCarController.redirectCompanyCarSelection(1).url),
          CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit"), BigDecimal(200.22), ApplicationConfig.companyCarFuelBenefitUrl),
          CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"), BigDecimal(321.12), controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform).url),
          CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.Entertaining"), BigDecimal(120653.99), controllers.benefits.routes.CompanyBenefitController.redirectCompanyBenefitSelection(1, Entertaining).url)
        )
      }
    }

    "generate a view model with the displayAddCompanyCar flag set to true" when {
      "no existing company car benefit is present" in {
        val taxCodeIncomeSources = Seq(TaxCodeIncome(
          EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOperation, Live))

        val employment = Employment("test employment", Some("EMPLOYER-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", 1, None, false, false)

        val model = IncomeSourceSummaryViewModel(1, "User Name", taxCodeIncomeSources, employment, emptyBenefits)
        model.displayAddCompanyCarLink mustBe true
      }
    }

    "generate a view model with the displayAddCompanyCar flag set to false" when {
      "an existing company car benefit is present" in {
        val taxCodeIncomeSources = Seq(TaxCodeIncome(
          EmploymentIncome, Some(1), 100, "Test", "1100L", "Employer", Week1Month1BasisOperation, Live))

        val employment = Employment("test employment", Some("EMPLOYER-1122"), LocalDate.now(),
          None, Seq(annualAccount), "", "", 1, None, false, false)

        val companyCars = Seq(CompanyCarBenefit(1, BigDecimal(200.22), Seq(CompanyCar(1, "transit", false, Some(LocalDate.now), None, None))))
        val benefits = Benefits(companyCars, Seq.empty[GenericBenefit])
        val model = IncomeSourceSummaryViewModel(1, "User Name", taxCodeIncomeSources, employment, benefits)

        model.displayAddCompanyCarLink mustBe false
      }
    }
  }

  val emptyBenefits = Benefits(Seq.empty[CompanyCarBenefit], Seq.empty[GenericBenefit])
  val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  val latestPayment = Payment(new LocalDate().minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)
  val annualAccount = AnnualAccount("KEY", uk.gov.hmrc.tai.model.TaxYear(), Available, Seq(latestPayment, secondPayment, thirdPayment, firstPayment), Nil)
}
