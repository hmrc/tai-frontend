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

package uk.gov.hmrc.tai.forms.pensions

import play.api.data.FormError
import play.api.i18n.Messages
import utils.BaseSpec

class WhatDoYouWantToTellUsFormSpec extends BaseSpec {

  "WhatDoYouWantToTellUs Form" must {
    "return no errors with valid data" in {

      val validatedFormForPensionDetails = form.bind(validDetails)
      validatedFormForPensionDetails.errors mustBe empty
    }

    "pre populate the form correctly" in {

      val prePopForm = form.fill("test details")
      prePopForm.data must contain("pensionDetails" -> "test details")
    }

    "return an error" when {
      "employment details are blank" in {

        val validatedFormForPensionDetails = form.bind(emptyDetails)
        validatedFormForPensionDetails.errors must contain(
          FormError("pensionDetails", List(Messages("tai.updatePension.whatDoYouWantToTellUs.textarea.error.blank")))
        )
      }
    }

    "return an error" when {
      "employment details have exceeded the maximum characters" in {

        val validatedFormForPensionDetails = form.bind(exceededCharDetails)
        validatedFormForPensionDetails.errors must contain(
          FormError(
            "pensionDetails",
            List(
              Messages(
                "tai.updatePension.whatDoYouWantToTellUs.textarea.error.maximumExceeded",
                WhatDoYouWantToTellUsForm.pensionDetailsCharacterLimit
              )
            )
          )
        )
      }
    }
  }

  private lazy val form = WhatDoYouWantToTellUsForm.form

  private val exceedingCharacters = "a" * 501

  private val validDetails        = Map("pensionDetails" -> "test")
  private val exceededCharDetails = Map("pensionDetails" -> exceedingCharacters)
  private val emptyDetails        = Map("pensionDetails" -> "")
}
