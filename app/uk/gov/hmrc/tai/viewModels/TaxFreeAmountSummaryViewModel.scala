/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxFreeAmountDetails
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants

case class ChangeLinkViewModel(isDisplayed: Boolean, value: String = "", href: String = "")

case class TaxFreeAmountSummaryCategoryViewModel(
  headerCol1: String,
  headerCol2: String,
  hideHeaders: Boolean,
  hideCaption: Boolean,
  caption: String,
  rows: Seq[TaxFreeAmountSummaryRowViewModel])

case class TaxFreeAmountSummaryViewModel(summaryItems: Seq[TaxFreeAmountSummaryCategoryViewModel])

object TaxFreeAmountSummaryViewModel extends ViewModelHelper {

  def apply(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    taxFreeAmountTotal: BigDecimal,
    applicationConfig: ApplicationConfig)(implicit messages: Messages): TaxFreeAmountSummaryViewModel = {

    val personalAllowance = personalAllowanceVM(codingComponents)
    val additions = additionsVM(codingComponents, taxFreeAmountDetails: TaxFreeAmountDetails, applicationConfig)
    val deductions = deductionsVM(codingComponents, taxFreeAmountDetails: TaxFreeAmountDetails, applicationConfig)
    val total = totalRow(taxFreeAmountTotal)

    TaxFreeAmountSummaryViewModel(Seq(personalAllowance, additions, deductions, total))
  }

  private def personalAllowanceVM(codingComponents: Seq[CodingComponent])(implicit messages: Messages) = {

    val personalAllowance: Seq[CodingComponent] = codingComponents.filter(isPersonalAllowanceComponent)
    val personalAllowanceSum = personalAllowance match {
      case Nil => BigDecimal(0)
      case _   => personalAllowance.map(_.amount).sum
    }

    val personalAllowanceSumFormatted = withPoundPrefixAndSign(MoneyPounds(personalAllowanceSum, 0))

    TaxFreeAmountSummaryCategoryViewModel(
      Messages("tai.taxFreeAmount.table.columnOneHeader"),
      Messages("tai.taxFreeAmount.table.columnTwoHeader"),
      hideHeaders = false,
      hideCaption = true,
      Messages("tai.taxFreeAmount.table.allowances.caption"),
      Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""),
          personalAllowanceSumFormatted,
          ChangeLinkViewModel(false, "", "")
        ))
    )
  }

  private def additionsVM(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig)(implicit messages: Messages) = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = false,
    Messages("tai.taxFreeAmount.table.additions.caption"),
    additionRows(codingComponents, taxFreeAmountDetails: TaxFreeAmountDetails, applicationConfig)
  )

  private def additionRows(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig)(implicit messages: Messages): Seq[TaxFreeAmountSummaryRowViewModel] = {

    val additionComponents: Seq[CodingComponent] = codingComponents.collect {
      case cc @ CodingComponent(_: AllowanceComponentType, _, _, _, _) if !isPersonalAllowanceComponent(cc) => cc
    }
    if (additionComponents.isEmpty) {
      Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.additions.noAddition"),
          "£0",
          ChangeLinkViewModel(false, "", "")
        ))
    } else {
      val totalAmount = additionComponents.map(_.amount).sum
      val totalAmountFormatted = withPoundPrefixAndSign(MoneyPounds(totalAmount, 0))
      val totalsRow = Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.additions.total"),
          totalAmountFormatted,
          ChangeLinkViewModel(false, "", "")
        ))
      (additionComponents map (TaxFreeAmountSummaryRowViewModel(
        _,
        taxFreeAmountDetails: TaxFreeAmountDetails,
        applicationConfig))) ++ totalsRow
    }
  }

  private def deductionsVM(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig)(implicit messages: Messages) = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = false,
    Messages("tai.taxFreeAmount.table.deductions.caption"),
    deductionRows(codingComponents, taxFreeAmountDetails: TaxFreeAmountDetails, applicationConfig)
  )

  private def deductionRows(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig)(implicit messages: Messages): Seq[TaxFreeAmountSummaryRowViewModel] = {
    val deductionComponents: Seq[CodingComponent] = codingComponents.filter({
      _.componentType match {
        case _: AllowanceComponentType => false
        case _                         => true
      }
    })
    if (deductionComponents.isEmpty) {
      Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.deductions.noDeduction"),
          "£0",
          ChangeLinkViewModel(false, "", "")
        ))
    } else {
      val totalAmount = deductionComponents.map(_.amount).sum
      val totalAmountFormatted = withPoundPrefix(MoneyPounds(totalAmount, 0))
      val totalsRow = Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.deductions.total"),
          totalAmountFormatted,
          ChangeLinkViewModel(false, "", "")
        ))
      (deductionComponents map (TaxFreeAmountSummaryRowViewModel(_, taxFreeAmountDetails, applicationConfig))) ++ totalsRow
    }
  }

  private def totalRow(taxFreeAmountTotal: BigDecimal)(implicit messages: Messages) = {

    val totalAmountFormatted = withPoundPrefixAndSign(MoneyPounds(taxFreeAmountTotal, 0))

    TaxFreeAmountSummaryCategoryViewModel(
      Messages("tai.taxFreeAmount.table.columnOneHeader"),
      Messages("tai.taxFreeAmount.table.columnTwoHeader"),
      hideHeaders = true,
      hideCaption = true,
      Messages("tai.taxFreeAmount.table.totals.caption"),
      Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.totals.label"),
          totalAmountFormatted,
          ChangeLinkViewModel(false, "", "")
        ))
    )
  }

  private def isPersonalAllowanceComponent(codingComponent: CodingComponent): Boolean =
    codingComponent.componentType match {
      case PersonalAllowancePA | PersonalAllowanceAgedPAA | PersonalAllowanceElderlyPAE => true
      case _                                                                            => false
    }
}

case class TaxFreeAmountSummaryRowViewModel(label: TaxSummaryLabel, value: String, link: ChangeLinkViewModel)

object TaxFreeAmountSummaryRowViewModel extends ViewModelHelper {

  def apply(label: String, value: String, link: ChangeLinkViewModel): TaxFreeAmountSummaryRowViewModel =
    new TaxFreeAmountSummaryRowViewModel(TaxSummaryLabel(label), value, link)

  def apply(
    codingComponent: CodingComponent,
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig)(implicit messages: Messages): TaxFreeAmountSummaryRowViewModel = {
    val label: TaxSummaryLabel = TaxSummaryLabel(
      codingComponent.componentType,
      codingComponent.employmentId,
      taxFreeAmountDetails,
      codingComponent.amount)
    val value = withPoundPrefix(MoneyPounds(codingComponent.amount, 0))
    val link = createChangeLink(codingComponent, applicationConfig)

    TaxFreeAmountSummaryRowViewModel(label, value, link)
  }

  private def createChangeLink(codingComponent: CodingComponent, applicationConfig: ApplicationConfig)(
    implicit messages: Messages): ChangeLinkViewModel =
    codingComponent.componentType match {
      case MedicalInsurance =>
        val url = routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform)
          .url
        ChangeLinkViewModel(isDisplayed = true, Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"), url)
      case MarriageAllowanceReceived =>
        val url = routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.MarriageAllowanceService)
          .url
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages("tai.taxFreeAmount.table.taxComponent.MarriageAllowanceReceived"),
          url)
      case MarriageAllowanceTransferred =>
        val url = routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.MarriageAllowanceService)
          .url
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages("tai.taxFreeAmount.table.taxComponent.MarriageAllowanceTransferred"),
          url)
      case CarBenefit =>
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages("tai.taxFreeAmount.table.taxComponent.CarBenefit"),
          applicationConfig.cocarFrontendUrl
        )
      case CarFuelBenefit =>
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages("tai.taxFreeAmount.table.taxComponent.CarFuelBenefit"),
          applicationConfig.cocarFrontendUrl
        )
      case companyBenefit: BenefitComponentType =>
        val url = controllers.benefits.routes.CompanyBenefitController
          .redirectCompanyBenefitSelection(codingComponent.employmentId.getOrElse(0), companyBenefit)
          .url
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages(s"tai.taxFreeAmount.table.taxComponent.${codingComponent.componentType.toString}"),
          url)
      case allowanceComponentType: AllowanceComponentType =>
        val url = applicationConfig.taxFreeAllowanceLinkUrl
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages(s"tai.taxFreeAmount.table.taxComponent.${allowanceComponentType.toString}"),
          url)
      case _ =>
        ChangeLinkViewModel(isDisplayed = false)
    }
}
