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

package uk.gov.hmrc.tai.forms.formValidator

import org.scalatestplus.play.PlaySpec
import play.api.data.FormError

class FormValidatorSpec extends PlaySpec {

  "FormValidator" should {

    "return Nil" when {

      "a Nil Seq is supplied" in {

        val result = FormValidator.validate[String]("", Nil, formErrorKey)

        result mustBe Nil
      }

      "a supplied validator returns true" in {

        val validator = ((x: String) => { true }, "invalid")

        val result = FormValidator.validate[String]("", Seq(validator), formErrorKey)

        result mustBe Nil
      }
    }

    "return a FormError containing the required message" when {

      "one validator returns true and another validator returns false" in {

        val validator1 = ((x: String) => { true }, "invalid1")
        val validator2 = ((x: String) => { false }, "invalid2")

        val result = FormValidator.validate[String]("", Seq(validator1, validator2), formErrorKey)

        result mustBe Seq(FormError(formErrorKey, "invalid2"))
      }
    }
  }

  private val formErrorKey: String = "testFormErrorKey"
}