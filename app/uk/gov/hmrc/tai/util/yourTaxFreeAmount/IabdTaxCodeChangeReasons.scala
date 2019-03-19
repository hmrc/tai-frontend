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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.util.MonetaryUtil

class IabdTaxCodeChangeReasons(totalTax: TotalTax) {

  def reasons(iabdPairs: AllowancesAndDeductionPairs)(implicit messages: Messages): Seq[String] = {

    val combinedBenefits = iabdPairs.allowances ++ iabdPairs.deductions

    val whatsChangedPairs = combinedBenefits.filter(pair => pair.previous.isDefined && pair.current.isDefined)
    val whatsNewPairs = combinedBenefits.filter(pair => pair.previous.isEmpty && pair.current.isDefined)

    whatsNewPairs.flatMap(translateNewBenefits(_)) ++ whatsChangedPairs.flatMap(translateChangedBenefits(_))
  }

  private def translateNewBenefits(pair: CodingComponentPair)(implicit messages: Messages): Option[String] = {

    val createNewBenefitsMessage: (TaxComponentType, BigDecimal) => String = (taxComponentType: TaxComponentType, currentAmount: BigDecimal) => {
      val createYouHaveMessage: String => String = (text: String) => {
        val amountDue = TaxAmountDueFromUnderpayment.amountDue(currentAmount, totalTax)
        messages(text, MonetaryUtil.withPoundPrefix(amountDue.toInt))
      }

      taxComponentType match {
        case EarlyYearsAdjustment => messages("tai.taxCodeComparison.iabd.you.have.claimed.expenses")
        case UnderPaymentFromPreviousYear => createYouHaveMessage("tai.taxCodeComparison.iabd.you.have.underpaid")
        case EstimatedTaxYouOweThisYear => createYouHaveMessage("tai.taxCodeComparison.iabd.we.estimated.you.have.underpaid")
        case _ => messages(
          "tai.taxCodeComparison.iabd.added",
          CodingComponentTypeDescription.componentTypeToString(taxComponentType),
          MonetaryUtil.withPoundPrefix(currentAmount.toInt)
        )
      }
    }

    pair.current map (currentAmount => createNewBenefitsMessage(pair.componentType, currentAmount))
  }

  private def translateChangedBenefits(pair: CodingComponentPair)(implicit messages: Messages): Option[String] = {

    val createAmmendmentMessage: (BigDecimal, BigDecimal) => String = (previousAmount: BigDecimal, currentAmount: BigDecimal) => {
      val adjustmentMessage: String = {
        if (previousAmount < currentAmount) {
          messages("tai.taxCodeComparison.iabd.increased")
        } else {
          messages("tai.taxCodeComparison.iabd.decreased")
        }
      }

      messages(
        "tai.taxCodeComparison.iabd.ammended",
        CodingComponentTypeDescription.componentTypeToString(pair.componentType),
        adjustmentMessage,
        MonetaryUtil.withPoundPrefix(previousAmount.toInt),
        MonetaryUtil.withPoundPrefix(currentAmount.toInt)
      )
    }

    (pair.previous, pair.current) match {
      case (Some(previousAmount), Some(currentAmount)) if(previousAmount != currentAmount) =>
        Some(createAmmendmentMessage(previousAmount, currentAmount))
      case _ => None
    }
  }
}
