/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.PayslipForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.PayPeriodConstants._
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.PaySlipAmountViewModel

class PayslipAmountViewSpec extends TaiViewSpec {

  val employer = IncomeSource(id = 1, name = "Employer")

  "Pay slip amount view" should {
    behave like pageWithBackLinkWithUrl(
      controllers.income.estimatedPay.update.routes.IncomeUpdatePayPeriodController.payPeriodPage().url)
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeController.cancel(employer.id).url))
    behave like pageWithCombinedHeaderNewTemplate(
      messages("tai.payslip.preHeading", employer.name),
      messages("tai.payslip.title.month"),
      Some(messages("tai.ptaHeader.accessible.preHeading")))
  }

  val payslipViewModel = PaySlipAmountViewModel(PayslipForm.createForm("errText"), Some(Monthly), None, employer)

  private def payslipAmount = inject[PayslipAmountView]
  override def view: Html = payslipAmount(payslipViewModel)
}
