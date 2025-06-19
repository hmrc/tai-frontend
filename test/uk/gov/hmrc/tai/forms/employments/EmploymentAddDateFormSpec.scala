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

package uk.gov.hmrc.tai.forms.employments

import play.api.data.FormError
import utils.BaseSpec

import java.time.LocalDate

class EmploymentAddDateFormSpec extends BaseSpec {

  val errorMsgs = EmploymentAddDateForm.errorMsgs("employer")

  "EmploymentAddDateForm" must {
    "return no errors with valid data" in {
      val validatedFormForValidDate         = form.bind(validDate)
      val validatedFormForValidLeapYearDate = form.bind(validLeapYearDate)

      validatedFormForValidDate.errors mustBe empty
      validatedFormForValidLeapYearDate.errors mustBe empty
    }

    "deconstruct a local date correctly" in {
      val prePopForm = form.fill(LocalDate.of(2014, 8, 15))

      prePopForm.data must contain(EmploymentAddDateForm.EmploymentFormDay -> "15")
      prePopForm.data must contain(EmploymentAddDateForm.EmploymentFormMonth -> "8")
      prePopForm.data must contain(EmploymentAddDateForm.EmploymentFormYear -> "2014")
    }

    "return an error" when {
      "day is blank" in {
        val validatedFormNoDayError = form.bind(invalidNoDayValue)

        validatedFormNoDayError.errors must contain(FormError(DayTag, List(errorMsgs.enterDay)))
      }

      "month is blank" in {
        val validatedFormNoMonthError = form.bind(invalidNoMonthValue)

        validatedFormNoMonthError.errors must contain(FormError(MonthTag, List(errorMsgs.enterMonth)))
      }

      "year is blank" in {
        val validatedFormNoYearError = form.bind(invalidNoYearValue)

        validatedFormNoYearError.errors must contain(FormError(YearTag, List(errorMsgs.enterYear)))
      }

      "multiple (day and year) fields are blank" in {
        val validatedFormNoDayNoYearError = form.bind(invalidNoDayNoYearValue)

        validatedFormNoDayNoYearError.errors must contain(FormError(DayTag, List(errorMsgs.enterDayAndYear)))
      }

      "date format is not a valid date" in {
        val validatedFormForInvalidDay   = form.bind(invalidDay)
        val validatedFormForInvalidMonth = form.bind(invalidMonth)
        val validatedFormForInvalidYear  = form.bind(invalidYear)
        val validatedFormForInvalidDate  = form.bind(invalidLeapYearDate)

        validatedFormForInvalidDay.errors   must contain(FormError(DayTag, List(errorMsgs.mustBeValidDay)))
        validatedFormForInvalidMonth.errors must contain(FormError(MonthTag, List(errorMsgs.mustBeValidMonth)))
        validatedFormForInvalidYear.errors  must contain(FormError(YearTag, List(errorMsgs.mustBeValidYear)))
        validatedFormForInvalidDate.errors  must contain(FormError(DayTag, List(errorMsgs.mustBeReal)))
      }

      "date is in future" in {
        val validatedFormForValidDate = form.bind(validFutureDate)

        validatedFormForValidDate.errors must contain(FormError(DayTag, List(errorMsgs.mustBeFuture)))

      }
    }
  }

  private val employmentAddDateForm = EmploymentAddDateForm("employer")
  private val form                  = employmentAddDateForm.form

  private val DayTag: String   = EmploymentAddDateForm.EmploymentFormDay
  private val MonthTag: String = EmploymentAddDateForm.EmploymentFormMonth
  private val YearTag: String  = EmploymentAddDateForm.EmploymentFormYear

  private val validDate         = Map(DayTag -> "10", MonthTag -> "4", YearTag -> "2015")
  private val validFutureDate   = Map(DayTag -> "10", MonthTag -> "4", YearTag -> s"${LocalDate.now().getYear + 1}")
  private val validLeapYearDate = Map(DayTag -> "29", MonthTag -> "2", YearTag -> "2016")

  private val invalidDay          = Map(DayTag -> "Bar", MonthTag -> "4", YearTag -> "2015")
  private val invalidMonth        = Map(DayTag -> "1", MonthTag -> "Foo", YearTag -> "2015")
  private val invalidYear         = Map(DayTag -> "1", MonthTag -> "4", YearTag -> "Baz")
  private val invalidLeapYearDate = Map(DayTag -> "29", MonthTag -> "2", YearTag -> "2015")

  private val invalidNoDayValue       = Map(DayTag -> "", MonthTag -> "4", YearTag -> "2015")
  private val invalidNoMonthValue     = Map(DayTag -> "4", MonthTag -> "", YearTag -> "2015")
  private val invalidNoYearValue      = Map(DayTag -> "4", MonthTag -> "12", YearTag -> "")
  private val invalidNoDayNoYearValue = Map(DayTag -> "", MonthTag -> "12", YearTag -> "")
}
