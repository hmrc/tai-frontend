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
import play.api.data.{FieldMapping, Form}
import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.LocalDateFormatter
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm._

import java.time.LocalDate

case class RemoveCompanyBenefitStopDateForm(benefitName: String, employerName: String) {
  def form(implicit messages: Messages): Form[LocalDate] = {
    implicit val dateFormatter: Formatter[LocalDate] = new LocalDateFormatter(
      formDay = BenefitFormDay,
      formMonth = BenefitFormMonth,
      formYear = BenefitFormYear,
      errorMsgs = errorMsgs
    )

    val localDateMapping: FieldMapping[LocalDate] = of[LocalDate]
    Form(localDateMapping)
  }
}

object RemoveCompanyBenefitStopDateForm {
  val BenefitFormHint = "benefitEndDateForm-hint"
  val BenefitFormDay = "benefitEndDateForm-day"
  val BenefitFormMonth = "benefitEndDateForm-month"
  val BenefitFormYear = "benefitEndDateForm-year"

  def errorMsgs(implicit messages: Messages) = LocalDateFormatter.ErrorMessages(
    enterDate = Messages("tai.benefits.ended.stopDate.error.enterDate"),
    enterDay = Messages("tai.benefits.ended.stopDate.error.enterDay"),
    enterMonth = Messages("tai.benefits.ended.stopDate.error.enterMonth"),
    enterYear = Messages("tai.benefits.ended.stopDate.error.enterYear"),
    enterDayAndMonth = Messages("tai.benefits.ended.stopDate.error.enterDayAndMonth"),
    enterDayAndYear = Messages("tai.benefits.ended.stopDate.error.enterDayAndYear"),
    enterMonthAndYear = Messages("tai.benefits.ended.stopDate.error.enterMonthAndYear"),
    mustBeValidDay = Messages("tai.benefits.ended.stopDate.error.mustBeValidDay"),
    mustBeValidMonth = Messages("tai.benefits.ended.stopDate.error.mustBeValidMonth"),
    mustBeValidYear = Messages("tai.benefits.ended.stopDate.error.mustBeValidYear"),
    mustBeReal = Messages("tai.benefits.ended.stopDate.error.mustBeReal"),
    mustBeFuture = Messages("tai.benefits.ended.stopDate.error.mustBeFuture"),
    mustBeAfter1900 = Messages("tai.benefits.ended.stopDate.error.mustBeAfter1900")
  )
}
