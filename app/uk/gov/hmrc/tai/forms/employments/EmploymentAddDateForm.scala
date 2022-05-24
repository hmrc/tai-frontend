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

package uk.gov.hmrc.tai.forms.employments

import java.time.LocalDate
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.Messages

import scala.util.Try

case class EmploymentAddDateForm(employerName: String) {

  def form(implicit messages: Messages) = {

    implicit val dateFormatter = new Formatter[LocalDate] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

        val dayErrors: Boolean = data.getOrElse(EmploymentFormDay, "").isEmpty

        val monthErrors: Boolean = data.getOrElse(EmploymentFormMonth, "").isEmpty

        val yearErrors: Boolean = data.getOrElse(EmploymentFormYear, "").isEmpty

        val errors = if (dayErrors || monthErrors || yearErrors) {
          Seq(FormError(key = EmploymentFormDay, message = Messages("tai.add.date.error.blank", employerName)))
        } else {
          Nil
        }

        if (errors.isEmpty) {
          val inputDate: Option[LocalDate] = Try(
            for {
              day   <- data.get(EmploymentFormDay).map(Integer.parseInt)
              month <- data.get(EmploymentFormMonth).map(Integer.parseInt)
              year  <- data.get(EmploymentFormYear).map(Integer.parseInt)
            } yield LocalDate.of(year, month, day)
          ).getOrElse(None)

          inputDate match {
            case Some(date) if date.isAfter(LocalDate.now()) =>
              Left(Seq(FormError(key = EmploymentFormDay, message = Messages("tai.date.error.future"))))
            case Some(d) => Right(d)
            case _       => Left(Seq(FormError(key = EmploymentFormDay, message = Messages("tai.date.error.invalid"))))
          }
        } else {
          Left(errors)
        }
      }

      override def unbind(key: String, value: LocalDate): Map[String, String] = Map(
        EmploymentFormDay   -> value.getDayOfMonth.toString,
        EmploymentFormMonth -> value.getMonthValue.toString,
        EmploymentFormYear  -> value.getYear.toString
      )
    }

    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]

    Form(localDateMapping)
  }

  val EmploymentFormDay = "tellUsStartDateForm-day"
  val EmploymentFormMonth = "tellUsStartDateForm-month"
  val EmploymentFormYear = "tellUsStartDateForm-year"
}
