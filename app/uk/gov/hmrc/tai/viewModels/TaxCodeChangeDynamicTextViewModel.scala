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
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{CodingComponentPair, CodingComponentPairDescription, CodingComponentTypeDescription, YourTaxFreeAmountComparison}

case class TaxCodeChangeDynamicTextViewModel(reasons: Seq[String])

object TaxCodeChangeDynamicTextViewModel {

  def apply(taxCodeChange: TaxCodeChange, taxFreeAmountComparison: YourTaxFreeAmountComparison, employmentsMap: Map[Int, String])
           (implicit messages: Messages): TaxCodeChangeDynamicTextViewModel = {

    val allowancesChange: Seq[String] = translatePairsToDynamicText(taxFreeAmountComparison.iabdPairs.allowances, employmentsMap)
    val deductionsChange: Seq[String] = translatePairsToDynamicText(taxFreeAmountComparison.iabdPairs.deductions, employmentsMap)

    val reasons = allowancesChange ++ deductionsChange

    TaxCodeChangeDynamicTextViewModel(reasons)
  }

  private def translatePairsToDynamicText(pairs: Seq[CodingComponentPair], employmentsMap: Map[Int, String])(implicit messages: Messages): Seq[String] = {
    val changedPairs = pairs.filter(pair => pair.previous.isDefined && pair.current.isDefined)

    val describedChangedPairs = changedPairs.flatMap(translateChangedCodingComponentPair(_, employmentsMap))
    describedChangedPairs
  }

  private def translateChangedCodingComponentPair(pair: CodingComponentPair, employmentsMap: Map[Int, String])(implicit messages: Messages): Option[String] = {
    val direction: Option[String] = pair match {
      case CodingComponentPair(_, _, p: Some[BigDecimal], c: Some[BigDecimal]) if c.x > p.x => Some("increased")
      case CodingComponentPair(_, _, p: Some[BigDecimal], c: Some[BigDecimal]) if c.x < p.x => Some("decreased")
      case _ => None
    }

    val componentType: String = CodingComponentTypeDescription.componentTypeToString(pair.componentType)
    val employmentName = employmentsMap(pair.employmentId.getOrElse(0))
//      List("Job expenses from Sainsburys has increased from Some(50) to Some(100)", "Car benefit from from Tesco has increased from Some(1000) to Some(2000)")
//      List("Job Expense from Sainsburys has increased from 50 to 100", "Car Benefit from TESCO has increased from 1000 to 2000") (TaxCodeChangeDynamicTextViewModelSpec.scala:43)

    direction.map(d => {
      s"${componentType} from ${employmentName} has $d from ${pair.previous.getOrElse(0)} to ${pair.current.getOrElse(0)}"
    })
  }
}
