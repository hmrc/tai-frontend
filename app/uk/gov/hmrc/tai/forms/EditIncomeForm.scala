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

package uk.gov.hmrc.tai.forms

import uk.gov.hmrc.tai.forms.formValidator.TaiValidator
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.EmploymentAmount
import uk.gov.hmrc.tai.util.{DateHelper, FormHelper}
import uk.gov.hmrc.play.language.LanguageUtils.Dates


case class EditIncomeForm(name : String, description : String,
                            employmentId: Int = 0,
                            newAmount : Option[String] = None,
                            oldAmount : Int = 0,
                            worksNumber : Option[String] = None,
                            jobTitle : Option[String] = None,
                            startDate : Option[LocalDate] = None,
                            endDate : Option[LocalDate] = None,
                            isLive : Boolean=true,
                            isOccupationalPension : Boolean=false,
                            hasMultipleIncomes : Boolean=false,
                            payToDate : Option[String] = None) {

  def toEmploymentAmount() : EmploymentAmount = {
    new EmploymentAmount(
      name = name,
      description = description,
      employmentId = employmentId,
      newAmount = FormHelper.convertCurrencyToInt(newAmount),
      oldAmount = oldAmount,
      worksNumber = worksNumber,
      jobTitle = jobTitle,
      startDate = startDate,
      endDate = endDate,
      isLive = isLive,
      isOccupationalPension = isOccupationalPension
    )
  }
}

object EditIncomeForm {
  implicit val formats = Json.format[EditIncomeForm]

  def create(preFillData: EmploymentAmount, hasMultipleIncomes: Boolean=false, taxablePayYTD: BigDecimal=BigDecimal(0))(implicit messages: Messages) = {

    val newAmount = if (preFillData.oldAmount != preFillData.newAmount) {
      Some(preFillData.newAmount.toString)
    } else {
      None
    }
    val editIncomeForm = new EditIncomeForm(
      name = preFillData.name,
      description = preFillData.description,
      employmentId= preFillData.employmentId,
      newAmount = newAmount,
      oldAmount = preFillData.oldAmount,
      worksNumber = preFillData.worksNumber,
      jobTitle = preFillData.jobTitle,
      startDate = preFillData.startDate,
      endDate = preFillData.endDate,
      isLive= preFillData.isLive,
      isOccupationalPension = preFillData.isOccupationalPension,
      hasMultipleIncomes = hasMultipleIncomes
    )
    EditIncomeForm.createForm(preFillData.name, taxablePayYTD).fill(editIncomeForm)
  }

  def apply(employmentAmount: EmploymentAmount, newAmount: String,
                                                payToDate: Option[String]): EditIncomeForm = {
    EditIncomeForm(
      employmentAmount.name,
      employmentAmount.description,
      employmentAmount.employmentId,
      Some(newAmount),
      employmentAmount.oldAmount,
      employmentAmount.worksNumber,
      employmentAmount.jobTitle,
      employmentAmount.startDate,
      employmentAmount.endDate,
      employmentAmount.isLive,
      employmentAmount.isOccupationalPension,
      false,
      payToDate)
  }

  def bind(employerName: String, taxablePayYTD: BigDecimal = BigDecimal(0), payDate: Option[LocalDate] = None,
           errMessage: Option[String] = None)(implicit request: Request[_], messages: Messages) = createForm(employerName, taxablePayYTD, payDate, errMessage).bindFromRequest

  private def createForm (employerName: String, taxablePayYTD: BigDecimal, payDate: Option[LocalDate] = None, errMessage: Option[String] = None)(implicit messages: Messages) :Form[EditIncomeForm] = {

    val monthAndYearName = payDate.map (date => DateHelper.getMonthAndYear(Dates.formatDate(date))).getOrElse("")

    val errMsg = errMessage.getOrElse("error.tai.updateDataEmployment.enterLargerValue")
    Form[EditIncomeForm](
      mapping(
        "name" -> text,
        "description" -> text,
        "employmentId" -> number,
        "newAmount" -> TaiValidator.validateTaxAmounts(messages("error.tai.updateDataEmployment.blankValue"),
           messages("error.invalid.monetaryAmount.format.invalid"),
           messages("error.tai.updateDataEmployment.maxLength"),
           messages(errMsg, MoneyPounds(taxablePayYTD, 0, true).quantity, monthAndYearName, employerName),
          taxablePayYTD),
        "oldAmount" -> number,
        "worksNumber" -> optional(text),
        "jobTitle" -> optional(text),
        "startDate" -> optional(jodaLocalDate),
        "endDate" -> optional(jodaLocalDate),
        "isLive" -> boolean,
        "isOccupationalPension" -> boolean,
        "hasMultipleIncomes" -> boolean,
        "payToDate" -> optional(text)
      )(EditIncomeForm.apply)(EditIncomeForm.unapply)
    )
  }
}

