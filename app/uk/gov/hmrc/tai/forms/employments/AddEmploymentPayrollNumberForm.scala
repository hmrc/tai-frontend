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

package uk.gov.hmrc.tai.forms.employments

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.constants.AddEmploymentPayrollNumberConstants._
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.voa.play.form.ConditionalMappings._

case class AddEmploymentPayrollNumberForm(payrollNumberChoice: Option[String], payrollNumberEntry: Option[String])

object AddEmploymentPayrollNumberForm {

  private def yesNoChoiceValidation(implicit messages: Messages) = Constraint[Option[String]]("") {
    case Some(txt) if txt == FormValuesConstants.YesValue || txt == FormValuesConstants.NoValue =>
      Valid
    case _                                                                                      =>
      Invalid(Messages("tai.addEmployment.employmentPayrollNumber.error.selectOption"))
  }

  def form(implicit messages: Messages): Form[AddEmploymentPayrollNumberForm] = Form[AddEmploymentPayrollNumberForm](
    mapping(
      PayrollNumberChoice -> optional(text).verifying(yesNoChoiceValidation),
      PayrollNumberEntry  -> mandatoryIfEqual(
        PayrollNumberChoice,
        FormValuesConstants.YesValue,
        text.verifying(Messages("tai.addEmployment.employmentPayrollNumber.error.blank"), _.nonEmpty)
      )
    )(AddEmploymentPayrollNumberForm.apply)(form => Some(Tuple2(form.payrollNumberChoice, form.payrollNumberEntry)))
  )

}
