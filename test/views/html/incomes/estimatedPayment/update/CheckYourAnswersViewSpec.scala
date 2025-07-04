/*
 * Copyright 2023 HM Revenue & Customs
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

package views.html.incomes.estimatedPayment.update

import play.twirl.api.Html
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.CheckYourAnswersViewModel

class CheckYourAnswersViewSpec extends TaiViewSpec {

  val employer                = IncomeSource(id = 1, name = "employer1")
  val paymentFrequency        = "monthly"
  val totalPay                = "10000"
  val hasDeductions           = "Yes"
  val taxablePay              = Some("1800")
  val hasBonusOrOvertime      = "Yes"
  val hasExtraBonusOrOvertime = "Yes"
  val totalBonusOrOvertime    = Some("3000")
  val payPeriodInDays         = Some("3")

  private val template = inject[CheckYourAnswersView]

  override def view: Html = template(viewModel)

  def viewModel =
    CheckYourAnswersViewModel(
      paymentFrequency,
      payPeriodInDays,
      totalPay,
      hasDeductions,
      taxablePay,
      hasBonusOrOvertime,
      totalBonusOrOvertime,
      employer,
      "backUrl"
    )

  "checkYourAnswers" should {

    behave like pageWithTitle(messages("tai.checkYourAnswers.title"))
    behave like pageWithCombinedHeaderNewFormatNew(
      messages("tai.incomes.edit.preHeading", employer.name),
      messages("tai.checkYourAnswers.heading")
    )
    behave like pageWithCancelLink(controllers.routes.IncomeController.cancel(employer.id))
    behave like pageWithBackLinkWithUrl("backUrl")

    "display journey confirmation lines" in {

      val monthlyPaymentFrequency = "Monthly"
      val totalPay                = "£10,000"

      doc must haveCheckYourAnswersSummaryLineNew(
        1,
        messages("tai.estimatedPay.update.checkYourAnswers.paymentFrequency")
      )
      doc must haveCheckYourAnswersSummaryLineAnswerNew(1, monthlyPaymentFrequency)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        1,
        controllers.income.estimatedPay.update.routes.IncomeUpdatePayPeriodController.payPeriodPage().url
      )

      doc must haveCheckYourAnswersSummaryLineNew(
        2,
        messages("tai.estimatedPay.update.checkYourAnswers.grossPay.month")
      )
      doc must haveCheckYourAnswersSummaryLineAnswerNew(2, totalPay)
      doc must haveCheckYourAnswersSummaryLineChangeLink(
        2,
        controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipAmountPage().url
      )

    }

    "display a continue button" in {
      doc must haveLinkElement(
        "estimatedPayLink",
        controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
          .estimatedPayPage(employer.id)
          .url,
        messages("tai.WhatDoYouWantToDo.submit")
      )
    }

  }
}
