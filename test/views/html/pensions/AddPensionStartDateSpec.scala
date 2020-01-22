/*
 * Copyright 2020 HM Revenue & Customs
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

package views.html.pensions

import org.joda.time.LocalDate
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.pensions.PensionAddDateForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class AddPensionStartDateSpec extends TaiViewSpec {
  private val pensionName = "Pension name"
  private val addDateForm = PensionAddDateForm(pensionName)
  private val globalErrorMessage: String = "day error message"
  private val formWithErrors: Form[LocalDate] = addDateForm.form.withError("", globalErrorMessage)
  private lazy val pensionStartDateForm: Form[LocalDate] = addDateForm.form.bind(
    Map(
      addDateForm.PensionFormDay -> "9",
      "month"                    -> "6",
      "year"                     -> "2017"
    ))
  override def view: Html = views.html.pensions.addPensionStartDate(pensionStartDateForm, pensionName)

  "Add pension start date form" should {
    behave like pageWithTitle(messages("tai.addPensionProvider.startDateForm.pagetitle"))
    behave like pageWithCombinedHeader(
      messages("add.missing.pension"),
      messages("tai.addPensionProvider.startDateForm.title", pensionName))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/check-income-tax/add-pension-provider/first-payment-date")
    behave like pageWithCancelLink(controllers.pensions.routes.AddPensionProviderController.cancel())

    "have an error box at the top of the page with a link to the error field" when {
      "a form with errors is passed into the view" in {
        def view: Html = views.html.pensions.addPensionStartDate(formWithErrors, pensionName)

        val errorSummary = doc(view).select("#error-summary-display a").text

        errorSummary mustBe globalErrorMessage
      }
    }

    "have a label in the form" in {
      val legendItem1 = doc(view).select("legend .form-label").text

      legendItem1 mustBe Messages("tai.addPensionProvider.startDateForm.label", pensionName)
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
        def view: Html = views.html.pensions.addPensionStartDate(formWithErrors, pensionName)

        val errorMessage = doc(view).select(".error-message").text
        val fieldSetError = doc(view).select("form > div").hasClass("form-group-error")

        fieldSetError mustBe true
        errorMessage mustBe globalErrorMessage
      }
    }
  }

}
