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

package uk.gov.hmrc.tai.forms.employments

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import uk.gov.hmrc.tai.util.constants.FormValuesConstants

class AddEmploymentPayrollNumberFormSpec extends PlaySpec with OneAppPerSuite with I18nSupport with FormValuesConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "AddEmploymentPayrollNumberFormSpec" must {
    "return no errors with valid 'yes' choice and payroll number" in {
      val validYesChoice = Json.obj(choice -> YesValue, payroll -> "123456")
      val validatedForm = form.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AddEmploymentPayrollNumberForm(Some(YesValue), Some("123456")))
    }

    "return no errors with valid 'no' choice and no payroll number" in {
      val validNoChoice = Json.obj(choice -> NoValue, payroll -> "")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AddEmploymentPayrollNumberForm(Some(NoValue), None))
    }

    "return no errors with valid 'no' choice and payroll number as space" in {
      val validNoChoice = Json.obj(choice -> NoValue, payroll -> " ")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AddEmploymentPayrollNumberForm(Some(NoValue), None))
    }

    "return no errors with valid 'no' choice and payroll number" in {
      val validNoChoice = Json.obj(choice -> NoValue, payroll -> "123456")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AddEmploymentPayrollNumberForm(Some(NoValue), None))
    }

    "return an error for invalid choice" in {
      val invalidChoice = Json.obj(choice -> "", payroll -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(Messages("tai.addEmployment.employmentPayrollNumber.error.selectOption"))
      invalidatedForm.value mustBe None
    }

    "return an error for invalid choice with empty values" in {
      val invalidChoice = Json.obj(payroll -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(Messages("tai.addEmployment.employmentPayrollNumber.error.selectOption"))
      invalidatedForm.value mustBe None
    }

    "return errors with valid 'yes' choice and no payroll number" in {
      val invalidYesChoice = Json.obj(choice -> Some(YesValue), payroll -> "")
      val invalidatedForm = form.bind(invalidYesChoice)

      invalidatedForm.errors.head.messages mustBe List(Messages("tai.addEmployment.employmentPayrollNumber.error.blank"))
      invalidatedForm.value mustBe None
    }

  }

  val choice = AddEmploymentPayrollNumberForm.PayrollNumberChoice
  val payroll = AddEmploymentPayrollNumberForm.PayrollNumberEntry

  private val form = AddEmploymentPayrollNumberForm.form

}
