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
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm.StopDateChoice
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

import java.time.format.DateTimeFormatter

class RemoveCompanyBenefitStopDateViewSpec extends TaiViewSpec {

  private val removeCompanyBenefitStopDate = inject[RemoveCompanyBenefitStopDateView]

  "stop date" should {

    behave like pageWithTitle(messages("tai.benefits.ended.stopDate.heading", benefitType, empName))
    behave like pageWithCombinedHeaderNewTemplate(
      messages("tai.benefits.ended.journey.preHeader"),
      messages("tai.benefits.ended.stopDate.heading", benefitType, empName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )
    behave like pageWithCancelLink(controllers.benefits.routes.RemoveCompanyBenefitController.cancel())
    behave like pageWithBackLinkNew
    behave like pageWithContinueButtonFormNew("/check-income-tax/remove-company-benefit/stop-date")

    "have two radio buttons with relevant text" in {
      doc must haveInputLabelWithText(
        idBeforeTaxYearEnd,
        messages("tai.benefits.ended.stopDate.radio.beforeTaxYearEnd", startOfCurrentTaxYear))
      doc must haveInputLabelWithText(
        idOnOrAfterTaxYearEnd,
        messages("tai.benefits.ended.stopDate.radio.onOrAfterTaxYearEnd", startOfCurrentTaxYear))
    }

    "display an explanation text paragraph" in {
      doc.getElementById("stopDate-container").text.replace(" ", "\u00A0") mustBe Html(
        Messages("tai.benefits.ended.stopDate.panel", startOfCurrentTaxYear, endOfCurrentTaxYear)
          .replace(" ", "\u00A0")).toString()
    }

    "display error message" when {
      val taxYearStart = Dates.formatDate(TaxYear().start)

      "form has error" in {
        val errorView = removeCompanyBenefitStopDate(formWithErrors, benefitType, empName)
        doc(errorView) must haveClassWithText(
          messages("tai.income.error.form.summary") + " " +
            messages("tai.benefits.ended.stopDate.radio.error", taxYearStart),
          "govuk-error-summary")
      }

      "a decision has not been made" in {
        val errorView = removeCompanyBenefitStopDate(formWithErrors, benefitType, empName)
        doc(errorView) must haveErrorLinkWithTextNew(messages("tai.benefits.ended.stopDate.radio.error", taxYearStart))
      }
    }
  }

  private val idBeforeTaxYearEnd = "stopDateChoice"
  private val idOnOrAfterTaxYearEnd = "stopDateChoice-2"
  private val startOfCurrentTaxYear = TaxYear().start.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
  private val endOfCurrentTaxYear = TaxYear().end.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
  private lazy val benefitType = "Expenses"
  private lazy val empName = "EmployerA"

  private lazy val formWithErrors: Form[Option[String]] = RemoveCompanyBenefitStopDateForm.form.bind(
    Map(
      StopDateChoice -> ""
    ))

  override def view: Html =
    removeCompanyBenefitStopDate(RemoveCompanyBenefitStopDateForm.form, benefitType, empName)
}
