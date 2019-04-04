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

package uk.gov.hmrc.tai.model

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.TaxComponentType
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.viewModels.TaxSummaryLabel

case class CodingComponentPair(componentType: TaxComponentType, employmentId: Option[Int], previous: Option[BigDecimal], current: Option[BigDecimal])

case class CodingComponentPairModel(label: TaxSummaryLabel, previous: BigDecimal, current: BigDecimal)

object CodingComponentPairModel {
  def apply(labelText: String, previousAmount: BigDecimal, currentAmount: BigDecimal): CodingComponentPairModel = {
    CodingComponentPairModel(TaxSummaryLabel(labelText) ,previousAmount, currentAmount)
  }

  def apply(codingComponentPair: CodingComponentPair,
            employmentIds: Map[Int, String],
            companyCarBenefits: Seq[CompanyCarBenefit],
            totalTax: TotalTax)
           (implicit messages: Messages): CodingComponentPairModel = {

    val previousAmount: BigDecimal = codingComponentPair.previous.getOrElse(0)
    val currentAmount: BigDecimal = codingComponentPair.current.getOrElse(0)
    val label = TaxSummaryLabel(codingComponentPair.componentType, codingComponentPair.employmentId, companyCarBenefits, employmentIds, currentAmount, totalTax)

    CodingComponentPairModel(label, previousAmount, currentAmount)
  }
}
