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
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class EditPensionSpec extends TaiViewSpec with MockitoSugar{

  private val empId = 1
  private val employerName = "fakeFieldValue"

  "Edit Pension view" should {
    behave like pageWithBackLink
    behave like pageWithCombinedHeader(
      messages("tai.howToUpdate.preHeading", employerName),
      messages("tai.incomes.pension.heading")
    )
  }

  val editIncomeForm = mock[Form[EditIncomeForm]]

  val field = mock[Field]
  when(field.value).thenReturn(Some("fakeFieldValue"))
  when(field.name).thenReturn("fakeFieldValue")
  when(editIncomeForm(any())).thenReturn(field)
  when(editIncomeForm.errors(anyString())).thenReturn(Nil)

  override def view: Html = views.html.incomes.editPension(editIncomeForm, hasMultipleIncomes = false, empId, "0",false)
}
