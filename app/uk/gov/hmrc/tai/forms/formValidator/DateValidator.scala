/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.tai.forms.formValidator

import play.api.data.Forms._
import play.api.data.Mapping
import uk.gov.hmrc.tai.forms.formValidator.DateValidator._
import uk.gov.hmrc.tai.model.TaxYear

import java.time.LocalDate
import scala.util.Try

trait DateValidator {

  // scalastyle:off cyclomatic.complexity
  def dateTuple: Mapping[Option[LocalDate]] =
    tuple(
      year  -> optional(text),
      month -> optional(text),
      day   -> optional(text)
    ).verifying(
      "error.invalid.date.format",
      data =>
        (data._1, data._2, data._3) match {
          case (Some(y), Some(m), Some(d)) =>
            Try(LocalDate.of(y.trim.toInt, m.trim.toInt, d.trim.toInt)).isSuccess
          case (None, None, None)          => true
          case _                           => false
        }
    ).verifying(
      "error.invalid.date.future",
      data =>
        (data._1, data._2, data._3) match {
          case (Some(y), Some(m), Some(d)) =>
            val now = LocalDate.now()
            Try(!now.isBefore(LocalDate.of(y.trim.toInt, m.trim.toInt, d.trim.toInt))).getOrElse(true)
          case _                           => true
        }
    ).verifying(
      "error.invalid.date.past",
      data =>
        (data._1, data._2, data._3) match {
          case (Some(y), Some(m), Some(d)) =>
            Try(!TaxYear().start.isAfter(LocalDate.of(y.trim.toInt, m.trim.toInt, d.trim.toInt))).getOrElse(true)
          case _                           => true
        }
    ).transform(
      {
        case (Some(y), Some(m), Some(d)) => Try(LocalDate.of(y.trim.toInt, m.toInt, d.toInt)).toOption
        case _                           => None
      },
      (date: Option[LocalDate]) =>
        date match {
          case Some(d) => (Some(d.getYear.toString), Some(d.getMonth.toString), Some(d.getDayOfMonth.toString))
          case _       => (None, None, None)
        }
    )
}

object DateValidator {
  val day   = "day"
  val month = "month"
  val year  = "year"
}
