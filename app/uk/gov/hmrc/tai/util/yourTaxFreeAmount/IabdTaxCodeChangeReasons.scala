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
import uk.gov.hmrc.tai.model.CodingComponentPair
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.util.MonetaryUtil

class IabdTaxCodeChangeReasons(totalTax: TotalTax)  {

  def reasons(iabdPairs: AllowancesAndDeductionPairs)(implicit messages: Messages): Seq[String] = {

    allowanceReasons(iabdPairs) ++ deductionReasons(iabdPairs)
    
  }

  private def allowanceReasons(iabdPairs: AllowancesAndDeductionPairs)(implicit messages: Messages): Seq[String] = {
    val whatsChangedPairs = iabdPairs.allowances.filter(pair => pair.previous.isDefined && pair.current.isDefined)
    val whatsNewPairs = iabdPairs.allowances.filter(pair => pair.previous.isEmpty && pair.current.isDefined)

    whatsNewPairs.map(translateNewBenefits(_)) ++
      whatsChangedPairs.flatMap(translateChangedBenefits(_))
  }

  private def deductionReasons(iabdPairs: AllowancesAndDeductionPairs)(implicit messages: Messages): Seq[String] = {
    val whatsChangedPairs = iabdPairs.deductions.filter(pair => pair.previous.isDefined && pair.current.isDefined)
    val whatsNewPairs = iabdPairs.deductions.filter(pair => pair.previous.isEmpty && pair.current.isDefined)
    val whatsNewUnderpaymentPairs = whatsNewPairs.filter(pair => pair.componentType == UnderPaymentFromPreviousYear || pair.componentType == EstimatedTaxYouOweThisYear || pair.componentType == EarlyYearsAdjustment)
    val whatsNewOtherPairs = whatsNewPairs.filterNot(pair => pair.componentType == UnderPaymentFromPreviousYear || pair.componentType == EstimatedTaxYouOweThisYear || pair.componentType == EarlyYearsAdjustment)

    whatsNewOtherPairs.map(translateNewBenefits(_)) ++
      whatsChangedPairs.flatMap(translateChangedBenefits(_)) ++
      whatsNewUnderpaymentPairs.map(translateNewBenefits(_))
  }

  private def translateNewBenefits(pair: CodingComponentPair)(implicit  messages: Messages): String = {
    def createYouHaveMessage(text: String): String = {
      pair.current match {
        case Some(value) => {
          val amountDue = TaxAmountDueFromUnderpayment.amountDue(value, totalTax)
          messages(text, MonetaryUtil.withPoundPrefix(amountDue.toInt))
        }
        case None => genericBenefitMessage
      }
    }

    pair.componentType match {
      case EarlyYearsAdjustment => messages("tai.taxCodeComparison.iabd.you.have.claimed.expenses")
      case UnderPaymentFromPreviousYear => createYouHaveMessage("tai.taxCodeComparison.iabd.you.have.underpaid")
      case EstimatedTaxYouOweThisYear => createYouHaveMessage("tai.taxCodeComparison.iabd.we.estimated.you.have.underpaid")
      case _ => messageWithComponentType(pair)
    }
  }

  private def translateChangedBenefits(pair: CodingComponentPair)(implicit messages: Messages): Option[String] = {
    val hasAnythingChanged: Boolean = pair match {
      case CodingComponentPair(_, _, previousAmount: Some[BigDecimal], currentAmount: Some[BigDecimal]) =>
        currentAmount.x != previousAmount.x
      case _ => false
    }

    (hasAnythingChanged) match {
      case true => Some(messageWithComponentType(pair))
      case false => None
    }
  }

  private def messageWithComponentType(pair: CodingComponentPair)(implicit messages: Messages) = {
    messages("tai.taxCodeComparison.iabd.updated", pair.componentType.toMessage())
  }

  private def genericBenefitMessage(implicit messages: Messages): String = {
    messages("taxCode.change.yourTaxCodeChanged.paragraph")
  }
}
