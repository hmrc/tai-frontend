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

package views.html.incomes.previousYears

import play.api.data.Form
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.tai.forms.income.previousYears.UpdateIncomeDetailsDecisionForm
import uk.gov.hmrc.tai.forms.income.previousYears.UpdateIncomeDetailsDecisionForm.UpdateIncomeChoice
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.TaxPeriodLabelService
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class UpdateIncomeDetailsDecisionViewSpec extends TaiViewSpec {

  val taxYear = TaxYear().prev
  private val UpdateIncomeDetailsDecision = inject[UpdateIncomeDetailsDecisionView]

  "decision" should {

    behave like pageWithTitle(
      messages("tai.income.previousYears.decision.title", TaxPeriodLabelService.taxPeriodLabel(taxYear.year)))
    behave like pageWithCombinedHeaderNewTemplate(
      messages("tai.income.previousYears.journey.preHeader"),
      messages("tai.income.previousYears.decision.header", TaxPeriodLabelService.taxPeriodLabel(taxYear.year)),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithBackLink
    behave like pageWithCancelLink(controllers.routes.PayeControllerHistoric.payePage(taxYear))
    behave like pageWithYesNoRadioButton(
      UpdateIncomeDetailsDecisionForm.UpdateIncomeChoice,
      UpdateIncomeDetailsDecisionForm.UpdateIncomeChoice + "-2",
      messages("tai.income.previousYears.decision.radio.yes"),
      messages("tai.income.previousYears.decision.radio.no")
    )
    behave like pageWithContinueButtonForm("/check-income-tax/update-income-details/decision")

  }

  "display 'I want to:' h2" in {
    doc must haveH2HeadingWithText(messages("tai.income.previousYears.decision.IWantTo"))
  }

  "display paragraphs" in {
    doc must haveParagraphWithText(messages("tai.income.previousYears.decision.paragraph.one"))
    doc must haveParagraphWithText(messages("tai.income.previousYears.decision.paragraph.two"))
    doc must haveParagraphWithText(messages("tai.income.previousYears.decision.paragraph.three"))
  }

  "display error message" when {
    "form has error" in {
      val errorView: HtmlFormat.Appendable = UpdateIncomeDetailsDecision(formWithErrors, taxYear)
      doc(errorView) must haveClassWithText(
        messages("tai.error.message") + " " + messages("tai.income.previousYears.decision.error"),
        "govuk-error-message")
    }
  }

  "display error message" when {
    "a decision has not been made" in {
      val view: HtmlFormat.Appendable = UpdateIncomeDetailsDecision(formWithErrors, taxYear)
      doc(view) must haveErrorLinkWithTextNew(messages("tai.income.previousYears.decision.error"))
    }
  }

  private lazy val formWithErrors: Form[Option[String]] = UpdateIncomeDetailsDecisionForm.form.bind(
    Map(
      UpdateIncomeChoice -> ""
    ))

  override def view: HtmlFormat.Appendable = UpdateIncomeDetailsDecision(UpdateIncomeDetailsDecisionForm.form, taxYear)
}
