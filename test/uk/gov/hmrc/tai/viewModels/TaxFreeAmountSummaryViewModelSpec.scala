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

import controllers.routes
import org.mockito.Mockito.when
import play.api.i18n.Messages
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxFreeAmountDetails
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import utils.BaseSpec

class TaxFreeAmountSummaryViewModelSpec extends BaseSpec {

  "TaxFreeAmountSummaryRowViewModel apply method" must {
    "build a label generated specifically to the coding component" in {
      val personalAllowance     = TaxFreeAmountSummaryRowViewModel(
        CodingComponent(PersonalAllowancePA, Some(123), 111, "PersonalAllowancePA"),
        taxFreeAmountDetails,
        appConfig
      )
      val foreignDividendIncome = TaxFreeAmountSummaryRowViewModel(
        CodingComponent(ForeignDividendIncome, Some(123), 111, "ForeignDividendIncome"),
        taxFreeAmountDetails,
        appConfig
      )

      personalAllowance.label.value mustBe Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA")
      foreignDividendIncome.label.value mustBe Messages("tai.taxFreeAmount.table.taxComponent.ForeignDividendIncome")
    }

    "build a value formatted with pound prefix" when {
      "the value in negative" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(ForeignDividendIncome, Some(123), -11500, "ForeignDividendIncome"),
          taxFreeAmountDetails,
          appConfig
        )
        row.value mustBe "£11,500"
      }

      "the value in positive" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(ForeignDividendIncome, Some(123), 2500, "ForeignDividendIncome"),
          taxFreeAmountDetails,
          appConfig
        )
        row.value mustBe "£2,500"
      }
    }

    "build a NOT displayable link" when {
      "coding component is another type" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(DividendTax, Some(1), 1, "HotelAndMealExpenses"),
          taxFreeAmountDetails,
          appConfig
        )
        row.link mustBe ChangeLinkViewModel(false)
      }
    }

    "build a displayable Medical Insurance LinkViewModel" when {
      "coding component is Medical Insurance" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(MedicalInsurance, Some(234), 11500, "MedicalInsurance"),
          taxFreeAmountDetails,
          appConfig
        )
        val url = routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform)
          .url
        row.link mustBe ChangeLinkViewModel(
          true,
          Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"),
          url
        )
      }
    }
    "build a displayable Marriage Allowance LinkViewModel" when {
      "coding component is Marriage Allowance Received" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(MarriageAllowanceReceived, Some(234), 11500, "MarriageAllowanceReceived"),
          taxFreeAmountDetails,
          appConfig
        )
        val url = routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.MarriageAllowanceService)
          .url
        row.link mustBe ChangeLinkViewModel(
          true,
          Messages("tai.taxFreeAmount.table.taxComponent.MarriageAllowanceReceived"),
          url
        )
      }
      "coding component is Marriage Allowance Transferred" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(MarriageAllowanceTransferred, Some(234), 11500, "MarriageAllowanceTransferred"),
          taxFreeAmountDetails,
          appConfig
        )
        val url = routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.MarriageAllowanceService)
          .url
        row.link mustBe ChangeLinkViewModel(
          true,
          Messages("tai.taxFreeAmount.table.taxComponent.MarriageAllowanceTransferred"),
          url
        )
      }
    }
    "build a displayable Car Benefit LinkViewModel" which {
      "has the employment id added to it" when {
        "tax component is CarBenefit and employment id is present" in {
          val row = TaxFreeAmountSummaryRowViewModel(
            CodingComponent(CarBenefit, Some(234), 11500, "CarBenefit"),
            taxFreeAmountDetails,
            appConfig
          )
          val url = appConfig.cocarFrontendUrl
          row.link mustBe ChangeLinkViewModel(true, Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"), url)
        }
      }
      "has zero as employment id" when {
        "tax component is CarBenefit and employment id is NOT present" in {
          val row = TaxFreeAmountSummaryRowViewModel(
            CodingComponent(CarBenefit, None, 11500, "CarBenefit"),
            taxFreeAmountDetails,
            appConfig
          )
          val url = appConfig.cocarFrontendUrl
          row.link mustBe ChangeLinkViewModel(true, Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"), url)
        }
      }
    }

    "build a displayable Company Benefit LinkViewModel" which {
      "has the employment id and benefit type added to it" when {
        "coding component is of type BenefitComponentType" in {
          val row = TaxFreeAmountSummaryRowViewModel(
            CodingComponent(TaxableExpensesBenefit, Some(234), 11500, "TaxableExpensesBenefit"),
            taxFreeAmountDetails,
            appConfig
          )
          val url = controllers.benefits.routes.CompanyBenefitController
            .redirectCompanyBenefitSelection(234, TaxableExpensesBenefit)
            .url
          row.link mustBe ChangeLinkViewModel(
            true,
            Messages("tai.taxFreeAmount.table.taxComponent.TaxableExpensesBenefit"),
            url
          )
        }
      }
    }

    "build a displayable Car Fuel Benefit LinkViewModel" which {
      "has the employment id added to it" when {
        "tax component is CarFuelBenefit and employment id is present" in {
          val row = TaxFreeAmountSummaryRowViewModel(
            CodingComponent(CarFuelBenefit, Some(234), 11500, "CarFuelBenefit"),
            taxFreeAmountDetails,
            appConfig
          )
          val url = appConfig.cocarFrontendUrl
          row.link mustBe ChangeLinkViewModel(
            true,
            Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit"),
            url
          )
        }
      }
      "has zero as employment id" when {
        "tax component is CarFuelBenefit and employment id is NOT present" in {
          val row = TaxFreeAmountSummaryRowViewModel(
            CodingComponent(CarFuelBenefit, None, 11500, "CarFuelBenefit"),
            taxFreeAmountDetails,
            appConfig
          )
          val url = appConfig.cocarFrontendUrl
          row.link mustBe ChangeLinkViewModel(
            true,
            Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit"),
            url
          )
        }
      }
    }

    "build a displayable Allowance LinkViewModel" when {
      "coding component is of type AllowanceComponentType" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(PersonalPensionPayments, Some(1), 1, "PersonalPensionPayments"),
          taxFreeAmountDetails,
          appConfig
        )
        val url = appConfig.taxFreeAllowanceLinkUrl
        row.link mustBe ChangeLinkViewModel(
          true,
          Messages("tai.taxFreeAmount.table.taxComponent.PersonalPensionPayments"),
          url
        )
      }
    }

    "append employer name in label of the row" when {
      "employment id is available in employment name map" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(ForeignDividendIncome, Some(1), 111, "ForeignDividendIncome"),
          taxFreeAmountDetails,
          appConfig
        )
        row.label.value mustBe
          s"${Messages("tai.taxFreeAmount.table.taxComponent.ForeignDividendIncome")} ${Messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentNames(1))}"
      }

      "employment id is not available in employment name map" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(ForeignDividendIncome, Some(123), 111, "ForeignDividendIncome"),
          taxFreeAmountDetails,
          appConfig
        )
        row.label.value mustBe Messages("tai.taxFreeAmount.table.taxComponent.ForeignDividendIncome")
      }

      "employment id is not available in coding components" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(ForeignDividendIncome, None, 111, "ForeignDividendIncome"),
          taxFreeAmountDetails,
          appConfig
        )
        row.label.value mustBe Messages("tai.taxFreeAmount.table.taxComponent.ForeignDividendIncome")
      }
    }

    "display the company car make and model" when {
      "the coding component type is company car benefit" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(CarBenefit, Some(10), 11500, "CarBenefit"),
          taxFreeAmountDetails,
          appConfig
        )
        row.label.value mustBe Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit", "Make Model1")
      }

      "employment id is none" in {
        val row =
          TaxFreeAmountSummaryRowViewModel(
            CodingComponent(CarBenefit, None, 11500, "CarBenefit"),
            taxFreeAmountDetails,
            appConfig
          )
        row.label.value mustBe Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit", "Car benefit")
      }

      "employment id does not exist in company car benefits" in {
        val row = TaxFreeAmountSummaryRowViewModel(
          CodingComponent(CarBenefit, Some(1), 11500, "CarBenefit"),
          taxFreeAmountDetails,
          appConfig
        )
        row.label.value mustBe s"${Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit", "Car benefit")} ${Messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentNames(1))}"
      }
    }

    "display the HICBC row" when {
      val mockAppConfig = mock[ApplicationConfig]
      "the coding component type is HICBC and non empty link is in config" in {
        when(mockAppConfig.hicbcUpdateUrl).thenReturn("non empty link")

        val row = TaxFreeAmountSummaryRowViewModel(
          codingComponent = CodingComponent(
            componentType = HICBCPaye,
            employmentId = Some(10),
            amount = 11500,
            description = "HICBC PAYE",
            inputAmount = Some(BigDecimal(88))
          ),
          taxFreeAmountDetails = taxFreeAmountDetails,
          applicationConfig = mockAppConfig
        )
        row.label.value mustBe "High Income Child Benefit Charge – based on a charge of £88"
        row.link.href mustBe "non empty link"
        row.link.isDisplayed mustBe true
      }

      "the coding component type is HICBC and empty link is in config" in {
        when(mockAppConfig.hicbcUpdateUrl).thenReturn("")

        val row = TaxFreeAmountSummaryRowViewModel(
          codingComponent = CodingComponent(
            componentType = HICBCPaye,
            employmentId = Some(10),
            amount = 11500,
            description = "HICBC PAYE",
            inputAmount = Some(BigDecimal(77))
          ),
          taxFreeAmountDetails = taxFreeAmountDetails,
          applicationConfig = mockAppConfig
        )
        row.label.value mustBe "High Income Child Benefit Charge – based on a charge of £77"
        row.link.href mustBe ""
        row.link.isDisplayed mustBe false
      }
    }
  }

  val employmentNames = Map(1 -> "Employer1", 2 -> "Employer2", 3 -> "Employer3")

  val companyCarBenefit10  =
    CompanyCarBenefit(10, 1000, List(CompanyCar(10, "Make Model1", true, None, None, None)), Some(1))
  val companyCarBenefit12  =
    CompanyCarBenefit(12, 1000, List(CompanyCar(10, "Make Model2", true, None, None, None)), Some(1))
  val companyCarBenefits   = Seq(companyCarBenefit10, companyCarBenefit12)
  val taxBand              = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
  val incomeCatergories    = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
  val totalTax: TotalTax   = TotalTax(1000, Seq(incomeCatergories), None, None, None)
  val taxFreeAmountDetails = TaxFreeAmountDetails(employmentNames, companyCarBenefits, totalTax)
}
