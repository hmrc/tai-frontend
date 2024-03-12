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
import play.api.mvc.Call
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Ceased, Live, NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.util.constants.TaiConstants.{EmployeePensionIForm, InvestIncomeIform, OtherIncomeIform, StateBenefitsIform}
import uk.gov.hmrc.tai.util.{MoneyPounds, TaxYearRangeUtil => Dates, ViewModelHelper}

case class IncomeSourceViewModel(
  name: String,
  amount: String,
  taxCode: String,
  displayTaxCode: Boolean,
  taxDistrictNumber: String,
  payeNumber: String,
  payrollNumber: String,
  displayPayrollNumber: Boolean,
  endDate: String,
  displayEndDate: Boolean,
  detailsLinkLabel: String,
  detailsLinkUrl: String,
  taxCodeUrl: Option[Call] = None,
  displayDetailsLink: Boolean = true,
  companyBenefitLinkLabel: String,
  companyBenefitLinkUrl: String
)

object IncomeSourceViewModel extends ViewModelHelper {

  def createFromEmployment(employment: Employment)(implicit messages: Messages): IncomeSourceViewModel = {
    val amountNumeric: BigDecimal = (
      for {
        latestAccount <- employment.latestAnnualAccount
        latestPayment <- latestAccount.latestPayment
      } yield latestPayment.amountYearToDate
    ).getOrElse(0)

    val amountString = if (amountNumeric != BigDecimal(0)) withPoundPrefixAndSign(MoneyPounds(amountNumeric, 0)) else ""

    val endDate: Option[String] = employment.endDate.map(Dates.formatDate(_))

    IncomeSourceViewModel(
      name = employment.name,
      amount = amountString,
      taxCode = "",
      displayTaxCode = false,
      taxDistrictNumber = employment.taxDistrictNumber,
      payeNumber = employment.payeNumber,
      payrollNumber = employment.payrollNumber.getOrElse(""),
      displayPayrollNumber = employment.payrollNumber.isDefined,
      endDate = endDate.getOrElse(""),
      displayEndDate = endDate.isDefined,
      detailsLinkLabel = messages("tai.incomeTaxSummary.employment.link"),
      detailsLinkUrl =
        controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(employment.sequenceNumber).url,
      taxCodeUrl = Some(controllers.routes.YourTaxCodeController.taxCode(employment.sequenceNumber)),
      companyBenefitLinkLabel = messages("tai.incomeTaxSummary.companyBenefits.link"),
      companyBenefitLinkUrl = controllers.routes.TaxAccountSummaryController.onPageLoad().url
    )
  }

  def createFromTaxedIncome(taxedIncome: TaxedIncome)(implicit messages: Messages): IncomeSourceViewModel = {
    val endDate: Option[String] = taxedIncome.employment.endDate.map(Dates.formatDate(_))

    def getLinkLabel(messageKey: String): String =
      taxedIncome.taxCodeIncome.componentType match {
        case EmploymentIncome if taxedIncome.employment.employmentStatus == Live =>
          messages(s"tai.incomeTaxSummary.$messageKey.link")
        case EmploymentIncome if taxedIncome.employment.employmentStatus != Live =>
          messages("tai.incomeTaxSummary.employment.link")
        case PensionIncome =>
          messages("tai.incomeTaxSummary.pension.link")
        case _ =>
          messages("tai.incomeTaxSummary.income.link")
      }

    val detailsLinkLabel = getLinkLabel("employmentAndBenefits")
    val companyBenefitLinkLabel = getLinkLabel("companyBenefits")

    val detailsLinkUrl =
      if (
        taxedIncome.taxCodeIncome.componentType == EmploymentIncome && taxedIncome.employment.employmentStatus != Live
      ) {
        controllers.routes.YourIncomeCalculationController
          .yourIncomeCalculationPage(taxedIncome.employment.sequenceNumber)
          .url
      } else {
        controllers.routes.IncomeSourceSummaryController.onPageLoad(taxedIncome.employment.sequenceNumber).url
      }

    val companyBenefitLinkUrl =
      if (
        taxedIncome.taxCodeIncome.componentType == EmploymentIncome && taxedIncome.employment.employmentStatus != Live
      ) {
        controllers.routes.IncomeSourceSummaryController.onPageLoad(taxedIncome.employment.sequenceNumber).url
      } else {
        controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }

    IncomeSourceViewModel(
      name = taxedIncome.employment.name,
      amount = withPoundPrefixAndSign(MoneyPounds(taxedIncome.taxCodeIncome.amount, 0)),
      taxCode = taxedIncome.taxCodeIncome.taxCode,
      displayTaxCode =
        taxedIncome.employment.employmentStatus == Live || taxedIncome.employment.employmentStatus == Ceased,
      taxDistrictNumber = taxedIncome.employment.taxDistrictNumber,
      payeNumber = taxedIncome.employment.payeNumber,
      payrollNumber = taxedIncome.employment.payrollNumber.getOrElse(""),
      displayPayrollNumber = taxedIncome.employment.payrollNumber.isDefined,
      endDate = endDate.getOrElse(""),
      displayEndDate = taxedIncome.employment.employmentStatus != Live && endDate.isDefined,
      detailsLinkLabel = detailsLinkLabel,
      detailsLinkUrl = detailsLinkUrl,
      taxCodeUrl = Some(controllers.routes.YourTaxCodeController.taxCode(taxedIncome.employment.sequenceNumber)),
      companyBenefitLinkLabel = companyBenefitLinkLabel,
      companyBenefitLinkUrl = companyBenefitLinkUrl
    )
  }

  def apply(taxCodeIncome: TaxCodeIncome, employment: Employment)(implicit
    messages: Messages
  ): IncomeSourceViewModel = {

    val endDate: Option[String] = employment.endDate.map(Dates.formatDate(_))

    def getLinkLabel(messageKey: String): String =
      taxCodeIncome.componentType match {
        case EmploymentIncome if employment.employmentStatus == Live =>
          messages(s"tai.incomeTaxSummary.$messageKey.link")
        case EmploymentIncome if employment.employmentStatus != Live =>
          messages("tai.incomeTaxSummary.employment.link")
        case PensionIncome =>
          messages("tai.incomeTaxSummary.pension.link")
        case _ =>
          messages("tai.incomeTaxSummary.income.link")
      }

    val detailsLinkLabel = getLinkLabel("employmentAndBenefits")
    val companyBenefitLinkLabel = getLinkLabel("companyBenefits")

    val incomeSourceSummaryUrl =
      if (taxCodeIncome.componentType == EmploymentIncome && employment.employmentStatus != Live) {
        controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(employment.sequenceNumber).url
      } else {
        controllers.routes.IncomeSourceSummaryController.onPageLoad(employment.sequenceNumber).url
      }

    IncomeSourceViewModel(
      name = employment.name,
      amount = withPoundPrefixAndSign(MoneyPounds(taxCodeIncome.amount, 0)),
      taxCode = taxCodeIncome.taxCode,
      displayTaxCode = employment.employmentStatus == Live,
      taxDistrictNumber = employment.taxDistrictNumber,
      payeNumber = employment.payeNumber,
      payrollNumber = employment.payrollNumber.getOrElse(""),
      displayPayrollNumber = employment.payrollNumber.isDefined,
      endDate = endDate.getOrElse(""),
      displayEndDate = employment.employmentStatus != Live && endDate.isDefined,
      detailsLinkLabel = detailsLinkLabel,
      detailsLinkUrl = incomeSourceSummaryUrl,
      taxCodeUrl = Some(controllers.routes.YourTaxCodeController.taxCode(employment.sequenceNumber)),
      companyBenefitLinkLabel = companyBenefitLinkLabel,
      companyBenefitLinkUrl = incomeSourceSummaryUrl
    )
  }

  def apply(nonTaxCodeIncome: NonTaxCodeIncome)(implicit messages: Messages): Seq[IncomeSourceViewModel] =
    nonTaxCodeIncome.otherNonTaxCodeIncomes
      .withFilter(
        _.incomeComponentType != BankOrBuildingSocietyInterest
      )
      .map { otherNonTaxCodeIncome =>
        val model = IncomeSourceViewModel(
          name = messages("tai.typeDecodes." + otherNonTaxCodeIncome.incomeComponentType.toString),
          amount = withPoundPrefixAndSign(MoneyPounds(otherNonTaxCodeIncome.amount, 0)),
          taxCode = "",
          displayTaxCode = false,
          taxDistrictNumber = "",
          payeNumber = "",
          payrollNumber = "",
          displayPayrollNumber = false,
          endDate = "",
          displayEndDate = false,
          detailsLinkLabel = messages("tai.updateOrRemove"),
          detailsLinkUrl = "",
          taxCodeUrl = None,
          companyBenefitLinkLabel = "",
          companyBenefitLinkUrl = ""
        )

        otherNonTaxCodeIncome.incomeComponentType match {
          case _: OtherIncomes =>
            model.copy(
              detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(OtherIncomeIform).url,
              companyBenefitLinkUrl = controllers.routes.AuditController.auditLinksToIForm(OtherIncomeIform).url
            )
          case _: TaxableStateBenefits =>
            model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(StateBenefitsIform).url)
          case _: EmploymentPensions =>
            model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(EmployeePensionIForm).url)
          case _: SavingAndInvestments =>
            model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(InvestIncomeIform).url)
          case _ => model.copy(displayDetailsLink = false)
        }
      }
}
