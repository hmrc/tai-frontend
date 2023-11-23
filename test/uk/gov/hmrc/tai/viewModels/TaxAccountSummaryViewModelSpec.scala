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
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.{IncomeSources, TaxYear}
import uk.gov.hmrc.tai.service.ThreeWeeks
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.TaiConstants._
import utils.{BaseSpec, TaxAccountSummaryTestData}

import java.time.format.DateTimeFormatter

class TaxAccountSummaryViewModelSpec extends BaseSpec with TaxAccountSummaryTestData {

  "TaxAccountSummaryViewModel apply method" must {
    "return a view model" which {
      "has header relating to current tax year" in {
        val expectedHeader =
          Messages("tai.incomeTaxSummary.heading.part1", TaxYearRangeUtil.currentTaxYearRange)

        val sut = TaxAccountSummaryViewModel(taxAccountSummary, ThreeWeeks, nonTaxCodeIncome, noIncomesSources, Seq())
        sut.header mustBe expectedHeader
      }

      "has title relating to current tax year" in {
        val expectedTitle =
          Messages("tai.incomeTaxSummary.heading.part1", TaxYearRangeUtil.currentTaxYearRange)
        val sut = TaxAccountSummaryViewModel(taxAccountSummary, ThreeWeeks, nonTaxCodeIncome, noIncomesSources, Seq())
        sut.title mustBe expectedTitle
      }

      "has correctly formatted positive tax free amount and estimated income" when {
        "taxAccountSummary has positive values" in {
          val sut = TaxAccountSummaryViewModel(taxAccountSummary, ThreeWeeks, nonTaxCodeIncome, noIncomesSources, Seq())
          sut.taxFreeAmount mustBe "£2,222"
          sut.estimatedIncomeTaxAmount mustBe "£1,111"
        }
      }

      "has correctly formatted negative tax free amount and estimated income" when {
        "taxAccountSummary has a negative values" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(-54321, -12345, 333.32, 444.44, 111.11),
            ThreeWeeks,
            nonTaxCodeIncome,
            noIncomesSources,
            Seq()
          )
          sut.taxFreeAmount mustBe s"${uk.gov.hmrc.tai.util.constants.TaiConstants.EncodedMinusSign}£12,345"
          sut.estimatedIncomeTaxAmount mustBe s"${uk.gov.hmrc.tai.util.constants.TaiConstants.EncodedMinusSign}£54,321"
        }
      }

      "has correctly formatted zero tax free amount and estimated income" when {
        "taxAccountSummary has zero values" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            noIncomesSources,
            Seq()
          )
          sut.taxFreeAmount mustBe "£0"
          sut.estimatedIncomeTaxAmount mustBe "£0"
        }
      }

      "has correctly formatted lastTaxYearEnd" in {
        val expectedLastTaxYearEnd = TaxYear().end.minusYears(1).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val sut = TaxAccountSummaryViewModel(taxAccountSummary, ThreeWeeks, nonTaxCodeIncome, noIncomesSources, Seq())
        sut.lastTaxYearEnd mustBe expectedLastTaxYearEnd
      }

      "has empty employments, pensions and ceasedEmployments list" when {
        "taxCodeIncomes and employments are empty" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            noIncomesSources,
            Seq()
          )
          sut.employments mustBe Seq.empty[IncomeSourceViewModel]
          sut.pensions mustBe Seq.empty[IncomeSourceViewModel]
          sut.ceasedEmployments mustBe Seq.empty[IncomeSourceViewModel]
        }

        "taxCodeIncomes doesn't have the corresponding element as employments, and none of the employments have an end date" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            noIncomesSources,
            Seq()
          )
          sut.employments mustBe Seq.empty[IncomeSourceViewModel]
          sut.pensions mustBe Seq.empty[IncomeSourceViewModel]
          sut.ceasedEmployments mustBe Seq.empty[IncomeSourceViewModel]
        }

        "employments doesn't have the corresponding element as taxCodeIncomes" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            noIncomesSources,
            Seq()
          )
          sut.employments mustBe Seq.empty[IncomeSourceViewModel]
          sut.pensions mustBe Seq.empty[IncomeSourceViewModel]
          sut.ceasedEmployments mustBe Seq.empty[IncomeSourceViewModel]
        }
      }
      "has an employment list" when {
        "taxCodeIncomes and employments have matching employments" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            incomeSources,
            Seq()
          )

          sut.employments.size mustBe 2
          sut.employments.head.name mustBe "Employer name1"
          sut.employments(1).name mustBe "Employer name2"
        }
      }
      "has a pension list" when {
        "taxCodeIncomes and employments have matching pensions" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            incomeSources,
            Seq()
          )
          sut.pensions.size mustBe 2
          sut.pensions.head.name mustBe "Pension name1"
          sut.pensions(1).name mustBe "Pension name2"
        }
      }
      "has a ceased employment list" when {
        "taxCodeIncomes and employments have matching ceased and potentially ceased employments" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            incomeSources.copy(liveEmploymentIncomeSources = Seq(), livePensionIncomeSources = Seq()),
            Seq()
          )
          sut.ceasedEmployments.size mustBe 2
          sut.ceasedEmployments.head.name mustBe "Employer name3"
          sut.ceasedEmployments(1).name mustBe "Employer name4"
        }
      }
      "has no ceased employment list" when {
        "employments ceased before the tax year" in {
          val ceasedEmployments = incomeSources.ceasedEmploymentIncomeSources.map { taxCodeIncome =>
            val employment = taxCodeIncome.employment.copy(endDate = Some(TaxYear().prev.start))
            taxCodeIncome.copy(employment = employment)
          }

          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            incomeSources.copy(
              liveEmploymentIncomeSources = Seq(),
              livePensionIncomeSources = Seq(),
              ceasedEmploymentIncomeSources = ceasedEmployments
            ),
            Seq()
          )
          sut.ceasedEmployments.size mustBe 0
        }
      }
      "has the iya banner boolean set to true" when {
        "tax account summary shows an in year adjustment value greater than zero" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0.01, 0.02, 0.01),
            ThreeWeeks,
            nonTaxCodeIncome,
            noIncomesSources,
            Seq()
          )
          sut.displayIyaBanner mustBe true
        }
      }
      "has the iya banner boolean set to false" when {
        "tax account summary shows an in year adjsutment value of zero or less" in {
          val sut = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, 0, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            noIncomesSources,
            Seq()
          )
          sut.displayIyaBanner mustBe false
          val sutNeg = TaxAccountSummaryViewModel(
            TaxAccountSummary(0, 0, -0.01, 0, 0),
            ThreeWeeks,
            nonTaxCodeIncome,
            noIncomesSources,
            Seq()
          )
          sutNeg.displayIyaBanner mustBe false
        }
      }

      "has the other income sources with links" when {
        "other income sources with untaxed interest are available and bank accounts are not available" in {
          val otherIncomeSourceViewModel2 = otherIncomeSourceViewModel.copy(
            name = Messages("tai.typeDecodes.Profit"),
            amount = "£100",
            detailsLinkLabel = Messages("tai.updateOrRemove"),
            detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(OtherIncomeIform).url
          )

          val sut = TaxAccountSummaryViewModel(taxAccountSummary, ThreeWeeks, nonTaxCodeIncome, noIncomesSources, Seq())

          sut.otherIncomeSources mustBe Seq(otherIncomeSourceViewModel2)
        }

        "other income sources with untaxed interest are available and bank accounts are also available" in {
          val otherIncomeSourceViewModel2 = otherIncomeSourceViewModel.copy(
            name = Messages("tai.typeDecodes.Profit"),
            amount = "£100",
            detailsLinkLabel = Messages("tai.updateOrRemove"),
            detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(OtherIncomeIform).url
          )

          val nonTaxCodeIncomeWithBankAccounts = NonTaxCodeIncome(
            Some(
              uk.gov.hmrc.tai.model.domain.income
                .UntaxedInterest(UntaxedInterestIncome, None, 100, "Untaxed Interest")
            ),
            Seq(
              OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
            )
          )

          val sut = TaxAccountSummaryViewModel(
            taxAccountSummary,
            ThreeWeeks,
            nonTaxCodeIncomeWithBankAccounts,
            noIncomesSources,
            Seq()
          )
          sut.otherIncomeSources mustBe Seq(otherIncomeSourceViewModel2)
        }

        "other income sources without untaxed interest are available" in {
          val nonTaxCodeIncomeWithOutUntaxedInterest = nonTaxCodeIncome.copy(untaxedInterest = None)
          val otherIncomeSourceViewModel1 = otherIncomeSourceViewModel.copy(
            name = Messages("tai.typeDecodes.Profit"),
            amount = "£100",
            detailsLinkLabel = Messages("tai.updateOrRemove"),
            detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(OtherIncomeIform).url
          )

          val sut = TaxAccountSummaryViewModel(
            taxAccountSummary,
            ThreeWeeks,
            nonTaxCodeIncomeWithOutUntaxedInterest,
            noIncomesSources,
            Seq()
          )

          sut.otherIncomeSources mustBe Seq(otherIncomeSourceViewModel1)
        }

        "multiple other income with untaxed interest are present" in {
          val otherIncomeSourceViewModel2 = otherIncomeSourceViewModel.copy(
            name = Messages("tai.typeDecodes.Tips"),
            amount = "£100",
            detailsLinkLabel = Messages("tai.updateOrRemove"),
            detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(OtherIncomeIform).url
          )
          val otherIncomeSourceViewModel3 = otherIncomeSourceViewModel.copy(
            name = Messages("tai.typeDecodes.OccupationalPension"),
            amount = "£100",
            detailsLinkLabel = Messages("tai.updateOrRemove"),
            detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(EmployeePensionIForm).url
          )
          val otherIncomeSourceViewModel4 = otherIncomeSourceViewModel.copy(
            name = Messages("tai.typeDecodes.UkDividend"),
            amount = "£100",
            detailsLinkLabel = Messages("tai.updateOrRemove"),
            detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(InvestIncomeIform).url
          )

          val otherIncomeSourceViewModel5 = otherIncomeSourceViewModel.copy(
            name = Messages("tai.typeDecodes.JobSeekersAllowance"),
            amount = "£100",
            detailsLinkLabel = Messages("tai.updateOrRemove"),
            detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(StateBenefitsIform).url
          )

          val nonTaxCodeIncomeWithBankAccounts = NonTaxCodeIncome(
            Some(
              uk.gov.hmrc.tai.model.domain.income
                .UntaxedInterest(UntaxedInterestIncome, None, 100, "Untaxed Interest")
            ),
            Seq(
              OtherNonTaxCodeIncome(Tips, None, 100, "Tips"),
              OtherNonTaxCodeIncome(OccupationalPension, None, 100, "OccupationalPension"),
              OtherNonTaxCodeIncome(UkDividend, None, 100, "UkDividend"),
              OtherNonTaxCodeIncome(JobSeekersAllowance, None, 100, "JobSeekersAllowance")
            )
          )

          val sut = TaxAccountSummaryViewModel(
            taxAccountSummary,
            ThreeWeeks,
            nonTaxCodeIncomeWithBankAccounts,
            noIncomesSources,
            Seq()
          )

          sut.otherIncomeSources mustBe Seq(
            otherIncomeSourceViewModel2,
            otherIncomeSourceViewModel3,
            otherIncomeSourceViewModel4,
            otherIncomeSourceViewModel5
          )

        }
      }

      "has no other income sources" when {
        "there is no other income sources are available" in {
          val nonTaxCodeIncomeWithOutAnything = NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          val sut = TaxAccountSummaryViewModel(
            taxAccountSummary,
            ThreeWeeks,
            nonTaxCodeIncomeWithOutAnything,
            noIncomesSources,
            Seq()
          )
          sut.otherIncomeSources mustBe Seq.empty[IncomeSourceViewModel]
        }
      }
    }

    "return a view model instance for an end dated employment record that has no matching TaxCodeIncomeSource record" in {
      val sut = TaxAccountSummaryViewModel(
        TaxAccountSummary(0, 0, 0, 0, 0),
        ThreeWeeks,
        nonTaxCodeIncome,
        noIncomesSources,
        Seq(ceasedEmployment.copy(sequenceNumber = nonMatchingSequenceNumber))
      )
      sut.ceasedEmployments.size mustBe 1
      sut.ceasedEmployments.head mustBe IncomeSourceViewModel(
        name = "Ceased employer name",
        amount = "£123",
        taxCode = "",
        displayTaxCode = false,
        "DIST123",
        "PAYE543",
        payrollNumber = "123ABC",
        displayPayrollNumber = true,
        endDate = s"21 April ${TaxYear().next.year}",
        displayEndDate = true,
        detailsLinkLabel = Messages("tai.incomeTaxSummary.employment.link"),
        detailsLinkUrl =
          controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(nonMatchingSequenceNumber).url,
        taxCodeUrl = Some(controllers.routes.YourTaxCodeController.taxCode(nonMatchingSequenceNumber)),
        true
      )
    }
  }

  val otherIncomeSourceViewModel = IncomeSourceViewModel(
    "",
    "",
    "",
    displayTaxCode = false,
    "",
    "",
    "",
    displayPayrollNumber = false,
    "",
    displayEndDate = false,
    "",
    "",
    displayDetailsLink = true
  )

  val noIncomesSources = IncomeSources(Seq(), Seq(), Seq())
  val incomeSources =
    IncomeSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources)

}
