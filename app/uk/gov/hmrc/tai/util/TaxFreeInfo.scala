/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

case class TaxFreeInfo(date: String, annualTaxFreeAmount: BigDecimal, personalAllowance : BigDecimal)

object TaxFreeInfo extends TaxAccountCalculator with isPersonalAllowance {

  def apply(date: String, codingComponents: Seq[CodingComponent])(implicit messages: Messages): TaxFreeInfo = {
    val annualTaxFreeAmount = taxFreeAmount(codingComponents)
    val personalAllowanceAmount = sumOfPersonalAllowances(codingComponents)

    TaxFreeInfo(date, annualTaxFreeAmount, personalAllowanceAmount)
  }

  private def sumOfPersonalAllowances(codingComponents: Seq[CodingComponent]): BigDecimal = {
    codingComponents.filter(isPersonalAllowanceComponent).map(_.amount).sum
  }
}