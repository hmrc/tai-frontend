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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import uk.gov.hmrc.tai.forms.income.incomeCalculator.PayslipForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.PayPeriodConstants._
import utils.BaseSpec

class PaySlipAmountViewModelSpec extends BaseSpec {

  val form     = PayslipForm.createForm("tai.payslip.error.form.totalPay.input.mandatory")
  val employer = IncomeSource(id = 1, name = "employer name")

  "PaySlipAmountViewModel" must {
    "have a monthly title for a monthly pay period" in {
      val payPeriod = Some(Monthly)
      val viewModel = PaySlipAmountViewModel(form, payPeriod, None, employer)

      viewModel.payPeriodTitle mustBe messagesApi("tai.payslip.title.month")
    }

    "have a weekly title for a weekly pay period" in {
      val payPeriod = Some(Weekly)
      val viewModel = PaySlipAmountViewModel(form, payPeriod, None, employer)

      viewModel.payPeriodTitle mustBe messagesApi("tai.payslip.title.week")
    }

    "have a X-day title for a X-pay period" in {
      val payPeriod = Some(Other)
      val days      = Some("123")
      val viewModel = PaySlipAmountViewModel(form, payPeriod, days, employer)

      viewModel.payPeriodTitle mustBe messagesApi("tai.payslip.title.days", days.getOrElse(""))
    }

    "throw an exception if there is no pay period defined" in {
      val exception = intercept[RuntimeException] {
        val payPeriod = None
        PaySlipAmountViewModel(form, payPeriod, Some("123"), employer)
      }

      exception.getMessage mustBe "No pay period found"
    }

    "throw an exception if there is no pay period in days defined" in {
      val exception = intercept[RuntimeException] {
        val payPeriod       = Some(Other)
        val payPeriodInDays = None
        PaySlipAmountViewModel(form, payPeriod, payPeriodInDays, employer)
      }

      exception.getMessage mustBe "No days found for pay period"
    }
  }
}
