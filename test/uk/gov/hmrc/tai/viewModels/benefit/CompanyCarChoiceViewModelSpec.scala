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

package uk.gov.hmrc.tai.viewModels.benefit

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.util.JourneyCacheConstants

class CompanyCarChoiceViewModelSpec extends PlaySpec
  with JourneyCacheConstants {

  "Company car view model" must {

    "create instance of view model" when {

      "company car model and provider are present in the provided cache map" in {
        val cacheMap = Map[String, String](CompanyCar_CarModelKey -> "XJ", CompanyCar_CarProviderKey -> "company name")
        val result = CompanyCarChoiceViewModel(cacheMap)
        result mustBe CompanyCarChoiceViewModel("XJ", "company name")
      }

      "throw a runtime exception" when {

        "car model is absent from the provided cache map" in {
          val cacheMap = Map[String, String](CompanyCar_CarProviderKey -> "company name")
          val ex = the[RuntimeException] thrownBy CompanyCarChoiceViewModel(cacheMap)
          ex.getMessage mustBe "No company car model found in supplied cache map"
        }

        "car provider is absent from the provided cache map" in {
          val cacheMap = Map[String, String](CompanyCar_CarModelKey -> "XJ")
          val ex = the[RuntimeException] thrownBy CompanyCarChoiceViewModel(cacheMap)
          ex.getMessage mustBe "No company car provider found in supplied cache map"
        }
      }
    }
  }
}
