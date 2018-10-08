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

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.views.formatting.Money
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

class UpdateIncomeEstimateCheckYourAnswersViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Update income estimate check your answers view model" must {
    "return all journey lines" when {
      "taxable pay and bonus or overtime pay is provided" in {

        val viewModel = createViewModel(Some(hasExtraBonusOrOvertime), Some(totalBonusOrOvertime), Some(taxablePay))

        viewModel.journeyConfirmationLines.size mustBe 7
        viewModel.journeyConfirmationLines mustEqual
          Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, taxablePayAnswer, hasBonusOrOvertimeAnswer,
            hasExtraBonusOrOvertimeAnswer, totalBonusOrOvertimeAnswer)
      }
    }

    "return relevant journey lines" when {
      "no bonus or overtime is provided" in {

        val viewModel = createViewModel(taxablePay = Some(taxablePay))

        viewModel.journeyConfirmationLines.size mustBe 5
        viewModel.journeyConfirmationLines mustEqual
          Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, taxablePayAnswer, hasBonusOrOvertimeAnswer)
      }


      "there are no payslip deductions" in {
        val viewModel = createViewModel(Some(hasExtraBonusOrOvertime), Some(totalBonusOrOvertime))

        viewModel.journeyConfirmationLines.size mustBe 6
        viewModel.journeyConfirmationLines mustEqual
          Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, hasBonusOrOvertimeAnswer,
            hasExtraBonusOrOvertimeAnswer, totalBonusOrOvertimeAnswer)
      }

      "no bonus or overtime is changed from yes to no" in {

        val viewModel = createViewModel(Some("no"), Some(totalBonusOrOvertime), Some(taxablePay))
        viewModel.journeyConfirmationLines.size mustBe 5
        viewModel.journeyConfirmationLines mustEqual
            Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, taxablePayAnswer, hasBonusOrOvertimeAnswer)

      }

    }


  }

  lazy val paymentFrequencyAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
    paymentFrequency,
    controllers.routes.IncomeUpdateCalculatorController.payPeriodPage().url
  )

  lazy val totalPayAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.totalPay", "month"),
    Money.pounds(BigDecimal(totalPay)).toString().trim.replace("&pound;", "\u00A3"),
    controllers.routes.IncomeUpdateCalculatorController.payslipAmountPage().url
  )

  lazy val hasDeductionAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
    hasDeductions,
    controllers.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url
  )

  lazy val taxablePayAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.taxablePay", "month"),
    Money.pounds(BigDecimal(taxablePay)).toString().trim.replace("&pound;", "\u00A3"),
    controllers.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url
  )

  lazy val hasBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime"),
    hasBonusOrOvertime,
    controllers.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
  )

  lazy val hasExtraBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.hasExtraBonusOrOvertime"),
    hasExtraBonusOrOvertime,
    controllers.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
  )

  lazy val totalBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.totalBonusOrOvertime", "month"),
    Money.pounds(BigDecimal(totalBonusOrOvertime)).toString().trim.replace("&pound;", "\u00A3"),
    controllers.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url
  )

  def createViewModel(hasExtraBonusOrOvertime: Option[String] = None,
                      totalBonusOrOvertime: Option[String] = None,
                      taxablePay: Option[String] = None): UpdateIncomeEstimateCheckYourAnswersViewModel = {

    UpdateIncomeEstimateCheckYourAnswersViewModel(
      paymentFrequency,
      totalPay,
      hasDeductions,
      taxablePay,
      hasBonusOrOvertime,
      hasExtraBonusOrOvertime,
      totalBonusOrOvertime
    )
  }

  val paymentFrequency = "Monthly"
  val totalPay = "10000"
  val hasDeductions = "Yes"
  val taxablePay = "1800"
  val hasBonusOrOvertime = "Yes"
  val hasExtraBonusOrOvertime = "Yes"
  val totalBonusOrOvertime = "3000"

}
