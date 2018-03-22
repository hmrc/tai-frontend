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

import controllers.routes
import play.api.Play.current
import play.api.i18n.Messages

import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.util.{TaiConstants, ViewModelHelper}

case class TaxFreeAmountSummaryCategoryViewModel(headerCol1: String,
                                                 headerCol2: String,
                                                 hideHeaders: Boolean,
                                                 hideCaption: Boolean,
                                                 caption: String,
                                                 rows: Seq[TaxFreeAmountSummaryRowViewModel])

case class TaxFreeAmountSummaryRowViewModel(label: String,
                                            value: String,
                                            link: ChangeLinkViewModel)

object TaxFreeAmountSummaryRowViewModel extends ViewModelHelper {

  def apply(codingComponent: CodingComponent, employmentName: Map[Int, String], companyCarBenefits: Seq[CompanyCarBenefit])(implicit messages: Messages): TaxFreeAmountSummaryRowViewModel = {

    def isCarBenefitComponent(codingComponent: CodingComponent) = codingComponent.componentType == CarBenefit

    val label =
      codingComponent.employmentId match {
        case Some(id) if employmentName.get(id).isDefined && isCarBenefitComponent(codingComponent) =>
          val makeModel = companyCarForEmployment(id, companyCarBenefits).getOrElse(Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"))
          s"""${Messages("tai.taxFreeAmount.table.taxComponent.CarBenefitMakeModel", makeModel)}
             |${Messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentName(id))}""".stripMargin
        case Some(id) if employmentName.get(id).isDefined =>
          s"""${Messages(s"tai.taxFreeAmount.table.taxComponent.${codingComponent.componentType.toString}")}
             |${Messages("tai.taxFreeAmount.table.taxComponent.from.employment", employmentName(id))}""".stripMargin
        case _ => Messages(s"tai.taxFreeAmount.table.taxComponent.${codingComponent.componentType.toString}")
      }

    val value = withPoundPrefix(MoneyPounds(codingComponent.amount, 0))

    val link = codingComponent.componentType match {
      case MedicalInsurance =>
        val url = routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform).url
        ChangeLinkViewModel(isDisplayed = true, Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"), url)
      case MarriageAllowanceReceived =>
        val url = routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.MarriageAllowanceService).url
        ChangeLinkViewModel(isDisplayed = true, Messages("tai.taxFreeAmount.table.taxComponent.MarriageAllowanceReceived"), url)
      case MarriageAllowanceTransferred =>
        val url = routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.MarriageAllowanceService).url
        ChangeLinkViewModel(isDisplayed = true, Messages("tai.taxFreeAmount.table.taxComponent.MarriageAllowanceTransferred"), url)
      case CarBenefit =>
        val url = s"${ApplicationConfig.updateCompanyCarDetailsUrl}/${codingComponent.employmentId.getOrElse(0)}"
        ChangeLinkViewModel(isDisplayed = true, Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"), url)
      case CarFuelBenefit =>
        val url = s"${ApplicationConfig.updateCompanyCarDetailsUrl}/${codingComponent.employmentId.getOrElse(0)}"
        ChangeLinkViewModel(isDisplayed = true, Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit"), url)
      case companyBenefit : BenefitComponentType =>
        val url = controllers.benefits.routes.CompanyBenefitController.redirectCompanyBenefitSelection(codingComponent.employmentId.getOrElse(0), companyBenefit).url
        ChangeLinkViewModel(isDisplayed = true, Messages(s"tai.taxFreeAmount.table.taxComponent.${codingComponent.componentType.toString}"), url)
      case allowanceComponentType : AllowanceComponentType =>
        val url = ApplicationConfig.taxFreeAllowanceLinkUrl
        ChangeLinkViewModel(isDisplayed = true, Messages(s"tai.taxFreeAmount.table.taxComponent.${allowanceComponentType.toString}"), url)
      case _ =>
        ChangeLinkViewModel(isDisplayed = false)
    }

    TaxFreeAmountSummaryRowViewModel(label, value, link)
  }

  private[viewModels] def companyCarForEmployment(employmentId: Int, companyCarBenefits: Seq[CompanyCarBenefit]): Option[String] =
    for {
      carBenefits <- companyCarBenefits.find(_.employmentSeqNo == employmentId)
      model <- carBenefits.companyCars.headOption.map(_.makeModel)
    } yield model

}

case class ChangeLinkViewModel(isDisplayed: Boolean,
                               value: String = "",
                               href: String = "")
