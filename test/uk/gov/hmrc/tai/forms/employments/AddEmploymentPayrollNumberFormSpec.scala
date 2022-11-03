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

package uk.gov.hmrc.tai.forms.employments

import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.tai.util.constants.{AddEmploymentPayrollNumberConstants, FormValuesConstants}
import utils.BaseSpec

class AddEmploymentPayrollNumberFormSpec extends BaseSpec {

  "AddEmploymentPayrollNumberFormSpec" must {
    "return no errors with valid 'yes' choice and payroll number" in {
      val validYesChoice = Json.obj(choice -> FormValuesConstants.YesValue, payroll -> "123456")
      val validatedForm = form.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(
        AddEmploymentPayrollNumberForm(Some(FormValuesConstants.YesValue), Some("123456")))
    }

    "return no errors with valid 'no' choice and no payroll number" in {
      val validNoChoice = Json.obj(choice -> FormValuesConstants.NoValue, payroll -> "")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AddEmploymentPayrollNumberForm(Some(FormValuesConstants.NoValue), None))
    }

    "return no errors with valid 'no' choice and payroll number as space" in {
      val validNoChoice = Json.obj(choice -> FormValuesConstants.NoValue, payroll -> " ")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AddEmploymentPayrollNumberForm(Some(FormValuesConstants.NoValue), None))
    }

    "return no errors with valid 'no' choice and payroll number" in {
      val validNoChoice = Json.obj(choice -> FormValuesConstants.NoValue, payroll -> "123456")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AddEmploymentPayrollNumberForm(Some(FormValuesConstants.NoValue), None))
    }

    "return an error for invalid choice" in {
      val invalidChoice = Json.obj(choice -> "", payroll -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(
        Messages("tai.addEmployment.employmentPayrollNumber.error.selectOption"))
      invalidatedForm.value mustBe None
    }

    "return an error for invalid choice with empty values" in {
      val invalidChoice = Json.obj(payroll -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(
        Messages("tai.addEmployment.employmentPayrollNumber.error.selectOption"))
      invalidatedForm.value mustBe None
    }

    "return errors with valid 'yes' choice and no payroll number" in {
      val invalidYesChoice = Json.obj(choice -> Some(FormValuesConstants.YesValue), payroll -> "")
      val invalidatedForm = form.bind(invalidYesChoice)

      invalidatedForm.errors.head.messages mustBe List(
        Messages("tai.addEmployment.employmentPayrollNumber.error.blank"))
      invalidatedForm.value mustBe None
    }

  }

  val choice = AddEmploymentPayrollNumberConstants.PayrollNumberChoice
  val payroll = AddEmploymentPayrollNumberConstants.PayrollNumberEntry

  private val form = AddEmploymentPayrollNumberForm.form

}
