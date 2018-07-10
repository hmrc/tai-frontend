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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.model.domain.{MaintenancePayments => _, _}
import uk.gov.hmrc.tai.service.estimatedIncomeTax.EstimatedIncomeTaxService
import uk.gov.hmrc.tai.util.{BandTypesConstants, ViewModelHelper}
import uk.gov.hmrc.urls.Link

import scala.math.BigDecimal

case class DetailedIncomeTaxEstimateViewModel(
                                       nonSavings: List[TaxBand],
                                       savings: Seq[TaxBand],
                                       dividends: List[TaxBand],
                                       taxRegion: String,
                                       incomeTaxEstimate: BigDecimal,
                                       incomeEstimate: BigDecimal,
                                       taxFreeAllowance: BigDecimal,
                                       additionalTaxTable: Seq[AdditionalTaxDetailRow],
                                       additionalTaxTableTotal: BigDecimal,
                                       reductionTaxTable: Seq[ReductionTaxRow],
                                       reductionTaxTableTotal: BigDecimal,
                                       incomeTaxReducedToZeroMessage: Option[String],
                                       hasPotentialUnderPayment: Boolean,
                                       ssrValue: Option[BigDecimal],
                                       psrValue: Option[BigDecimal],
                                       totalDividendIncome: BigDecimal,
                                       taxFreeDividendAllowance: BigDecimal,
                                       selfAssessmentAndPayeText: Option[String],
                                       hasTaxRelief: Boolean = false
                                     ) extends ViewModelHelper


object DetailedIncomeTaxEstimateViewModel extends BandTypesConstants with EstimatedIncomeTaxHelper with ComplexEstimatedIncomeTaxHelper {

  def apply(totalTax: TotalTax, taxCodeIncomes: Seq[TaxCodeIncome],taxAccountSummary: TaxAccountSummary, codingComponents: Seq[CodingComponent],
            nonTaxCodeIncome: NonTaxCodeIncome)(implicit messages: Messages): DetailedIncomeTaxEstimateViewModel = {

    val nonSavings = totalTax.incomeCategories.filter(_.incomeCategoryType == NonSavingsIncomeCategory).
      flatMap(_.taxBands).filter(_.income > 0).filterNot(_.rate == 0)

    val savings = totalTax.incomeCategories.filter {
      category => category.incomeCategoryType == UntaxedInterestIncomeCategory ||
        category.incomeCategoryType == BankInterestIncomeCategory || category.incomeCategoryType == ForeignInterestIncomeCategory
    }.flatMap(_.taxBands).filter(_.income > 0)//.filterNot(_.rate == 0)

    val dividends = totalTax.incomeCategories.filter {
        category => category.incomeCategoryType == UkDividendsIncomeCategory ||
          category.incomeCategoryType == ForeignDividendsIncomeCategory
      }.flatMap(_.taxBands).filter(_.income > 0).toList

    val filteredCategories = totalTax.incomeCategories.filter {
      category => category.incomeCategoryType == UkDividendsIncomeCategory ||
        category.incomeCategoryType == ForeignDividendsIncomeCategory
    }
    val taxbandsNonzeroIncome = filteredCategories.flatMap(_.taxBands).filter(_.income > 0)
    val taxbandsNonzeroRate = taxbandsNonzeroIncome.filterNot(_.rate == 0)

    val taxRegion = findTaxRegion(taxCodeIncomes)
    val taxBands = EstimatedIncomeTaxService.taxBand(totalTax).toList
    val paBand = createPABand(taxAccountSummary.taxFreeAllowance)
    val mergedTaxBands = retrieveTaxBands(taxBands :+ paBand)
    val additionalTaxTable = createAdditionalTaxTable(codingComponents, totalTax)
    val additionalTaxTableTotal = additionalTaxTable.map(_.amount).sum
    val reductionTaxTable = createReductionsTable(codingComponents, totalTax)
    val reductionTaxTableTotal = reductionTaxTable.map(_.amount).sum
    val incomeTaxReducedToZero = incomeTaxReducedToZeroMessage(taxAccountSummary.totalEstimatedTax <= 0 && reductionTaxTable.nonEmpty)
    val hasPotentialUnderPayment = EstimatedIncomeTaxService.hasPotentialUnderPayment(taxAccountSummary.totalInYearAdjustmentIntoCY,
      taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne)
    val ssrValue = fetchIncome(mergedTaxBands, StarterSavingsRate)
    val psrValue = fetchIncome(mergedTaxBands, PersonalSavingsRate)
    val dividendIncome = totalDividendIncome(totalTax.incomeCategories)
    val taxFreeDividend = taxFreeDividendAllowance(totalTax.incomeCategories)
    val hasTaxRelief = EstimatedIncomeTaxService.hasTaxRelief(totalTax)
    val mergedNonSavingsBand = (nonSavings :+ paBand).toList.sortBy(_.rate)

    val nonCodedIncomePresent = !nonTaxCodeIncome.otherNonTaxCodeIncomes.filter(_.incomeComponentType == NonCodedIncome).isEmpty
    val additionIncomePayableText: Option[String] = if (nonCodedIncomePresent) Option(messages("tai.estimatedIncome.selfAssessmentAndPayeText")) else None

    DetailedIncomeTaxEstimateViewModel(
      mergedNonSavingsBand,
      savings,
      dividends,
      taxRegion,
      taxAccountSummary.totalEstimatedTax,
      taxAccountSummary.totalEstimatedIncome,
      taxAccountSummary.taxFreeAllowance,
      additionalTaxTable,
      additionalTaxTableTotal,
      reductionTaxTable,
      reductionTaxTableTotal,
      incomeTaxReducedToZero,
      hasPotentialUnderPayment,
      ssrValue,
      psrValue,
      dividendIncome,
      taxFreeDividend,
      additionIncomePayableText,
      hasTaxRelief
    )
  }

  def createAdditionalTaxTable(codingComponent: Seq[CodingComponent], totalTax: TotalTax)(implicit messages: Messages): Seq[AdditionalTaxDetailRow] = {

    val underPaymentRow = createAdditionalTaxRow(underPaymentFromPreviousYear(codingComponent), Messages("tai.taxCalc.UnderpaymentPreviousYear.title"), None)
    val inYearRow = createAdditionalTaxRow(inYearAdjustment(codingComponent), Messages("tai.taxcode.deduction.type-45"),
      Some(routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url))
    val outstandingDebtRow = createAdditionalTaxRow(outstandingDebt(codingComponent), Messages("tai.taxCalc.OutstandingDebt.title"), None)
    val childBenefitRow = createAdditionalTaxRow(taxAdjustmentComp(totalTax.otherTaxDue, tax.ChildBenefit), Messages("tai.taxCalc.childBenefit.title"), None)
    val excessGiftAidRow = createAdditionalTaxRow(taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessGiftAidTax), Messages("tai.taxCalc.excessGiftAidTax.title"), None)
    val excessWidowAndOrphansRow = createAdditionalTaxRow(taxAdjustmentComp(totalTax.otherTaxDue, tax.ExcessWidowsAndOrphans), Messages("tai.taxCalc.excessWidowsAndOrphans.title"), None)
    val pensionPaymentsRow = createAdditionalTaxRow(taxAdjustmentComp(totalTax.otherTaxDue, tax.PensionPaymentsAdjustment), Messages("tai.taxCalc.pensionPaymentsAdjustment.title"), None)

    Seq(underPaymentRow, inYearRow, outstandingDebtRow, childBenefitRow, excessGiftAidRow, excessWidowAndOrphansRow, pensionPaymentsRow).flatten
  }

  def createAdditionalTaxRow(row: Option[BigDecimal], description: String, url: Option[String]): Option[AdditionalTaxDetailRow] = {
    row.map(amount => AdditionalTaxDetailRow(description, amount, url))
  }

  def createReductionsTable(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit messages: Messages): Seq[ReductionTaxRow] = {
    val nonCodedIncome = totalTax.taxOnOtherIncome
    val nonCodedIncomeRow = createReductionTaxRow(nonCodedIncome, Messages("tai.taxCollected.atSource.otherIncome.description"),
      Messages("tai.taxCollected.atSource.otherIncome.title"))

    val ukDividend = taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxCreditOnUKDividends)
    val ukDividendRow = createReductionTaxRow(ukDividend, Messages("tai.taxCollected.atSource.dividends.description", 10),
      Messages("tai.taxCollected.atSource.dividends.title"))

    val bankInterest = taxAdjustmentComp(totalTax.alreadyTaxedAtSource, tax.TaxOnBankBSInterest)
    val bankInterestRow = createReductionTaxRow(bankInterest, Messages("tai.taxCollected.atSource.bank.description", 20),
      Messages("tai.taxCollected.atSource.bank.title"))

    val marriageAllowanceRow = createMarriageAllowanceRow(codingComponents, totalTax)
    val maintenancePaymentRow = createMaintenancePaymentRow(codingComponents, totalTax)

    val enterpriseInvestmentScheme = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.EnterpriseInvestmentSchemeRelief)
    val enterpriseInvestmentSchemeRow = createReductionTaxRow(enterpriseInvestmentScheme,
      Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.description"),
      Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.title"))

    val concessionRelief = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.ConcessionalRelief)
    val concessionReliefRow = createReductionTaxRow(concessionRelief,
      Messages("tai.taxCollected.atSource.concessionalRelief.description"),
      Messages("tai.taxCollected.atSource.concessionalRelief.title"))

    val doubleTaxationRelief = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.DoubleTaxationRelief)
    val doubleTaxationReliefRow = createReductionTaxRow(doubleTaxationRelief,
      Messages("tai.taxCollected.atSource.doubleTaxationRelief.description"),
      Messages("tai.taxCollected.atSource.doubleTaxationRelief.title"))

    Seq(
      nonCodedIncomeRow,
      ukDividendRow,
      bankInterestRow,
      marriageAllowanceRow,
      maintenancePaymentRow,
      enterpriseInvestmentSchemeRow,
      concessionReliefRow,
      doubleTaxationReliefRow
    ).flatten
  }

  def createMarriageAllowanceRow(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit messages: Messages) = {
    val marriageAllowance = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MarriedCouplesAllowance)
    val marriageAllowanceNpsComponent = codingComponents.find { component =>
      component.componentType match {
        case compType if compType == MarriedCouplesAllowanceMAE ||
          compType == MarriedCouplesAllowanceMCCP || compType == MarriedCouplesAllowanceToWifeWMA => true
        case _ => false
      }
    }.map(_.amount).getOrElse(BigDecimal(0))
    createReductionTaxRow(marriageAllowance,
      Messages("tai.taxCollected.atSource.marriageAllowance.description", MoneyPounds(marriageAllowanceNpsComponent).quantity,
        Link.toInternalPage(
          url = routes.YourTaxCodeController.taxCodes().toString,
          value = Some(Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText"))
        ).toHtml.body),
      Messages("tai.taxCollected.atSource.marriageAllowance.title"))
  }

  def createMaintenancePaymentRow(codingComponents: Seq[CodingComponent], totalTax: TotalTax)(implicit messages: Messages) = {
    val maintenancePayment = taxAdjustmentComp(totalTax.reliefsGivingBackTax, tax.MaintenancePayments)
    val maintenancePaymentGross = codingComponents.find(_.componentType == MaintenancePayments).map(_.amount).getOrElse(BigDecimal(0))

    createReductionTaxRow(maintenancePayment,
      Messages("tai.taxCollected.atSource.maintenancePayments.description", MoneyPounds(maintenancePaymentGross).quantity,
        routes.YourTaxCodeController.taxCodes().url),
      Messages("tai.taxCollected.atSource.marriageAllowance.title"))
  }

  def incomeTaxReducedToZeroMessage(hasTaxReducedToZero: Boolean)(implicit messages: Messages): Option[String] = {
    Option(hasTaxReducedToZero).collect{
      case true => Messages("tai.estimatedIncome.reductionsTax.incomeTaxReducedToZeroMessage")
    }
  }

  def createReductionTaxRow(row: Option[BigDecimal], description: String, title: String)(implicit messages: Messages) = {
    row.map(amount => ReductionTaxRow(description, amount, title))
  }

  def totalDividendIncome(incomeCategories: Seq[IncomeCategory]): BigDecimal = {
    incomeCategories.filter {
            category => category.incomeCategoryType == UkDividendsIncomeCategory ||
              category.incomeCategoryType == ForeignDividendsIncomeCategory
          }.map(_.totalIncome).sum
  }

  def taxFreeDividendAllowance(incomeCategories: Seq[IncomeCategory]): BigDecimal = {
    val taxBands = incomeCategories.flatMap(_.taxBands)

    taxBands.find(_.bandType == DividendZeroRate).flatMap(_.upperBand).getOrElse(BigDecimal(0))

  }

  def dividendsAllowanceRates(list: List[BigDecimal]): String = list match {
    case h :: Nil => h + "%"
    case h :: tail if tail.size > 1 => h + "%, " + dividendsAllowanceRates(tail)
    case h :: tail :: Nil => h + "% and " + tail + "%"
  }



}
