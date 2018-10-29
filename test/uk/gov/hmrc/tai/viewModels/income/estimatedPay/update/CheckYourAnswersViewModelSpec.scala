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
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import uk.gov.hmrc.time.TaxYearResolver

class CheckYourAnswersViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with ViewModelHelper {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Update income estimate check your answers view model" must {
    "return all journey lines" when {
      "taxable pay and bonus or overtime pay is provided" in {

        val viewModel = createViewModel(Some(hasExtraBonusOrOvertime), Some(totalBonusOrOvertime), Some(taxablePay))

        viewModel.journeyConfirmationLines.size mustBe 7
        viewModel.journeyConfirmationLines mustEqual
          Seq(paymentFrequencyAnswer, totalPayAnswer, hasDeductionAnswer, taxablePayAnswer, hasBonusOrOvertimeAnswer,
            hasExtraBonusOrOvertimeAnswer, totalYearlyBonusOrOvertimeAnswer)
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
            hasExtraBonusOrOvertimeAnswer, totalYearlyBonusOrOvertimeAnswer)
      }

    }

    "return relevant bonus or overtime message" when {
      "period is yearly" in{
        val vieModel = createViewModel(Some(hasExtraBonusOrOvertime), Some(totalBonusOrOvertime))
        vieModel.journeyConfirmationLines must contain(totalYearlyBonusOrOvertimeAnswer)
      }
      "period is monthly" in {
        val vieModel = createViewModel(totalBonusOrOvertime = Some(totalBonusOrOvertime))
        vieModel.journeyConfirmationLines must contain(totalBonusOrOvertimeAnswer)
      }
    }


  }

  val monthlyPaymentFrequency = "Monthly"
  val zeroDecimalPlaces = 0

  lazy val paymentFrequencyAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"),
    monthlyPaymentFrequency,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payPeriodPage().url
  )

  lazy val totalPayAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.totalPay", "month"),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(totalPay),zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipAmountPage().url
  )

  lazy val hasDeductionAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.hasDeduction"),
    hasDeductions,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url
  )

  lazy val taxablePayAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.taxablePay", "month"),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(taxablePay),zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url
  )

  lazy val hasBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.hasBonusOrOvertime"),
    hasBonusOrOvertime,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
  )

  lazy val hasExtraBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.hasExtraBonusOrOvertime",
      htmlNonBroken(Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear)),
      htmlNonBroken(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear))),
    hasExtraBonusOrOvertime,
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url
  )

  lazy val totalBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.totalBonusOrOvertime", "month"),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(totalBonusOrOvertime),zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url
  )

  lazy val totalYearlyBonusOrOvertimeAnswer = CheckYourAnswersConfirmationLine(
    Messages("tai.estimatedPay.update.checkYourAnswers.totalYearlyBonusOrOvertime", currentTaxYearRangeHtmlNonBreak),
    withPoundPrefixAndSign(MoneyPounds(BigDecimal(totalBonusOrOvertime),zeroDecimalPlaces)),
    controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url
  )

  def createViewModel(hasExtraBonusOrOvertime: Option[String] = None,
                      totalBonusOrOvertime: Option[String] = None,
                      taxablePay: Option[String] = None): CheckYourAnswersViewModel = {

    CheckYourAnswersViewModel(
      paymentFrequency,
      totalPay,
      hasDeductions,
      taxablePay,
      hasBonusOrOvertime,
      hasExtraBonusOrOvertime,
      totalBonusOrOvertime
    )
  }

  val paymentFrequency = "monthly"
  val totalPay = "10000"
  val hasDeductions = "Yes"
  val taxablePay = "1800"
  val hasBonusOrOvertime = "Yes"
  val hasExtraBonusOrOvertime = "Yes"
  val totalBonusOrOvertime = "3000"

}
