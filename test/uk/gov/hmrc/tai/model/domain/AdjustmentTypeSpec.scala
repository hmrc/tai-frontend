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

package uk.gov.hmrc.tai.model.domain

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json}

class AdjustmentTypeSpec extends PlaySpec {

  "AdjustmentTypeFormat" should {
    "create a valid object" when {
      "given a valid json value" in {
        JsString("NationalInsuranceAdjustment").as[AdjustmentType] mustBe NationalInsuranceAdjustment
        JsString("TaxAdjustment").as[AdjustmentType] mustBe TaxAdjustment
        JsString("IncomeAdjustment").as[AdjustmentType] mustBe IncomeAdjustment
      }

      "throw an exception" when {
        "given an invalid json value" in {
          val exception = the[IllegalArgumentException] thrownBy JsString("Wrong").as[AdjustmentType]
          exception.getMessage mustBe "Invalid adjustment type"
        }
      }

      "create a valid json value" when {
        "given a AdjustmentType object" in {
          Json.toJson(NationalInsuranceAdjustment) mustBe JsString("NationalInsuranceAdjustment")
          Json.toJson(TaxAdjustment) mustBe JsString("TaxAdjustment")
          Json.toJson(IncomeAdjustment) mustBe JsString("IncomeAdjustment")
        }
      }
    }
  }

}
