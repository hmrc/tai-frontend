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

package views.html.incomes

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.income.incomeCalculator.PayslipDeductionsForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class PayslipDeductionsViewSpec extends TaiViewSpec {

  val employer = IncomeSource(id = 1, name = "Employer")

  "Pay slip deductions view" should {
    behave like pageWithBackLinkWithUrl(
      controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipAmountPage().url
    )
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeController.cancel(employer.id).url))
    behave like pageWithCombinedHeaderNewTemplateNew(
      messages("tai.payslipDeductions.preHeading", employer.name),
      messages("tai.payslipDeductions.heading"),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
  }

  private def payslipDeductions = inject[PayslipDeductionsView]
  override def view: Html       = payslipDeductions(PayslipDeductionsForm.createForm(), employer)
}
