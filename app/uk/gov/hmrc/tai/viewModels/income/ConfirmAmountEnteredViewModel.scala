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

package uk.gov.hmrc.tai.viewModels.income

import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.util.{MapForGoogleAnalytics, MonetaryUtil, TaxYearRangeUtil}
import uk.gov.hmrc.tai.util.ViewModelHelper.withPoundPrefix
import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants
import uk.gov.hmrc.tai.viewModels.GoogleAnalyticsSettings

case class ConfirmAmountEnteredViewModel(yearRange: String,
                                         employerName: String,
                                         mainText: String,
                                         onConfirm: String,
                                         onCancel: String,
                                         gaSettings: GoogleAnalyticsSettings)

object ConfirmAmountEnteredViewModel {

  private implicit def toMoneyPounds(amount: Int): MoneyPounds = MoneyPounds(amount, 0)

  def irregularPayCurrentYear(employmentId: Int, employerName: String, estimatedIncome: Int)(implicit messages: Messages): ConfirmAmountEnteredViewModel = {
    val currentYear = TaxYearRangeUtil.currentTaxYearRange
    val mainParagraphText = messages("tai.irregular.confirm.estimatedIncome", withPoundPrefix(estimatedIncome))
    val confirmUrl = controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.submitIncomeIrregularHours(employmentId).url.toString
    val onCancelUrl = controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url

    ConfirmAmountEnteredViewModel(
      employerName = employerName,
      yearRange = currentYear,
      mainText = mainParagraphText,
      onConfirm = confirmUrl,
      onCancel = onCancelUrl,
      gaSettings = GoogleAnalyticsSettings()
    )
  }

  def nextYearEstimatedPay(employmentId: Int, employerName: String, currentAmount: Int, estimatedIncome: Int)(implicit messages: Messages): ConfirmAmountEnteredViewModel = {
    val nextYearRange: String = TaxYearRangeUtil.futureTaxYearRangeHtmlNonBreak(1)
    val mainParagraphText = messages("tai.updateIncome.CYPlus1.confirm.paragraph", withPoundPrefix(estimatedIncome))
    val confirmUrl = controllers.income.routes.UpdateIncomeNextYearController.handleConfirm(employmentId).url
    val onCancelUrl = controllers.routes.IncomeTaxComparisonController.onPageLoad.url

    ConfirmAmountEnteredViewModel(
      employerName = employerName,
      yearRange = nextYearRange,
      mainText = mainParagraphText,
      onConfirm = confirmUrl,
      onCancel = onCancelUrl,
      gaSettings = gaSettings(currentAmount, estimatedIncome)
    )
  }

  private def gaSettings(currentAmount: Int, newAmount: Int): GoogleAnalyticsSettings = {
    val poundedCurrentAmount = MonetaryUtil.withPoundPrefix(currentAmount)
    val poundedNewAmount = MonetaryUtil.withPoundPrefix(newAmount)

    val amounts = Map("currentAmount" -> poundedCurrentAmount, "newAmount" -> poundedNewAmount)

    val dimensions: Option[Map[String, String]] = Some(Map(GoogleAnalyticsConstants.taiCYPlusOneEstimatedIncome -> MapForGoogleAnalytics.format(amounts)))
    GoogleAnalyticsSettings(dimensions = dimensions)
  }
}