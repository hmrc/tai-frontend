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

package uk.gov.hmrc.tai.forms.pensions

import play.api.data.Forms.of
import play.api.data.{FieldMapping, Form}
import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.LocalDateFormatter
import uk.gov.hmrc.tai.forms.pensions.PensionAddDateForm._

import java.time.LocalDate

case class PensionAddDateForm(employerName: String) {

  def form(implicit messages: Messages): Form[LocalDate] = {
    implicit val dateFormatter = new LocalDateFormatter(
      formDay = PensionFormDay,
      formMonth = PensionFormMonth,
      formYear = PensionFormYear,
      errorMsgs = errorMsgs(employerName)
    )

    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]

    Form(localDateMapping)
  }

}

object PensionAddDateForm {

  val PensionFormDay = "tellUsStartDateForm-day"
  val PensionFormMonth = "tellUsStartDateForm-month"
  val PensionFormYear = "tellUsStartDateForm-year"

  def errorMsgs(employerName: String)(implicit messages: Messages) = LocalDateFormatter.ErrorMessages(
    enterDate = Messages("tai.addPensionProvider.date.enterDate", employerName),
    enterDay = Messages("tai.addPensionProvider.date.enterDay"),
    enterMonth = Messages("tai.addPensionProvider.date.enterMonth"),
    enterYear = Messages("tai.addPensionProvider.date.enterYear"),
    enterDayAndMonth = Messages("tai.addPensionProvider.date.enterDayAndMonth"),
    enterDayAndYear = Messages("tai.addPensionProvider.date.enterDayAndYear"),
    enterMonthAndYear = Messages("tai.addPensionProvider.date.enterMonthAndYear"),
    mustBeValidDay = Messages("tai.addPensionProvider.date.mustBeValidDay"),
    mustBeValidMonth = Messages("tai.addPensionProvider.date.mustBeValidMonth"),
    mustBeValidYear = Messages("tai.addPensionProvider.date.mustBeValidYear"),
    mustBeReal = Messages("tai.addPensionProvider.date.mustBeReal"),
    mustBeFuture = Messages("tai.addPensionProvider.date.mustBeFuture"),
    mustBeAfter1900 = Messages("tai.addPensionProvider.date.mustBeAfter1900")
  )
}
