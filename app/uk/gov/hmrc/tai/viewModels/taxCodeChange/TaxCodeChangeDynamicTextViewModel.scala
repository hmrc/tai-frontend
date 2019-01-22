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

import uk.gov.hmrc.tai.model.domain.TaxCodeChange
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{CodingComponentPair, YourTaxFreeAmountComparison}

case class TaxCodeChangeDynamicTextViewModel(dynamicText: Seq[String])

object TaxCodeChangeDynamicTextViewModel {
  def apply(taxCodeChange: TaxCodeChange, taxFreeAmountComparison: YourTaxFreeAmountComparison): TaxCodeChangeDynamicTextViewModel = {

    val allowancesChange: Seq[String] = translatePairsToDynamicText(taxFreeAmountComparison.iabdPairs.allowances)
    val deductionsChange: Seq[String] = translatePairsToDynamicText(taxFreeAmountComparison.iabdPairs.deductions)

    val reasons = allowancesChange ++ deductionsChange

    TaxCodeChangeDynamicTextViewModel(reasons)
  }

  private def translatePairsToDynamicText(pairs: Seq[CodingComponentPair]): Seq[String] = {
//    val removedCodingComponentPairs = pairs.filter(pair => pair.previous.isDefined && pair.current.isEmpty)
//    val addedCodingComponentPairs = pairs.filter(pair => pair.previous.isEmpty && pair.current.isDefined)
    val changedPairs = pairs.filter(pair => pair.previous.isDefined && pair.current.isDefined)

    val describedChangedPairs = changedPairs.flatMap(translateChangedCodingComponentPair)
//    val describedAddedPairs = addedCodingComponentPairs.flatMap(translateAddedCodingComponentPair)
//    val describedRemovedPairs = removedCodingComponentPairs.flatMap(translateRemovedCodingComponentPair)

    describedChangedPairs
  }

  private def translateRemovedCodingComponentPair(pair: CodingComponentPair): Option[String] = {
    None
  }

  private def translateAddedCodingComponentPair(pair: CodingComponentPair): Option[String] = {
    None
  }

  private def translateChangedCodingComponentPair(pair: CodingComponentPair): Option[String] = {
    val direction: Option[String] = pair match {
      case CodingComponentPair(_, _, p: Some[BigDecimal], c: Some[BigDecimal]) if c.x > p.x => Some("increased")
      case CodingComponentPair(_, _, p: Some[BigDecimal], c: Some[BigDecimal]) if c.x < p.x => Some("decreased")
      case _ => None
    }

    direction.map(d => {
      s"${pair.componentType} for ${pair.employmentId} has $d from ${pair.previous} to ${pair.current}"
    })
  }
}
