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

package views.html.employments

import java.time.LocalDate
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.EmploymentAddDateForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class AddEmploymentStartDateFormViewSpec extends TaiViewSpec {
  private val employmentName = "Employer name"
  private val addDateForm = EmploymentAddDateForm(employmentName)
  private val globalErrorMessage: String = "day error message"
  private val formWithErrors: Form[LocalDate] = addDateForm.form.withError("", globalErrorMessage)
  private lazy val employmentStartDateForm: Form[LocalDate] = addDateForm.form.bind(
    Map(
      addDateForm.EmploymentFormDay -> "1",
      "month"                       -> "1",
      "year"                        -> "2017"
    ))
  private val template = inject[AddEmploymentStartDateFormView]

  override def view: Html =
    template(employmentStartDateForm, employmentName)

  "Add employment start date form" should {
    behave like pageWithTitle(messages("tai.addEmployment.startDateForm.pagetitle"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonFormNew("/check-income-tax/add-employment/employment-start-date")
    behave like pageWithCancelLink(controllers.employments.routes.AddEmploymentController.cancel())

    "have an error box at the top of the page with a link to the error field" when {
      "a form with errors is passed into the view" in {
        def view: Html = template(formWithErrors, employmentName)

        val errorSummary = doc(view).select(" .govuk-list.govuk-error-summary__list a").text

        errorSummary mustBe globalErrorMessage
      }
    }

    "have a label in the form" in {
      val legendItem1 = doc(view).select("legend .form-label").text

      legendItem1 contains Messages("tai.addEmployment.startDateForm.label", employmentName)
    }

    "have a form hint" in {
      val legendItem2 = doc(view).select("#tellUsStartDateForm-hint").text

      legendItem2 mustBe Messages("tai.label.date.example")
    }

    "have a form input for day with relevant label" in {
      val labelDay = doc(view).select("label[for=tellUsStartDateForm-day]")
      val inputLabelDay = labelDay.text
      val numberOfInputs = doc(view).select("#tellUsStartDateForm-day").size

      inputLabelDay mustBe Messages("tai.label.day")
      numberOfInputs mustBe 1
    }

    "have a form input for month with relevant label" in {
      val labelMonth = doc(view).select("label[for=tellUsStartDateForm-month]")
      val inputLabelMonth = labelMonth.text
      val numberOfInputs = doc(view).select("#tellUsStartDateForm-month").size

      inputLabelMonth mustBe Messages("tai.label.month")
      numberOfInputs mustBe 1
    }

    "have a form input for year with relevant label" in {
      val labelYear = doc(view).select("label[for=tellUsStartDateForm-year]")
      val inputLabelYear = labelYear.text
      val numberOfInputs = doc(view).select("#tellUsStartDateForm-year").size

      inputLabelYear mustBe Messages("tai.label.year")
      numberOfInputs mustBe 1
    }

    "have an error message with the form inputs" when {
      "there is a form with an error" in {
        def view: Html = template(formWithErrors, employmentName)

        val errorMessage = doc(view).select(".govuk-error-message").text
        val fieldSetError = doc(view).select("form > div").hasClass("govuk-form-group--error")
        fieldSetError mustBe true
        errorMessage contains globalErrorMessage
      }
    }
  }

}
