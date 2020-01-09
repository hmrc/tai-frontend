/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json

class EmploymentNameFormSpec extends PlaySpec with OneAppPerSuite with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "EmploymentNameForm" must {
    "return no errors with valid data" in {
      val validatedFormForValidName = form.bind(validName)

      validatedFormForValidName.errors mustBe empty
    }

    "pre populate the form correctly" in {
      val prePopForm = form.fill("The name of the company")
      prePopForm.data must contain("employmentName" -> "The name of the company")
    }

    "return an error" when {
      "name is blank" in {
        val validatedFormNoDayError = form.bind(emptyName)

        validatedFormNoDayError.errors must contain(
          FormError("employmentName", List("Enter the name of your employer")))
      }
    }
  }

  private val form = EmploymentNameForm.form

  private val validName = Json.obj("employmentName" -> "the employer name")
  private val emptyName = Json.obj("employmentName" -> "")

}
