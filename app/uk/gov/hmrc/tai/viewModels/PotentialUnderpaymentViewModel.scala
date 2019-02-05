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

package uk.gov.hmrc.tai.viewModels

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{EstimatedTaxYouOweThisYear, TaxAccountSummary}
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants._
import uk.gov.hmrc.urls.Link

case class PotentialUnderpaymentViewModel(iyaCYAmount: BigDecimal,
                                          iyaTaxCodeChangeAmount: BigDecimal,
                                          iyaCYPlusOneAmount: BigDecimal,
                                          iyaTotalAmount: BigDecimal,
                                          pageTitle: String,
                                          returnLink: Html,
                                          gaDimensions: Option[Map[String, String]] = None)

object PotentialUnderpaymentViewModel extends ViewModelHelper {

  def apply(taxAccountSummary: TaxAccountSummary, codingComponents: Seq[CodingComponent], referer: String, resourceName: String)(implicit messages:Messages): PotentialUnderpaymentViewModel = {

    val iyaTaxCodeChangeAmount = codingComponents.collectFirst {
      case CodingComponent(EstimatedTaxYouOweThisYear, _, amount, _, _) => amount
    }.getOrElse(BigDecimal(0))

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

    PotentialUnderpaymentViewModel(
      taxAccountSummary.totalInYearAdjustmentIntoCY,
      iyaTaxCodeChangeAmount,
      taxAccountSummary.totalInYearAdjustmentIntoCYPlusOne,
      taxAccountSummary.totalInYearAdjustment,
      Messages("tai.iya.tax.you.owe.title"),
      returnLink(referer, resourceName),
      gaDimensions
    )
  }

  def returnLink(referer: String, resourceName: String)(implicit messages:Messages): Html = {

    def createLink(message: String, defaultReferer : String = referer): Html = Link.toInternalPage(url=defaultReferer,value=Some(message)).toHtml

    resourceName match {
      case "tax-free-allowance"           => createLink(messages("tai.iya.tax.free.amount.return.link"))
      case "detailed-income-tax-estimate" => createLink(messages("tai.iya.detailed.paye.return.link"))
      case "your-tax-free-amount"         => createLink(messages("tai.iya.tax.code.change.return.link"))
      case "underpayment-estimate"        => createLink(messages("tai.label.back"), routes.TaxAccountSummaryController.onPageLoad.toString)
      case _                              => createLink(messages("tai.label.back"))
    }
  }

}
