/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.tai.forms.employments.{IrregularPayForm, IrregularPayFormData}
import uk.gov.hmrc.tai.util.constants.IrregularPayConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.EmploymentViewModel

class EndEmploymentIrregularPaymentErrorPageSpec extends TaiViewSpec with IrregularPayConstants {

  "Irregular pay page" must {
    behave like pageWithTitle(messages("tai.irregular.preHeadingText"))
    behave like pageWithBackLink
    behave like pageWithCancelLink(controllers.routes.IncomeSourceSummaryController.onPageLoad(model.empId))
    behave like pageWithCombinedHeader(
      messages("tai.irregular.preHeadingText"),
      messages("tai.irregular.heading", model.employerName))
    behave like pageWithContinueButtonForm("/check-income-tax/end-employment/handle-irregular-payment")

    "display paragraphs" in {
      doc(view) must haveParagraphWithText(messages("tai.irregular.para1", model.employerName))
      doc(view) must haveParagraphWithText(messages("tai.irregular.para2", model.employerName))
      doc(view) must haveParagraphWithText(messages("tai.irregular.para3"))
      doc(view) must haveElementAtPathWithText("legend span", messages("tai.irregular.para4"))
    }

    "display radio buttons" in {
      doc must haveElementAtPathWithId("fieldset input", "irregularPayDecision-contactemployer")
      doc must haveElementAtPathWithId("fieldset input", "irregularPayDecision-updatedetails")
      doc must haveElementAtPathWithText("fieldset label", messages("tai.irregular.option1", model.employerName))
      doc must haveElementAtPathWithText("fieldset label", messages("tai.irregular.option2"))
    }

    "display error message" when {
      "form has error" in {
        val errorView = template(formWithErrors, model)
        doc(errorView) must haveClassWithText(messages("tai.error.chooseOneOption"), "error-message")
      }
    }
  }

  private lazy val formWithErrors: Form[IrregularPayFormData] = IrregularPayForm.createForm.bind(
    Map(
      IrregularPayDecision -> ""
    ))

  private lazy val validForm: Form[IrregularPayFormData] = IrregularPayForm.createForm.bind(
    Map(
      IrregularPayDecision -> ContactEmployer
    ))

  private lazy val model = EmploymentViewModel("TEST", 1)

  private val template = inject[EndEmploymentIrregularPaymentError]

  override def view: Html = template(validForm, model)
}
