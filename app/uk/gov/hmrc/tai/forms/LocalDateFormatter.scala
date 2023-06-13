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

import cats.implicits._
import play.api.data.FormError
import play.api.data.format.Formatter

import java.time.LocalDate

case class LocalDateFormatter(
  formDay: String,
  formMonth: String,
  formYear: String,
  errorMsgs: LocalDateFormatter.ErrorMessages
) extends Formatter[LocalDate] {
  import errorMsgs._

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
    val emptyDay: Boolean = data.getOrElse(formDay, "").isEmpty
    val emptyMonth: Boolean = data.getOrElse(formMonth, "").isEmpty
    val emptyYear: Boolean = data.getOrElse(formYear, "").isEmpty

    val errors = errorIfEmpty(emptyDay, emptyMonth, emptyYear)

    if (errors.isEmpty) {
      validateDate(data.get(formDay), data.get(formMonth), data.get(formYear))
    } else {
      Left(errors)
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      formDay   -> value.getDayOfMonth.toString,
      formMonth -> value.getMonthValue.toString,
      formYear  -> value.getYear.toString
    )

  private def extractYear(maybeYear: Option[String]) =
    Either
      .catchNonFatal(maybeYear.map { str =>
        val year = Integer.parseInt(str.filterNot(_.isWhitespace))
        if (str.filterNot(_.isWhitespace).length != 4) throw new NumberFormatException("Must be 4 digits long")
        year
      }.get)
      .leftMap(_ => List(FormError(key = formYear, message = mustBeValidYear)))

  private def extractMonth(maybeMonth: Option[String]) =
    Either
      .catchNonFatal(maybeMonth.map { str =>
        val month = Integer.parseInt(str.filterNot(_.isWhitespace))
        if (month < 1 || month > 12) throw new NumberFormatException("1 <= Month <= 12")
        month
      }.get)
      .leftMap(_ => List(FormError(key = formMonth, message = mustBeValidMonth)))

  private def extractDay(maybeDay: Option[String]) =
    Either
      .catchNonFatal(maybeDay.map { str =>
        val day = Integer.parseInt(str.filterNot(_.isWhitespace))
        if (day < 1 || day > 31) throw new NumberFormatException("1 <= day <= 31")
        day
      }.get)
      .leftMap(_ => List(FormError(key = formDay, message = mustBeValidDay)))

  private def validateDate(maybeDay: Option[String], maybeMonth: Option[String], maybeYear: Option[String]) = {
    val dayOrError = extractDay(maybeDay)
    val monthOrError = extractMonth(maybeMonth)
    val yearOrError = extractYear(maybeYear)

    val inputDate = (dayOrError, monthOrError, yearOrError)
      .parMapN { case (day, month, year) =>
        if (year < 1900) {
          Left(List(FormError(key = formYear, message = mustBeAfter1900)))
        } else {
          Either
            .catchNonFatal(LocalDate.of(year, month, day))
            .leftMap(_ => List(FormError(key = formDay, message = mustBeReal)))
        }
      }
      .flatten
      .leftMap {
        case errs if errs.size > 1 =>
          List(FormError(key = formDay, message = mustBeReal))
        case errs =>
          errs
      }

    inputDate.flatMap {
      case date if date.isAfter(LocalDate.now()) =>
        Left(Seq(FormError(key = formDay, message = mustBeFuture)))
      case x =>
        Right(x)
    }
  }

  def errorIfEmpty(emptyDay: Boolean, emptyMonth: Boolean, emptyYear: Boolean): Seq[FormError] = {
    (emptyDay, emptyMonth, emptyYear) match {
      case (true, true, true) =>
        FormError(key = formDay, enterDate).some
      case (true, false, false) =>
        FormError(key = formDay, enterDay).some
      case (false, true, false) =>
        FormError(key = formMonth, enterMonth).some
      case (false, false, true) =>
        FormError(key = formYear, enterYear).some
      case (true, true, false) =>
        FormError(key = formDay, enterDayAndMonth).some
      case (true, false, true) =>
        FormError(key = formDay, enterDayAndYear).some
      case (false, true, true) =>
        FormError(key = formMonth, enterMonthAndYear).some
      case (false, false, false) =>
        none
    }
  }.toSeq
}

object LocalDateFormatter {
  case class ErrorMessages(
    enterDate: String,
    enterDay: String,
    enterMonth: String,
    enterYear: String,
    enterDayAndMonth: String,
    enterDayAndYear: String,
    enterMonthAndYear: String,
    mustBeReal: String,
    mustBeValidDay: String,
    mustBeValidMonth: String,
    mustBeValidYear: String,
    mustBeFuture: String,
    mustBeAfter1900: String
  )
}
