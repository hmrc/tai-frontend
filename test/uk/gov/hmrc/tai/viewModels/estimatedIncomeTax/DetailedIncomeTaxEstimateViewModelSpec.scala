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
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, OtherNonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{MaintenancePayments => _, _}
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.util.BandTypesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{AdditionalTaxDetailRow, ReductionTaxRow}
import uk.gov.hmrc.urls.Link

import scala.collection.immutable.Seq

class DetailedIncomeTaxEstimateViewModelSpec extends TaiViewSpec with BandTypesConstants {

  val taxAccountSummary = TaxAccountSummary(18573,0,0,0,0)
  val totalTax = TotalTax(0,Seq.empty[IncomeCategory],None,None,None)
  val viewModel = DetailedIncomeTaxEstimateViewModel(totalTax,
    Seq.empty[TaxCodeIncome],taxAccountSummary,Seq.empty[CodingComponent],NonTaxCodeIncome(None,Seq.empty[OtherNonTaxCodeIncome]))

  override def view: Html = views.html.estimatedIncomeTax.detailedIncomeTaxEstimate(viewModel)

  "createAdditionalTaxTable" must {

    "return additional tax detail rows" when {

      "there are additional tax due" in {
        val otherTaxDue = Seq(
          TaxAdjustmentComponent(tax.ExcessGiftAidTax, 100),
          TaxAdjustmentComponent(tax.ExcessWidowsAndOrphans, 100),
          TaxAdjustmentComponent(tax.PensionPaymentsAdjustment, 200),
          TaxAdjustmentComponent(tax.ChildBenefit, 300)
        )
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, Some(tax.TaxAdjustment(700, otherTaxDue)), None, None)
        val codingComponents = Seq(
          CodingComponent(UnderPaymentFromPreviousYear, None, 100, "", Some(10)),
          CodingComponent(EstimatedTaxYouOweThisYear, None, 0, "", Some(50)),
          CodingComponent(OutstandingDebt, None, 150, "")
        )

        val result = DetailedIncomeTaxEstimateViewModel.createAdditionalTaxTable(codingComponents, totalTax)

        result mustBe Seq(
          AdditionalTaxDetailRow(Messages("tai.taxCalc.UnderpaymentPreviousYear.title"), 10, None),
          AdditionalTaxDetailRow(Messages("tai.taxcode.deduction.type-45"), 50, Some(routes.PotentialUnderpaymentController.potentialUnderpaymentPage().url)),
          AdditionalTaxDetailRow(Messages("tai.taxCalc.OutstandingDebt.title"), 150, None),
          AdditionalTaxDetailRow(Messages("tai.taxCalc.childBenefit.title"), 300, None),
          AdditionalTaxDetailRow(Messages("tai.taxCalc.excessGiftAidTax.title"), 100, None),
          AdditionalTaxDetailRow(Messages("tai.taxCalc.excessWidowsAndOrphans.title"), 100, None),
          AdditionalTaxDetailRow(Messages("tai.taxCalc.pensionPaymentsAdjustment.title"), 200, None)
        )
      }
    }

    "return empty row" when {

      "there is no additional tax due" in {
        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)

        val result = DetailedIncomeTaxEstimateViewModel.createAdditionalTaxTable(Seq.empty[CodingComponent], totalTax)

        result mustBe Seq.empty[AdditionalTaxDetailRow]
      }
    }
  }

  "createReductionsTable" must {

    "return reduction tax table" when {

      "there are components present which can reduce the tax" in {

        val alreadyTaxedAtSource = Seq(
          TaxAdjustmentComponent(tax.TaxOnBankBSInterest, 100),
          TaxAdjustmentComponent(tax.TaxCreditOnUKDividends, 200),
          TaxAdjustmentComponent(tax.TaxCreditOnForeignInterest, 300),
          TaxAdjustmentComponent(tax.TaxCreditOnForeignIncomeDividends, 400)
        )

        val reliefsGivingBackTax = Seq(
          TaxAdjustmentComponent(tax.EnterpriseInvestmentSchemeRelief, 500),
          TaxAdjustmentComponent(tax.ConcessionalRelief, 600),
          TaxAdjustmentComponent(tax.MaintenancePayments, 700),
          TaxAdjustmentComponent(tax.MarriedCouplesAllowance, 800),
          TaxAdjustmentComponent(tax.DoubleTaxationRelief, 900)
        )

        val taxReliefComponent = Seq(
          TaxAdjustmentComponent(tax.GiftAidPaymentsRelief, 1000),
          TaxAdjustmentComponent(tax.PersonalPensionPaymentRelief, 1100)
        )

        val totalTax = TotalTax(0, Seq.empty[IncomeCategory],
          Some(tax.TaxAdjustment(3500, reliefsGivingBackTax)),
          None,
          Some(tax.TaxAdjustment(1000, alreadyTaxedAtSource)),
          Some(100),
          Some(tax.TaxAdjustment(2100, taxReliefComponent))
        )

        val codingComponents = Seq(
          CodingComponent(MarriedCouplesAllowanceMAE, None, 1200, "", None)
        )

        val result = DetailedIncomeTaxEstimateViewModel.createReductionsTable(codingComponents, totalTax)

        result mustBe Seq(
          ReductionTaxRow(Messages("tai.taxCollected.atSource.otherIncome.description"), 100, Messages("tai.taxCollected.atSource.otherIncome.title")),
          ReductionTaxRow(Messages("tai.taxCollected.atSource.dividends.description", 10), 200, Messages("tai.taxCollected.atSource.dividends.title")),
          ReductionTaxRow(Messages("tai.taxCollected.atSource.bank.description", 20), 100, Messages("tai.taxCollected.atSource.bank.title")),
          ReductionTaxRow(Messages("tai.taxCollected.atSource.marriageAllowance.description", MoneyPounds(1200).quantity,
            Link.toInternalPage(
              url = routes.YourTaxCodeController.taxCodes().toString,
              value = Some(Messages("tai.taxCollected.atSource.marriageAllowance.description.linkText"))
            ).toHtml.body), 800, Messages("tai.taxCollected.atSource.marriageAllowance.title")),
          ReductionTaxRow(Messages("tai.taxCollected.atSource.maintenancePayments.description", MoneyPounds(1200).quantity,
            routes.YourTaxCodeController.taxCodes().url), 700, Messages("tai.taxCollected.atSource.marriageAllowance.title")),
          ReductionTaxRow(Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.description"),
            500, Messages("tai.taxCollected.atSource.enterpriseInvestmentSchemeRelief.title")),
          ReductionTaxRow(Messages("tai.taxCollected.atSource.concessionalRelief.description"),
            600, Messages("tai.taxCollected.atSource.concessionalRelief.title")),
          ReductionTaxRow(Messages("tai.taxCollected.atSource.doubleTaxationRelief.description"),
            900, Messages("tai.taxCollected.atSource.doubleTaxationRelief.title")),
          ReductionTaxRow(Messages("gift.aid.tax.relief",0,1000),
            1000, Messages("gift.aid.savings")),
          ReductionTaxRow(Messages("personal.pension.payment.relief",0,1100),
            1100, Messages("personal.pension.payments"))
        )
      }
    }

    "return empty reduction tax table" when {

      "there is no reduction in tax" in {

        val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
        val result = DetailedIncomeTaxEstimateViewModel.createReductionsTable(Seq.empty[CodingComponent], totalTax)

        result mustBe Seq.empty[ReductionTaxRow]
      }
    }
  }
}
