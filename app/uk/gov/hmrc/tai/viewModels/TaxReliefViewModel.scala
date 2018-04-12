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

package uk.gov.hmrc.tai.viewModels

import uk.gov.hmrc.tai.model.domain.AllowanceComponentType
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.util.ViewModelHelper

case class TaxReliefViewModel(
                               hasGiftAid: Boolean,
                               hasPersonalPension: Boolean,
                               giftAid: BigDecimal,
                               personalPension: BigDecimal,
                               giftAidRelief: BigDecimal,
                               personalPensionPaymentRelief: BigDecimal,
                               hasNoTaxableIncome: Boolean
                             ) extends ViewModelHelper

object TaxReliefViewModel {
  def apply(codingComponents: Seq[CodingComponent], totalTax: TotalTax): TaxReliefViewModel = {
    val giftAidPayment = findTaxReliefComponentAmount(totalTax, GiftAidPayments)
    val personalPensionPayment = findTaxReliefComponentAmount(totalTax, PersonalPensionPayment)
    val giftAidRelief = findTaxReliefComponentAmount(totalTax, GiftAidPaymentsRelief) getOrElse BigDecimal(0)
    val personalPensionRelief = findTaxReliefComponentAmount(totalTax, PersonalPensionPaymentRelief) getOrElse BigDecimal(0)

    TaxReliefViewModel(
      giftAidPayment.isDefined,
      personalPensionPayment.isDefined,
      giftAidPayment getOrElse BigDecimal(0),
      personalPensionPayment getOrElse BigDecimal(0),
      giftAidRelief,
      personalPensionRelief,
      hasNoTaxableIncome(codingComponents, totalTax)
    )
  }

  private def hasNoTaxableIncome(codingComponents: Seq[CodingComponent], totalTax: TotalTax) = {
    val totalIncome = totalTax.incomeCategories.map(_.totalIncome).sum
    val totalAllowance = codingComponents.filter(component => {
      component.componentType match {
        case _: AllowanceComponentType => true
        case _ => false
      }
    }).map(_.amount).sum

    totalIncome > totalAllowance
  }

  private def findTaxReliefComponentAmount(totalTax: TotalTax, component: TaxReliefComponent) = {
    totalTax.taxReliefComponent.
      flatMap(_.taxAdjustmentComponents.find(_.taxAdjustmentType == component)).map(_.taxAdjustmentAmount)
  }

}
