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

package uk.gov.hmrc.tai.forms.income.incomeCalculator

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tai.forms.formValidator.TaiValidator

case class PayslipForm(totalSalary: Option[String] = None)

object PayslipForm {
  implicit val formats: OFormat[PayslipForm] = Json.format[PayslipForm]

  def createForm(errorText: String)(implicit messages: Messages): Form[PayslipForm] =
    Form[PayslipForm](
      mapping(
        "totalSalary" -> TaiValidator.validateNewAmounts(
          messages(errorText),
          messages("tai.payslip.error.form.totalPay.input.invalid"),
          messages("error.tai.updateDataEmployment.maxLength")))(PayslipForm.apply)(PayslipForm.unapply)
    )
}
