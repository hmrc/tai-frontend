/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels

import uk.gov.hmrc.tai.model.domain.UnderPaymentFromPreviousYear
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

final case class UnderpaymentDue(allowanceReducedBy: BigDecimal, sourceAmount: BigDecimal)

object UnderpaymentDue {
  def apply(codingComponents: Seq[CodingComponent]): UnderpaymentDue = {

    val underpaymentComponent = codingComponents.filter(_.componentType == UnderPaymentFromPreviousYear).headOption

    underpaymentComponent match {
      case Some(component) =>
        component.inputAmount match {
          case Some(amount) => UnderpaymentDue(component.amount, amount)
          case _            => UnderpaymentDue(0, 0)
        }
      case _ =>
        UnderpaymentDue(0, 0)
    }
  }
}
