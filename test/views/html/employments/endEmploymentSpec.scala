/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.tai.forms.employments.EmploymentEndDateForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.EmploymentViewModel


class endEmploymentSpec extends TaiViewSpec {

  private val employmentId: Int = 0
  private val employmentName = "employer name"
  private val eedf = EmploymentEndDateForm(employmentName)

  private val globalErrorMessage: String = "day error message"
  private val formWithErrors: Form[LocalDate] = eedf.form.withError("", globalErrorMessage)
  private lazy val employmentEndDateForm: Form[LocalDate] = eedf.form.bind(Map(
    eedf.EmploymentFormDay -> "1",
    "month" -> "1",
    "year" -> "2017"
  ))

  private val viewmodel = EmploymentViewModel(employmentName, employmentId)

  override def view: Html = views.html.employments.endEmployment(employmentEndDateForm, viewmodel)

  "Tell us about your employments page" should {

    behave like pageWithTitle(messages("tai.endEmployment.endDateForm.title", employmentName))

    behave like pageWithCombinedHeader(
      messages("tai.endEmployment.preHeadingText"),
      messages("tai.endEmployment.endDateForm.title", employmentName))

    behave like pageWithBackLink
    behave like pageWithCancelLink(controllers.employments.routes.EndEmploymentController.cancel(viewmodel.empId))
    behave like pageWithContinueButtonForm(s"/check-income-tax/end-employment/date/$employmentId")

    "have an error box at the top of the page with a link to the error field" when {
      "a form with errors is passed into the view" in {
        def view: Html = views.html.employments.endEmployment(formWithErrors, viewmodel)
        val errorSummary = doc(view).select("#error-summary-display a").text

        errorSummary mustBe globalErrorMessage
      }
    }

    "have a legend in the form" in {
      def view: Html = views.html.employments.endEmployment(employmentEndDateForm, viewmodel)
      val legendItem1 = doc(view).select("legend .form-label").text

      legendItem1 mustBe Messages("tai.endEmployment.endDateForm.label", employmentName)
    }

    "have a form hint" in {
      val legendItem2 = doc(view).select("legend .form-hint").text

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
        def view: Html = views.html.employments.endEmployment(formWithErrors, viewmodel)
        val errorMessage = doc(view).select(".error-message").text
        val fieldSetError = doc(view).select("form > div").hasClass("form-group-error")

        fieldSetError mustBe true
        errorMessage mustBe globalErrorMessage
      }
    }

    "have a 'continue' button" in {

        val continueButton = doc(view).select("button[type=submit]").text

        continueButton mustBe Messages("tai.submit")
    }
  }
}
