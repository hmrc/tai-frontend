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

package views.html.benefits

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm.DecisionChoice
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class UpdateOrRemoveCompanyBenefitSpec extends TaiViewSpec{


  "decision" should {
    behave like pageWithTitle(messages("tai.benefits.updateOrRemove.decision.title"))
    behave like pageWithCombinedHeader(messages("tai.benefits.updateOrRemove.journey.preHeader"),
      messages("tai.benefits.updateOrRemove.decision.heading",benefitType,employerName))
    behave like pageWithBackButton(controllers.routes.TaxAccountSummaryController.onPageLoad)
    behave like pageWithCancelLink(controllers.routes.TaxAccountSummaryController.onPageLoad)
    behave like pageWithContinueButtonForm(s"/check-income-tax/update-or-remove-company-benefit/decision")

    "have two radio buttons with relevant text" in {
      doc must haveInputLabelWithText(idYesIGetThisBenefit, messages("tai.benefits.updateOrRemove.decision.radio.yes"))
      doc must haveInputLabelWithText(idNoIDontGetThisBenefit, messages("tai.benefits.updateOrRemove.decision.radio.no"))

    }

    "have a legend" in {
      doc must haveElementAtPathWithText("legend span[id=radioGroupLegendMain]",
        Messages("tai.benefits.updateOrRemove.decision.heading", benefitType,employerName))
    }

    "display error message" when {
      "form has error" in {
        val errorView = views.html.benefits.updateOrRemoveCompanyBenefitDecision(formWithErrors, benefitType, employerName)
        doc(errorView) must haveClassWithText(messages("tai.error.chooseOneOption"), "error-notification")
      }
    }

    "a decision has not been made" in {
      val errorView = views.html.benefits.updateOrRemoveCompanyBenefitDecision(formWithErrors, benefitType, employerName)
      doc(errorView) must haveErrorLinkWithText(messages("tai.error.chooseOneOption"))
    }


  }

  private lazy val formWithErrors: Form[Option[String]] = UpdateOrRemoveCompanyBenefitDecisionForm.form.bind(Map(
    DecisionChoice -> ""
  ))

  private val idYesIGetThisBenefit = "decisionChoice-yesigetthisbenefit"
  private val idNoIDontGetThisBenefit= "decisionChoice-noidontgetthisbenefit"
  private lazy val benefitType = "Expenses"
  private lazy val employerName = "EmployerA"

  override def view = views.html.benefits.updateOrRemoveCompanyBenefitDecision(UpdateOrRemoveCompanyBenefitDecisionForm.form,benefitType,employerName)
}

