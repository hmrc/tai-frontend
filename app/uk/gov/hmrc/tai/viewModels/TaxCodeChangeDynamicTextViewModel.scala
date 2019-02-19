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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.TaxCodeChange
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{CodingComponentPair, CodingComponentTypeDescription, YourTaxFreeAmountComparison}

case class TaxCodeChangeDynamicTextViewModel(reasons: Seq[String])

object TaxCodeChangeDynamicTextViewModel {

  def apply(taxCodeChange: TaxCodeChange, taxFreeAmountComparison: YourTaxFreeAmountComparison)
           (implicit messages: Messages): TaxCodeChangeDynamicTextViewModel = {

    val allowancesChange: Seq[String] = translatePairsToDynamicText(taxFreeAmountComparison.iabdPairs.allowances)
    val deductionsChange: Seq[String] = translatePairsToDynamicText(taxFreeAmountComparison.iabdPairs.deductions)

    val reasons = createReasons(allowancesChange, deductionsChange)

    TaxCodeChangeDynamicTextViewModel(reasons)
  }

  private def createReasons(allowancesChange: Seq[String], deductionsChange: Seq[String])
                           (implicit messages: Messages): Seq[String] = {
    val reasons = allowancesChange ++ deductionsChange

    val genericReasonsForTaxCodeChange = reasons filter (_ == genericTaxCodeChangeReason)
    if (genericReasonsForTaxCodeChange.isEmpty && reasons.size <= 4) {
      reasons
    } else {
      Seq(genericTaxCodeChangeReason)
    }
  }

  private def translatePairsToDynamicText(pairs: Seq[CodingComponentPair])(implicit messages: Messages): Seq[String] = {
    val changedPairs = pairs.filter(pair => pair.previous.isDefined && pair.current.isDefined)
    changedPairs.flatMap(translateChangedCodingComponentPair(_)).distinct
  }

  private def translateChangedCodingComponentPair(pair: CodingComponentPair)(implicit messages: Messages): Option[String] = {
    val hasAnythingChanged: Boolean = pair match {
      case CodingComponentPair(_, _, previousAmount: Some[BigDecimal], currentAmount: Some[BigDecimal]) =>
        currentAmount.x != previousAmount.x
      case _ => false
    }

    (hasAnythingChanged) match {
      case true =>
        val componentType: String = CodingComponentTypeDescription.componentTypeToString(pair.componentType)
        Some(messages("tai.taxCodeComparison.iabd.new.allowanceOrDeduction", componentType))
      case false => None
    }
  }

  private def genericTaxCodeChangeReason(implicit messages: Messages): String = {
    messages("taxCode.change.yourTaxCodeChanged.paragraph")
  }
}
