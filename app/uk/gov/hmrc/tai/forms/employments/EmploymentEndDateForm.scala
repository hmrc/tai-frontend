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

import play.api.data.Forms.of
import play.api.data.{FieldMapping, Form}
import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.LocalDateFormatter
import uk.gov.hmrc.tai.forms.employments.EmploymentEndDateForm._

import java.time.LocalDate

case class EmploymentEndDateForm(employerName: String) {

  def form(implicit messages: Messages): Form[LocalDate] = {

    implicit val dateFormatter = new LocalDateFormatter(
      formDay = EmploymentFormDay,
      formMonth = EmploymentFormMonth,
      formYear = EmploymentFormYear,
      errorMsgs = errorMsgs(employerName)
    )

    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]

    Form(localDateMapping)
  }
}

object EmploymentEndDateForm {
  val EmploymentFormDay   = "tellUsAboutEmploymentForm-day"
  val EmploymentFormMonth = "tellUsAboutEmploymentForm-month"
  val EmploymentFormYear  = "tellUsAboutEmploymentForm-year"

  def errorMsgs(employerName: String)(implicit messages: Messages) = LocalDateFormatter.ErrorMessages(
    enterDate = Messages("tai.endEmployment.endDateForm.error.enterDate", employerName),
    enterDay = Messages("tai.endEmployment.endDateForm.error.enterDay", employerName),
    enterMonth = Messages("tai.endEmployment.endDateForm.error.enterMonth", employerName),
    enterYear = Messages("tai.endEmployment.endDateForm.error.enterYear", employerName),
    enterDayAndMonth = Messages("tai.endEmployment.endDateForm.error.enterDayAndMonth", employerName),
    enterDayAndYear = Messages("tai.endEmployment.endDateForm.error.enterDayAndYear", employerName),
    enterMonthAndYear = Messages("tai.endEmployment.endDateForm.error.enterMonthAndYear", employerName),
    mustBeValidDay = Messages("tai.endEmployment.endDateForm.error.mustBeValidDay", employerName),
    mustBeValidMonth = Messages("tai.endEmployment.endDateForm.error.mustBeValidMonth", employerName),
    mustBeValidYear = Messages("tai.endEmployment.endDateForm.error.mustBeValidYear", employerName),
    mustBeReal = Messages("tai.endEmployment.endDateForm.error.mustBeReal", employerName),
    mustBeFuture = Messages("tai.endEmployment.endDateForm.error.mustBeFuture", employerName),
    mustBeAfter1900 = Messages("tai.endEmployment.endDateForm.error.mustBeAfter1900", employerName)
  )

}
