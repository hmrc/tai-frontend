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
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class CompanyCarFuelBenefitEndDateSpec extends TaiViewSpec {

  "End company car fuel benefit page" should {

    behave like pageWithTitle(messages("tai.companyCar.fuelBenefitEndDate.title"))

    behave like pageWithCombinedHeader(
      messages("tai.companyCar.fuelBenefitEndDate.sub.heading"),
      messages("tai.companyCar.fuelBenefitEndDate.heading"))

    behave like pageWithBackLink

    behave like pageWithCancelLink(routes.TaxFreeAmountController.taxFreeAmount())

    behave like pageWithContinueButtonForm("/check-income-tax/end-company-car/fuel-end-date")

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

    "correctly summarise form errors at the page level" in {

      val emptyFormDoc = doc(views.html.benefits.fuelBenefitEndDate(errorForm))

      emptyFormDoc.select(".error-summary--show h2").text mustBe Messages("tai.income.error.form.summary")

      val errorAnchor = emptyFormDoc.select(".error-summary--show li a").get(0)
      errorAnchor.attributes.get("href") mustBe "#dateForm_day"
      errorAnchor.attributes.get("id") mustBe "dateForm_day-error-summary"
      errorAnchor.text mustBe Messages("tai.companyCar.fuelBenefitEndDate.blank")
    }

    "correctly highlight error items at the form level" in {

      val emptyFormDoc = doc(views.html.benefits.fuelBenefitEndDate(errorForm))

      emptyFormDoc.select(".error-message").text mustBe Messages("tai.companyCar.fuelBenefitEndDate.blank")
      emptyFormDoc.select("form > .form-group").hasClass("form-group-error") mustBe true
    }
  }

  private val testDateForm = DateForm(Messages("tai.companyCar.fuelBenefitEndDate.blank"))

  private lazy val testFuelEndDateForm: Form[LocalDate] = testDateForm.form.bind(Map(
    testDateForm.DateFormDay -> "1",
    testDateForm.DateFormMonth -> "1",
    testDateForm.DateFormYear -> "2017"
  ))

  private lazy val errorForm: Form[LocalDate] = testDateForm.form.bind(Map("dateForm_day" -> ""))

  override def view: Html = views.html.benefits.fuelBenefitEndDate(testFuelEndDateForm)
}
