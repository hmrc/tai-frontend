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
import uk.gov.hmrc.tai.forms.BonusPaymentsForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BonusPaymentsSpec extends TaiViewSpec with MockitoSugar {

  lazy val Id = 1
  lazy val employerName = "Employer"
  lazy val bonusPaymentsForm = mock[Form[BonusPaymentsForm]]

  "Bonus payments view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeSourceSummaryController.onPageLoad(Id).url))
    behave like pageWithCombinedHeader(
      messages("tai.bonusPayments.preHeading", employerName),
      messages("tai.bonusPayments.heading"))
  }

  lazy val field = mock[Field]
  when(field.errors).thenReturn(Nil)
  when(field.value).thenReturn(Some("fakeFieldValue"))
  when(field.name).thenReturn("fakeFieldValue")
  when(bonusPaymentsForm(any())).thenReturn(field)
  when(bonusPaymentsForm.errors).thenReturn(Nil)
  when(bonusPaymentsForm.errors(anyString())).thenReturn(Nil)
  when(bonusPaymentsForm.error(any())).thenReturn(None)
  when(bonusPaymentsForm.hasErrors).thenReturn(false)

  override def view: Html = views.html.incomes.bonusPayments(bonusPaymentsForm,Id, employerName, false, false)
}
