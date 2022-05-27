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
import uk.gov.hmrc.tai.forms.employments.EmploymentEndDateForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.EmploymentViewModel

class EndEmploymentViewSpec extends TaiViewSpec {

  private val employmentId: Int = 0
  private val employmentName = "employer name"
  private val eedf = EmploymentEndDateForm(employmentName)

  private val globalErrorMessage: String = "day error message"
  private val formWithErrors: Form[LocalDate] = eedf.form.withError("", globalErrorMessage)
  private lazy val employmentEndDateForm: Form[LocalDate] = eedf.form.bind(
    Map(
      eedf.EmploymentFormDay -> "1",
      "month"                -> "1",
      "year"                 -> "2017"
    ))

  private val viewmodel = EmploymentViewModel(employmentName, employmentId)

  private val template = inject[EndEmploymentView]

  override def view: Html = template(employmentEndDateForm, viewmodel)

  "Tell us about your employments page" should {

    behave like pageWithTitle(messages("tai.endEmployment.endDateForm.pagetitle"))

    behave like pageWithCombinedHeaderNewTemplate(
      messages("tai.endEmployment.preHeadingText"),
      messages("tai.endEmployment.endDateForm.title", employmentName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )

    behave like pageWithBackLink
    behave like pageWithCancelLink(controllers.employments.routes.EndEmploymentController.cancel(viewmodel.empId))
    behave like pageWithContinueButtonFormNew(s"/check-income-tax/end-employment/date/$employmentId")

    "have an error box at the top of the page with a link to the error field" when {
      "a form with errors is passed into the view" in {
        def view: Html = template(formWithErrors, viewmodel)
        val errorSummary = doc(view).select(".govuk-list.govuk-error-summary__list a").text

        errorSummary mustBe globalErrorMessage
      }
    }

    "have a legend in the form" in {
      def view: Html = template(employmentEndDateForm, viewmodel)
      val legendItem1 = doc(view).select("#date-you-left-hint").text

      legendItem1 mustBe Messages("tai.endEmployment.endDateForm.label", employmentName)
    }

    "have a form hint" in {
      val legendItem2 = doc(view).select("#date-example-hint").text

      legendItem2 mustBe Messages("tai.label.date.example")
    }

    "have a form input for day with relevant label" in {
      val labelDay = doc(view).select("label[for=tellUsAboutEmploymentForm-day]")
      val inputLabelDay = labelDay.text
      val numberOfInputs = doc(view).select("#tellUsAboutEmploymentForm-day").size

      inputLabelDay mustBe Messages("tai.label.day")
      numberOfInputs mustBe 1
    }

    "have a form input for month with relevant label" in {
      val labelMonth = doc(view).select("label[for=tellUsAboutEmploymentForm-month]")
      val inputLabelMonth = labelMonth.text
      val numberOfInputs = doc(view).select("#tellUsAboutEmploymentForm-month").size

      inputLabelMonth mustBe Messages("tai.label.month")
      numberOfInputs mustBe 1
    }

    "have a form input for year with relevant label" in {
      val labelYear = doc(view).select("label[for=tellUsAboutEmploymentForm-year]")
      val inputLabelYear = labelYear.text
      val numberOfInputs = doc(view).select("#tellUsAboutEmploymentForm-year").size

      inputLabelYear mustBe Messages("tai.label.year")
      numberOfInputs mustBe 1
    }

    "have an error message with the form inputs" when {
      "there is a form with an error" in {
        def view: Html = template(formWithErrors, viewmodel)
        val errorMessage = doc(view).select(".error-message").text
        val fieldSetError = doc(view).select("form div").hasClass("govuk-form-group--error")

        fieldSetError mustBe true
//        errorMessage mustBe globalErrorMessage
      }
    }

    "have a 'continue' button" in {

      val continueButton = doc(view).select(".govuk-button").text

      continueButton mustBe Messages("tai.submit")
    }
  }
}
