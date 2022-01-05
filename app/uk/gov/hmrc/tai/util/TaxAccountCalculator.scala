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

package uk.gov.hmrc.tai.util

import uk.gov.hmrc.tai.model.domain.AllowanceComponentType
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

trait TaxAccountCalculator {
  def taxFreeAmount(codingComponents: Seq[CodingComponent]): BigDecimal
}

class TaxAccountCalculatorImpl extends TaxAccountCalculator {
  override def taxFreeAmount(codingComponents: Seq[CodingComponent]): BigDecimal =
    codingComponents.foldLeft(BigDecimal(0))((total: BigDecimal, component: CodingComponent) =>
      component.componentType match {
        case _: AllowanceComponentType => total + component.amount
        case _                         => total - component.amount
    })
}
