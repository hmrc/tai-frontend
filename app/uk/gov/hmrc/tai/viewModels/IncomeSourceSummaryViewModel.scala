/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, GenericBenefit}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{CarFuelBenefit, _}
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants


case class IncomeSourceSummaryViewModel(empId: Int,
                                        displayName: String,
                                        empOrPensionName: String,
                                        estimatedTaxableIncome: BigDecimal,
                                        incomeReceivedToDate: BigDecimal,
                                        taxCode: String,
                                        pensionOrPayrollNumber: String,
                                        isPension: Boolean,
                                        benefits: Seq[CompanyBenefitViewModel] = Seq.empty[CompanyBenefitViewModel],
                                        displayAddCompanyCarLink: Boolean = true,
                                        estimatedPayJourneyCompleted: Boolean) extends ViewModelHelper {
  def startOfCurrentYear(implicit messages: Messages): String = Dates.formatDate(TaxYear().start)

  def endOfCurrentYear(implicit messages: Messages): String = Dates.formatDate(TaxYear().end)
}

object IncomeSourceSummaryViewModel {
  def apply(empId: Int, displayName: String, taxCodeIncomeSources: Seq[TaxCodeIncome], employment: Employment, benefits: Benefits,
            estimatedPayJourneyCompleted: Boolean)(implicit messages: Messages): IncomeSourceSummaryViewModel = {
    val amountYearToDate = for {
      latestAnnualAccount <- employment.latestAnnualAccount
      latestPayment <- latestAnnualAccount.latestPayment
    } yield latestPayment.amountYearToDate

    val taxCodeIncomeSource = taxCodeIncomeSources.find(_.employmentId.contains(empId)).
      getOrElse(throw new RuntimeException(s"Income details not found for employment id $empId"))

    val benefitVMs = companyBenefitViewModels(empId, benefits)
    val displayAddCompanyCar = !benefitVMs.map(_.name).contains(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))

    IncomeSourceSummaryViewModel(empId,
      displayName,
      taxCodeIncomeSource.name,
      taxCodeIncomeSource.amount,
      amountYearToDate.getOrElse(0),
      taxCodeIncomeSource.taxCode,
      employment.payrollNumber.getOrElse(""),
      taxCodeIncomeSource.componentType == PensionIncome,
      benefitVMs,
      displayAddCompanyCar,
      estimatedPayJourneyCompleted
    )
  }

  private def companyBenefitViewModels(empId: Int, benefits: Benefits)(implicit messages: Messages): Seq[CompanyBenefitViewModel] = {
    val ccBenVMs = benefits.companyCarBenefits collect {
      case CompanyCarBenefit(`empId`, grossAmount, _, _) =>
        val changeUrl = controllers.routes.CompanyCarController.redirectCompanyCarSelection(empId).url
        CompanyBenefitViewModel(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"), grossAmount, changeUrl)
    }

    val otherBenVMs = benefits.otherBenefits collect {
      case GenericBenefit(MedicalInsurance, Some(`empId`), amount) =>
        val benefitName = Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance")
        val changeUrl = controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform).url
        CompanyBenefitViewModel(benefitName, amount, changeUrl)

      case GenericBenefit(CarFuelBenefit, Some(`empId`), amount) =>
        val benefitName = Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit")
        val changeUrl = ApplicationConfig.companyCarFuelBenefitUrl
        CompanyBenefitViewModel(benefitName, amount, changeUrl)

      case GenericBenefit(benefitType, Some(`empId`), amount) if benefitType != MedicalInsurance && benefitType != CarFuelBenefit =>
        val benefitName = Messages(s"tai.taxFreeAmount.table.taxComponent.${benefitType.toString}")
        val changeUrl = controllers.benefits.routes.CompanyBenefitController.redirectCompanyBenefitSelection(empId, benefitType).url
        CompanyBenefitViewModel(benefitName, amount, changeUrl)
    }

    ccBenVMs ++ otherBenVMs

  }

}

case class CompanyBenefitViewModel(name: String, amount: BigDecimal, changeUrl: String)
