/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.data.{Field, Form}
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.PayslipForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.PaySlipAmountViewModel

class PaySlipAmountSpec extends TaiViewSpec with EditIncomePayPeriodConstants {

  val employer = IncomeSource(id = 1, name = "Employer")

  "Pay slip amount view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeController.cancel(employer.id).url))
    behave like pageWithCombinedHeader(
      messages("tai.payslip.preHeading", employer.name),
      messages("tai.payslip.title.month"))
  }

  val payslipForm = mock[Form[PayslipForm]]

  val field = mock[Field]
  when(field.value).thenReturn(Some("fakeFieldValue"))
  when(field.name).thenReturn("fakeFieldValue")
  when(payslipForm(any())).thenReturn(field)
  when(payslipForm.errors).thenReturn(Nil)
  when(payslipForm.errors(anyString())).thenReturn(Nil)
  when(payslipForm.hasErrors).thenReturn(false)

  val payslipViewModel = PaySlipAmountViewModel(payslipForm, Some(MONTHLY), None, employer)

  override def view: Html = views.html.incomes.payslipAmount(payslipViewModel)
}
