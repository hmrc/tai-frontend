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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Ceased, NonTaxCodeIncome, OtherNonTaxCodeIncome, PotentiallyCeased}
import uk.gov.hmrc.tai.util.constants.TaiConstants.{EmployeePensionIForm, InvestIncomeIform, OtherIncomeIform, StateBenefitsIform}
import utils.{BaseSpec, TaxAccountSummaryTestData}

import java.time.LocalDate

class IncomeSourceViewModelSpec extends BaseSpec with TaxAccountSummaryTestData {

  "IncomeSourceViewModel apply method" must {
    "return a viewmodel" which {
      "has the name field coming from employment model" in {
        val sut = IncomeSourceViewModel(taxCodeIncome, employment)
        sut.name mustBe employment.name
      }
      "has the amount field as positive formatted value coming from taxCodeIncome model" in {
        val sut = IncomeSourceViewModel(taxCodeIncome, employment)
        sut.amount mustBe Some("£1,111")
      }
      "has the amount field as negative formatted value coming from taxCodeIncome model" in {
        val taxCodeIncomeNegative = taxCodeIncome.copy(amount = -1111)
        val sut                   = IncomeSourceViewModel(taxCodeIncomeNegative, employment)
        sut.amount mustBe Some(s"${uk.gov.hmrc.tai.util.constants.TaiConstants.EncodedMinusSign}£1,111")
      }
      "has the amount field as zero formatted value coming from taxCodeIncome model" in {
        val taxCodeIncomeZero = taxCodeIncome.copy(amount = 0)
        val sut               = IncomeSourceViewModel(taxCodeIncomeZero, employment)
        sut.amount mustBe Some("£0")
      }
      "has the displayTaxCode field as true" when {
        "employment status is live" in {
          val sut = IncomeSourceViewModel(taxCodeIncome, employment)
          sut.displayTaxCode mustBe true
        }
      }
      "has the displayTaxCode field as false" when {
        "employment status is not live" in {
          val ceasedEmployment            = employment.copy(employmentStatus = Ceased)
          val potantiallyCeasedEmployment = employment.copy(employmentStatus = PotentiallyCeased)
          val sut1                        = IncomeSourceViewModel(taxCodeIncome, ceasedEmployment)
          val sut2                        = IncomeSourceViewModel(taxCodeIncome, potantiallyCeasedEmployment)
          sut1.displayTaxCode mustBe false
          sut2.displayTaxCode mustBe false
        }
      }
      "has the payrollNumber field as empty and displayPayrollNumber as false" when {
        "employment model doesn't have payrollNo" in {
          val employmentWithoutPayrollNo = employment.copy(payrollNumber = None)
          val sut                        = IncomeSourceViewModel(taxCodeIncome, employmentWithoutPayrollNo)
          sut.payrollNumber mustBe ""
          sut.displayPayrollNumber mustBe false
        }
      }
      "has the payrollNumber field as the same as employment model and displayPayrollNumber as true" when {
        "employment model has payrollNo" in {
          val sut = IncomeSourceViewModel(taxCodeIncome, employment)
          sut.payrollNumber mustBe "123ABC"
          sut.displayPayrollNumber mustBe true
        }
      }
      "has the endDate field as empty and displayEndDate as false" when {
        "employment model doesn't have endDate and employment status is live" in {
          val employmentWithoutPayrollNo = employment.copy(endDate = None)
          val sut                        = IncomeSourceViewModel(taxCodeIncome, employmentWithoutPayrollNo)
          sut.endDate mustBe ""
          sut.displayEndDate mustBe false
        }
        "employment model has endDate and employment status is live" in {
          val employmentWithoutPayrollNo = employment.copy(endDate = Some(LocalDate.of(2018, 4, 21)))
          val sut                        = IncomeSourceViewModel(taxCodeIncome, employmentWithoutPayrollNo)
          sut.endDate mustBe "21 April 2018"
          sut.displayEndDate mustBe false
        }
        "employment model doesn't have endDate and employment status is ceased" in {
          val employmentWithoutPayrollNo = employment.copy(endDate = None)
          val taxCodeIncomeCeased        = taxCodeIncome.copy(status = Ceased)
          val sut                        = IncomeSourceViewModel(taxCodeIncomeCeased, employmentWithoutPayrollNo)
          sut.endDate mustBe ""
          sut.displayEndDate mustBe false
        }
        "employment model doesn't have endDate and employment status is potentially ceased" in {
          val employmentWithoutPayrollNo     = employment.copy(endDate = None)
          val taxCodeIncomePotentiallyCeased = taxCodeIncome.copy(status = PotentiallyCeased)
          val sut                            = IncomeSourceViewModel(taxCodeIncomePotentiallyCeased, employmentWithoutPayrollNo)
          sut.endDate mustBe ""
          sut.displayEndDate mustBe false
        }
      }
      "has formatted endDate field as same as employment model and displayEndDate as true" when {
        "employment model has endDate and employment status is ceased" in {
          val ceasedEmployment = employment.copy(employmentStatus = Ceased)
          val sut              = IncomeSourceViewModel(taxCodeIncome, ceasedEmployment)
          sut.endDate mustBe s"21 April ${TaxYear().next.year}"
          sut.displayEndDate mustBe true
        }
        "employment model has endDate and employment status is potentially ceased" in {
          val potentiallyCeasedEmployment = employment.copy(employmentStatus = PotentiallyCeased)
          val sut                         = IncomeSourceViewModel(taxCodeIncome, potentiallyCeasedEmployment)
          sut.endDate mustBe s"21 April ${TaxYear().next.year}"
          sut.displayEndDate mustBe true
        }
      }
      "has details link with employment and benefits label" when {
        "income source type is employment" in {
          val sut = IncomeSourceViewModel(taxCodeIncome, employment)
          sut.detailsLinkLabel mustBe Messages("tai.incomeTaxSummary.employmentAndBenefits.link")
          sut.detailsLinkUrl mustBe controllers.routes.IncomeSourceSummaryController
            .onPageLoad(employment.sequenceNumber)
            .url
        }
      }
      "has details link with employment only label for ceased employment" when {
        "income source type is employment" in {
          val sut = IncomeSourceViewModel(taxCodeIncome, ceasedEmployment)
          sut.detailsLinkLabel mustBe Messages("tai.incomeTaxSummary.employment.link")
          sut.detailsLinkUrl mustBe controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(1).url
        }
      }
      "has details link with pension label" when {
        "income source type is pension" in {
          val pension = taxCodeIncome.copy(componentType = PensionIncome)
          val sut     = IncomeSourceViewModel(pension, employment)
          sut.detailsLinkLabel mustBe Messages("tai.incomeTaxSummary.pension.link")
          sut.detailsLinkUrl mustBe controllers.routes.IncomeSourceSummaryController
            .onPageLoad(employment.sequenceNumber)
            .url
        }
      }
      "has details link with income label" when {
        "income source type is not pension or employment" in {
          val pension = taxCodeIncome.copy(componentType = JobSeekerAllowanceIncome)
          val sut     = IncomeSourceViewModel(pension, employment)
          sut.detailsLinkLabel mustBe Messages("tai.incomeTaxSummary.income.link")
        }
      }
      "has correct details link" when {
        val otherIncomesType         = OtherNonTaxCodeIncome(NonCodedIncome, None, 0, "OtherIncomes")
        val employmentPensionsType   = OtherNonTaxCodeIncome(OccupationalPension, None, 0, "EmploymentPensions")
        val taxableStateBenefitsType = OtherNonTaxCodeIncome(StatePension, None, 0, "TaxableStateBenefits")
        val savingAndInvestmentsType = OtherNonTaxCodeIncome(ForeignDividendIncome, None, 0, "SavingAndInvestments")
        val genericType              = OtherNonTaxCodeIncome(UntaxedInterestIncome, None, 0, "Generic")

        List(
          (otherIncomesType, OtherIncomeIform),
          (employmentPensionsType, EmployeePensionIForm),
          (taxableStateBenefitsType, StateBenefitsIform),
          (savingAndInvestmentsType, InvestIncomeIform)
        ) foreach { item =>
          s"non tax code income source type is ${item._1.description}" in {

            val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq(item._1))

            val sut = IncomeSourceViewModel(nonTaxCodeIncome)

            sut.head.detailsLinkUrl mustBe controllers.routes.AuditController.auditLinksToIForm(item._2).url
          }
        }
        s"non tax code income source type is generic" in {

          val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq(genericType))

          val sut = IncomeSourceViewModel(nonTaxCodeIncome)

          sut.head.displayDetailsLink mustBe false
        }
      }
    }
  }

  "createFromEmployment" must {
    val sut = IncomeSourceViewModel.createFromEmployment(ceasedEmployment, Seq(annualAccount))
    "return a viewmodel" which {
      "has the name field coming from employment model" in {
        val sut = IncomeSourceViewModel.createFromEmployment(ceasedEmployment, Seq(annualAccount))
        sut.name mustBe ceasedEmployment.name
      }
      "has the amount field as the latest payment 'amountYearToDate' value" in {
        val sut = IncomeSourceViewModel.createFromEmployment(ceasedEmployment, Seq(annualAccount))
        sut.amount mustBe Some("£123")
      }
      "does not display any message" when {
        "the latest annual account is absent" in {
          val sut = IncomeSourceViewModel.createFromEmployment(ceasedEmployment, Nil)
          sut.amount mustBe Some("")
        }
        "the latest payment is None" in {
          val sut =
            IncomeSourceViewModel.createFromEmployment(
              ceasedEmployment,
              Seq(annualAccount.copy(payments = Nil))
            )
          sut.amount mustBe Some("")
        }
      }
      "has an empty taxCode field, and a 'false' value corresponding boolean set to instruct non display" in {
        val sut = IncomeSourceViewModel.createFromEmployment(employment, Nil)
        sut.taxCode mustBe Some("")
        sut.displayTaxCode mustBe false
      }
      "has a payrollNumber field and the corresponding boolean set to instruct display" when {
        "employment model has payrollNo" in {
          val sut = IncomeSourceViewModel.createFromEmployment(ceasedEmployment, Seq(annualAccount))
          sut.payrollNumber mustBe "123ABC"
          sut.displayPayrollNumber mustBe true
        }
      }
      "has no payrollNumber field and the corresponding boolean set to instruct non display" when {
        "employment model has payrollNo" in {
          val sut = IncomeSourceViewModel.createFromEmployment(ceasedEmployment.copy(payrollNumber = None), Nil)
          sut.payrollNumber mustBe ""
          sut.displayPayrollNumber mustBe false
        }
      }
      "has the endDate field as empty and the corresponding boolean set to instruct non display" when {
        "employment model doesn't have endDate" in {
          val sut =
            IncomeSourceViewModel.createFromEmployment(ceasedEmployment.copy(endDate = None), Seq(annualAccount))
          sut.endDate mustBe ""
          sut.displayEndDate mustBe false
        }
      }
      "has a formatted endDate field and the corresponding boolean set to instruct display" when {
        "employment model has  endDate" in {
          val sut = IncomeSourceViewModel.createFromEmployment(ceasedEmployment, Seq(annualAccount))
          sut.endDate mustBe s"21 April ${TaxYear().next.year}"
          sut.displayEndDate mustBe true
        }
      }
      "has details link with employment only label " in {
        sut.detailsLinkLabel mustBe Messages("tai.incomeTaxSummary.employment.link")
        sut.detailsLinkUrl mustBe controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(1).url
      }
    }
  }

  "createFromTaxedIncome" must {
    "transform a taxedIncome to a IncomeSourceViewModel for a live employment" in {
      val taxedIncome = TaxedIncome(Some(liveEmployment1), empEmployment1)
      val actual      = IncomeSourceViewModel.createFromTaxedIncome(taxedIncome)
      val expected    =
        IncomeSourceViewModel(
          name = "Employer name1",
          amount = Some("£1,111"),
          taxCode = Some("1150L"),
          displayTaxCode = true,
          taxDistrictNumber = "DIST1",
          payeNumber = "PAYE1",
          payrollNumber = "1ABC",
          displayPayrollNumber = true,
          endDate = "",
          displayEndDate = false,
          detailsLinkLabel = messagesApi("tai.incomeTaxSummary.employmentAndBenefits.link"),
          detailsLinkUrl =
            controllers.routes.IncomeSourceSummaryController.onPageLoad(taxedIncome.employment.sequenceNumber).url,
          taxCodeUrl = Some(controllers.routes.YourTaxCodeController.taxCode(taxedIncome.employment.sequenceNumber)),
          displayDetailsLink = true,
          companyBenefitLinkLabel = messagesApi("tai.incomeTaxSummary.companyBenefits.link"),
          companyBenefitLinkUrl = controllers.benefits.routes.CompanyBenefitsSummaryController
            .onPageLoad(taxedIncome.employment.sequenceNumber)
            .url
        )

      actual mustBe expected
    }

    "detailsLinkLabel" must {
      "be the correct label for ceased employments" in {
        val taxedIncome = TaxedIncome(Some(taxCodeIncomeCeased), ceasedEmployment)
        val actual      = IncomeSourceViewModel.createFromTaxedIncome(taxedIncome)

        actual.detailsLinkLabel mustBe messagesApi("tai.incomeTaxSummary.employment.link")
      }

      "be the correct label for pension income" in {
        val taxedIncome = TaxedIncome(Some(livePension3), empEmployment1.copy(employmentType = PensionIncome))
        val actual      = IncomeSourceViewModel.createFromTaxedIncome(taxedIncome)

        actual.detailsLinkLabel mustBe messagesApi("tai.incomeTaxSummary.pension.link")
      }

      "be the correct label for any other income" in {
        val taxedIncome = TaxedIncome(
          Some(livePension3.copy(componentType = OtherIncome)),
          empEmployment1.copy(employmentType = OtherIncome)
        )
        val actual      = IncomeSourceViewModel.createFromTaxedIncome(taxedIncome)

        actual.detailsLinkLabel mustBe messagesApi("tai.incomeTaxSummary.income.link")
      }
    }

    "detailsLinkUrl is yourIncomeCalculationPage for a ceased employment" in {
      val taxedIncome = TaxedIncome(Some(taxCodeIncome), ceasedEmployment)
      val actual      = IncomeSourceViewModel.createFromTaxedIncome(taxedIncome)

      actual.detailsLinkUrl mustBe controllers.routes.YourIncomeCalculationController
        .yourIncomeCalculationPage(taxedIncome.employment.sequenceNumber)
        .url
    }
  }
}
