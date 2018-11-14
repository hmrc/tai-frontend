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

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.filters.TaxAccountFilter
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.util.constants.TaiConstants.{EmployeePensionIForm, InvestIncomeIform, OtherIncomeIform, StateBenefitsIform}
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.time.TaxYearResolver


case class TaxAccountSummaryViewModel(header: String,
                                      title: String,
                                      taxFreeAmount: String,
                                      estimatedIncomeTaxAmount: String,
                                      lastTaxYearEnd: String,
                                      employments: Seq[IncomeSourceViewModel],
                                      pensions: Seq[IncomeSourceViewModel],
                                      ceasedEmployments: Seq[IncomeSourceViewModel],
                                      displayIyaBanner: Boolean,
                                      isAnyFormInProgress: Boolean,
                                      otherIncomeSources: Seq[IncomeSourceViewModel]
                                     )

object TaxAccountSummaryViewModel extends ViewModelHelper with TaxAccountFilter {
  def apply(taxCodeIncomes: Seq[TaxCodeIncome],
            employments: Seq[Employment],
            taxAccountSummary: TaxAccountSummary,
            isAnyFormInProgress: Boolean,
            nonTaxCodeIncome: NonTaxCodeIncome)(implicit messages: Messages): TaxAccountSummaryViewModel = {

    val header = messages("tai.incomeTaxSummary.heading.part1") + " " + currentTaxYearRangeHtmlNonBreak
    val title = messages("tai.incomeTaxSummary.heading.part1") + " " + currentTaxYearRangeHtmlNonBreak

    val taxFreeAmount = withPoundPrefixAndSign(MoneyPounds(taxAccountSummary.taxFreeAmount, 0))
    val estimatedIncomeTaxAmount = withPoundPrefixAndSign(MoneyPounds(taxAccountSummary.totalEstimatedTax, 0))

    val employmentTaxCodeIncomes = taxCodeIncomes filter liveEmployment
    val employmentViewModels = viewModelsFromMatchingIncomeSources(employmentTaxCodeIncomes, employments)

    val pensionTaxCodeIncomes = taxCodeIncomes filter livePension
    val pensionsViewModels = viewModelsFromMatchingIncomeSources(pensionTaxCodeIncomes, employments)

    val ceasedEmploymentTaxCodeIncomes = taxCodeIncomes filter ceasedEmployment
    val ceasedEmploymentViewModels =
      viewModelsFromMatchingIncomeSources(ceasedEmploymentTaxCodeIncomes, employments) ++
      viewModelsFromNonMatchingCeasedEmployments(taxCodeIncomes, employments)

    val lastTaxYearEnd = Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear.minusYears(1))

    TaxAccountSummaryViewModel(
      header,
      title,
      taxFreeAmount,
      estimatedIncomeTaxAmount,
      lastTaxYearEnd,
      employmentViewModels,
      pensionsViewModels,
      ceasedEmploymentViewModels,
      taxAccountSummary.totalInYearAdjustmentIntoCY > 0,
      isAnyFormInProgress,
      IncomeSourceViewModel(nonTaxCodeIncome))

  }

  private def viewModelsFromMatchingIncomeSources(taxCodeIncomes: Seq[TaxCodeIncome],
                                                  employments: Seq[Employment])(implicit messages: Messages): Seq[IncomeSourceViewModel] = {
    taxCodeIncomes.flatMap {
      (t: TaxCodeIncome) =>
        t.employmentId.flatMap {
          (id: Int) => employments.find(_.sequenceNumber == id).map(IncomeSourceViewModel(t, _))
        }
    }
  }

  private def viewModelsFromNonMatchingCeasedEmployments(taxCodeIncomes: Seq[TaxCodeIncome],
                                                         employments: Seq[Employment])(implicit messages: Messages): Seq[IncomeSourceViewModel] = {
    val unmatchedCeased = employments
      .withFilter(emp => !taxCodeIncomes.exists(tci => tci.employmentId.isDefined && tci.employmentId.get == emp.sequenceNumber))
      .withFilter(_.endDate.isDefined)
    unmatchedCeased.map(IncomeSourceViewModel(_))
  }
}

case class IncomeSourceViewModel(name: String,
                                 amount: String,
                                 taxCode: String,
                                 displayTaxCode: Boolean,
                                 payrollNumber: String,
                                 displayPayrollNumber: Boolean,
                                 endDate: String,
                                 displayEndDate: Boolean,
                                 detailsLinkLabel: String,
                                 detailsLinkUrl: String,
                                 displayDetailsLink: Boolean = true
                                )

object IncomeSourceViewModel extends ViewModelHelper {

  def apply(employment: Employment)(implicit messages: Messages): IncomeSourceViewModel = {

    val amountNumeric: BigDecimal = (
      for {
        latestAccount <- employment.latestAnnualAccount
        latestPayment <- latestAccount.latestPayment
      } yield latestPayment.amountYearToDate
      ).getOrElse(0)


    val amountString = if(amountNumeric != BigDecimal(0)) withPoundPrefixAndSign(MoneyPounds(amountNumeric, 0)) else ""

    val endDate: Option[String] = employment.endDate.map( Dates.formatDate(_) )

    IncomeSourceViewModel(
      employment.name,
      amountString,
      "",
      false,
      employment.payrollNumber.getOrElse(""),
      employment.payrollNumber.isDefined,
      endDate.getOrElse(""),
      endDate.isDefined,
      messages("tai.incomeTaxSummary.employment.link"),
      controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(employment.sequenceNumber).url,
      true)
  }

  def apply(taxCodeIncome: TaxCodeIncome,
            employment: Employment)(implicit messages: Messages): IncomeSourceViewModel = {

    val endDate: Option[String] = employment.endDate.map( Dates.formatDate(_) )
    val detailsLinkLabel = taxCodeIncome.componentType match {
      case EmploymentIncome if taxCodeIncome.status == Live => messages("tai.incomeTaxSummary.employmentAndBenefits.link")
      case EmploymentIncome if taxCodeIncome.status != Live => messages("tai.incomeTaxSummary.employment.link")
      case PensionIncome => messages("tai.incomeTaxSummary.pension.link")
      case _ => messages("tai.incomeTaxSummary.income.link")
    }

    val incomeSourceSummaryUrl =
        if(taxCodeIncome.componentType == EmploymentIncome && taxCodeIncome.status != Live)
          controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(employment.sequenceNumber).url
        else
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employment.sequenceNumber).url
    
    IncomeSourceViewModel(
      employment.name,
      withPoundPrefixAndSign(MoneyPounds(taxCodeIncome.amount, 0)),
      taxCodeIncome.taxCodeWithEmergencySuffix,
      taxCodeIncome.status == Live,
      employment.payrollNumber.getOrElse(""),
      employment.payrollNumber.isDefined,
      endDate.getOrElse(""),
      taxCodeIncome.status != Live && endDate.isDefined,
      detailsLinkLabel,
      incomeSourceSummaryUrl)
  }

  def apply(nonTaxCodeIncome: NonTaxCodeIncome)(implicit messages: Messages): Seq[IncomeSourceViewModel] = {

    val untaxedInterest = nonTaxCodeIncome.untaxedInterest.map(u =>
      IncomeSourceViewModel(
        messages("tai.typeDecodes." + u.incomeComponentType.toString),
        withPoundPrefixAndSign(MoneyPounds(u.amount, 0)),
        "",
        displayTaxCode = false,
        "",
        displayPayrollNumber = false,
        "",
        displayEndDate = false,
        messages("tai.bbsi.viewDetails"),
        controllers.income.bbsi.routes.BbsiController.untaxedInterestDetails().url,
        displayDetailsLink = u.bankAccounts.nonEmpty
        )
    )

    val otherIncomeSources = nonTaxCodeIncome.otherNonTaxCodeIncomes.map(otherNonTaxCodeIncome => {

      val model = IncomeSourceViewModel(
        messages("tai.typeDecodes." + otherNonTaxCodeIncome.incomeComponentType.toString),
        withPoundPrefixAndSign(MoneyPounds(otherNonTaxCodeIncome.amount, 0)),
        "",
        displayTaxCode = false,
        "",
        displayPayrollNumber = false,
        "",
        displayEndDate = false,
        messages("tai.updateOrRemove"),
        "",
        displayDetailsLink = true
      )
      otherNonTaxCodeIncome.incomeComponentType match {
        case _: OtherIncomes => model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(OtherIncomeIform).url)
        case _: TaxableStateBenefits => model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(StateBenefitsIform).url)
        case _: EmploymentPensions => model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(EmployeePensionIForm).url)
        case _: SavingAndInvestments => model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(InvestIncomeIform).url)
        case _ => model.copy(displayDetailsLink = false)
      }
    }
    )

    untaxedInterest.map(_ +: otherIncomeSources).getOrElse(otherIncomeSources)
  }
}
