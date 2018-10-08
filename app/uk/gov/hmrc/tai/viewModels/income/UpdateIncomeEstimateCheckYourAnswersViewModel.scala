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

    val paymentFrequencyAnswer = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
      Some(paymentFrequency.capitalize),
      controllers.routes.IncomeUpdateCalculatorController.payPeriodPage().url)

    val totalPayAnswer = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.totalPay", timePeriod(paymentFrequencyAnswer.get.answer)),
      Some(totalPay),
      controllers.routes.IncomeUpdateCalculatorController.payslipAmountPage().url,
      isMonetaryValue
    )

    val hasDeductionAnswer = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
      Some(hasDeductions),
      controllers.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url
    )

    val taxablePayAnswer = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.taxablePay", timePeriod(paymentFrequencyAnswer.get.answer)),
      taxablePay,
      controllers.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url,
      isMonetaryValue
    )

    val hasBonusOrOvertimeAnswer = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime"),
      Some(hasBonusOrOvertime),
      controllers.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
    )

    val hasExtraBonusOrOvertimeAnswer = createCheckYourAnswerConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.hasExtraBonusOrOvertime"),
      hasExtraBonusOrOvertime,
      controllers.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
    )

    val totalBonusOrOvertimeMessage =
      if(hasExtraBonusOrOvertime.getOrElse("") == "Yes"){
      Messages("tai.estimatedPay.update.checkYourAnswers.totalYearlyBonusOrOvertime")
     }else{
        Messages("tai.estimatedPay.update.checkYourAnswers.totalBonusOrOvertime", timePeriod(paymentFrequencyAnswer.get.answer))
      }

    val totalBonusOrOvertimeAnswer = createCheckYourAnswerConfirmationLine(
      totalBonusOrOvertimeMessage,
      totalBonusOrOvertime,
      controllers.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url,
      isMonetaryValue
    )

    Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, taxablePayAnswer, hasBonusOrOvertimeAnswer,
      hasExtraBonusOrOvertimeAnswer, totalBonusOrOvertimeAnswer).flatten
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

  def timePeriod(paymentFrequency: String):String = {
    paymentFrequency match {
      case "Monthly" => "month"
      case "Weekly" => "week"
      case "Fortnightly" => "fortnight"
      case "Other" => "period"
    }
  }
}
