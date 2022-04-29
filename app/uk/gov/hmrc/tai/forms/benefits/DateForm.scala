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

package uk.gov.hmrc.tai.forms.benefits

import java.time.LocalDate
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.Messages

import scala.util.Try

case class DateForm(emptyDateMessage: String) {

  def form(implicit messages: Messages) = {
    implicit val dateFormatter = new Formatter[LocalDate] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

        val dayErrors: Boolean = data.getOrElse(DateFormDay, "").length == 0

        val monthErrors: Boolean = data.getOrElse(DateFormMonth, "").length == 0

        val yearErrors: Boolean = data.getOrElse(DateFormYear, "").length == 0

        val errors = if (dayErrors || monthErrors || yearErrors) {
          Seq(FormError(key = DateFormDay, message = emptyDateMessage))
        } else {
          Nil
        }

        if (errors.isEmpty) {
          val inputDate: Option[LocalDate] = Try(
            for {
              day   <- data.get(DateFormDay).map(Integer.parseInt)
              month <- data.get(DateFormMonth).map(Integer.parseInt)
              year  <- data.get(DateFormYear).map(Integer.parseInt)
            } yield LocalDate.of(year, month, day)
          ).getOrElse(None)

          inputDate match {
            case Some(date) => Right(date)
            case _          => Left(Seq(FormError(key = DateFormDay, message = Messages("tai.date.error.invalid"))))
          }
        } else {
          Left(errors)
        }
      }

      override def unbind(key: String, value: LocalDate): Map[String, String] = Map(
        DateFormDay   -> value.getDayOfMonth.toString,
        DateFormMonth -> value.getMonth.toString,
        DateFormYear  -> value.getYear.toString
      )
    }

    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]

    Form(localDateMapping)
  }

  val DateFormDay = "dateForm_day"
  val DateFormMonth = "dateForm_month"
  val DateFormYear = "dateForm_year"
}

object DateForm {

  def verifyDate(dateForm: Form[LocalDate], startDateInString: Option[String])(
    implicit messages: Messages): Form[LocalDate] =
    if (!dateForm.hasErrors) {
      val day = dateForm.data.get("dateForm_day").map(Integer.parseInt)
      val month = dateForm.data.get("dateForm_month").map(Integer.parseInt)
      val year = dateForm.data.get("dateForm_year").map(Integer.parseInt)

      val formErrors =
        (year, month, day) match {
          case (Some(y), Some(m), Some(d)) if LocalDate.of(y, m, d).isAfter(LocalDate.now()) =>
            Seq(FormError(key = "dateForm", Messages("tai.date.error.invalid")))
          case (Some(y), Some(m), Some(d)) =>
            startDateInString match {
              case Some(dateInString) if LocalDate.of(y, m, d).isBefore(LocalDate.parse(dateInString)) =>
                Seq(FormError(key = "dateForm", Messages("tai.date.error.invalid")))
              case _ => Nil
            }
          case _ => Nil
        }
      addErrorsToForm(dateForm, formErrors)
    } else dateForm

  private def addErrorsToForm[A](form: Form[A], formErrors: Seq[FormError]): Form[A] =
    formErrors.foldLeft(form)((f, e) => f.withError(e))
}
