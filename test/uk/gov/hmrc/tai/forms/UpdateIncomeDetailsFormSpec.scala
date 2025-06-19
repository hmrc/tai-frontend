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

package uk.gov.hmrc.tai.forms

import play.api.data.FormError
import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.income.previousYears.UpdateIncomeDetailsForm
import utils.BaseSpec

class UpdateIncomeDetailsFormSpec extends BaseSpec {

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
        validatedFormForHistoricEmploymentDetails.errors must contain(
          FormError("employmentDetails", List(Messages("tai.income.previousYears.details.textarea.error.blank")))
        )
      }

      "income details have exceeded the maximum characters" in {

        val validatedFormForHistoricEmploymentDetails = form.bind(exceededCharDetails)
        validatedFormForHistoricEmploymentDetails.errors must contain(
          FormError(
            "employmentDetails",
            List(
              Messages(
                "tai.income.previousYears.details.textarea.error.maximumExceeded",
                UpdateIncomeDetailsForm.historicEmploymentDetailsCharLimit
              )
            )
          )
        )
      }

      "new line should be counted as one character so the form bound to 499 chars and a newline should be valid" in {
        val validatedFormForHistoricEmploymentDetails = form.bind(validDetailsWithNewline)
        validatedFormForHistoricEmploymentDetails.errors mustBe empty
      }
    }

  }

  private lazy val form = UpdateIncomeDetailsForm.form

  private val exceedingCharacters = "a" * 501

  private val validDetails            = Map("employmentDetails" -> "test")
  private val validDetailsWithNewline = Map("employmentDetails" -> ("m\r\nm" + "m" * 497))
  private val exceededCharDetails     = Map("employmentDetails" -> exceedingCharacters)
  private val emptyDetails            = Map("employmentDetails" -> "")

}
