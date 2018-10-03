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
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

case class UpdateIncomeEstimateCheckYourAnswersViewModel(paymentFrequency: String,
                                                         totalPay: String,
                                                         hasDeductions: String,
                                                         taxablePay: Option[String],
                                                         hasBonusOrOvertime: String,
                                                         hasExtraBonusOrOvertime: Option[String],
                                                         totalBonusOrOvertime: Option[String]) {

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {

    val paymentFrequencyAnswer = CheckYourAnswersConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
      paymentFrequency,
      controllers.routes.IncomeUpdateCalculatorController.payPeriodPage().url
    )

    val totalPayAnswer = CheckYourAnswersConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.totalPay"),
      totalPay,
      controllers.routes.IncomeUpdateCalculatorController.payslipAmountPage().url
    )

    val hasDeductionAnswer = CheckYourAnswersConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
      hasDeductions,
      controllers.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url
    )

    val taxablePayAnswer = CheckYourAnswersConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.taxablePay"),
      taxablePay.get,
      controllers.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url
    )

    val hasBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime"),
      hasBonusOrOvertime,
      controllers.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
    )

    val hasExtraBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.hasExtraBonusOrOvertime"),
      hasExtraBonusOrOvertime.get,
      controllers.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
    )

    val totalBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
      Messages("tai.estimatedPay.update.checkYourAnswers.totalBonusOrOvertime"),
      totalBonusOrOvertime.get,
      controllers.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url
    )

    Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, taxablePayAnswer, hasBonusOrOvertimeAnswer,
      hasExtraBonusOrOvertimeAnswer, totalBonusOrOvertimeAnswer)
  }

}
