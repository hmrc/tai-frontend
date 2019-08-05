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

import controllers.FakeTaiPlayApplication
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil, ViewModelHelper}
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

class CheckYourAnswersViewModelSpec
    extends PlaySpec with FakeTaiPlayApplication with I18nSupport with ViewModelHelper with PropertyChecks
    with EditIncomePayPeriodConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Update income estimate check your answers view model" must {
    "return all journey lines" when {
      "taxable pay and bonus or overtime pay is provided" in {

        val viewModel = createViewModel(Some(totalBonusOrOvertime), Some(taxablePay))

        viewModel.journeyConfirmationLines.size mustBe 6
        viewModel.journeyConfirmationLines mustEqual
          Seq(
            monthlyPaymentFrequencyAnswer,
            totalPayAnswer,
            hasDeductionAnswer,
            taxablePayAnswer,
            hasBonusOrOvertimeAnswer,
            totalYearlyBonusOrOvertimeAnswer)
      }
    }

    "return relevant journey lines" when {
      "no bonus or overtime is provided" in {

        val viewModel = createViewModel(taxablePay = Some(taxablePay))

        viewModel.journeyConfirmationLines.size mustBe 5
        viewModel.journeyConfirmationLines mustEqual
          Seq(
            monthlyPaymentFrequencyAnswer,
            totalPayAnswer,
            hasDeductionAnswer,
            taxablePayAnswer,
            hasBonusOrOvertimeAnswer)
      }

      "there are no payslip deductions" in {
        val viewModel = createViewModel(Some(totalBonusOrOvertime))

        viewModel.journeyConfirmationLines.size mustBe 5
        viewModel.journeyConfirmationLines mustEqual
          Seq(
            monthlyPaymentFrequencyAnswer,
            totalPayAnswer,
            hasDeductionAnswer,
            hasBonusOrOvertimeAnswer,
            totalYearlyBonusOrOvertimeAnswer)
      }

    }

    "return relevant bonus or overtime message" when {
      "period is yearly" in {
        val viewModel = createViewModel(Some(totalBonusOrOvertime))
        viewModel.journeyConfirmationLines must contain(totalYearlyBonusOrOvertimeAnswer)
      }
    }

    "return a journey line for a taxable pay monetary amount" in {

      val taxablePayQuestion = messagesApi("tai.estimatedPay.update.checkYourAnswers.taxablePay.month")

      val validValues =
        Table(
          ("values"),
          ("£10000"),
          ("10000"),
          ("10,000"),
          ("10000.00")
        )

      forAll(validValues) { (monetaryValue: String) =>
        val viewModel = createViewModel(taxablePay = Some(monetaryValue))
        val taxablePayAnwser = viewModel.journeyConfirmationLines
          .filter(checkYourAnswerConfirmationLine => checkYourAnswerConfirmationLine.question == taxablePayQuestion)
          .head
          .answer

        taxablePayAnwser mustBe "£10,000"
      }
    }

    "return relevant payment frequency answer " when {

      "payment frequency is Weekly" in {
        val confirmationLine = createPaymentFrequencyConfirmationLine(messagesApi("tai.payPeriod.weekly"))

        val viewModel = createViewModel(paymentFrequency = WEEKLY)
        viewModel.journeyConfirmationLines must contain(confirmationLine)
      }

      "payment frequency is fortnightly" in {
        val confirmationLine = createPaymentFrequencyConfirmationLine(messagesApi("tai.payPeriod.fortnightly"))

        val viewModel = createViewModel(paymentFrequency = FORTNIGHTLY)
        viewModel.journeyConfirmationLines must contain(confirmationLine)
      }

      "payment frequency is OTHER" in {

        val payPeriodInDays = "3"

        val confirmationLine =
          createPaymentFrequencyConfirmationLine(messagesApi("tai.payPeriod.dayPeriod", payPeriodInDays))

        val viewModel = createViewModel(payPeriodInDays = Some(payPeriodInDays), paymentFrequency = OTHER)
        viewModel.journeyConfirmationLines must contain(confirmationLine)
      }

    }

  }

  val zeroDecimalPlaces = 0

  lazy val monthlyPaymentFrequencyAnswer = createPaymentFrequencyConfirmationLine()

  lazy val totalPayAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.grossPay.month"),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(totalPay), zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipAmountPage().url
  )

  lazy val hasDeductionAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
    hasDeductions,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url
  )

  lazy val taxablePayAnswer = CheckYourAnswersConfirmationLine(
    messagesApi("tai.estimatedPay.update.checkYourAnswers.taxablePay.month"),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(taxablePay), zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url
  )

  lazy val hasBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    messagesApi(
      "tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime",
      TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited),
    hasBonusOrOvertime,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
  )

  lazy val totalYearlyBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    messagesApi(
      "tai.estimatedPay.update.checkYourAnswers.totalYearlyBonusOrOvertime",
      TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(totalBonusOrOvertime), zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url
  )

  def createPaymentFrequencyConfirmationLine(answer: String = MONTHLY.capitalize) =
    CheckYourAnswersConfirmationLine(
      messagesApi("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
      answer,
      controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payPeriodPage().url
    )

  def createViewModel(
    totalBonusOrOvertime: Option[String] = None,
    taxablePay: Option[String] = None,
    payPeriodInDays: Option[String] = None,
    paymentFrequency: String = MONTHLY): CheckYourAnswersViewModel =
    CheckYourAnswersViewModel(
      paymentFrequency,
      payPeriodInDays,
      totalPay,
      hasDeductions,
      taxablePay,
      hasBonusOrOvertime,
      totalBonusOrOvertime,
      employer = IncomeSource(1, "employer name")
    )

  val totalPay = "10000"
  val hasDeductions = "Yes"
  val taxablePay = "1800"
  val hasBonusOrOvertime = "Yes"
  val totalBonusOrOvertime = "3000"
}
