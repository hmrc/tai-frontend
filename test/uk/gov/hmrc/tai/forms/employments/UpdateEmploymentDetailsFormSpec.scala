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

import play.api.data.FormError
import play.api.i18n.Messages
import utils.BaseSpec

class UpdateEmploymentDetailsFormSpec extends BaseSpec {

  "UpdateEmploymentDetailsForm" must {
    "return no errors with valid data" in {

      val validatedFormForEmploymentDetails = form.bind(validDetails)
      validatedFormForEmploymentDetails.errors mustBe empty
    }

    "pre populate the form correctly" in {

      val prePopForm = form.fill("test details")
      prePopForm.data must contain("employmentDetails" -> "test details")
    }

    "return an error" when {
      "employment details are blank" in {

        val validatedFormForEmploymentDetails = form.bind(emptyDetails)
        validatedFormForEmploymentDetails.errors must contain(
          FormError(
            "employmentDetails",
            List(Messages("tai.updateEmployment.whatDoYouWantToTellUs.textarea.error.blank"))
          )
        )
      }
    }

    "return an error" when {
      "employment details have exceeded the maximum characters" in {

        val validatedFormForEmploymentDetails = form.bind(exceededCharDetails)
        validatedFormForEmploymentDetails.errors must contain(
          FormError(
            "employmentDetails",
            List(
              Messages(
                "tai.updateEmployment.whatDoYouWantToTellUs.textarea.error.maximumExceeded",
                UpdateEmploymentDetailsForm.employmentDetailsCharacterLimit
              )
            )
          )
        )
      }
    }
  }

  private lazy val form = UpdateEmploymentDetailsForm.form

  private val exceedingCharacters = "a" * 501

  private val validDetails        = Map("employmentDetails" -> "test")
  private val exceededCharDetails = Map("employmentDetails" -> exceedingCharacters)
  private val emptyDetails        = Map("employmentDetails" -> "")
}
