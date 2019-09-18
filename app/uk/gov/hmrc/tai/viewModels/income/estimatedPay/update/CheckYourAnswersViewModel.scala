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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil, ViewModelHelper}
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

case class CheckYourAnswersViewModel(
  paymentFrequency: String,
  payPeriodInDays: Option[String],
  totalPay: String,
  hasDeductions: String,
  taxablePay: Option[String],
  hasBonusOrOvertime: String,
  totalBonusOrOvertime: Option[String],
  employer: IncomeSource)
    extends ViewModelHelper with DynamicPayPeriodTitle {

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {
    val isMonetaryValue = true

    val paymentFrequencyAnswer = paymentFrequency match {
      case OTHER => messages("tai.payPeriod.dayPeriod", payPeriodInDays.getOrElse("0"))
      case _     => messages(s"tai.payPeriod.$paymentFrequency")
    }

    val paymentFrequencyConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
      Some(paymentFrequencyAnswer),
      controllers.income.estimatedPay.update.routes.IncomeUpdatePayPeriodController.payPeriodPage().url
    )

    val grossPayMessages = Map(
      MONTHLY     -> "tai.estimatedPay.update.checkYourAnswers.grossPay.month",
      WEEKLY      -> "tai.estimatedPay.update.checkYourAnswers.grossPay.week",
      FORTNIGHTLY -> "tai.estimatedPay.update.checkYourAnswers.grossPay.2week",
      OTHER       -> "tai.estimatedPay.update.checkYourAnswers.grossPay.days"
    )

    val totalPayConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages(dynamicTitle(Some(paymentFrequency), payPeriodInDays, grossPayMessages)),
      Some(totalPay),
      controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipAmountPage().url,
      isMonetaryValue
    )

    val hasDeductionConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
      Some(messages(s"tai.label.${hasDeductions.toLowerCase}")),
      controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage().url
    )

    val taxablePayMessages = Map(
      MONTHLY     -> "tai.estimatedPay.update.checkYourAnswers.taxablePay.month",
      WEEKLY      -> "tai.estimatedPay.update.checkYourAnswers.taxablePay.week",
      FORTNIGHTLY -> "tai.estimatedPay.update.checkYourAnswers.taxablePay.2week",
      OTHER       -> "tai.estimatedPay.update.checkYourAnswers.taxablePay.days"
    )

    val taxablePayConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages(dynamicTitle(Some(paymentFrequency), payPeriodInDays, taxablePayMessages)),
      taxablePay,
      controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.taxablePayslipAmountPage().url,
      isMonetaryValue
    )

    val hasBonusOrOvertimeConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages(
        "tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime",
        TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited),
      Some(messages(s"tai.label.${hasBonusOrOvertime.toLowerCase}")),
      controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url
    )

    val totalBonusOrOvertimeConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages(
        "tai.estimatedPay.update.checkYourAnswers.totalYearlyBonusOrOvertime",
        TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited),
      totalBonusOrOvertime,
      controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusOvertimeAmountPage().url,
      isMonetaryValue
    )

    Seq(
      paymentFrequencyConfirmationLine,
      totalPayConfirmationLine,
      hasDeductionConfirmationLine,
      taxablePayConfirmationLine,
      hasBonusOrOvertimeConfirmationLine,
      totalBonusOrOvertimeConfirmationLine
    ).flatten
  }

  private def createCheckYourAnswerConfirmationLine(
    message: String,
    answer: Option[String],
    changeUrl: String,
    isMonetaryValue: Boolean = false)(implicit messages: Messages): Option[CheckYourAnswersConfirmationLine] = {

    def wholePoundsOnlyFormatting(amount: String) = amount.replaceAll("£|,|\\.\\d+", "")

    val zeroDecimalPlaces = 0

    (answer, isMonetaryValue) match {
      case (Some(answer), true) => {
        Some(
          CheckYourAnswersConfirmationLine(
            message,
            withPoundPrefixAndSign(MoneyPounds(BigDecimal(wholePoundsOnlyFormatting(answer)), zeroDecimalPlaces)),
            changeUrl))
      }
      case (Some(answer), false) => Some(CheckYourAnswersConfirmationLine(message, answer, changeUrl))
      case _                     => None
    }
  }

}
