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
import uk.gov.hmrc.tai.util.TaxYearRangeUtil

case class BonusOvertimeAmountForm(amount: Option[String] = None)

object BonusOvertimeAmountForm {
  implicit val formats: OFormat[BonusOvertimeAmountForm] = Json.format[BonusOvertimeAmountForm]

  def createForm(nonEmptyMessage: Option[String] = None, notAnAmountMessage: Option[String] = None)(
    implicit messages: Messages): Form[BonusOvertimeAmountForm] =
    Form[BonusOvertimeAmountForm](
      mapping(
        "amount" -> TaiValidator.validateNewAmounts(
          messages(
            "tai.bonusPaymentsAmount.error.form.mandatory",
            TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited),
          messages("tai.bonusPaymentsAmount.error.form.input.invalid"),
          messages("error.tai.updateDataEmployment.maxLength")
        ))(BonusOvertimeAmountForm.apply)(BonusOvertimeAmountForm.unapply)
    )

}
