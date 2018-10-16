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

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.tai.forms.formValidator.TaiValidator

case class EditIncomeIrregularHoursForm(income: Option[String])

object EditIncomeIrregularHoursForm {
  implicit val formats = Json.format[EditIncomeIrregularHoursForm]

  def createForm(taxablePayYTD: Option[BigDecimal] = None)(implicit messages: Messages): Form[EditIncomeIrregularHoursForm] = {

    Form[EditIncomeIrregularHoursForm](

        mapping("income" -> TaiValidator.validateTaxAmounts(messages("error.tai.updateDataEmployment.blankValue"),
                                                            messages("tai.payslip.error.form.notAnAmount"),
                                                            messages("error.tai.updateDataEmployment.maxLength"),
                                                            taxablePayYTD.fold("")(_ =>"amount less than already paid error"),
                                                            taxablePayYTD.getOrElse(BigDecimal(0))
        )

        )(EditIncomeIrregularHoursForm.apply)(EditIncomeIrregularHoursForm.unapply)
    )
  }
}
