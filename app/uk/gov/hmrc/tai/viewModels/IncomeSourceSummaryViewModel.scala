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

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.Messages
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.{CarFuelBenefit, _}
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, GenericBenefit}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.util.{TaiConstants, ViewModelHelper}
import uk.gov.hmrc.time.TaxYearResolver

case class IncomeSourceSummaryViewModel(empId: Int,
                                        displayName: String,
                                        empOrPensionName: String,
                                        estimatedTaxableIncome: BigDecimal,
                                        incomeReceivedToDate: BigDecimal,
                                        taxCode: String,
                                        pensionOrPayrollNumber: String,
                                        isPension: Boolean,
                                        benefits: Seq[CompanyBenefitViewModel] = Seq.empty[CompanyBenefitViewModel],
                                        displayAddCompanyCarLink: Boolean = true) extends ViewModelHelper {
  val startOfCurrentYear: String = TaxYearResolver.startOfCurrentTaxYear.toString("d MMMM yyyy")

  val endOfCurrentYear: String = TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM yyyy")
}

object IncomeSourceSummaryViewModel {
  def apply(empId: Int, displayName: String, taxCodeIncomeSources: Seq[TaxCodeIncome], employment: Employment, benefits: Benefits): IncomeSourceSummaryViewModel = {
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
      taxCodeIncomeSource.taxCodeWithEmergencySuffix,
      employment.payrollNumber.getOrElse(""),
      taxCodeIncomeSource.componentType == PensionIncome,
      benefitVMs,
      displayAddCompanyCar)
  }

  private def companyBenefitViewModels(empId: Int, benefits: Benefits): Seq[CompanyBenefitViewModel] = {
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
