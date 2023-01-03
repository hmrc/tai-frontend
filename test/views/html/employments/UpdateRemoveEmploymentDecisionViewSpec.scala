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
import uk.gov.hmrc.tai.forms.employments.UpdateRemoveEmploymentForm
import uk.gov.hmrc.tai.util.constants.{EmploymentDecisionConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class UpdateRemoveEmploymentDecisionViewSpec extends TaiViewSpec {
  val employmentName = "Employment Name"
  val empId = 1
  val updateRemoveEmploymentForm: Form[Option[String]] =
    UpdateRemoveEmploymentForm
      .form("Some employer")
      .bind(Map(EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.YesValue))

  private val template = inject[UpdateRemoveEmploymentDecisionView]

  override def view: Html =
    template(updateRemoveForm = updateRemoveEmploymentForm, employmentName = employmentName, empId = empId)

  "update_remove_employment_decision" must {
    behave like pageWithTitle(messages("tai.employment.decision.legend", employmentName))

    behave like pageWithBackLinkNew

    "display preheading" in {
      doc must haveElementAtPathWithText(
        ".govuk-caption-xl",
        messages("tai.ptaHeader.accessible.preHeading") + " " + messages("tai.employment.decision.preHeading"))
    }

    "display label with heading" in {
      doc must haveElementAtPathWithText(
        ".govuk-fieldset__legend--xl",
        messages("tai.employment.decision.heading", employmentName))
    }

    behave like pageWithYesNoRadioButton(
      EmploymentDecisionConstants.EmploymentDecision,
      EmploymentDecisionConstants.EmploymentDecision + "-2",
      messages("tai.employment.decision.radio1"),
      messages("tai.employment.decision.radio2")
    )

    behave like pageWithContinueButtonFormNew("/check-income-tax/update-remove-employment/decision")

    behave like pageWithCancelLink(controllers.employments.routes.EndEmploymentController.cancel(empId))
  }
}
