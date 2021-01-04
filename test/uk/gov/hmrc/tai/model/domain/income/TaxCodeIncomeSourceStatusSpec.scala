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

package uk.gov.hmrc.tai.model.domain.income

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, JsString}

class TaxCodeIncomeSourceStatusSpec extends PlaySpec {
  "taxCodeIncomeSourceStatusReads" must {
    "read the field correctly" when {
      "json string is Live" in {
        JsString("Live").as[TaxCodeIncomeSourceStatus] mustBe Live
      }
      "json string is PotentiallyCeased" in {
        JsString("PotentiallyCeased")
          .as[TaxCodeIncomeSourceStatus] mustBe PotentiallyCeased
      }
      "json string is Ceased" in {
        JsString("Ceased").as[TaxCodeIncomeSourceStatus] mustBe Ceased
      }
    }
    "throw JsResultException" when {
      "provided with unrecognized status" in {
        val ex = the[JsResultException] thrownBy JsString("Some Status")
          .as[TaxCodeIncomeSourceStatus]
        ex.getMessage must include("Invalid Tax component type")
      }
    }
  }
}
