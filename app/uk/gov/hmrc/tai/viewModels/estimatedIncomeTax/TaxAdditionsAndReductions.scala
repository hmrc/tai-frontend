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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.{TaxAdjustment, TaxAdjustmentType}
import uk.gov.hmrc.tai.model.domain.{EstimatedTaxYouOweThisYear, OutstandingDebt, UnderPaymentFromPreviousYear}

trait TaxAdditionsAndReductions {

  def taxAdjustmentComp(taxAdjustment: Option[TaxAdjustment], adjustmentType: TaxAdjustmentType) = {
    taxAdjustment.
      flatMap(_.taxAdjustmentComponents.find(_.taxAdjustmentType == adjustmentType))
      .map(_.taxAdjustmentAmount)
  }

  def underPaymentFromPreviousYear(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == UnderPaymentFromPreviousYear).flatMap(_.inputAmount)

  def inYearAdjustment(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == EstimatedTaxYouOweThisYear).flatMap(_.inputAmount)

  def outstandingDebt(codingComponents: Seq[CodingComponent]) = codingComponents.find(_.componentType == OutstandingDebt).map(_.amount)

}
