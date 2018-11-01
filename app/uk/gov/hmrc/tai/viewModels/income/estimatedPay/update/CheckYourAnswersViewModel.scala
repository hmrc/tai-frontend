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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.util.{MonetaryUtil, ViewModelHelper}
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import uk.gov.hmrc.time.TaxYearResolver

case class CheckYourAnswersViewModel(paymentFrequency: String,
                                     totalPay: String,
                                     hasDeductions: String,
                                     taxablePay: Option[String],
                                     hasBonusOrOvertime: String,
                                     hasExtraBonusOrOvertime: Option[String],
                                     totalBonusOrOvertime: Option[String]) extends ViewModelHelper {

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {
    val isMonetaryValue = true

    val paymentFrequencyConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
      Some(messages(s"tai.payPeriod.$paymentFrequency")),
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payPeriodPage().url)

    val totalPayConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages("tai.estimatedPay.update.checkYourAnswers.totalPay", timePeriod(paymentFrequency)),
      Some(totalPay),
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipAmountPage().url,
      isMonetaryValue
    )

    val hasDeductionConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
      Some(messages(s"tai.label.${hasDeductions.toLowerCase}")),
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url
    )

    val taxablePayConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages("tai.estimatedPay.update.checkYourAnswers.taxablePay", timePeriod(paymentFrequency)),
      taxablePay,
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url,
      isMonetaryValue
    )

    val hasBonusOrOvertimeConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages("tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime"),
      Some(messages(s"tai.label.${hasBonusOrOvertime.toLowerCase}")),
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
    )

    val hasExtraBonusOrOvertimeAnswer =
      if (hasExtraBonusOrOvertime.isDefined) {
        Some(messages(s"tai.label.${hasExtraBonusOrOvertime.get.toLowerCase}"))
      }else{
        hasExtraBonusOrOvertime
      }

    val hasExtraBonusOrOvertimeConfirmationLine = createCheckYourAnswerConfirmationLine(
      messages("tai.estimatedPay.update.checkYourAnswers.hasExtraBonusOrOvertime",
        htmlNonBroken(Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear)),
        htmlNonBroken(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear))),
      hasExtraBonusOrOvertimeAnswer,
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
    )

    val totalBonusOrOvertimeMessage =
      if(hasExtraBonusOrOvertime.getOrElse("") == "Yes"){
      messages("tai.estimatedPay.update.checkYourAnswers.totalYearlyBonusOrOvertime", currentTaxYearRangeHtmlNonBreak)
     }else{
        Messages("tai.estimatedPay.update.checkYourAnswers.totalBonusOrOvertime", timePeriod(paymentFrequency))
      }

    val totalBonusOrOvertimeConfirmationLine = createCheckYourAnswerConfirmationLine(
      totalBonusOrOvertimeMessage,
      totalBonusOrOvertime,
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url,
      isMonetaryValue
    )

    Seq(paymentFrequencyConfirmationLine, totalPayConfirmationLine, hasDeductionConfirmationLine, taxablePayConfirmationLine,
      hasBonusOrOvertimeConfirmationLine, hasExtraBonusOrOvertimeConfirmationLine, totalBonusOrOvertimeConfirmationLine).flatten
  }

  private def createCheckYourAnswerConfirmationLine(message: String, answer: Option[String], changeUrl: String,
                                                    isMonetaryValue: Boolean = false)(implicit messages: Messages): Option[CheckYourAnswersConfirmationLine] = {

    val zeroDecimalPlaces = 0

    (answer,isMonetaryValue) match {
      case (Some(answer),true) => {
        val wholePounds = MonetaryUtil.wholePoundsNumericOnly(answer)
        Some(CheckYourAnswersConfirmationLine(message,
          withPoundPrefixAndSign(MoneyPounds(BigDecimal(wholePounds),zeroDecimalPlaces)),
          changeUrl))
      }
      case (Some(answer),false) => Some(CheckYourAnswersConfirmationLine(message, answer, changeUrl))
      case _ => None
    }
  }

  def timePeriod(paymentFrequency: String)(implicit messages: Messages):String = {

    val timePeriodMessage = paymentFrequency match {
      case "monthly" => messages("tai.estimatedPay.update.checkYourAnswers.timePeriod.month")
      case "weekly" => messages("tai.estimatedPay.update.checkYourAnswers.timePeriod.week")
      case "fortnightly" => messages("tai.estimatedPay.update.checkYourAnswers.timePeriod.fortnight")
      case "other" => messages("tai.estimatedPay.update.checkYourAnswers.timePeriod.period")
    }

    timePeriodMessage.toLowerCase()
  }
}
