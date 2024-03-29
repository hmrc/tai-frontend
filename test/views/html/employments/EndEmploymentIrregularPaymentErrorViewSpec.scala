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

package views.html.employments

import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.IrregularPayForm
import uk.gov.hmrc.tai.util.constants.IrregularPayConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.EmploymentViewModel

class EndEmploymentIrregularPaymentErrorViewSpec extends TaiViewSpec {

  "Irregular pay page" must {
    behave like pageWithTitle(messages("tai.irregular.preHeadingText"))
    behave like pageWithBackLink()
    behave like pageWithCancelLink(controllers.routes.IncomeSourceSummaryController.onPageLoad(model.empId))
    behave like pageWithCombinedHeaderNewTemplateNew(
      messages("tai.irregular.preHeadingText"),
      messages("tai.irregular.heading", model.employerName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithContinueButtonForm("/check-income-tax/end-employment/handle-irregular-payment")

    "display paragraphs" in {
      doc(view) must haveParagraphWithText(messages("tai.irregular.para1", model.employerName))
      doc(view) must haveParagraphWithText(messages("tai.irregular.para2", model.employerName))
      doc(view) must haveParagraphWithText(messages("tai.irregular.para3"))
      doc(view) must haveParagraphWithText(messages("tai.irregular.para4"))
    }

    "display radio buttons" in {
      doc must haveElementAtPathWithId("fieldset input", "irregularPayDecision")
      doc must haveElementAtPathWithId("fieldset input", "irregularPayDecision-2")
      doc must haveElementAtPathWithText("fieldset label", messages("tai.irregular.option1", model.employerName))
      doc must haveElementAtPathWithText("fieldset label", messages("tai.irregular.option2"))
    }

    "display error message" when {
      "form has error" in {
        val errorView = template(formWithErrors, model)
        doc(errorView) must haveClassWithText(
          messages("tai.errorMessage.heading") + " " + messages("tai.error.chooseOneOption"),
          "govuk-error-summary"
        )
      }
    }
  }

  private lazy val formWithErrors: Form[Option[String]] = IrregularPayForm.createForm.bind(
    Map(
      IrregularPayConstants.IrregularPayDecision -> ""
    )
  )

  private lazy val validForm: Form[Option[String]] = IrregularPayForm.createForm.bind(
    Map(
      IrregularPayConstants.IrregularPayDecision -> IrregularPayConstants.ContactEmployer
    )
  )

  private lazy val model = EmploymentViewModel("TEST", 1)

  private val template = inject[EndEmploymentIrregularPaymentErrorView]

  override def view: Html = template(validForm, model)
}
