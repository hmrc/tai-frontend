/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.tai.forms.formValidator.FormValidator
import org.joda.time.LocalDate
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.Messages

import scala.util.Try

case class DateForm(validations: Seq[((LocalDate) => Boolean, String)], blankDateMessage: String) extends FormValidator {

  def form(implicit messages: Messages) = {
    implicit val dateFormatter = new Formatter[LocalDate] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

        val dayErrors: Boolean = data.getOrElse(DateFormDay, "").isEmpty
        val monthErrors: Boolean = data.getOrElse(DateFormMonth, "").isEmpty
        val yearErrors: Boolean = data.getOrElse(DateFormYear, "").isEmpty

        val errors = if (dayErrors || monthErrors || yearErrors) {
          Seq(FormError(key = DateFormDay, message = blankDateMessage))
        } else {
          Nil
        }

        if (errors.isEmpty) {
          val inputDate: Option[LocalDate] = Try(
            for {
              day <- data.get(DateFormDay).map(Integer.parseInt)
              month <- data.get(DateFormMonth).map(Integer.parseInt)
              year <- data.get(DateFormYear).map(Integer.parseInt)
            } yield new LocalDate(year, month, day)
          ).getOrElse(None)

          inputDate match {
            case Some(date) => {

              val validationResult = validate[LocalDate](date, validations, DateFormDay)

              if (validationResult.isEmpty) {
                Right(date)
              } else {
                Left(validationResult.seq)
              }
            }
            case _ => Left(Seq(FormError(key = DateFormDay, message = Messages("tai.date.error.invalid"))))
          }
        } else {
          Left(errors)
        }
      }

      override def unbind(key: String, value: LocalDate): Map[String, String] = Map(
        DateFormDay -> value.getDayOfMonth.toString,
        DateFormMonth -> value.getMonthOfYear.toString,
        DateFormYear -> value.getYear.toString
      )
    }

    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]

    Form(localDateMapping)
  }

  val DateFormDay = "DateForm_day"
  val DateFormMonth = "DateForm_month"
  val DateFormYear = "DateForm_year"
}
