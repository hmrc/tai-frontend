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

import controllers.FakeTaiPlayApplication
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import uk.gov.hmrc.time.TaxYearResolver

class CheckYourAnswersViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with ViewModelHelper
  with PropertyChecks {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Update income estimate check your answers view model" must {
    "return all journey lines" when {
      "taxable pay and bonus or overtime pay is provided" in {

        val viewModel = createViewModel(Some(totalBonusOrOvertime), Some(taxablePay))

        viewModel.journeyConfirmationLines.size mustBe 6
        viewModel.journeyConfirmationLines mustEqual
          Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, taxablePayAnswer, hasBonusOrOvertimeAnswer,
            totalYearlyBonusOrOvertimeAnswer)
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
        val viewModel = createViewModel(Some(totalBonusOrOvertime))

        viewModel.journeyConfirmationLines.size mustBe 5
        viewModel.journeyConfirmationLines mustEqual
          Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, hasBonusOrOvertimeAnswer,
            totalYearlyBonusOrOvertimeAnswer)
      }

    }

    "return relevant bonus or overtime message" when {
      "period is yearly" in{
        val viewModel = createViewModel(Some(totalBonusOrOvertime))
        viewModel.journeyConfirmationLines must contain(totalYearlyBonusOrOvertimeAnswer)
      }
    }

    "return a journey line for a taxable pay monetary amount" in {

      val taxablePayQuestion = messagesApi("tai.estimatedPay.update.checkYourAnswers.taxablePay", "month")

      val validValues =
        Table(
          ("values"),
          ("£10000"),
          ("10000"),
          ("10,000"),
          ("10000.00")
        )

      forAll (validValues) { (monetaryValue: String) =>
        val viewModel = createViewModel(taxablePay = Some(monetaryValue))
        val taxablePayAnwser = viewModel.journeyConfirmationLines.filter(
          checkYourAnswerConfirmationLine => checkYourAnswerConfirmationLine.question == taxablePayQuestion).head.answer

        taxablePayAnwser mustBe "£10,000"
      }
    }


  }

  val monthlyPaymentFrequency = "Monthly"
  val zeroDecimalPlaces = 0

  lazy val paymentFrequencyAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
    monthlyPaymentFrequency,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payPeriodPage().url
  )

  lazy val totalPayAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.totalPay", "month"),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(totalPay),zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipAmountPage().url
  )

  lazy val hasDeductionAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
    hasDeductions,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url
  )

  lazy val taxablePayAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.taxablePay", "month"),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(taxablePay),zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url
  )

  lazy val hasBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime", currentTaxYearRangeHtmlNonBreakBetween),
    hasBonusOrOvertime,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
  )

  lazy val totalYearlyBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.totalYearlyBonusOrOvertime", currentTaxYearRangeHtmlNonBreak),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(totalBonusOrOvertime),zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url
  )

  def createViewModel(totalBonusOrOvertime: Option[String] = None, taxablePay: Option[String] = None): CheckYourAnswersViewModel = {

    CheckYourAnswersViewModel(
      paymentFrequency,
      totalPay,
      hasDeductions,
      taxablePay,
      hasBonusOrOvertime,
      totalBonusOrOvertime
    )
  }

  val paymentFrequency = "monthly"
  val totalPay = "10000"
  val hasDeductions = "Yes"
  val taxablePay = "1800"
  val hasBonusOrOvertime = "Yes"
  val totalBonusOrOvertime = "3000"

}
