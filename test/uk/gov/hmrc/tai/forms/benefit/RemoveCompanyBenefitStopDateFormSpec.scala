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

package uk.gov.hmrc.tai.forms.benefit

import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm
import utils.BaseSpec

import java.time.LocalDate

class RemoveCompanyBenefitStopDateFormSpec extends BaseSpec {

  private val form = RemoveCompanyBenefitStopDateForm("benefit", "employment").form

  "RemoveCompanyBenefitStopDateFormSpec" must {
    "return no errors with valid date" in {
      val validDate     = Map(
        RemoveCompanyBenefitStopDateForm.BenefitFormDay   -> "15",
        RemoveCompanyBenefitStopDateForm.BenefitFormMonth -> "01",
        RemoveCompanyBenefitStopDateForm.BenefitFormYear  -> "2023"
      )
      val validatedForm = form.bind(validDate)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe LocalDate.parse("2023-01-15")
    }

    "return an error for invalid date" in {
      val invalidDate     = Map(
        RemoveCompanyBenefitStopDateForm.BenefitFormDay   -> "",
        RemoveCompanyBenefitStopDateForm.BenefitFormMonth -> "",
        RemoveCompanyBenefitStopDateForm.BenefitFormYear  -> ""
      )
      val invalidatedForm = form.bind(invalidDate)

      invalidatedForm.errors.head.messages mustBe List(RemoveCompanyBenefitStopDateForm.errorMsgs.enterDate)
      invalidatedForm.value mustBe None
    }

  }
}
