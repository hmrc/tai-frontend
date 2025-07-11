/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.data.FormBinding.Implicits.formBinding
import play.api.data.Forms._
import play.api.data.format.Formats.localDateFormat
import play.api.data.{Form, Forms}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Request
import uk.gov.hmrc.tai.forms.formValidator.TaiValidator
import uk.gov.hmrc.tai.model.EmploymentAmount
import uk.gov.hmrc.tai.util.{DateHelper, FormHelper, MoneyPounds, TaxYearRangeUtil => Dates}

import java.time.LocalDate

case class EditIncomeForm(
  name: String,
  description: String,
  employmentId: Int = 0,
  newAmount: Option[String] = None,
  oldAmount: Option[Int] = Some(0),
  worksNumber: Option[String] = None,
  jobTitle: Option[String] = None,
  startDate: Option[LocalDate] = None,
  endDate: Option[LocalDate] = None,
  isLive: Boolean = true,
  isOccupationalPension: Boolean = false,
  hasMultipleIncomes: Boolean = false,
  payToDate: Option[String] = None
) {}

object EditIncomeForm {
  implicit val formats: OFormat[EditIncomeForm] = Json.format[EditIncomeForm]

  def create(
    preFillData: EmploymentAmount,
    newAmount: Option[String],
    hasMultipleIncomes: Boolean = false,
    taxablePayYTD: BigDecimal = BigDecimal(0)
  )(implicit messages: Messages): Form[EditIncomeForm] = {

    val editIncomeForm = new EditIncomeForm(
      name = preFillData.name,
      description = preFillData.description,
      employmentId = preFillData.employmentId,
      newAmount = newAmount,
      oldAmount = preFillData.oldAmount,
      worksNumber = preFillData.worksNumber,
      jobTitle = preFillData.jobTitle,
      startDate = preFillData.startDate,
      endDate = preFillData.endDate,
      isLive = preFillData.isLive,
      isOccupationalPension = preFillData.isOccupationalPension,
      hasMultipleIncomes = hasMultipleIncomes
    )
    EditIncomeForm.createForm(preFillData.name, taxablePayYTD).fill(editIncomeForm)
  }

  def apply(employmentAmount: EmploymentAmount, newAmount: String, payToDate: Option[String]): EditIncomeForm =
    EditIncomeForm(
      employmentAmount.name,
      employmentAmount.description,
      employmentAmount.employmentId,
      FormHelper.stripNumber(Some(newAmount)),
      employmentAmount.oldAmount,
      employmentAmount.worksNumber,
      employmentAmount.jobTitle,
      employmentAmount.startDate,
      employmentAmount.endDate,
      employmentAmount.isLive,
      employmentAmount.isOccupationalPension,
      hasMultipleIncomes = false,
      payToDate
    )

  def bind(
    employerName: String,
    taxablePayYTD: BigDecimal = BigDecimal(0),
    payDate: Option[LocalDate] = None,
    errMessage: Option[String] = None
  )(implicit request: Request[_], messages: Messages): Form[EditIncomeForm] =
    createForm(employerName, taxablePayYTD, payDate, errMessage).bindFromRequest()

  private def createForm(
    employerName: String,
    taxablePayYTD: BigDecimal,
    payDate: Option[LocalDate] = None,
    errMessage: Option[String] = None
  )(implicit messages: Messages): Form[EditIncomeForm] = {

    val monthAndYearName = payDate.map(date => DateHelper.getMonthAndYear(Dates.formatDate(date))).getOrElse("")

    val errMsg = errMessage.getOrElse("error.tai.updateDataEmployment.enterLargerValue")
    Form[EditIncomeForm](
      mapping(
        "name"                  -> text,
        "description"           -> text,
        "employmentId"          -> number,
        "newAmount"             -> TaiValidator.validateTaxAmounts(
          messages("error.tai.updateDataEmployment.blankValue"),
          messages("error.tai.update.estimatedTaxableIncome.input.invalid"),
          messages("error.tai.updateDataEmployment.maxLength"),
          messages(errMsg, MoneyPounds(taxablePayYTD, 0, roundUp = true).quantity, monthAndYearName, employerName),
          taxablePayYTD
        ),
        "oldAmount"             -> optional(number),
        "worksNumber"           -> optional(text),
        "jobTitle"              -> optional(text),
        "startDate"             -> optional(Forms.of[java.time.LocalDate]),
        "endDate"               -> optional(Forms.of[java.time.LocalDate]),
        "isLive"                -> boolean,
        "isOccupationalPension" -> boolean,
        "hasMultipleIncomes"    -> boolean,
        "payToDate"             -> optional(text)
      )(customApply)(customUnapply)
    )
  }

  val customApply: (
    String,
    String,
    Int,
    Option[String],
    Option[Int],
    Option[String],
    Option[String],
    Option[LocalDate],
    Option[LocalDate],
    Boolean,
    Boolean,
    Boolean,
    Option[String]
  ) => EditIncomeForm = (
    name: String,
    description: String,
    employmentId: Int,
    newAmount: Option[String],
    oldAmount: Option[Int],
    worksNumber: Option[String],
    jobTitle: Option[String],
    startDate: Option[LocalDate],
    endDate: Option[LocalDate],
    isLive: Boolean,
    isOccupationalPension: Boolean,
    hasMultipleIncomes: Boolean,
    payToDate: Option[String]
  ) =>
    EditIncomeForm(
      name,
      description,
      employmentId,
      FormHelper.stripNumber(newAmount),
      oldAmount,
      worksNumber,
      jobTitle,
      startDate,
      endDate,
      isLive,
      isOccupationalPension,
      hasMultipleIncomes,
      payToDate
    )

  val customUnapply: EditIncomeForm => Option[
    (
      String,
      String,
      Int,
      Option[String],
      Option[Int],
      Option[String],
      Option[String],
      Option[LocalDate],
      Option[LocalDate],
      Boolean,
      Boolean,
      Boolean,
      Option[String]
    )
  ] = form =>
    Some(
      (
        form.name,
        form.description,
        form.employmentId,
        form.newAmount,
        form.oldAmount,
        form.worksNumber,
        form.jobTitle,
        form.startDate,
        form.endDate,
        form.isLive,
        form.isOccupationalPension,
        form.hasMultipleIncomes,
        form.payToDate
      )
    )
}
