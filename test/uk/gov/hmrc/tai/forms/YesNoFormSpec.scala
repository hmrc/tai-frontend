/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import utils.BaseSpec

class YesNoFormSpec extends BaseSpec with FormValuesConstants {

  "YesNoFormSpec" must {
    "return no errors with valid 'yes' " in {
      val validYesChoice = Json.obj(YesNoChoice -> YesValue)
      val validatedForm = form.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(YesNoForm(Some(YesValue)))
    }

    "return no errors with valid 'no' choice" in {
      val validNoChoice = Json.obj(YesNoChoice -> NoValue)
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(YesNoForm(Some(NoValue)))
    }

    "return an error for an empty yes/no choice" in {
      val invalidChoice = Json.obj(YesNoChoice -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(errorMessage)
      invalidatedForm.value mustBe None
    }
  }

  lazy val errorMessage = "select yes or no"
  val form = YesNoForm.form(errorMessage)
}
