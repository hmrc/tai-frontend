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

package views.html.benefits

import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm.DecisionChoice
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.benefit.CompanyBenefitDecisionViewModel

class UpdateOrRemoveCompanyBenefitDecisionViewSpec extends TaiViewSpec {

  private val updateOrRemoveCompanyBenefitDecision = inject[UpdateOrRemoveCompanyBenefitDecisionView]

  "decision" should {
    behave like pageWithTitle(messages("tai.benefits.updateOrRemove.decision.heading", benefitType, employerName))
    behave like pageWithCombinedHeaderNewTemplate(
      messages("tai.benefits.updateOrRemove.journey.preHeader"),
      messages("tai.benefits.updateOrRemove.decision.heading", benefitType, employerName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithBackLinkNew
    behave like pageWithCancelLink(controllers.benefits.routes.RemoveCompanyBenefitController.cancel())
    behave like pageWithContinueButtonFormNew(s"/check-income-tax/company-benefit/decision")

    "have two radio buttons with relevant text" in {
      doc must haveInputLabelWithText(idYesIGetThisBenefit, messages("tai.benefits.updateOrRemove.decision.radio.yes"))
      doc must haveInputLabelWithText(
        idNoIDontGetThisBenefit,
        messages("tai.benefits.updateOrRemove.decision.radio.no"))

    }

    "display error message" when {
      "form has error" in {
        val errorView = updateOrRemoveCompanyBenefitDecision(viewModelWithErrors)
        doc(errorView) must haveClassWithText(
          messages("tai.income.error.form.summary") + " " +
            messages("tai.benefits.updateOrRemove.decision.radio.error"),
          "govuk-error-summary")
      }
    }

    "a decision has not been made" in {
      val errorView = updateOrRemoveCompanyBenefitDecision(viewModelWithErrors)
      doc(errorView) must haveErrorLinkWithTextNew(messages("tai.benefits.updateOrRemove.decision.radio.error"))
    }

  }

  private lazy val formWithErrors: Form[Option[String]] = UpdateOrRemoveCompanyBenefitDecisionForm.form.bind(
    Map(
      DecisionChoice -> ""
    ))

  private val idYesIGetThisBenefit = "decisionChoice"
  private val idNoIDontGetThisBenefit = "decisionChoice-2"
  private lazy val benefitType = "Expenses"
  private lazy val employerName = "EmployerA"
  private lazy val viewModel =
    CompanyBenefitDecisionViewModel(benefitType, employerName, UpdateOrRemoveCompanyBenefitDecisionForm.form)
  private lazy val viewModelWithErrors = CompanyBenefitDecisionViewModel(benefitType, employerName, formWithErrors)

  override def view: Html = updateOrRemoveCompanyBenefitDecision(viewModel)
}
