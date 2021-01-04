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

package uk.gov.hmrc.tai.forms.pensions

import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import utils.BaseSpec

class PensionProviderNameFormSpec extends BaseSpec {

  "PensionProviderNameForm" must {
    "return no errors with valid data" in {
      val validatedFormForValidName = form.bind(validName)

      validatedFormForValidName.errors mustBe empty
    }

    "pre populate the form correctly" in {
      val prePopForm = form.fill("The name of company")
      prePopForm.data must contain("pensionProviderName" -> "The name of company")
    }

    "return an error" when {
      "name is blank" in {
        val validatedFormNoDayError = form.bind(emptyName)

        validatedFormNoDayError.errors must contain(
          FormError("pensionProviderName", List(Messages("tai.pensionProviderName.error.blank"))))
      }
    }
  }

  private val form = PensionProviderNameForm.form

  private val validName = Json.obj("pensionProviderName" -> "the employer name")
  private val emptyName = Json.obj("pensionProviderName" -> "")
}
