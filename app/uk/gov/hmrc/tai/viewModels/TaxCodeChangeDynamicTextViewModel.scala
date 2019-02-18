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

  def apply(taxCodeChange: TaxCodeChange, taxFreeAmountComparison: YourTaxFreeAmountComparison, employmentsMap: Map[Int, String])
           (implicit messages: Messages): TaxCodeChangeDynamicTextViewModel = {

    val allowancesChange: Seq[String] = translatePairsToDynamicText(taxFreeAmountComparison.iabdPairs.allowances, employmentsMap)
    val deductionsChange: Seq[String] = translatePairsToDynamicText(taxFreeAmountComparison.iabdPairs.deductions, employmentsMap)

    val reasons = createReasons(allowancesChange, deductionsChange)

    TaxCodeChangeDynamicTextViewModel(reasons)
  }

  private def createReasons(allowancesChange: Seq[String], deductionsChange: Seq[String])
                           (implicit messages: Messages): Seq[String] = {
    val reasons = allowancesChange ++ deductionsChange
    val genericReasonsForTaxCodeChange = reasons filter (_ == genericTaxCodeChangeReason)
    if (genericReasonsForTaxCodeChange.isEmpty || reasons.size <= 4) {
      reasons
    } else {
      Seq(genericTaxCodeChangeReason)
    }
  }

  private def translatePairsToDynamicText(pairs: Seq[CodingComponentPair], employmentsMap: Map[Int, String])(implicit messages: Messages): Seq[String] = {
    val changedPairs = pairs.filter(pair => pair.previous.isDefined && pair.current.isDefined)
    changedPairs.map(translateChangedCodingComponentPair(_, employmentsMap))
  }

  private def translateChangedCodingComponentPair(pair: CodingComponentPair, employmentsMap: Map[Int, String])(implicit messages: Messages): String = {
    val direction: Option[String] = pair match {
      case CodingComponentPair(_, _, previousAmount: Some[BigDecimal], currentAmount: Some[BigDecimal])
        if currentAmount.x > previousAmount.x => Some("increased")
      case CodingComponentPair(_, _, previousAmount: Some[BigDecimal], currentAmount: Some[BigDecimal])
        if currentAmount.x < previousAmount.x => Some("decreased")
      case _ => None
    }

    (pair.employmentId, pair.previous, pair.current, direction) match {
      case (Some(id), Some(previousAmount), Some(currentAmount), Some(d)) =>

        val employmentName: String = employmentsMap(id)
        val componentType: String = CodingComponentTypeDescription.componentTypeToString(pair.componentType)

        s"${componentType} from ${employmentName} has ${d} from ${previousAmount} to ${currentAmount}"
      case _ => genericTaxCodeChangeReason
    }
  }

  private def genericTaxCodeChangeReason(implicit messages: Messages): String = {
    Messages("taxCode.change.yourTaxCodeChanged.paragraph")
  }
}
