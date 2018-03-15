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

import uk.gov.hmrc.tai.model.domain.{EstimatedTaxYouOweThisYear, TaxAccountSummary}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.util.ViewModelHelper

case class PotentialUnderpaymentViewModelNEW(iyaCYAmount: BigDecimal,
                                             iyaTaxCodeChangeAmount: BigDecimal,
                                             iyaCYPlusOneAmount: BigDecimal,
                                             iyaTotalAmount: BigDecimal,
                                             pageTitle: String,
                                             gaDimensions: Option[Map[String, String]] = None)

object PotentialUnderpaymentViewModelNEW extends ViewModelHelper {

  def apply(taxAccountSummary: TaxAccountSummary, codingComponents: Seq[CodingComponent]): PotentialUnderpaymentViewModelNEW = {

    val iyaTaxCodeChangeAmount = codingComponents.collect({
      case CodingComponent(EstimatedTaxYouOweThisYear, _, amount, _) => amount
    }).headOption.getOrElse(BigDecimal(0))

    val gaDimensions =
      (taxAccountSummary.totalInYearAdjustmentIntoCY, taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne) match {
        case (cy, ny) if cy > 0 && ny <= 0 =>
          Some(Map("valueOfIycdcPayment" -> taxAccountSummary.totalInYearAdjustmentIntoCY.toString(), "iycdcReconciliationStatus" -> "Current Year"))
        case (cy, ny) if cy == 0 && ny > 0 =>
          Some(Map("valueOfIycdcPayment" -> taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne.toString(), "iycdcReconciliationStatus" -> "Next Year"))
        case (cy, ny) if cy > 0 && ny > 0 =>
          Some(Map("valueOfIycdcPayment" -> taxAccountSummary.totalInYearAdjustmentIntoCY.toString(), "iycdcReconciliationStatus" -> "Current and Next Year"))
        case _ => None
      }

    val title =
      if(taxAccountSummary.totalInYearAdjustmentIntoCY > 0 && taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne <= 0){
        Messages("tai.iya.tax.you.owe.title")
      } else {
        Messages("tai.iya.tax.you.owe.cy-plus-one.title")
      }

    PotentialUnderpaymentViewModelNEW(
      taxAccountSummary.totalInYearAdjustmentIntoCY,
      iyaTaxCodeChangeAmount,
      taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne,
      taxAccountSummary.totalInYearAdjustment,
      title,
      gaDimensions
    )
  }
}
