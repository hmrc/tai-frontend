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

package views.html.benefits

import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}

import java.time.LocalDate

class RemoveCompanyBenefitStopDateViewSpec extends TaiViewSpec {

  private val removeCompanyBenefitStopDate = inject[RemoveCompanyBenefitStopDateView]

  "stop date" should {

    behave like pageWithTitle(messages("tai.benefits.ended.stopDate.heading", benefitType, empName))
    behave like pageWithCombinedHeaderNewTemplateNew(
      messages("tai.benefits.ended.journey.preHeader"),
      messages("tai.benefits.ended.stopDate.heading", benefitType, empName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithCancelLink(controllers.benefits.routes.RemoveCompanyBenefitController.cancel())
    behave like haveLinkWithUrlWithID("backLink", controllers.benefits.routes.CompanyBenefitController.decision().url)
    behave like pageWithContinueButtonFormNew("/check-income-tax/remove-company-benefit/stop-date")

    "have a hint" in {
      doc must haveHintWithText(
        BenefitFormHint,
        Messages("tai.label.date.example")
      )
    }

    "have a date input" in {
      doc must haveInputLabelWithText(BenefitFormDay, messages("tai.label.day"))
      doc must haveInputLabelWithText(BenefitFormMonth, messages("tai.label.month"))
      doc must haveInputLabelWithText(BenefitFormYear, messages("tai.label.year"))
    }

    "display error message" when {

      "form has error" in {
        val errorView = removeCompanyBenefitStopDate(formWithErrors, benefitType, empName)
        doc(errorView) must haveClassWithText(
          messages("tai.income.error.form.summary") + " " +
            messages("tai.benefits.ended.stopDate.error.enterDate"),
          "govuk-error-summary"
        )
      }

      "a decision has not been made" in {
        val errorView = removeCompanyBenefitStopDate(formWithErrors, benefitType, empName)
        doc(errorView) must haveErrorLinkWithTextNew(messages("tai.benefits.ended.stopDate.error.enterDate"))
      }
    }
  }

  private lazy val benefitType = "Expenses"
  private lazy val empName = "EmployerA"

  private lazy val formWithErrors: Form[LocalDate] = RemoveCompanyBenefitStopDateForm(benefitType, empName).form.bind(
    Map(
      BenefitFormDay -> ""
    )
  )

  override def view: Html =
    removeCompanyBenefitStopDate(RemoveCompanyBenefitStopDateForm(benefitType, empName).form, benefitType, empName)
}
