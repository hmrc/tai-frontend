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

class EmployersSpec extends PlaySpec {

  "Employers" must {

    val json = Json.obj(
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

    val data = Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2021-01")))

    "deserialise valid values" in {

      val result = json.as[Employers]

      result shouldBe Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2021-01")))

    }

    "deserialise invalid values" in {

      val invalidJson = Json.obj(
        "name"              -> 123,
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

      val ex = intercept[JsResultException] {
        invalidJson.as[Employers]
      }

      ex.getMessage shouldBe "JsResultException(errors:List((/name,List(JsonValidationError(List(error.expected.jsstring),WrappedArray())))))"

    }

    "deserialise invalid key" in {

      val invalidJson = Json.obj(
        "name"       -> "ASDA",
        "invalidKey" -> "ABC-DEFGHIJ",
        "claims" -> Json.arr(
          Json.obj(
            "yearAndMonth" -> "2020-12"
          ),
          Json.obj(
            "yearAndMonth" -> "2021-01"
          )
        )
      )

      val ex = intercept[JsResultException] {
        invalidJson.as[Employers]
      }

      ex.getMessage shouldBe "JsResultException(errors:List((/employerReference,List(JsonValidationError(List(error.path.missing),WrappedArray())))))"

    }

    "serialise to json" in {

      Json.toJson(data) shouldBe Json.obj(
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
    }

    "serialise/deserialise to the same value" in {

      val result = Json.toJson(data).as[Employers]

      result shouldBe data

    }

  }

}
