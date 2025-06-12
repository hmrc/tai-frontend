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

import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxFreeAmountDetails
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.{MoneyPounds, ViewModelHelper}
import uk.gov.hmrc.tai.util.constants.TaiConstants

case class ChangeLinkViewModel(
  isDisplayed: Boolean,
  value: String = "",
  href: String = "",
  linkText: String = "tai.updateOrRemove"
)

case class TaxFreeAmountSummaryCategoryViewModel(
  headerCol1: String,
  headerCol2: String,
  hideHeaders: Boolean,
  hideCaption: Boolean,
  caption: String,
  rows: Seq[TaxFreeAmountSummaryRowViewModel]
)

case class TaxFreeAmountSummaryViewModel(summaryItems: Seq[TaxFreeAmountSummaryCategoryViewModel])

object TaxFreeAmountSummaryViewModel extends ViewModelHelper {

  def apply(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    taxFreeAmountTotal: BigDecimal,
    applicationConfig: ApplicationConfig
  )(implicit messages: Messages): TaxFreeAmountSummaryViewModel = {

    val personalAllowance = personalAllowanceVM(codingComponents)
    val additions         = additionsVM(codingComponents, taxFreeAmountDetails, applicationConfig)
    val deductions        = deductionsVM(codingComponents, taxFreeAmountDetails, applicationConfig)
    val total             = totalRow(taxFreeAmountTotal)

    TaxFreeAmountSummaryViewModel(Seq(personalAllowance, additions, deductions, total))
  }

  private def personalAllowanceVM(codingComponents: Seq[CodingComponent])(implicit messages: Messages) = {

    val personalAllowance: Seq[CodingComponent] = codingComponents.filter(isPersonalAllowanceComponent)
    val personalAllowanceSum                    = personalAllowance match {
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
          ChangeLinkViewModel(isDisplayed = false)
        )
      )
    )
  }

  private def additionsVM(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig
  )(implicit messages: Messages) = TaxFreeAmountSummaryCategoryViewModel(
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
    applicationConfig: ApplicationConfig
  )(implicit messages: Messages): Seq[TaxFreeAmountSummaryRowViewModel] = {

    val additionComponents: Seq[CodingComponent] = codingComponents.collect {
      case cc @ CodingComponent(_: AllowanceComponentType, _, _, _, _) if !isPersonalAllowanceComponent(cc) => cc
    }
    if (additionComponents.isEmpty) {
      Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.additions.noAddition"),
          "£0",
          ChangeLinkViewModel(isDisplayed = false)
        )
      )
    } else {
      val totalAmount          = additionComponents.map(_.amount).sum
      val totalAmountFormatted = withPoundPrefixAndSign(MoneyPounds(totalAmount, 0))
      val totalsRow            = Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.additions.total"),
          totalAmountFormatted,
          ChangeLinkViewModel(isDisplayed = false)
        )
      )
      (additionComponents map (TaxFreeAmountSummaryRowViewModel(
        _,
        taxFreeAmountDetails: TaxFreeAmountDetails,
        applicationConfig
      ))) ++ totalsRow
    }
  }

  private def deductionsVM(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig
  )(implicit messages: Messages) = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = false,
    Messages("tai.taxFreeAmount.table.deductions.caption"),
    deductionRows(codingComponents, taxFreeAmountDetails, applicationConfig)
  )

  private def deductionRows(
    codingComponents: Seq[CodingComponent],
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig
  )(implicit messages: Messages): Seq[TaxFreeAmountSummaryRowViewModel] = {
    val deductionComponents: Seq[CodingComponent] = codingComponents.filter {
      _.componentType match {
        case _: AllowanceComponentType => false
        case _                         => true
      }
    }
    if (deductionComponents.isEmpty) {
      Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.deductions.noDeduction"),
          "£0",
          ChangeLinkViewModel(isDisplayed = false)
        )
      )
    } else {
      val totalAmount          = deductionComponents.map(_.amount).sum
      val totalAmountFormatted = withPoundPrefix(MoneyPounds(totalAmount, 0))
      val totalsRow            = Seq(
        TaxFreeAmountSummaryRowViewModel(
          Messages("tai.taxFreeAmount.table.deductions.total"),
          totalAmountFormatted,
          ChangeLinkViewModel(isDisplayed = false)
        )
      )
      (deductionComponents map (TaxFreeAmountSummaryRowViewModel(
        _,
        taxFreeAmountDetails,
        applicationConfig
      ))) ++ totalsRow
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
          ChangeLinkViewModel(isDisplayed = false)
        )
      )
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

  private val blockedAllowanceComponentTypes: Set[AllowanceComponentType]                              = Set(
    CommunityInvestmentTaxCredit,
    ConcessionRelief,
    DoubleTaxationRelief,
    EnterpriseInvestmentScheme,
    MarriedCouplesAllowanceMAE,
    MarriedCouplesAllowanceMCCP,
    RetirementAnnuityPayments,
    SurplusMarriedCouplesAllowanceToWifeWAE,
    ForeignPensionAllowance,
    EarlyYearsAdjustment,
    LoanInterestAmount,
    LossRelief,
    MaintenancePayments,
    GiftsSharesCharity
  )
  def apply(label: String, value: String, link: ChangeLinkViewModel): TaxFreeAmountSummaryRowViewModel =
    TaxFreeAmountSummaryRowViewModel(TaxSummaryLabel(label), value, link)

  def apply(
    codingComponent: CodingComponent,
    taxFreeAmountDetails: TaxFreeAmountDetails,
    applicationConfig: ApplicationConfig
  )(implicit messages: Messages): TaxFreeAmountSummaryRowViewModel = {
    val label = TaxSummaryLabel(codingComponent, taxFreeAmountDetails)
    val value = withPoundPrefix(MoneyPounds(codingComponent.amount, 0))
    val link  = createChangeLink(codingComponent, applicationConfig)
    TaxFreeAmountSummaryRowViewModel(label, value, link)
  }

  private def createChangeLink(
    codingComponent: CodingComponent,
    applicationConfig: ApplicationConfig
  )(implicit messages: Messages): ChangeLinkViewModel = {

    def labelKey(componentType: TaxComponentType): String =
      s"tai.taxFreeAmount.table.taxComponent.${componentType.toString}"

    codingComponent.componentType match {
      case MedicalInsurance =>
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages(labelKey(MedicalInsurance)),
          routes.ExternalServiceRedirectController
            .auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform)
            .url
        )

      case MarriageAllowanceReceived | MarriageAllowanceTransferred =>
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages(labelKey(codingComponent.componentType)),
          routes.ExternalServiceRedirectController
            .auditInvalidateCacheAndRedirectService(TaiConstants.MarriageAllowanceService)
            .url
        )

      case CarBenefit | CarFuelBenefit =>
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages(labelKey(codingComponent.componentType)),
          applicationConfig.cocarFrontendUrl
        )

      case HICBCPaye =>
        ChangeLinkViewModel(
          isDisplayed = applicationConfig.hicbcUpdateUrl.nonEmpty,
          Messages(""),
          applicationConfig.hicbcUpdateUrl,
          "tai.updateOrRemove.hicbc"
        )

      case benefit: BenefitComponentType =>
        ChangeLinkViewModel(
          isDisplayed = true,
          Messages(labelKey(benefit)),
          controllers.benefits.routes.CompanyBenefitController
            .redirectCompanyBenefitSelection(codingComponent.employmentId.getOrElse(0), benefit)
            .url
        )

      case allowance: AllowanceComponentType =>
        if (blockedAllowanceComponentTypes.contains(allowance)) {
          ChangeLinkViewModel(isDisplayed = false)
        } else {
          ChangeLinkViewModel(
            isDisplayed = true,
            Messages(labelKey(allowance)),
            applicationConfig.taxFreeAllowanceLinkUrl
          )
        }

      case _ =>
        ChangeLinkViewModel(isDisplayed = false)
    }
  }
}
