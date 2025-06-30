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

package uk.gov.hmrc.tai.forms

import play.api.data.Forms.of
import play.api.data.{FieldMapping, Form}
import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.DateForm._
import uk.gov.hmrc.tai.forms.formValidator.FormValidator

import java.time.LocalDate

case class DateForm(validations: Seq[(LocalDate => Boolean, String)], blankDateMessage: String) extends FormValidator {

  def form(implicit messages: Messages): Form[LocalDate] = {
    implicit val dateFormatter: LocalDateFormatter = new LocalDateFormatter(
      formDay = DateFormDay,
      formMonth = DateFormMonth,
      formYear = DateFormYear,
      errorMsgs = errorMsgs
    )

    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]

    Form(localDateMapping)
  }

}

object DateForm {
  val DateFormDay   = "DateForm_day"
  val DateFormMonth = "DateForm_month"
  val DateFormYear  = "DateForm_year"

  def errorMsgs(implicit messages: Messages) = LocalDateFormatter.ErrorMessages(
    enterDate = Messages("tai.date.error.enterDate"),
    enterDay = Messages("tai.date.error.enterDay"),
    enterMonth = Messages("tai.date.error.enterMonth"),
    enterYear = Messages("tai.date.error.enterYear"),
    enterDayAndMonth = Messages("tai.date.error.enterDayAndMonth"),
    enterDayAndYear = Messages("tai.date.error.enterDayAndYear"),
    enterMonthAndYear = Messages("tai.date.error.enterMonthAndYear"),
    mustBeValidDay = Messages("tai.date.error.mustBeValidDay"),
    mustBeValidMonth = Messages("tai.date.error.mustBeValidMonth"),
    mustBeValidYear = Messages("tai.date.error.mustBeValidYear"),
    mustBeReal = Messages("tai.date.error.mustBeReal"),
    mustBeFuture = Messages("tai.date.error.mustBeFuture"),
    mustBeAfter1900 = Messages("tai.date.error.mustBeAfter1900")
  )
}
