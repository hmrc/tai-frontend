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

package uk.gov.hmrc.tai.model

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, Json}
import utils.BaseSpec

class JrsClaimsSpec extends PlaySpec with BaseSpec {

  val jrsClaims = new JrsClaims(
    List(
      Employers("Co-Operative", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2021-02"))),
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12")))
    )
  )

  "JrsClaims" must {

    val json = Json.obj(
      "employers" -> Json.arr(
        Json.obj(
          "name"              -> "ASDA",
          "employerReference" -> "ABC-DEFGHIJ",
          "claims" -> Json.arr(
            Json.obj(
              "yearAndMonth" -> "2021-01"
            ),
            Json.obj(
              "yearAndMonth" -> "2020-12"
            )
          )
        )
      )
    )

    val data = JrsClaims(List(Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2020-12")))))

    "deserialise valid values" in {

      val result = json.as[JrsClaims]

      result mustBe JrsClaims(
        List(Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2020-12"))))
      )

    }

    "deserialise invalid values" in {

      val invalidJson = Json.obj(
        "employers" -> "invalid"
      )

      val ex = intercept[JsResultException] {
        invalidJson.as[YearAndMonth]
      }

      ex.getMessage mustBe "JsResultException(errors:List((/yearAndMonth,List(JsonValidationError(List(error.path.missing),List())))))"

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

      ex.getMessage mustBe "JsResultException(errors:List((/employers,List(JsonValidationError(List(error.path.missing),List())))))"

    }

    "serialise to json" in {

      Json.toJson(data) mustBe json

    }

    "serialise/deserialise to the same value" in {

      val result = Json.toJson(data).as[JrsClaims]

      result mustBe data

    }

    "hasMultipleEmployments should return true if there is more than one claim" in {

      jrsClaims.hasMultipleEmployments mustBe true

    }

    "hasMultipleEmployments should return false if there is only one claim" in {

      data.hasMultipleEmployments mustBe false

    }

    "employerMessageKey should return employers when hasMultipleEmployments is true" in {

      jrsClaims.employerMessageKey mustBe "employers"

    }

    "employerMessageKey should return employer when hasMultipleEmployments is false" in {

      data.employerMessageKey mustBe "employer"

    }

    "JrsClaims apply method should return sorted data" in {

      JrsClaims(appConfig, jrsClaims) mustBe Some(
        JrsClaims(
          List(
            Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"))),
            Employers("Co-Operative", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2021-02")))
          )
        )
      )
    }

    "JrsClaims apply method should return none for empty employer list" in {

      JrsClaims(appConfig, JrsClaims(List.empty)) mustBe None

    }

  }
}
