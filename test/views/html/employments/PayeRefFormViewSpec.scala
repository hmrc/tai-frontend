/*
 * Copyright 2025 HM Revenue & Customs
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

package views.html.employments

import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.PayeRefForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class PayeRefFormViewSpec extends TaiViewSpec {

  private val template    = inject[PayeRefFormView]
  private val companyName = "Some Company Ltd"

  private def viewFor(form: Form[String], journey: String): Html =
    template(form, companyName, journey)

  override def view: Html = viewFor(PayeRefForm.form, "employment")

  "PayeRefFormView (employment journey)" must {

    behave like pageWithTitle(messages("tai.payeRefForm.title", companyName))
    behave like pageWithBackLink()

    "display the correct pre-heading (employment)" in {
      doc must haveElementAtPathWithText(
        ".govuk-caption-xl",
        messages("tai.ptaHeader.accessible.preHeading") + " " + messages("add.missing.employment")
      )
    }

    "show the page heading label including the company name" in {
      doc must haveElementAtPathWithText(
        ".govuk-label--xl",
        messages("tai.payeRefForm.title", companyName)
      )
    }

    "render the PAYE reference input with hint" in {
      doc must haveInputLabelWithText("payeReference", messages("tai.payeRefForm.title", companyName))
      doc must haveHintWithText("payeReference-hint", messages("tai.payeRefForm.hint"))
    }

    "post to the employment submit PAYE ref route" in {
      val d = doc(view)
      d.select("form").attr("action") mustBe controllers.employments.routes.AddEmploymentController
        .submitPayeReference()
        .url
    }

    "have the employment back link" in {
      val d = doc(view)
      d.select("a[class=govuk-back-link]").attr("href") mustBe
        controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url
    }

    "show a continue button" in {
      doc must haveElementWithId("submitButton")
    }

    "display an error summary and inline error for a blank value" in {
      val errorForm = PayeRefForm.form.withError("payeReference", "tai.payeRefForm.required")
      val d         = doc(viewFor(errorForm, "employment"))
      d.select(".govuk-error-message").text must include(messages("tai.payeRefForm.required"))
    }

    "preserve a previously entered value" in {
      val filled = PayeRefForm.form.bind(Map("payeReference" -> "123/ABC123"))
      val d      = doc(viewFor(filled, "employment"))
      d.getElementById("payeReference").attr("value") mustBe "123/ABC123"
    }
  }

  "PayeRefFormView (pension journey)" must {

    "display the correct pre-heading (pension)" in {
      val d = doc(viewFor(PayeRefForm.form, "pension"))
      d must haveElementAtPathWithText(
        ".govuk-caption-xl",
        messages("tai.ptaHeader.accessible.preHeading") + " " + messages("add.missing.pension")
      )
    }

    "post to the pension submit PAYE ref route" in {
      val d = doc(viewFor(PayeRefForm.form, "pension"))
      d.select("form").attr("action") mustBe controllers.pensions.routes.AddPensionProviderController
        .submitPayeReference()
        .url
    }

    "have the pension back link" in {
      val d = doc(viewFor(PayeRefForm.form, "pension"))
      d.select("a[class=govuk-back-link]").attr("href") mustBe
        controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url
    }
  }
}
