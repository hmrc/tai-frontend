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

package controllers.viewModels

import controllers.ViewModelFactory
import controllers.auth.TaiUser
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.TaxSummaryDetails
import uk.gov.hmrc.tai.model.nps2.DeductionType
import uk.gov.hmrc.tai.util.TaxSummaryHelper
import uk.gov.hmrc.tai.viewModels.PotentialUnderpaymentViewModel

object PotentialUnderpaymentPageVM extends ViewModelFactory {
  override type ViewModelType = PotentialUnderpaymentViewModel
  override def createObject(nino: Nino, details: TaxSummaryDetails)(implicit user: TaiUser, hc: HeaderCarrier): PotentialUnderpaymentViewModel = {

    def getIyaCYTaxCodeAmount :Option[BigDecimal] = {
      val deductions = details.taxCodeDetails.flatMap(_.deductions.map(deductionList => deductionList))
      val matchingDeductions = deductions.getOrElse(List()).filter(deduction => deduction.componentType == Some(DeductionType.InYearAdjustment.code))
      matchingDeductions match {
        case deduction :: rest => deduction.amount
        case _ => None
      }
    }

    val iyaCY = TaxSummaryHelper.getIyaCY(details)
    val iyaCYPlusOne = TaxSummaryHelper.getIyaCYPlusOne(details)

    PotentialUnderpaymentViewModel(
      displayCYOnly = {iyaCY > 0 && iyaCYPlusOne <= 0},
      displayCYPlusOneOnly = {iyaCY == 0 && iyaCYPlusOne > 0},
      displayCYAndCYPlusOneOnly = {iyaCY > 0 && iyaCYPlusOne > 0},
      displayNoValues = {iyaCY == 0 && iyaCYPlusOne == 0},
      iyaCYAmount = iyaCY,
      iyaCYPlusOneAmount= iyaCYPlusOne,
      iyaTotalAmount = TaxSummaryHelper.getTotalIya(details),
      iyaTaxCodeChangeAmount= getIyaCYTaxCodeAmount.getOrElse(BigDecimal(0))
    )
  }
}
