/*
 * Copyright 2019 HM Revenue & Customs
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

package views.html

import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class CanWeContactByPhoneSpec extends TaiViewSpec with FormValuesConstants {

  override def view: Html = views.html.can_we_contact_by_phone(Some(authedUser), viewModel, form)

  "CanWeContactByPhone page" must {

    behave like pageWithTitle(messages("main heading"))

    behave like pageWithCombinedHeader("pre heading","main heading")

    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("continueUrl")
    behave like pageWithYesNoRadioButton(YesNoTextEntryForm.YesNoChoice+"-yes", YesNoTextEntryForm.YesNoChoice+"-no")
    behave like pageWithCancelLink(Call("GET", "cancelUrl"))

    "display an input field for text entry" in {
      doc.getElementById("yesNoTextEntry") must not be null
      doc must haveInputLabelWithText("yesNoTextEntry",
        s"${Messages("tai.phoneNumber")} ${Messages("tai.canWeContactByPhone.telephoneNumber.hint")}")
    }

    "display an explanation text paragraph" in {
      doc must haveParagraphWithText(Messages("tai.canWeContactByPhone.explanation"))
    }

    "display an error notification" when {
      "the supplied form has errors" in {
        val formWithErrors: Form[YesNoTextEntryForm] = YesNoTextEntryForm.form("answer yes or no", "provide text").
          withError(YesNoChoice, "answer yes or no")
        def sut = views.html.can_we_contact_by_phone(Some(authedUser), viewModel, formWithErrors)

        val errorMessage = doc(sut).select(".error-message").text
        errorMessage mustBe "answer yes or no"
      }
    }
  }

  private val form: Form[YesNoTextEntryForm] = YesNoTextEntryForm.form("enter yes or no", "enter a text value").bind(Map(
    YesNoChoice -> NoValue
  ))

  private val viewModel = CanWeContactByPhoneViewModel("pre heading", "main heading", "backUrl", "continueUrl", "cancelUrl")
}
