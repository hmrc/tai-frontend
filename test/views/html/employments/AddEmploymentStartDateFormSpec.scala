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

import org.joda.time.LocalDate
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.employments.EmploymentAddDateForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class AddEmploymentStartDateFormSpec extends TaiViewSpec {
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
  private val template = inject[add_employment_start_date_form]

  override def view: Html =
    template(employmentStartDateForm, employmentName)

  "Add employment start date form" should {
    behave like pageWithTitle(messages("tai.addEmployment.startDateForm.pagetitle"))
    behave like pageWithCombinedHeader(
      messages("add.missing.employment"),
      messages("tai.addEmployment.startDateForm.title", employmentName))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/check-income-tax/add-employment/employment-start-date")
    behave like pageWithCancelLink(controllers.employments.routes.AddEmploymentController.cancel())

    "have an error box at the top of the page with a link to the error field" when {
      "a form with errors is passed into the view" in {
        def view: Html = template(formWithErrors, employmentName)

        val errorSummary = doc(view).select("#error-summary-display a").text

        errorSummary mustBe globalErrorMessage
      }
    }

    "have a label in the form" in {
      val legendItem1 = doc(view).select("legend .form-label").text

      legendItem1 mustBe Messages("tai.addEmployment.startDateForm.label", employmentName)
    }

    "have a form hint" in {
      val legendItem2 = doc(view).select(".form-hint").text

      legendItem2 mustBe Messages("tai.label.date.example")
    }

    "have a form input for day with relevant label" in {
      val labelDay = doc(view).select(".form-group-day .form-label")
      val inputLabelDay = labelDay.text
      val numberOfInputs = doc(view).select(".form-group-day input").size

      inputLabelDay mustBe Messages("tai.label.day")
      numberOfInputs mustBe 1
    }

    "have a form input for month with relevant label" in {
      val labelMonth = doc(view).select(".form-group-month .form-label")
      val inputLabelMonth = labelMonth.text
      val numberOfInputs = doc(view).select(".form-group-month input").size

      inputLabelMonth mustBe Messages("tai.label.month")
      numberOfInputs mustBe 1
    }

    "have a form input for year with relevant label" in {
      val labelYear = doc(view).select(".form-group-year .form-label")
      val inputLabelYear = labelYear.text
      val numberOfInputs = doc(view).select(".form-group-year input").size

      inputLabelYear mustBe Messages("tai.label.year")
      numberOfInputs mustBe 1
    }

    "have an error message with the form inputs" when {
      "there is a form with an error" in {
        def view: Html = template(formWithErrors, employmentName)

        val errorMessage = doc(view).select(".error-message").text
        val fieldSetError = doc(view).select("form > div").hasClass("form-group-error")

        fieldSetError mustBe true
        errorMessage mustBe globalErrorMessage
      }
    }
  }

}
