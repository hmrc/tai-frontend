/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.tai.forms.employments.EmploymentAddDateForm._

import java.time.LocalDate

case class EmploymentAddDateForm(employerName: String) {

  def form(implicit messages: Messages): Form[LocalDate] = {

    implicit val dateFormatter: LocalDateFormatter = new LocalDateFormatter(
      formDay = EmploymentFormDay,
      formMonth = EmploymentFormMonth,
      formYear = EmploymentFormYear,
      errorMsgs = errorMsgs(employerName)
    )

    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]

    Form(localDateMapping)
  }

}

object EmploymentAddDateForm {
  val EmploymentFormDay   = "tellUsStartDateForm-day"
  val EmploymentFormMonth = "tellUsStartDateForm-month"
  val EmploymentFormYear  = "tellUsStartDateForm-year"

  def errorMsgs(employerName: String)(implicit messages: Messages) = LocalDateFormatter.ErrorMessages(
    enterDate = Messages("tai.addEmployment.startDateForm.error.enterDate", employerName),
    enterDay = Messages("tai.addEmployment.startDateForm.error.enterDay", employerName),
    enterMonth = Messages("tai.addEmployment.startDateForm.error.enterMonth", employerName),
    enterYear = Messages("tai.addEmployment.startDateForm.error.enterYear", employerName),
    enterDayAndMonth = Messages("tai.addEmployment.startDateForm.error.enterDayAndMonth", employerName),
    enterDayAndYear = Messages("tai.addEmployment.startDateForm.error.enterDayAndYear", employerName),
    enterMonthAndYear = Messages("tai.addEmployment.startDateForm.error.enterMonthAndYear", employerName),
    mustBeValidDay = Messages("tai.addEmployment.startDateForm.error.mustBeValidDay", employerName),
    mustBeValidMonth = Messages("tai.addEmployment.startDateForm.error.mustBeValidMonth", employerName),
    mustBeValidYear = Messages("tai.addEmployment.startDateForm.error.mustBeValidYear", employerName),
    mustBeReal = Messages("tai.addEmployment.startDateForm.error.mustBeReal", employerName),
    mustBeFuture = Messages("tai.addEmployment.startDateForm.error.mustBeFuture", employerName),
    mustBeAfter1900 = Messages("tai.addEmployment.startDateForm.error.mustBeAfter1900", employerName)
  )
}
