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

import controllers.routes
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.benefits.DateForm
import uk.gov.hmrc.tai.forms.benefits.DateForm._
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class CompanyCarEndDateSpec extends TaiViewSpec {

  "Change company car page" should {

    behave like pageWithTitle(messages("tai.companyCar.endDate.title"))

    behave like pageWithCombinedHeader(
      messages("tai.companyCar.endDate.sub.heading"),
      messages("tai.companyCar.endDate.heading"))

    behave like pageWithBackButton(routes.CompanyCarController.getCompanyCarDetails())

    behave like pageWithCancelLink(routes.TaxFreeAmountControllerNew.taxFreeAmount())

    behave like pageWithContinueButtonForm("/check-income-tax/end-company-car/car-end-date")


    "have a form input for day with relevant label" in {
      val labelDay = doc(view).select("form > fieldset > div.form-group-day > label.form-label")
      val inputLabelDay = labelDay.text
      val numberOfInputs = doc(view).select("form > fieldset > div.form-group-day > input").size

      inputLabelDay mustBe Messages("tai.label.day")
      numberOfInputs mustBe 1
    }

    "have a form input for month with relevant label" in {
      val labelMonth = doc(view).select("form > fieldset > div.form-group-month > label.form-label")
      val inputLabelMonth = labelMonth.text
      val numberOfInputs = doc(view).select("form > fieldset > div.form-group-month > input").size

      inputLabelMonth mustBe Messages("tai.label.month")
      numberOfInputs mustBe 1
    }

    "have a form input for year with relevant label" in {
      val labelYear = doc(view).select("form > fieldset > div.form-group-year > label.form-label")
      val inputLabelYear = labelYear.text
      val numberOfInputs = doc(view).select("form > fieldset > div.form-group-year > input").size

      inputLabelYear mustBe Messages("tai.label.year")
      numberOfInputs mustBe 1
    }

    "throws error" when {

      "the form has errors" in {
        def view: Html = views.html.benefits.companyCarEndDate(errorForm)
        val errorMessage = doc(view).select(".error-notification").text
        val fieldSetError = doc(view).select("fieldset").hasClass("form-field-group--error")

        fieldSetError mustBe true
        errorMessage mustBe "error on page"
      }

      "there is no date provided" in {
        def view: Html = views.html.benefits.companyCarEndDate(emptyCompanyEndDateForm)
        val errorMessage = doc(view).select(".error-notification").text

        errorMessage mustBe "Enter the date you gave the car back"
      }

      "input date is before the start date" in {
        val errorForm = verifyDate(companyCarEndDateBeforeStartDateForm, Some("2016-11-11"))
        val errorMessage = errorForm.error("dateForm").map(_.message)

        errorForm.hasErrors mustBe true
        errorMessage mustBe Some(Messages("tai.date.error.invalid"))
      }

      "input date is a future date" in {
        val errorForm = verifyDate(companyCarEndDateIsFutureForm, Some(new LocalDate().plusDays(1).toString))
        val errorMessage = errorForm.error("dateForm").map(_.message)

        errorForm.hasErrors mustBe true
        errorMessage mustBe Some(Messages("tai.date.error.invalid"))
      }
    }
  }
  private val testDateForm = DateForm(Messages("tai.companyCar.endDate.blank"))

  private lazy val testCompanyEndDateForm: Form[LocalDate] = testDateForm.form.bind(Map(
    testDateForm.DateFormDay -> "1",
    testDateForm.DateFormMonth -> "1",
    testDateForm.DateFormYear -> "2017"
  ))

  private lazy val emptyCompanyEndDateForm: Form[LocalDate] = testDateForm.form.bind(Map(
    testDateForm.DateFormDay -> "",
    testDateForm.DateFormMonth -> "",
    testDateForm.DateFormYear -> ""
  ))

  private lazy val companyCarEndDateBeforeStartDateForm: Form[LocalDate] = testDateForm.form.bind(Map(
    testDateForm.DateFormDay -> "1",
    testDateForm.DateFormMonth -> "10",
    testDateForm.DateFormYear -> "2016"
  ))

  private lazy val companyCarEndDateIsFutureForm: Form[LocalDate] = testDateForm.form.bind(Map(
    testDateForm.DateFormDay -> "1",
    testDateForm.DateFormMonth -> "10",
    testDateForm.DateFormYear -> "2017"
  ))

  private lazy val errorForm: Form[LocalDate] = testDateForm.form.withError("", "error on page")

  override def view: Html = views.html.benefits.companyCarEndDate(testCompanyEndDateForm)
}
