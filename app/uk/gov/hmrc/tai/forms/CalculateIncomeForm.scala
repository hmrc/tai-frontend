/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.mvc.Request
import uk.gov.hmrc.tai.model.EmploymentAmount

case class CalculateIncomeForm(
  name: String,
  employmentId: Int,
  ytdAmount: Option[String] = None,
  paymentDate: Option[LocalDate] = None)

object CalculateIncomeForm {
  implicit val formats = Json.format[CalculateIncomeForm]

  def create(preFillData: EmploymentAmount) = {

    val calculateIncomeForm = new CalculateIncomeForm(name = preFillData.name, employmentId = preFillData.employmentId)
    CalculateIncomeForm.createForm.fill(calculateIncomeForm)
  }

  def bind(implicit request: Request[_]) = createForm.bindFromRequest

  def createForm(): Form[CalculateIncomeForm] =
    Form[CalculateIncomeForm](
      mapping(
        "name"         -> text,
        "employmentId" -> number,
        "ytdAmount"    -> optional(text),
        "paymentDate"  -> TaiValidator.validateOptionalDate
      )(CalculateIncomeForm.apply)(CalculateIncomeForm.unapply)
    )
}
