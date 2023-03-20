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

package uk.gov.hmrc.tai.forms.benefits

import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm._

import java.time.LocalDate
import scala.util.Try

case class RemoveCompanyBenefitStopDateForm(benefitName: String, employerName: String) {
  def form(implicit messages: Messages): Form[LocalDate] = {
    implicit val dateFormatter: Formatter[LocalDate] = new Formatter[LocalDate] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

        val dayErrors: Boolean = data.getOrElse(BenefitFormDay, "").isEmpty

        val monthErrors: Boolean = data.getOrElse(BenefitFormMonth, "").isEmpty

        val yearErrors: Boolean = data.getOrElse(BenefitFormYear, "").isEmpty

        val errors =
          if (dayErrors || monthErrors || yearErrors) {
            Seq(
              FormError(key = BenefitFormDay, Messages("tai.benefits.ended.stopDate.error", benefitName, employerName)))
          } else {
            Nil
          }

        if (errors.isEmpty) {
          val inputDate: Option[LocalDate] = Try(
            for {
              day   <- data.get(BenefitFormDay).map(Integer.parseInt)
              month <- data.get(BenefitFormMonth).map(Integer.parseInt)
              year  <- data.get(BenefitFormYear).map(Integer.parseInt)
            } yield LocalDate.of(year, month, day)
          ).getOrElse(None)

          inputDate match {
            case Some(date) if date.isAfter(LocalDate.now()) =>
              Left(Seq(FormError(key = BenefitFormDay, message = Messages("tai.date.error.future"))))
            case Some(d) => Right(d)
            case _       => Left(Seq(FormError(key = BenefitFormDay, message = Messages("tai.date.error.invalid"))))
          }
        } else { Left(errors) }
      }

      override def unbind(key: String, value: LocalDate): Map[String, String] = Map(
        BenefitFormDay   -> value.getDayOfMonth.toString,
        BenefitFormMonth -> value.getMonthValue.toString,
        BenefitFormYear  -> value.getYear.toString
      )
    }
    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]
    Form(localDateMapping)
  }

}

object RemoveCompanyBenefitStopDateForm {
  val BenefitFormHint = "benefitEndDateForm-hint"
  val BenefitFormDay = "benefitEndDateForm-day"
  val BenefitFormMonth = "benefitEndDateForm-month"
  val BenefitFormYear = "benefitEndDateForm-year"
}
