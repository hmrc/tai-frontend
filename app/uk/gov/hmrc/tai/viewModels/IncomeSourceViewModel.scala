/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants.{EmployeePensionIForm, InvestIncomeIform, OtherIncomeIform, StateBenefitsIform}

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
  displayDetailsLink: Boolean = true)

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
      employment.name,
      amountString,
      "",
      displayTaxCode = false,
      employment.taxDistrictNumber,
      employment.payeNumber,
      employment.payrollNumber.getOrElse(""),
      displayPayrollNumber = employment.payrollNumber.isDefined,
      endDate.getOrElse(""),
      displayEndDate = endDate.isDefined,
      messages("tai.incomeTaxSummary.employment.link"),
      controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(employment.sequenceNumber).url,
      Some(controllers.routes.YourTaxCodeController.taxCode(employment.sequenceNumber))
    )
  }

  def createFromTaxedIncome(taxedIncome: TaxedIncome)(implicit messages: Messages): IncomeSourceViewModel = {
    val endDate: Option[String] = taxedIncome.employment.endDate.map(Dates.formatDate(_))

    val detailsLinkLabel = taxedIncome.taxCodeIncome.componentType match {
      case EmploymentIncome if taxedIncome.employment.employmentStatus == Live =>
        messages("tai.incomeTaxSummary.employmentAndBenefits.link")
      case EmploymentIncome if taxedIncome.employment.employmentStatus != Live =>
        messages("tai.incomeTaxSummary.employment.link")
      case PensionIncome => messages("tai.incomeTaxSummary.pension.link")
      case _             => messages("tai.incomeTaxSummary.income.link")
    }

    val detailsLinkUrl =
      if (taxedIncome.taxCodeIncome.componentType == EmploymentIncome && taxedIncome.employment.employmentStatus != Live) {
        controllers.routes.YourIncomeCalculationController
          .yourIncomeCalculationPage(taxedIncome.employment.sequenceNumber)
          .url
      } else {
        controllers.routes.IncomeSourceSummaryController.onPageLoad(taxedIncome.employment.sequenceNumber).url
      }

    IncomeSourceViewModel(
      taxedIncome.employment.name,
      withPoundPrefixAndSign(MoneyPounds(taxedIncome.taxCodeIncome.amount, 0)),
      taxedIncome.taxCodeIncome.taxCode,
      taxedIncome.employment.employmentStatus == Live,
      taxedIncome.employment.taxDistrictNumber,
      taxedIncome.employment.payeNumber,
      taxedIncome.employment.payrollNumber.getOrElse(""),
      taxedIncome.employment.payrollNumber.isDefined,
      endDate.getOrElse(""),
      taxedIncome.employment.employmentStatus != Live && endDate.isDefined,
      detailsLinkLabel,
      detailsLinkUrl,
      Some(controllers.routes.YourTaxCodeController.taxCode(taxedIncome.employment.sequenceNumber))
    )
  }

  def apply(taxCodeIncome: TaxCodeIncome, employment: Employment)(
    implicit messages: Messages): IncomeSourceViewModel = {

    val endDate: Option[String] = employment.endDate.map(Dates.formatDate(_))
    val detailsLinkLabel = taxCodeIncome.componentType match {
      case EmploymentIncome if employment.employmentStatus == Live =>
        messages("tai.incomeTaxSummary.employmentAndBenefits.link")
      case EmploymentIncome if employment.employmentStatus != Live => messages("tai.incomeTaxSummary.employment.link")
      case PensionIncome                                           => messages("tai.incomeTaxSummary.pension.link")
      case _                                                       => messages("tai.incomeTaxSummary.income.link")
    }

    val incomeSourceSummaryUrl =
      if (taxCodeIncome.componentType == EmploymentIncome && employment.employmentStatus != Live) {
        controllers.routes.YourIncomeCalculationController.yourIncomeCalculationPage(employment.sequenceNumber).url
      } else {
        controllers.routes.IncomeSourceSummaryController.onPageLoad(employment.sequenceNumber).url
      }

    IncomeSourceViewModel(
      employment.name,
      withPoundPrefixAndSign(MoneyPounds(taxCodeIncome.amount, 0)),
      taxCodeIncome.taxCode,
      employment.employmentStatus == Live,
      employment.taxDistrictNumber,
      employment.payeNumber,
      employment.payrollNumber.getOrElse(""),
      employment.payrollNumber.isDefined,
      endDate.getOrElse(""),
      employment.employmentStatus != Live && endDate.isDefined,
      detailsLinkLabel,
      incomeSourceSummaryUrl,
      Some(controllers.routes.YourTaxCodeController.taxCode(employment.sequenceNumber))
    )
  }

  def apply(nonTaxCodeIncome: NonTaxCodeIncome)(implicit messages: Messages): Seq[IncomeSourceViewModel] =
    nonTaxCodeIncome.otherNonTaxCodeIncomes
      .withFilter(
        _.incomeComponentType != BankOrBuildingSocietyInterest
      )
      .map(otherNonTaxCodeIncome => {

        val model = IncomeSourceViewModel(
          messages("tai.typeDecodes." + otherNonTaxCodeIncome.incomeComponentType.toString),
          withPoundPrefixAndSign(MoneyPounds(otherNonTaxCodeIncome.amount, 0)),
          "",
          displayTaxCode = false,
          "",
          "",
          "",
          displayPayrollNumber = false,
          "",
          displayEndDate = false,
          messages("tai.updateOrRemove"),
          ""
        )

        otherNonTaxCodeIncome.incomeComponentType match {
          case _: OtherIncomes =>
            model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(OtherIncomeIform).url)
          case _: TaxableStateBenefits =>
            model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(StateBenefitsIform).url)
          case _: EmploymentPensions =>
            model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(EmployeePensionIForm).url)
          case _: SavingAndInvestments =>
            model.copy(detailsLinkUrl = controllers.routes.AuditController.auditLinksToIForm(InvestIncomeIform).url)
          case _ => model.copy(displayDetailsLink = false)
        }
      })
}
