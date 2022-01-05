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

package uk.gov.hmrc.tai.forms

import play.api.data.validation.{Constraint, Invalid}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import utils.BaseSpec

class YesNoTextEntryFormSpec extends BaseSpec with FormValuesConstants {

  "YesNoTextEntryFormSpec" must {
    "return no errors with valid 'yes' choice and text field content" in {
      val validYesChoice = Json.obj(YesNoChoice -> YesValue, YesNoTextEntry -> "123456")
      val validatedForm = form.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(YesNoTextEntryForm(Some(YesValue), Some("123456")))
    }

    "return no errors with valid 'no' choice and no text field content" in {
      val validNoChoice = Json.obj(YesNoChoice -> NoValue, YesNoTextEntry -> "")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(YesNoTextEntryForm(Some(NoValue), None))
    }

    "return no errors with valid 'no' choice and text field content as space" in {
      val validNoChoice = Json.obj(YesNoChoice -> NoValue, YesNoTextEntry -> " ")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(YesNoTextEntryForm(Some(NoValue), None))
    }

    "return no errors with valid 'no' choice and text field content" in {
      val validNoChoice = Json.obj(YesNoChoice -> NoValue, YesNoTextEntry -> "123456")
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(YesNoTextEntryForm(Some(NoValue), None))
    }

    "return an error for an empty yes/no choice" in {
      val invalidChoice = Json.obj(YesNoChoice -> "", YesNoTextEntry -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List("select yes or no")
      invalidatedForm.value mustBe None
    }

    "return an error for an empty yes/no choice and missing value" in {
      val invalidChoice = Json.obj(YesNoChoice -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List("select yes or no")
      invalidatedForm.value mustBe None
    }

    "return errors with valid 'yes' choice and no text field content" in {
      val invalidYesChoice = Json.obj(YesNoChoice -> Some(YesValue), YesNoTextEntry -> "")
      val invalidatedForm = form.bind(invalidYesChoice)

      invalidatedForm.errors.head.messages mustBe List("enter some text")
      invalidatedForm.value mustBe None
    }

    "return errors with text field content that does not meet requirements of an additional constraint" in {
      val extraConstraint = Constraint[String]((textContent: String) => Invalid("bang"))
      val formWithExtraConstraint =
        YesNoTextEntryForm.form("select yes or no", "enter some text", Some(extraConstraint))

      val validChoiceUntilExtraConstraint = Json.obj(YesNoChoice -> Some(YesValue), YesNoTextEntry -> "123")
      val invalidatedForm = formWithExtraConstraint.bind(validChoiceUntilExtraConstraint)

      invalidatedForm.errors.head.messages mustBe List("bang")
      invalidatedForm.value mustBe None
    }
  }

  private val form = YesNoTextEntryForm.form("select yes or no", "enter some text")
}
