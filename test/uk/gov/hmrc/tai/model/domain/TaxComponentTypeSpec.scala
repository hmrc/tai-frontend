/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{JsResultException, JsString, Json}
import utils.BaseSpec

class TaxComponentTypeSpec extends BaseSpec {

  "toMessage" must {
    "return the tax component type as a user friendly label" in {
      val taxComponentType = GiftAidPayments

      taxComponentType.toMessage() mustBe "Gift Aid Payments"
    }
  }

  "Income component format" must {
    "create a valid object" when {
      "given a valid json value" in {
        JsString("EmploymentIncome").as[TaxComponentType] mustBe EmploymentIncome
        JsString("PensionIncome").as[TaxComponentType] mustBe PensionIncome
        JsString("JobSeekerAllowanceIncome").as[TaxComponentType] mustBe JobSeekerAllowanceIncome
        JsString("OtherIncome").as[TaxComponentType] mustBe OtherIncome
      }

      "throw an exception" when {
        "give an invalid json value" in {
          val exception = the[JsResultException] thrownBy JsString("Wrong").as[TaxComponentType]
          exception.getMessage must include("Invalid Tax component type")
        }
      }

      "create a valid json value" when {
        "given an Income Component Type" in {
          Json.toJson[TaxComponentType](EmploymentIncome) mustBe JsString("EmploymentIncome")
          Json.toJson[TaxComponentType](PensionIncome) mustBe JsString("PensionIncome")
          Json.toJson[TaxComponentType](JobSeekerAllowanceIncome) mustBe JsString("JobSeekerAllowanceIncome")
          Json.toJson[TaxComponentType](OtherIncome) mustBe JsString("OtherIncome")
        }
      }
    }
  }
}
