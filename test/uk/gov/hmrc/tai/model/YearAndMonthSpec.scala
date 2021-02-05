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

package uk.gov.hmrc.tai.model

import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, Json}

class YearAndMonthSpec extends PlaySpec {

  "YearAndMonth" must {

    val json = Json.obj(
      "yearAndMonth" -> "2020-12"
    )

    val data = YearAndMonth("2020-12")

    "deserialise valid values" in {

      val result = json.as[YearAndMonth]

      result shouldBe data

    }

    "deserialise invalid values" in {

      val invalidJson = Json.obj(
        "yearAndMonth" -> "invalid-date"
      )

      val ex = intercept[IllegalArgumentException] {
        invalidJson.as[YearAndMonth]
      }

      ex.getMessage shouldBe "Invalid format: \"invalid-date\""

    }

    "deserialise invalid key" in {

      val invalidJson = Json.obj(
        "invalidKey" -> "2020-12"
      )

      val ex = intercept[JsResultException] {
        invalidJson.as[YearAndMonth]
      }

      ex.getMessage shouldBe "JsResultException(errors:List((/yearAndMonth,List(JsonValidationError(List(error.path.missing),WrappedArray())))))"

    }

    "serialise to json" in {

      Json.toJson(data) shouldBe json
    }

    "serialise/deserialise to the same value" in {

      val result = Json.toJson(data).as[YearAndMonth]

      result shouldBe data

    }

  }

}
