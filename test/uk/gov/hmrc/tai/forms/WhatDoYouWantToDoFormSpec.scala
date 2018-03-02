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

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec

class WhatDoYouWantToDoFormSpec extends PlaySpec with FakeTaiPlayApplication {
  "calling the WhatDoYouWantToDoForm" should {
    "return the form with errors" when {
      "the taxYears field has an empty string" in {
        val wdywtdForm = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))
        wdywtdForm.hasErrors mustBe true
      }
    }

    "return the form without any errors" when {
      "the taxYears field has a non-empty string" in {
        val wdywtdForm = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> "currentTaxYear"))
        wdywtdForm.hasErrors mustBe false
      }
    }
  }
}
