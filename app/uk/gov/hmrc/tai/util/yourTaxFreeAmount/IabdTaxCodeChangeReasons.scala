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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.CodingComponentPair
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.util.MonetaryUtil

class IabdTaxCodeChangeReasons {

  def reasons(iabdPairs: AllowancesAndDeductionPairs)(implicit messages: Messages): Seq[String] =
    allowanceReasons(iabdPairs) ++ deductionReasons(iabdPairs)

  private def allowanceReasons(iabdPairs: AllowancesAndDeductionPairs)(implicit messages: Messages): Seq[String] = {
    val whatsChangedPairs = iabdPairs.allowances.filter(isChangedAmount)
    val whatsNewPairs     = iabdPairs.allowances.filter(isNewAmount)

    whatsNewPairs.flatMap(translateNewBenefits(_)) ++
      whatsChangedPairs.flatMap(translateChangedBenefits(_))
  }

  private def deductionReasons(iabdPairs: AllowancesAndDeductionPairs)(implicit messages: Messages): Seq[String] = {
    val whatsChangedPairs         = iabdPairs.deductions.filter(isChangedAmount)
    val whatsNewPairs             = iabdPairs.deductions.filter(isNewAmount)
    val whatsNewUnderpaymentPairs = whatsNewPairs.filter(reasonIsDebt)
    val whatsNewOtherPairs        = whatsNewPairs.filterNot(reasonIsDebt)

    whatsNewOtherPairs.flatMap(translateNewBenefits(_)) ++
      whatsChangedPairs.flatMap(translateChangedBenefits(_)) ++
      whatsNewUnderpaymentPairs.flatMap(translateNewBenefits(_))
  }

  private def reasonIsDebt(pair: CodingComponentPair): Boolean =
    pair.componentType == UnderPaymentFromPreviousYear || pair.componentType == EstimatedTaxYouOweThisYear || pair.componentType == EarlyYearsAdjustment

  private def isNewAmount(pair: CodingComponentPair): Boolean =
    pair.previous.isEmpty && pair.current.isDefined

  private def isChangedAmount(pair: CodingComponentPair): Boolean =
    pair.previous.isDefined && pair.current.isDefined

  private def buildMsgKey(msgKey: String, isSingular: Boolean) = s"$msgKey.${if (isSingular) "singular" else "plural"}"

  private def translateNewBenefits(pair: CodingComponentPair)(implicit messages: Messages): Option[String] = {

    def formattedValue(value: Option[BigDecimal]) =
      MonetaryUtil.withPoundPrefixBD(value.getOrElse(BigDecimal(0)))

    pair.current.map { currentAmount =>
      pair.componentType match {
        case EarlyYearsAdjustment         =>
          messages("tai.taxCodeComparison.iabd.you.have.claimed.expenses")
        case UnderPaymentFromPreviousYear =>
          messages("tai.taxCodeComparison.iabd.you.have.underpaid", formattedValue(pair.currentInputAmount))
        case EstimatedTaxYouOweThisYear   =>
          messages(
            "tai.taxCodeComparison.iabd.we.estimated.you.have.underpaid",
            formattedValue(pair.currentInputAmount)
          )
        case HICBCPaye                    =>
          val previousAmount: BigDecimal = pair.previous.getOrElse(0)
          val adjustmentMessage          =
            if (previousAmount < currentAmount) {
              messages("tai.taxCodeComparison.iabd.increased")
            } else {
              messages("tai.taxCodeComparison.iabd.reduced")
            }
          val msgInfo                    = HICBCPaye.toV2Message()
          messages(
            buildMsgKey("tai.taxCodeComparison.iabd.amended", msgInfo.isSingular),
            msgInfo.msg,
            adjustmentMessage,
            MonetaryUtil.withPoundPrefixBD(previousAmount),
            MonetaryUtil.withPoundPrefixBD(currentAmount)
          )

        case taxComponentType =>
          val msgInfo = taxComponentType.toV2Message()
          messages(
            "tai.taxCodeComparison.iabd.added",
            msgInfo.msg,
            MonetaryUtil.withPoundPrefix(currentAmount.toInt)
          )
      }
    }
  }

  private def translateChangedBenefits(pair: CodingComponentPair)(implicit messages: Messages): Option[String] = {

    val createAmendmentMessage: (BigDecimal, BigDecimal) => String =
      (previousAmount: BigDecimal, currentAmount: BigDecimal) => {
        val adjustmentMessage: String =
          if (previousAmount < currentAmount) {
            messages("tai.taxCodeComparison.iabd.increased")
          } else {
            messages("tai.taxCodeComparison.iabd.reduced")
          }

        val msgInfo = pair.componentType.toV2Message()

        messages(
          buildMsgKey("tai.taxCodeComparison.iabd.amended", msgInfo.isSingular),
          msgInfo.msg,
          adjustmentMessage,
          MonetaryUtil.withPoundPrefixBD(previousAmount),
          MonetaryUtil.withPoundPrefixBD(currentAmount)
        )
      }

    (pair.previous, pair.current) match {
      case (Some(previousAmount), Some(currentAmount)) if previousAmount != currentAmount =>
        Some(createAmendmentMessage(previousAmount, currentAmount))
      case _                                                                              => None
    }
  }
}
