/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.benefit

import play.api.i18n.Messages
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, GenericBenefit}
import uk.gov.hmrc.tai.model.domain.{CarFuelBenefit, MedicalInsurance}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.viewModels.CompanyBenefitViewModel

case class CompanyBenefitsSummaryViewModel(
  empOrPensionName: String,
  displayName: String,
  benefits: Seq[CompanyBenefitViewModel],
  displayAddCompanyCarLink: Boolean
)

object CompanyBenefitsSummaryViewModel {
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
        val changeUrl   = controllers.routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform)
          .url
        CompanyBenefitViewModel(benefitName, amount, changeUrl)

      case GenericBenefit(CarFuelBenefit, Some(`empId`), amount) =>
        val benefitName = Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit")
        val changeUrl   = applicationConfig.cocarFrontendUrl
        CompanyBenefitViewModel(benefitName, amount, changeUrl)

      case GenericBenefit(benefitType, Some(`empId`), amount) =>
        val benefitName = Messages(s"tai.taxFreeAmount.table.taxComponent.${benefitType.toString}")
        val changeUrl   =
          controllers.benefits.routes.CompanyBenefitController.redirectCompanyBenefitSelection(empId, benefitType).url
        CompanyBenefitViewModel(benefitName, amount, changeUrl)
    }

    ccBenVMs ++ otherBenVMs

  }

  def apply(
    empOrPensionName: String,
    displayName: String,
    empId: Int,
    applicationConfig: ApplicationConfig,
    benefits: Benefits
  )(implicit messages: Messages): CompanyBenefitsSummaryViewModel = {

    val benefitVMs           = companyBenefitViewModels(empId, benefits, applicationConfig)
    val displayAddCompanyCar =
      !benefitVMs.map(_.name).contains(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))

    CompanyBenefitsSummaryViewModel(
      empOrPensionName = empOrPensionName,
      displayName = displayName,
      benefits = benefitVMs,
      displayAddCompanyCarLink = displayAddCompanyCar
    )
  }
}
