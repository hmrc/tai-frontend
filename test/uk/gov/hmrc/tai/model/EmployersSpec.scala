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

package uk.gov.hmrc.tai.model

import org.joda.time.YearMonth
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, Json}
import utils.BaseSpec

class EmployersSpec extends PlaySpec with BaseSpec {

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

    val employersList = List(
      Employers("Co-Operative", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2021-02"))),
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12")))
    )

    "deserialise valid values" in {

      val result = json.as[Employers]

      result shouldBe Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2021-01")))

    }

    "deserialise invalid name" in {

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

      Json.toJson(data) shouldBe json
    }

    "serialise/deserialise to the same value" in {

      val result = Json.toJson(data).as[Employers]

      result shouldBe data

    }

    "sort the employer data in alphabetical order" in {

      val result =
        Employers.sortEmployerslist(appConfig, employersList)

      result shouldBe List(
        Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"))),
        Employers("Co-Operative", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2021-02")))
      )

    }

    "hasMultipleClaims should return true if there is more than one claim" in {

      data.hasMultipleClaims shouldBe true

    }

    "hasMultipleClaims should return false if there is only one claim" in {

      val result = Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12")))

      result.hasMultipleClaims shouldBe false

    }

  }

}
