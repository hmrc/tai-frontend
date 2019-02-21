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

class IabdTaxCodeChangeReasons {

  def reasons(iabdPairs: AllowancesAndDeductionPairs)(implicit messages: Messages): Seq[String] = {

    val combinedBenefits = iabdPairs.allowances ++ iabdPairs.deductions

    val whatsChangedinPairs = combinedBenefits.filter(pair => pair.previous.isDefined && pair.current.isDefined)
    whatsChangedinPairs.flatMap(translateChangedCodingComponentPair(_))
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
}
