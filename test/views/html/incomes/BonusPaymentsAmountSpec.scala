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
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.BonusOvertimeAmountForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BonusPaymentsAmountSpec extends TaiViewSpec with MockitoSugar {

  val id = 1
  val employerName = "Employer"

  val bonusPaymentsAmountForm = mock[Form[BonusOvertimeAmountForm]]

  val field = mock[Field]
  when(field.value).thenReturn(Some("fakeFieldValue"))
  when(field.name).thenReturn("fakeFieldValue")
  when(bonusPaymentsAmountForm(any())).thenReturn(field)
  when(bonusPaymentsAmountForm.errors).thenReturn(List())
  when(bonusPaymentsAmountForm.errors(anyString())).thenReturn(List())
  when(bonusPaymentsAmountForm.hasErrors).thenReturn(false)

  "Bonus payments amount view with monthly pay" should {
    behave like pageWithBackLink
    behave like pageWithCombinedHeader(
      messages("tai.bonusPaymentsAmount.preHeading", employerName),
      messages("tai.bonusPaymentsAmount.month.title"))
  }

  "Bonus payments amount view with yearly pay" should {

    val testView: Html = views.html.incomes.bonusPaymentAmount(bonusPaymentsAmountForm,"year",id, employerName)
    doc(testView) must haveHeadingWithText(messages("tai.bonusPaymentsAmount.year.title"))
  }




  override def view: Html = views.html.incomes.bonusPaymentAmount(bonusPaymentsAmountForm,"monthly",id, employerName)
}
