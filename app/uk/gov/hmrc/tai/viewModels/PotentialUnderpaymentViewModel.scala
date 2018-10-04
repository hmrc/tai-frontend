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
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.GoogleAnalyticsConstants._

case class PotentialUnderpaymentViewModel(iyaCYAmount: BigDecimal,
                                          iyaTaxCodeChangeAmount: BigDecimal,
                                          iyaCYPlusOneAmount: BigDecimal,
                                          iyaTotalAmount: BigDecimal,
                                          pageTitle: String,
                                          gaDimensions: Option[Map[String, String]] = None)

object PotentialUnderpaymentViewModel extends ViewModelHelper {

  def apply(taxAccountSummary: TaxAccountSummary, codingComponents: Seq[CodingComponent])(implicit messages:Messages): PotentialUnderpaymentViewModel = {

    val iyaTaxCodeChangeAmount = codingComponents.collect({
      case CodingComponent(EstimatedTaxYouOweThisYear, _, amount, _, _) => amount
    }).headOption.getOrElse(BigDecimal(0))

    val gaDimensions =
      (taxAccountSummary.totalInYearAdjustmentIntoCY, taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne) match {
        case (cy, ny) if cy > 0 && ny <= 0 =>
          Some(Map(valueOfIycdcPayment -> taxAccountSummary.totalInYearAdjustmentIntoCY.toString(), iycdcReconciliationStatus -> currentYear))
        case (cy, ny) if cy == 0 && ny > 0 =>
          Some(Map(valueOfIycdcPayment -> taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne.toString(), iycdcReconciliationStatus -> nextYear))
        case (cy, ny) if cy > 0 && ny > 0 =>
          Some(Map(valueOfIycdcPayment -> taxAccountSummary.totalInYearAdjustmentIntoCY.toString(), iycdcReconciliationStatus -> currentAndNextYear))
        case _ => None
      }

    val title =
      if(taxAccountSummary.totalInYearAdjustmentIntoCY > 0 && taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne <= 0){
        Messages("tai.iya.tax.you.owe.title")
      } else {
        Messages("tai.iya.tax.you.owe.cy-plus-one.title")
      }

    PotentialUnderpaymentViewModel(
      taxAccountSummary.totalInYearAdjustmentIntoCY,
      iyaTaxCodeChangeAmount,
      taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne,
      taxAccountSummary.totalInYearAdjustment,
      title,
      gaDimensions
    )
  }
}
