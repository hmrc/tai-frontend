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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.data.{Field, Form}
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.PayPeriodForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class PayPeriodSpec extends TaiViewSpec with MockitoSugar {

  val id = 1
  val employerName = "Employer"

  "Pay period view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeSourceSummaryController.onPageLoad(id).url))
    behave like pageWithCombinedHeader(
      messages("tai.payPeriod.preHeading", employerName),
      messages("tai.payPeriod.heading"))
  }

  val payPeriodForm = mock[Form[PayPeriodForm]]

  val field = mock[Field]
  when(field.value).thenReturn(Some("fakeFieldValue"))
  when(field.name).thenReturn("fakeFieldValue")
  when(field.errors).thenReturn(Nil)
  when(payPeriodForm(any())).thenReturn(field)
  when(payPeriodForm.errors).thenReturn(Nil)
  when(payPeriodForm.errors(anyString())).thenReturn(Nil)
  when(payPeriodForm.hasErrors).thenReturn(false)

  override def view: Html = views.html.incomes.payPeriod(payPeriodForm, id, employerName, true)
}
