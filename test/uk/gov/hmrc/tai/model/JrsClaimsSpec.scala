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

class JrsClaimsSpec extends PlaySpec {

  "JrsClaims" must {

    val json = Json.obj(
      "employers" -> Json.arr(
        Json.obj(
          "name"              -> "ASDA",
          "employerReference" -> "ABC-DEFGHIJ",
          "claims" -> Json.arr(
            Json.obj(
              "yearAndMonth" -> "2020-12"
            ),
            Json.obj(
              "yearAndMonth" -> "2021-01"
            )
          )
        )
      )
    )

    val data = JrsClaims(List(Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2021-01")))))

    "deserialise valid values" in {

      val result = json.as[JrsClaims]

      result shouldBe JrsClaims(
        List(Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2021-01")))))

    }

    "deserialise invalid values" in {

      val invalidJson = Json.obj(
        "employers" -> "invalid"
      )

      val ex = intercept[JsResultException] {
        invalidJson.as[YearAndMonth]
      }

      ex.getMessage shouldBe "Invalid format: \"invalid\""

    }

    "deserialise invalid key" in {

      val invalidJson = Json.obj(
        "invalidKey" -> Json.arr(
          Json.obj(
            "name"              -> "ASDA",
            "employerReference" -> "ABC-DEFGHIJ",
            "claims" -> Json.arr(
              Json.obj(
                "yearAndMonth" -> "2020-12"
              ),
              Json.obj(
                "yearAndMonth" -> "2021-01"
              )
            )
          )
        )
      )

      val ex = intercept[JsResultException] {
        invalidJson.as[JrsClaims]
      }

      ex.getMessage shouldBe "JsResultException(errors:List((/employers,List(JsonValidationError(List(error.path.missing),WrappedArray())))))"

    }

    "serialise to json" in {

      Json.toJson(data) shouldBe json

    }

    "serialise/deserialise to the same value" in {

      val result = Json.toJson(data).as[JrsClaims]

      result shouldBe data

    }

  }

}
