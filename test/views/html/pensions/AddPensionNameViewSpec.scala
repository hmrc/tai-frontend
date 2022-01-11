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

package views.html.pensions

import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.pensions.PensionProviderNameForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class AddPensionNameViewSpec extends TaiViewSpec {
  private val addPensionName = inject[AddPensionNameView]

  "Add Pension Provider name form page" should {
    behave like pageWithTitle(messages("tai.addPensionProvider.addNameForm.title"))
    behave like pageWithCombinedHeader(
      messages("add.missing.pension"),
      messages("tai.addPensionProvider.addNameForm.title"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/check-income-tax/add-pension-provider/name")
    behave like pageWithCancelLink(controllers.pensions.routes.AddPensionProviderController.cancel())

    "have an error box at the top of the page with a link to the error field" when {
      "a form with errors is passed into the view" in {
        val view: Html = addPensionName(formWithErrors)

        doc(view) must haveErrorLinkWithText(Messages("tai.pensionProviderName.error.blank"))
      }
    }
  }

  private lazy val formWithErrors: Form[String] = PensionProviderNameForm.form.bind(
    Map(
      "pensionProviderName" -> ""
    ))

  private lazy val pensionProviderNameForm: Form[String] = PensionProviderNameForm.form.bind(
    Map(
      "pensionProviderName" -> "the company"
    ))

  override def view: Html = addPensionName(pensionProviderNameForm)
}
