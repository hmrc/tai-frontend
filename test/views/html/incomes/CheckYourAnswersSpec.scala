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

package views.html.incomes

import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.UpdateIncomeEstimateCheckYourAnswersViewModel

class CheckYourAnswersSpec extends TaiViewSpec {

  val employerName = "employer1"

  "checkYourAnswers" should {

    behave like pageWithTitle(messages("tai.checkYourAnswers"))
    behave like pageWithCombinedHeader(messages("tai.incomes.edit.preHeading",employerName), messages("tai.checkYourAnswers"))
    behave like pageWithCancelLink(controllers.routes.TaxAccountSummaryController.onPageLoad)
    behave like pageWithBackLink

    "display confirmation static text" in{
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }

    "display journey confirmation lines" in {
      doc must haveCheckYourAnswersSummaryLine(1, messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency"))
      doc must haveCheckYourAnswersSummaryLineAnswer(1, paymentFrequency)
      doc must haveCheckYourAnswersSummaryLineChangeLink(1, controllers.routes.IncomeUpdateCalculatorController.payPeriodPage().url)


      doc must haveCheckYourAnswersSummaryLine(2, messages("tai.estimatedPay.update.checkYourAnswers.totalPay", "month"))
      doc must haveCheckYourAnswersSummaryLineAnswer(2, "£10,000")
      doc must haveCheckYourAnswersSummaryLineChangeLink(2, controllers.routes.IncomeUpdateCalculatorController.payslipAmountPage().url)
    }

    "display a continue button" in {
      doc must haveLinkElement("estimatedPayLink", controllers.routes.IncomeUpdateCalculatorController.estimatedPayPage().url,
        messages("tai.WhatDoYouWantToDo.submit"))
    }


  }

  override def view = views.html.incomes.checkYourAnswers(viewModel,employerName)
  def viewModel = UpdateIncomeEstimateCheckYourAnswersViewModel(paymentFrequency,totalPay, hasDeductions, Some(taxablePay),
    hasBonusOrOvertime, Some(hasExtraBonusOrOvertime), Some(totalBonusOrOvertime))


  val paymentFrequency = "Monthly"
  val totalPay = "10000"
  val hasDeductions = "Yes"
  val taxablePay = "1800"
  val hasBonusOrOvertime = "Yes"
  val hasExtraBonusOrOvertime = "Yes"
  val totalBonusOrOvertime = "3000"

}