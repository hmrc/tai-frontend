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

import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.tai.forms.formValidator.TaiValidator
import uk.gov.hmrc.tai.util.constants.TaiConstants.MONTH_AND_YEAR

case class AmountComparatorForm(income: Option[String])

object AmountComparatorForm {
  implicit val formats = Json.format[AmountComparatorForm]

  def createForm(latestPayDate: Option[String] = None,
                 taxablePayYTD: Option[BigDecimal] = None)
                (implicit messages: Messages): Form[AmountComparatorForm] = {

    val fallbackDate = LocalDate.now().toString(MONTH_AND_YEAR)

    val theDate = latestPayDate.fold(fallbackDate)(identity)

    Form[AmountComparatorForm](
      mapping("income" -> TaiValidator.validateTaxAmounts(
        messages("tai.irregular.error.blankValue"),
        messages("tai.irregular.instruction.wholePounds"),
        messages("error.tai.updateDataEmployment.maxLength"),
        taxablePayYTD.fold("")(messages("tai.irregular.error.error.incorrectTaxableIncome", _, theDate)),
        taxablePayYTD.getOrElse(0)
      ))(AmountComparatorForm.apply)(AmountComparatorForm.unapply)
    )
  }
}
