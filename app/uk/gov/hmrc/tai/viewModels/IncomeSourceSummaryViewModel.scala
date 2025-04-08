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
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, GenericBenefit}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates, ViewModelHelper}

case class IncomeSourceSummaryViewModel(
  empId: Int,
  displayName: String,
  empOrPensionName: String,
  estimatedTaxableIncome: Option[BigDecimal], // Changed to Option
  incomeReceivedToDate: BigDecimal,
  taxCode: Option[String], // Changed to Option
  pensionOrPayrollNumber: String,
  isPension: Boolean,
  benefits: Seq[CompanyBenefitViewModel] = Seq.empty[CompanyBenefitViewModel],
  displayAddCompanyCarLink: Boolean = true,
  estimatedPayJourneyCompleted: Boolean,
  rtiAvailable: Boolean,
  taxDistrictNumber: String,
  payeNumber: String,
  isUpdateInProgress: Boolean = false
) extends ViewModelHelper {
  def startOfCurrentYear(implicit messages: Messages): String = Dates.formatDate(TaxYear().start)

  def endOfCurrentYear(implicit messages: Messages): String = Dates.formatDate(TaxYear().end)
}

object IncomeSourceSummaryViewModel {
  def apply(
    empId: Int,
    displayName: String,
//    taxCodeIncomeSources: Seq[TaxCodeIncome],
    estimatedPayAmount: Option[BigDecimal], // Added
    taxCode: Option[String], // Added
    employment: Employment,
    benefits: Benefits,
    estimatedPayJourneyCompleted: Boolean,
    rtiAvailable: Boolean,
    applicationConfig: ApplicationConfig,
    cacheUpdatedIncomeAmount: Option[Int]
  )(implicit messages: Messages): IncomeSourceSummaryViewModel = {

    val amountYearToDate = for {
      latestAnnualAccount <- employment.latestAnnualAccount
      latestPayment       <- latestAnnualAccount.latestPayment
    } yield latestPayment.amountYearToDate

//    val taxCodeIncomeSource = taxCodeIncomeSources
//      .find(_.employmentId.contains(empId))
//      .getOrElse(throw new RuntimeException(s"Income details not found for employment id $empId"))

    val benefitVMs = companyBenefitViewModels(empId, benefits, applicationConfig)
    val displayAddCompanyCar =
      !benefitVMs.map(_.name).contains(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))

    val isUpdateInProgress = cacheUpdatedIncomeAmount match {
      case Some(cacheUpdateAMount) => cacheUpdateAMount != estimatedPayAmount.map(_.toInt).getOrElse(0)
      case None                    => false
    }

//    val isUpdateInProgress = cacheUpdatedIncomeAmount match {
//      case Some(cacheUpdateAMount) => cacheUpdateAMount != taxCodeIncomeSource.amount.toInt
//      case None                    => false
//    }

    IncomeSourceSummaryViewModel(
      empId = empId,
      displayName = displayName,
//      taxCodeIncomeSource.name,
//      taxCodeIncomeSource.amount,
      empOrPensionName = employment.name,
      estimatedTaxableIncome = estimatedPayAmount,
      incomeReceivedToDate = amountYearToDate.getOrElse(0),
//      taxCodeIncomeSource.taxCode,
      taxCode = taxCode,
      pensionOrPayrollNumber = employment.payrollNumber.getOrElse(""),
//      taxCodeIncomeSource.componentType == PensionIncome,
      isPension = employment.receivingOccupationalPension,
      benefits = benefitVMs,
      displayAddCompanyCarLink = displayAddCompanyCar,
      estimatedPayJourneyCompleted = estimatedPayJourneyCompleted,
      rtiAvailable = rtiAvailable,
      taxDistrictNumber = employment.taxDistrictNumber,
      payeNumber = employment.payeNumber,
      isUpdateInProgress = isUpdateInProgress
    )
  }

  private def companyBenefitViewModels(empId: Int, benefits: Benefits, applicationConfig: ApplicationConfig)(implicit
    messages: Messages
  ): Seq[CompanyBenefitViewModel] = {
    val ccBenVMs = benefits.companyCarBenefits collect { case CompanyCarBenefit(`empId`, grossAmount, _, _) =>
      val changeUrl = applicationConfig.cocarFrontendUrl
      CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"), grossAmount, changeUrl)
    }

    val otherBenVMs = benefits.otherBenefits collect {
      case GenericBenefit(MedicalInsurance, Some(`empId`), amount) =>
        val benefitName = Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance")
        val changeUrl = controllers.routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform)
          .url
        CompanyBenefitViewModel(benefitName, amount, changeUrl)

      case GenericBenefit(CarFuelBenefit, Some(`empId`), amount) =>
        val benefitName = Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit")
        val changeUrl = applicationConfig.cocarFrontendUrl
        CompanyBenefitViewModel(benefitName, amount, changeUrl)

      case GenericBenefit(benefitType, Some(`empId`), amount) =>
        val benefitName = Messages(s"tai.taxFreeAmount.table.taxComponent.${benefitType.toString}")
        val changeUrl =
          controllers.benefits.routes.CompanyBenefitController.redirectCompanyBenefitSelection(empId, benefitType).url
        CompanyBenefitViewModel(benefitName, amount, changeUrl)
    }

    ccBenVMs ++ otherBenVMs

  }

}
