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

package uk.gov.hmrc.tai.forms.pensions

import org.joda.time.LocalDate
import play.api.Play.current
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

import scala.util.Try


case class PensionAddDateForm(employerName: String) {

  implicit val dateFormatter = new Formatter[LocalDate] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

      val dayErrors: Boolean = data.getOrElse(PensionFormDay, "").isEmpty

      val monthErrors: Boolean = data.getOrElse(PensionFormMonth, "").isEmpty

      val yearErrors: Boolean = data.getOrElse(PensionFormYear, "").isEmpty

      val errors = if (dayErrors || monthErrors || yearErrors) {
        Seq(FormError(key = PensionFormDay, message = Messages("tai.addPensionProvider.date.error.blank", employerName)))
      } else {
        Nil
      }


      if (errors.isEmpty) {
        val inputDate: Option[LocalDate] = Try(
          for {
            day <- data.get(PensionFormDay).map(Integer.parseInt)
            month <- data.get(PensionFormMonth).map(Integer.parseInt)
            year <- data.get(PensionFormYear).map(Integer.parseInt)
          } yield new LocalDate(year, month, day)
        ).getOrElse(None)

        inputDate match {
          case Some(date) if date.isAfter(LocalDate.now()) => Left(Seq(FormError(key = PensionFormDay, message = Messages("tai.date.error.future"))))
          case Some(d) => Right(d)
          case _ => Left(Seq(FormError(key = PensionFormDay, message = Messages("tai.date.error.invalid"))))
        }
      } else {
        Left(errors)
      }
    }

    override def unbind(key: String, value: LocalDate): Map[String, String] = Map(
      PensionFormDay -> value.getDayOfMonth.toString,
      PensionFormMonth -> value.getMonthOfYear.toString,
      PensionFormYear -> value.getYear.toString
    )
  }

  val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]

  val form = Form(localDateMapping)

  val PensionFormDay = "tellUsStartDateForm_day"
  val PensionFormMonth = "tellUsStartDateForm_month"
  val PensionFormYear = "tellUsStartDateForm_year"
}
