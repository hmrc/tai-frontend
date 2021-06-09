/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants
import uk.gov.hmrc.tai.viewModels.GoogleAnalyticsSettings

sealed trait PayType
case object IrregularPay extends PayType
case object NextYearPay extends PayType

case class ConfirmAmountEnteredViewModel(
  yearRange: String,
  employerName: String,
  mainText: Option[String] = None,
  onConfirm: String,
  onCancel: String,
  estimatedIncome: Int)

object ConfirmAmountEnteredViewModel {

  private implicit def toMoneyPounds(amount: Int): MoneyPounds = MoneyPounds(amount, 0)

  def apply(employmentId: Int, empName: String, currentAmount: Int, estIncome: Int, payType: PayType)(
    implicit messages: Messages): ConfirmAmountEnteredViewModel = {

    val irregularPayCurrentYear = {
      ConfirmAmountEnteredViewModel(
        yearRange = TaxYearRangeUtil.currentTaxYearRange,
        employerName = empName,
        mainText = Some(messages("tai.incomes.confirm.save.message")),
        onConfirm = controllers.income.estimatedPay.update.routes.IncomeUpdateIrregularHoursController
          .submitIncomeIrregularHours(employmentId)
          .url
          .toString,
        onCancel = controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url,
        estimatedIncome = estIncome
      )
    }

    val nextYearEstimatedPay = {
      ConfirmAmountEnteredViewModel(
        yearRange = TaxYearRangeUtil.futureTaxYearRange(1),
        employerName = empName,
        onConfirm = controllers.income.routes.UpdateIncomeNextYearController.handleConfirm(employmentId).url,
        onCancel = controllers.routes.IncomeTaxComparisonController.onPageLoad.url,
        estimatedIncome = estIncome
      )
    }

    payType match {
      case IrregularPay => irregularPayCurrentYear
      case NextYearPay  => nextYearEstimatedPay
    }

  }

  def apply(empName: String, currentAmount: Int, estIncome: Int)(
    implicit messages: Messages): ConfirmAmountEnteredViewModel =
    ConfirmAmountEnteredViewModel(
      yearRange = TaxYearRangeUtil.currentTaxYearRange,
      employerName = empName,
      mainText = Some(messages("tai.incomes.confirm.save.message")),
      onConfirm = controllers.routes.IncomeController.updateEstimatedIncome().url,
      onCancel = controllers.routes.TaxAccountSummaryController.onPageLoad().url,
      estimatedIncome = estIncome
    )
}
