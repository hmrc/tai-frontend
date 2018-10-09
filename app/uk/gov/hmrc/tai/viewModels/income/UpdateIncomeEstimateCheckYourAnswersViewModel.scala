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

package uk.gov.hmrc.tai.viewModels.income

import play.api.i18n.Messages
import uk.gov.hmrc.play.views.formatting.Money
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

case class UpdateIncomeEstimateCheckYourAnswersViewModel(paymentFrequency: String,
                                                         totalPay: String,
                                                         hasDeductions: String,
                                                         taxablePay: Option[String],
                                                         hasBonusOrOvertime: String,
                                                         hasExtraBonusOrOvertime: Option[String],
                                                         totalBonusOrOvertime: Option[String]) {

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {
    val isMonetaryValue = true

    val paymentFrequencyConfirmationLine = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
      Some(messages(s"tai.payPeriod.$paymentFrequency")),
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payPeriodPage().url)

    val totalPayConfirmationLine = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.totalPay", timePeriod(paymentFrequency)),
      Some(totalPay),
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipAmountPage().url,
      isMonetaryValue
    )

    val hasDeductionConfirmationLine = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
      Some(messages(s"tai.label.${hasDeductions.toLowerCase}")),
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url
    )

    val taxablePayConfirmationLine = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.taxablePay", timePeriod(paymentFrequency)),
      taxablePay,
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url,
      isMonetaryValue
    )

    val hasBonusOrOvertimeConfirmationLine = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime"),
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
      Messages("tai.estimatedPay.update.checkYourAnswers.hasExtraBonusOrOvertime"),
      hasExtraBonusOrOvertimeAnswer,
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
    )

    val totalBonusOrOvertimeMessage =
      if(hasExtraBonusOrOvertime.getOrElse("") == "Yes"){
      Messages("tai.estimatedPay.update.checkYourAnswers.totalYearlyBonusOrOvertime")
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

    (answer,isMonetaryValue) match {
      case (Some(_),true) => Some(CheckYourAnswersConfirmationLine(message,
        Money.pounds(BigDecimal(answer.get)).toString().trim.replace("&pound;", "\u00A3"),
        changeUrl))
      case (Some(_),false) => Some(CheckYourAnswersConfirmationLine(message, answer.get, changeUrl))
      case _ => None
    }
  }

  def timePeriod(paymentFrequency: String)(implicit messages: Messages):String = {

    val timePeriodMessage = paymentFrequency match {
      case "monthly" => messages("tai.label.month")
      case "weekly" => messages("tai.label.week")
      case "fortnightly" => messages("tai.label.fortnight")
      case "other" => messages("tai.label.period")
    }

    timePeriodMessage.toLowerCase()
  }
}
