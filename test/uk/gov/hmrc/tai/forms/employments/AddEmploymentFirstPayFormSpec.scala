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

package uk.gov.hmrc.tai.forms.employments

import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.tai.util.constants.{AddEmploymentFirstPayChoiceConstants, FormValuesConstants}
import utils.BaseSpec

class AddEmploymentFirstPayFormSpec extends BaseSpec {

  "AddEmploymentFirstPayFormSpec" must {
    "return no errors with valid 'yes' choice" in {
      val validYesChoice = Json.obj(choice -> FormValuesConstants.YesValue)
      val validatedForm = form.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe Some(FormValuesConstants.YesValue)
    }

    "return no errors with valid 'no' choice and no payroll number" in {
      val validNoChoice = Json.obj(choice -> FormValuesConstants.NoValue)
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe Some(FormValuesConstants.NoValue)
    }

    "return an error for invalid choice" in {
      val invalidChoice = Json.obj(choice -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(Messages("tai.error.chooseOneOption"))
      invalidatedForm.value mustBe None
    }
  }

  val choice = AddEmploymentFirstPayChoiceConstants.FirstPayChoice

  private val form = AddEmploymentFirstPayForm.form

}
