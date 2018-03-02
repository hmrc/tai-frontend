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

package uk.gov.hmrc.tai.forms

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import uk.gov.hmrc.tai.forms.income.previousYears.UpdateIncomeDetailsForm

class UpdateIncomeDetailsFormSpec extends PlaySpec
with OneAppPerSuite with I18nSupport{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "UpdateIncomeDetailsForm" must {
    "return no errors" when {
      "provided with valid data" in {
        val validatedFormForHistoricEmploymentDetails = form.bind(validDetails)
        validatedFormForHistoricEmploymentDetails.errors mustBe empty
      }
    }

    "pre populate the data" when {
      "data is already present" in {
        val prePopForm = form.fill("test details")
        prePopForm.data must contain("employmentDetails" -> "test details")
      }
    }

    "return an error" when {
      "income details are blank" in {
        val validatedFormForHistoricEmploymentDetails = form.bind(emptyDetails)
        validatedFormForHistoricEmploymentDetails.errors must contain(FormError("employmentDetails",
          List(Messages("tai.income.previousYears.details.textarea.error.blank"))))
      }

      "income details have exceeded the maximum characters" in {

        val validatedFormForHistoricEmploymentDetails = form.bind(exceededCharDetails)
        validatedFormForHistoricEmploymentDetails.errors must contain(FormError("employmentDetails",
          List(Messages("tai.income.previousYears.details.textarea.error.maximumExceeded",
            UpdateIncomeDetailsForm.historicEmploymentDetailsCharLimit))))
      }
    }

  }

  private lazy val form = UpdateIncomeDetailsForm.form

  private val exceedingCharacters = "a" * 501

  private val validDetails = Json.obj("employmentDetails" -> "test")
  private val exceededCharDetails = Json.obj("employmentDetails" -> exceedingCharacters)
  private val emptyDetails = Json.obj("employmentDetails" -> "")


}
