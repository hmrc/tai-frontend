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

package uk.gov.hmrc.tai.forms.income.incomeCalculator

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tai.forms.formValidator.TaiValidator
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.TaxablePayPeriod

case class TaxablePayslipForm(taxablePay: Option[String] = None)

object TaxablePayslipForm {
  implicit val formats: OFormat[TaxablePayslipForm] = Json.format[TaxablePayslipForm]

  def createForm(
    netSalary: Option[String] = None,
    payPeriod: Option[String] = None,
    payPeriodInDays: Option[String] = None
  )(implicit messages: Messages): Form[TaxablePayslipForm] = {

    val validateErrorMessage = netSalary match {
      case Some(_) => TaxablePayPeriod.errorMessage(payPeriod, payPeriodInDays)
      case _       => messages("tai.taxablePayslip.error.form.incomes.radioButton.mandatory")
    }

    Form[TaxablePayslipForm](
      mapping(
        "taxablePay" -> TaiValidator.validateNewAmounts(
          validateErrorMessage,
          messages("tai.taxablePayslip.error.form.incomes.input.invalid"),
          messages("error.tai.updateDataEmployment.maxLength"),
          netSalary
        )
      )(TaxablePayslipForm.apply)(TaxablePayslipForm.unapply)
    )
  }
}
