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

package uk.gov.hmrc.tai.forms

import org.joda.time.LocalDate
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json

class DateFormSpec extends PlaySpec with OneAppPerSuite with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "DateForm" must {

    "return no errors" when {

      "return no errors with valid data" in {

        val validatedFormForValidDate = form.bind(validDate)
        val validatedFormForValidLeapYearDate = form.bind(validLeapYearDate)

        validatedFormForValidDate.errors mustBe empty
        validatedFormForValidLeapYearDate.errors mustBe empty
      }

      "a custom validator is supplied which returns true during validation" in {

        val validatorErrorMessage = "test error message"

        val customValidator = ((x: LocalDate) => { true }, validatorErrorMessage)

        val dateFormWithCustomValidator = DateForm(Seq(customValidator), blankDateErrorMessage)

        val validatedFormForValidDate = dateFormWithCustomValidator.form.bind(validDate)

        validatedFormForValidDate.errors mustBe empty
      }
    }

    "deconstruct a local date correctly" in {

      val prePopForm = form.fill(new LocalDate(2014, 8, 15))

      prePopForm.data must contain(dateForm.DateFormDay -> "15")
      prePopForm.data must contain(dateForm.DateFormMonth -> "8")
      prePopForm.data must contain(dateForm.DateFormYear -> "2014")
    }

    "return an error" when {

      "day is blank" in {

        val validatedFormNoDayError = form.bind(invalidNoDayValue)

        validatedFormNoDayError.errors must contain(FormError(DayTag, List(blankDateErrorMessage)))
      }

      "month is blank" in {

        val validatedFormNoMonthError = form.bind(invalidNoMonthValue)

        validatedFormNoMonthError.errors must contain(FormError(DayTag, List(blankDateErrorMessage)))
      }

      "year is blank" in {

        val validatedFormNoYearError = form.bind(invalidNoYearValue)

        validatedFormNoYearError.errors must contain(FormError(DayTag, List(blankDateErrorMessage)))
      }

      "multiple fields are blank" in {

        val validatedFormNoDayNoYearError = form.bind(invalidNoDayNoYearValue)

        validatedFormNoDayNoYearError.errors must be(List(FormError(DayTag, blankDateErrorMessage)))
      }

      "date format is not a valid date" in {

        val validatedFormForInvalidDay = form.bind(invalidDay)
        val validatedFormForInvalidMonth = form.bind(invalidMonth)
        val validatedFormForInvalidYear = form.bind(invalidYear)
        val validatedFormForInvalidDate = form.bind(invalidLeapYearDate)

        validatedFormForInvalidDay.errors must contain(FormError(DayTag, List(Messages("tai.date.error.invalid"))))
        validatedFormForInvalidMonth.errors must contain(FormError(DayTag, List(Messages("tai.date.error.invalid"))))
        validatedFormForInvalidYear.errors must contain(FormError(DayTag, List(Messages("tai.date.error.invalid"))))
        validatedFormForInvalidDate.errors must contain(FormError(DayTag, List(Messages("tai.date.error.invalid"))))
      }

      "date is in future" in {
//        val validatedFormForValidDate = form.bind(validFutureDate)
//
//        validatedFormForValidDate.errors must contain(FormError(DayTag, List(Messages("tai.date.error.future"))))

        val validatorErrorMessage = Messages("tai.date.error.future")

        val customValidator = ((x: LocalDate) => !x.isAfter(LocalDate.now()), validatorErrorMessage)

        val dateFormWithCustomValidator = DateForm(Seq(customValidator), blankDateErrorMessage)

        val validatedFormForValidDate = dateFormWithCustomValidator.form.bind(validFutureDate)

        validatedFormForValidDate.errors must be(List(FormError(DayTag, validatorErrorMessage)))

      }
    }
  }

  private val blankDateErrorMessage: String = "blank date error message"

  private val dateForm = DateForm(Nil, blankDateErrorMessage)
  private val form = dateForm.form

  private val DayTag: String = dateForm.DateFormDay
  private val MonthTag: String = dateForm.DateFormMonth
  private val YearTag: String = dateForm.DateFormYear

  private val validDate = Json.obj(DayTag -> 10, MonthTag -> 4, YearTag -> 2015)
  private val validFutureDate = Json.obj(DayTag -> 10, MonthTag -> 4, YearTag -> (LocalDate.now().getYear + 1))
  private val validLeapYearDate = Json.obj(DayTag -> 29, MonthTag -> 2, YearTag -> 2016)

  private val invalidDay = Json.obj(DayTag -> "Bar", MonthTag -> 4, YearTag -> 2015)
  private val invalidMonth = Json.obj(DayTag -> 1, MonthTag -> "Foo", YearTag -> 2015)
  private val invalidYear = Json.obj(DayTag -> 1, MonthTag -> 4, YearTag -> "Baz")
  private val invalidLeapYearDate = Json.obj(DayTag -> 29, MonthTag -> 2, YearTag -> 2015)

  private val invalidNoDayValue = Json.obj(DayTag -> "", MonthTag -> 4, YearTag -> 2015)
  private val invalidNoMonthValue = Json.obj(DayTag -> 4, MonthTag -> "", YearTag -> 2015)
  private val invalidNoYearValue = Json.obj(DayTag -> 4, MonthTag -> 12, YearTag -> "")
  private val invalidNoDayNoYearValue = Json.obj(DayTag -> "", MonthTag -> 12, YearTag -> "")
}
