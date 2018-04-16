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
import play.api.i18n.Messages

import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.{AllowanceComponentType, PersonalAllowancePA}
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.util.{TaxAccountCalculator, ViewModelHelper}

case class TaxFreeAmountViewModelNew(header: String,
                                     title: String,
                                     annualTaxFreeAmount: String,
                                     summaryItems: Seq[TaxFreeAmountSummaryCategoryViewModel])

object TaxFreeAmountViewModelNew extends TaxAccountCalculator with ViewModelHelper {

  def apply(codingComponents: Seq[CodingComponent], employmentName: Map[Int, String], companyCarBenefits: Seq[CompanyCarBenefit])(implicit messages: Messages): TaxFreeAmountViewModelNew = {

    val taxFreeAmountMsg = Messages("tai.taxFreeAmount.heading.pt1")

    val headerWithAdditionalMarkup = s"""$taxFreeAmountMsg ${currentTaxYearRangeHtmlNonBreak}"""
    val title = s"$taxFreeAmountMsg ${currentTaxYearRange}"

    val taxFreeAmountTotal: BigDecimal = taxFreeAmount(codingComponents)

    val personalAllowance = personalAllowanceVM(codingComponents)
    val additions = additionsVM(codingComponents, employmentName, companyCarBenefits)
    val deductions = deductionsVM(codingComponents, employmentName, companyCarBenefits)
    val total = totalRow(taxFreeAmountTotal)

    val vmList = Seq(personalAllowance, additions, deductions, total)

    TaxFreeAmountViewModelNew(headerWithAdditionalMarkup, title, withPoundPrefixAndSign(MoneyPounds(taxFreeAmountTotal, 0)), vmList)
  }

  private def personalAllowanceVM(codingComponents: Seq[CodingComponent])(implicit messages: Messages) = {

    val personalAllowance: Seq[CodingComponent] = codingComponents.filter(isPersonalAllowanceComponent)
    val personalAllowanceSum = personalAllowance match {
      case Nil => BigDecimal(0)
      case _ => personalAllowance.map(_.amount).sum
    }

    val personalAllowanceSumFormatted = withPoundPrefixAndSign(MoneyPounds(personalAllowanceSum, 0))

    TaxFreeAmountSummaryCategoryViewModel(
      Messages("tai.taxFreeAmount.table.columnOneHeader"),
      Messages("tai.taxFreeAmount.table.columnTwoHeader"),
      hideHeaders = false,
      hideCaption = true,
      Messages("tai.taxFreeAmount.table.allowances.caption"),
      Seq(TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""),
        personalAllowanceSumFormatted,
        ChangeLinkViewModel(false, "", "")
      ))
    )
  }

  private def additionsVM(codingComponents: Seq[CodingComponent], employmentName: Map[Int, String],
                          companyCarBenefits: Seq[CompanyCarBenefit])(implicit messages: Messages) = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = false,
    Messages("tai.taxFreeAmount.table.additions.caption"),
    additionRows(codingComponents, employmentName, companyCarBenefits)
  )

  private def additionRows(codingComponents: Seq[CodingComponent], employmentName: Map[Int, String],
                           companyCarBenefits: Seq[CompanyCarBenefit])(implicit messages: Messages): Seq[TaxFreeAmountSummaryRowViewModel] = {

    val additionComponents: Seq[CodingComponent] = codingComponents.collect {
      case cc @ CodingComponent(_: AllowanceComponentType, _, _, _, _) if !isPersonalAllowanceComponent(cc) => cc
    }
    if (additionComponents.isEmpty) {
      Seq(TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.additions.noAddition"),
        "£0",
        ChangeLinkViewModel(false, "", "")
      ))
    } else {
      val totalAmount = additionComponents.map(_.amount).sum
      val totalAmountFormatted = withPoundPrefixAndSign(MoneyPounds(totalAmount, 0))
      val totalsRow = Seq(TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.additions.total"),
        totalAmountFormatted,
        ChangeLinkViewModel(false, "", "")
      ))
      (additionComponents map (TaxFreeAmountSummaryRowViewModel(_, employmentName, companyCarBenefits))) ++ totalsRow
    }
  }

  private def deductionsVM(codingComponents: Seq[CodingComponent], employmentName: Map[Int, String],
                           companyCarBenefits: Seq[CompanyCarBenefit])(implicit messages: Messages) = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = false,
    Messages("tai.taxFreeAmount.table.deductions.caption"),
    deductionRows(codingComponents, employmentName, companyCarBenefits)
  )

  private def deductionRows(codingComponents: Seq[CodingComponent], employmentName: Map[Int, String],
                            companyCarBenefits: Seq[CompanyCarBenefit])(implicit messages: Messages): Seq[TaxFreeAmountSummaryRowViewModel] = {
    val deductionComponents: Seq[CodingComponent] = codingComponents.filter({_.componentType match{
      case _: AllowanceComponentType => false
      case _ => true
    }})
    if (deductionComponents.isEmpty) {
      Seq(TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.deductions.noDeduction"),
        "£0",
        ChangeLinkViewModel(false, "", "")
      ))
    } else {
      val totalAmount = deductionComponents.map(_.amount).sum
      val totalAmountFormatted = withPoundPrefix(MoneyPounds(totalAmount, 0))
      val totalsRow = Seq(TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.deductions.total"),
        totalAmountFormatted,
        ChangeLinkViewModel(false, "", "")
      ))
      (deductionComponents map (TaxFreeAmountSummaryRowViewModel(_, employmentName, companyCarBenefits))) ++ totalsRow
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
      Seq(TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.totals.label"),
        totalAmountFormatted,
        ChangeLinkViewModel(false, "", "")
      ))
    )
  }

  private def isPersonalAllowanceComponent(codingComponent: CodingComponent): Boolean = codingComponent.componentType match {
    case PersonalAllowancePA | PersonalAllowanceAgedPAA | PersonalAllowanceElderlyPAE => true
    case _ => false
  }

}
