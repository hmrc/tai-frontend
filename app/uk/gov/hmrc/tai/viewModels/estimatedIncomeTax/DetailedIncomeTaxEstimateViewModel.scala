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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.model.domain.{MaintenancePayments, _}
import uk.gov.hmrc.tai.service.estimatedIncomeTax.EstimatedIncomeTaxService
import uk.gov.hmrc.tai.util.constants.BandTypesConstants
import uk.gov.hmrc.tai.util.{IncomeTaxEstimateHelper, MoneyPounds, ViewModelHelper}
import uk.gov.hmrc.tai.viewModels.{HelpLink, TaxSummaryLabel}
import views.html.includes.link

case class DetailedIncomeTaxEstimateViewModel(
  nonSavings: List[TaxBand],
  savings: Seq[TaxBand],
  dividends: List[TaxBand],
  taxRegion: String,
  incomeTaxEstimate: BigDecimal,
  incomeEstimate: BigDecimal,
  taxFreeAllowance: BigDecimal,
  additionalTaxTable: Seq[AdditionalTaxDetailRow],
  reductionTaxTable: Seq[ReductionTaxRow],
  totalDividendIncome: BigDecimal,
  taxFreeDividendAllowance: BigDecimal,
  selfAssessmentAndPayeText: Option[String],
  taxOnIncomeTypeHeading: String,
  taxOnIncomeTypeDescription: String
) extends ViewModelHelper

object DetailedIncomeTaxEstimateViewModel extends IncomeTaxEstimateHelper {

  def apply(
    totalTax: TotalTax,
    taxCodeIncomes: Seq[TaxCodeIncome],
    taxAccountSummary: TaxAccountSummary,
    codingComponents: Seq[CodingComponent],
    nonTaxCodeIncome: NonTaxCodeIncome
  )(implicit messages: Messages): DetailedIncomeTaxEstimateViewModel = {

    val nonSavings = totalTax.incomeCategories
      .filter(_.incomeCategoryType == NonSavingsIncomeCategory)
      .flatMap(_.taxBands)
      .filter(_.income > 0)
      .filterNot(_.rate == 0)

    val savings = totalTax.incomeCategories
      .filter { category =>
        category.incomeCategoryType == UntaxedInterestIncomeCategory ||
        category.incomeCategoryType == BankInterestIncomeCategory || category.incomeCategoryType == ForeignInterestIncomeCategory
      }
      .flatMap(_.taxBands)
      .filter(_.income > 0)

    val dividends = totalTax.incomeCategories
      .filter { category =>
        category.incomeCategoryType == UkDividendsIncomeCategory ||
        category.incomeCategoryType == ForeignDividendsIncomeCategory
      }
      .flatMap(_.taxBands)
      .filter(_.income > 0)
      .toList

    val taxRegion                  = EstimatedIncomeTaxService.findTaxRegion(taxCodeIncomes)
    val paBand                     = EstimatedIncomeTaxService.createPABand(taxAccountSummary.taxFreeAllowance)
    val additionalTaxTable         = createAdditionalTaxTable(codingComponents, totalTax)
    val reductionTaxTable          = createReductionsTable(codingComponents, totalTax)
    val dividendIncome             = EstimatedIncomeTaxService.totalDividendIncome(totalTax.incomeCategories)
    val taxFreeDividend            = taxFreeDividendAllowance(totalTax.incomeCategories)
    val mergedNonSavingsBand       = (nonSavings :+ paBand).toList.sortBy(_.rate)
    val additionIncomePayableText  = nonTaxCodeIncome.otherNonTaxCodeIncomes
      .find(_.incomeComponentType == NonCodedIncome)
      .map(_ => messages("tai.estimatedIncome.selfAssessmentAndPayeText"))
    val taxOnIncomeTypeHeading     = getTaxOnIncomeTypeHeading(taxCodeIncomes)
    val taxOnIncomeTypeDescription = getTaxOnIncomeTypeDescription(taxCodeIncomes, taxAccountSummary)

    DetailedIncomeTaxEstimateViewModel(
      mergedNonSavingsBand,
      savings,
      dividends,
      taxRegion,
      taxAccountSummary.totalEstimatedTax,
      taxAccountSummary.totalEstimatedIncome,
      taxAccountSummary.taxFreeAllowance,
      additionalTaxTable,
      reductionTaxTable,
      dividendIncome,
      taxFreeDividend,
      additionIncomePayableText,
      taxOnIncomeTypeHeading,
      taxOnIncomeTypeDescription
    )
  }

  def createAdditionalTaxTable(codingComponent: Seq[CodingComponent], totalTax: TotalTax)(implicit
    messages: Messages
  ): Seq[AdditionalTaxDetailRow] = {

    val underPaymentRow          = createAdditionalTaxRow(
      EstimatedIncomeTaxService.underPaymentFromPreviousYear(codingComponent),
      TaxSummaryLabel(
        Messages("tai.taxCalc.UnderpaymentPreviousYear.title"),
        Some(
          HelpLink(
            Messages("what.is.underpayment"),
            controllers.routes.UnderpaymentFromPreviousYearController.underpaymentExplanation().url.toString,
            "underPaymentFromPreviousYear"
          )
        )
      )
    )
    val inYearRow                = createAdditionalTaxRow(
      EstimatedIncomeTaxService.inYearAdjustment(codingComponent),
      TaxSummaryLabel(
        Messages("tai.taxcode.deduction.type-45"),
        Some(
          HelpLink(
            Messages("what.is.tax.estimation"),
            controllers.routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url.toString,
            "estimatedTaxOwedLink"
          )
        )
      )
    )
    val outstandingDebtRow       = createAdditionalTaxRow(
      EstimatedIncomeTaxService.outstandingDebt(codingComponent),
      TaxSummaryLabel(Messages("tai.taxCalc.OutstandingDebt.title"))
    )
    val childBenefitRow          = createAdditionalTaxRow(
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.otherTaxDue, tax.ChildBenefit),
      TaxSummaryLabel(Messages("tai.taxCalc.childBenefit.title"))
    )
    val excessGiftAidRow         = createAdditionalTaxRow(
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessGiftAidTax),
      TaxSummaryLabel(Messages("tai.taxCalc.excessGiftAidTax.title"))
    )
    val excessWidowAndOrphansRow = createAdditionalTaxRow(
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessWidowsAndOrphans),
      TaxSummaryLabel(Messages("tai.taxCalc.excessWidowsAndOrphans.title"))
    )
    val pensionPaymentsRow       = createAdditionalTaxRow(
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.otherTaxDue, tax.PensionPaymentsAdjustment),
      TaxSummaryLabel(Messages("tai.taxCalc.pensionPaymentsAdjustment.title"))
    )

    Seq(
      underPaymentRow,
      inYearRow,
      outstandingDebtRow,
      childBenefitRow,
      excessGiftAidRow,
      excessWidowAndOrphansRow,
      pensionPaymentsRow
    ).flatten
  }

  private def createAdditionalTaxRow(row: Option[BigDecimal], label: TaxSummaryLabel): Option[AdditionalTaxDetailRow] =
    row.map(amount => AdditionalTaxDetailRow(label, amount))

  def createReductionsTable(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit
    messages: Messages
  ): Seq[ReductionTaxRow] = {
    val nonCodedIncome    = totalTax.taxOnOtherIncome
    val nonCodedIncomeRow = createReductionTaxRow(
      nonCodedIncome,
      Messages("tai.taxCollected.atSource.otherIncome.description"),
      Messages("tai.taxCollected.atSource.otherIncome.title")
    )

    val ukDividend    =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxCreditOnUKDividends)
    val ukDividendRow = createReductionTaxRow(
      ukDividend,
      Messages("tai.taxCollected.atSource.dividends.description", 10),
      Messages("tai.taxCollected.atSource.dividends.title")
    )

    val bankInterest    =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxOnBankBSInterest)
    val bankInterestRow = createReductionTaxRow(
      bankInterest,
      Messages("tai.taxCollected.atSource.bank.description", 20),
      Messages("tai.taxCollected.atSource.bank.title")
    )

    val marriageAllowanceRow  = createMarriageAllowanceRow(codingComponents, totalTax)
    val maintenancePaymentRow = createMaintenancePaymentRow(codingComponents, totalTax)

    val enterpriseInvestmentScheme    =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.EnterpriseInvestmentSchemeRelief)
    val enterpriseInvestmentSchemeRow = createReductionTaxRow(
      enterpriseInvestmentScheme,
      Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.description"),
      Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.title")
    )

    val concessionRelief    =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.ConcessionalRelief)
    val concessionReliefRow = createReductionTaxRow(
      concessionRelief,
      Messages("tai.taxCollected.atSource.concessionalRelief.description"),
      Messages("tai.taxCollected.atSource.concessionalRelief.title")
    )

    val doubleTaxationRelief    =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.DoubleTaxationRelief)
    val doubleTaxationReliefRow = createReductionTaxRow(
      doubleTaxationRelief,
      Messages("tai.taxCollected.atSource.doubleTaxationRelief.description"),
      Messages("tai.taxCollected.atSource.doubleTaxationRelief.title")
    )

    val giftAidPayments       = EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.taxReliefComponent, tax.GiftAidPayments)
    val giftAidPaymentsRelief =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.taxReliefComponent, tax.GiftAidPaymentsRelief)
    val brdReduction          = codingComponents.find(_.componentType == BRDifferenceTaxReduction).map(_.amount)
    val brdCharge             = codingComponents.find(_.componentType == BRDifferenceTaxCharge).map(_.amount)

    val giftAidPaymentsReliefRow = createReductionTaxRow(
      giftAidPaymentsRelief,
      (brdReduction, brdCharge) match {
        case (Some(reduction), None) =>
          Messages(
            "gift.aid.tax.relief.brd.reduction",
            giftAidPayments.getOrElse(0),
            giftAidPaymentsRelief.getOrElse(0),
            reduction
          )
        case (None, Some(charge))    =>
          Messages(
            "gift.aid.tax.relief.brd.charge",
            giftAidPayments.getOrElse(0),
            giftAidPaymentsRelief.getOrElse(0),
            charge
          )
        case _                       => Messages("gift.aid.tax.relief", giftAidPayments.getOrElse(0), giftAidPaymentsRelief.getOrElse(0))
      },
      Messages("gift.aid.savings")
    )

    val personalPensionPayments          =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.taxReliefComponent, tax.PersonalPensionPayment)
    val personalPensionPaymentsRelief    =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.taxReliefComponent, tax.PersonalPensionPaymentRelief)
    val personalPensionPaymentsReliefRow = createReductionTaxRow(
      personalPensionPaymentsRelief,
      Messages(
        "personal.pension.payment.relief",
        personalPensionPayments.getOrElse(0),
        personalPensionPaymentsRelief.getOrElse(0)
      ),
      Messages("personal.pension.payments")
    )

    Seq(
      nonCodedIncomeRow,
      ukDividendRow,
      bankInterestRow,
      marriageAllowanceRow,
      maintenancePaymentRow,
      enterpriseInvestmentSchemeRow,
      concessionReliefRow,
      doubleTaxationReliefRow,
      giftAidPaymentsReliefRow,
      personalPensionPaymentsReliefRow
    ).flatten
  }

  private def createReductionTaxRow(row: Option[BigDecimal], description: String, title: String) =
    row.map(amount => ReductionTaxRow(description, amount, title))

  private def createMarriageAllowanceRow(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit
    messages: Messages
  ) = {
    val marriageAllowance             =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MarriedCouplesAllowance)
    val marriageAllowanceNpsComponent = codingComponents
      .find { component =>
        component.componentType match {
          case compType
              if compType == MarriedCouplesAllowanceMAE ||
                compType == MarriedCouplesAllowanceMCCP || compType == MarriedCouplesAllowanceToWifeWMA =>
            true
          case _ => false
        }
      }
      .map(_.amount)
      .getOrElse(BigDecimal(0))

    val tabIndexLink = link(
      url = routes.YourTaxCodeController.taxCodes().url,
      copy = Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText"),
      tabindexMinusOne = true
    )

    createReductionTaxRow(
      marriageAllowance,
      Messages(
        "tai.taxCollected.atSource.marriageAllowance.description",
        MoneyPounds(marriageAllowanceNpsComponent).quantity,
        tabIndexLink
      ),
      Messages("tai.taxCollected.atSource.marriageAllowance.title")
    )
  }

  private def createMaintenancePaymentRow(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit
    messages: Messages
  ) = {
    val maintenancePayment      =
      EstimatedIncomeTaxService.taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MaintenancePayments)
    val maintenancePaymentGross =
      codingComponents.find(_.componentType == MaintenancePayments).map(_.amount).getOrElse(BigDecimal(0))

    createReductionTaxRow(
      maintenancePayment,
      Messages(
        "tai.taxCollected.atSource.maintenancePayments.description",
        MoneyPounds(maintenancePaymentGross).quantity,
        routes.YourTaxCodeController.taxCodes().url
      ),
      Messages("tai.taxCollected.atSource.maintenancePayments.title")
    )
  }

  def containsHRS1orHRS2(taxBands: Seq[TaxBand]): Boolean = {
    val bandTypes = taxBands.map(_.bandType)
    bandTypes.contains(BandTypesConstants.SavingsHigherRate) || bandTypes.contains(
      BandTypesConstants.SavingsAdditionalRate
    )
  }

  def taxFreeSavingsIncome(taxBands: Seq[TaxBand]): BigDecimal =
    taxBands
      .filter(band =>
        band.bandType == BandTypesConstants.PersonalSavingsRate || band.bandType == BandTypesConstants.StarterSavingsRate
      )
      .map(_.income)
      .sum

  def taxFreeDividendAllowance(incomeCategories: Seq[IncomeCategory]): BigDecimal = {
    val taxBands = incomeCategories.flatMap(_.taxBands)

    taxBands.find(_.bandType == BandTypesConstants.DividendZeroRate).flatMap(_.upperBand).getOrElse(BigDecimal(0))

  }
}
