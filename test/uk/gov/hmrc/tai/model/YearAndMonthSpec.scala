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

import play.api.i18n.Lang
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.YearAndMonth.firstClaimDate
import utils.BaseSpec

class YearAndMonthSpec extends BaseSpec {

  override lazy val appConfig = mock[ApplicationConfig]

  when(appConfig.jrsClaimsFromDate).thenReturn("2020-12")

  "YearAndMonth" must {

    val json = Json.obj(
      "yearAndMonth" -> "2020-12"
    )

    val data = YearAndMonth("2020-12")

    "deserialise valid values" in {

      val result = json.as[YearAndMonth]

      result mustBe data

    }

    "deserialise invalid values" in {

      val invalidJson = Json.obj(
        "yearAndMonth" -> "invalid-date"
      )

      val ex = intercept[JsResultException] {
        invalidJson.as[YearAndMonth]
      }

      ex.getMessage mustBe "JsResultException(errors:List((/yearAndMonth,List(JsonValidationError(List(Invalid date parsed),WrappedArray())))))"

    }

    "deserialise invalid key" in {

      val invalidJson = Json.obj(
        "invalidKey" -> "2020-12"
      )

      val ex = intercept[JsResultException] {
        invalidJson.as[YearAndMonth]
      }

      ex.getMessage mustBe "JsResultException(errors:List((/yearAndMonth,List(JsonValidationError(List(error.path.missing),WrappedArray())))))"

    }

    "serialise to json" in {

      Json.toJson(data) mustBe json
    }

    "serialise/deserialise to the same value" in {

      val result = Json.toJson(data).as[YearAndMonth]

      result mustBe data

    }

    "sort the claim data in ascending order" in {

      val result =
        YearAndMonth.sortYearAndMonth(List(YearAndMonth("2021-01"), YearAndMonth("2020-12")), appConfig)

      result mustBe List(YearAndMonth("2020-12"), YearAndMonth("2021-01"))

    }

    "sort the claim data should remove all the dates before the first claim date" in {

      val result =
        YearAndMonth
          .sortYearAndMonth(List(YearAndMonth("2021-02"), YearAndMonth("2020-12"), YearAndMonth("2020-11")), appConfig)

      result mustBe List(YearAndMonth("2020-12"), YearAndMonth("2021-02"))

    }

    "YearMonth should be formatted to MMMM YYYY" in {

      data.formatYearAndMonth(lang) mustBe "December 2020"

    }

    "YearMonth should be formatted to MMMM YYYY and Welsh if Welsh is the current language" in {

      val lang = Lang("cy")

      data.formatYearAndMonth(lang) mustBe "Rhagfyr 2020"

    }

    "formattedDate with no date should return first claim date formatted to MMMM YYYY" in {

      YearAndMonth.formattedDate(firstClaimDate(appConfig), lang) mustBe "December 2020"

    }

    "formattedDate with no date should return first claim date formatted to MMMM YYYY and Welsh if Welsh is the current language" in {

      val lang = Lang("cy")

      YearAndMonth.formattedDate(firstClaimDate(appConfig), lang) mustBe "Rhagfyr 2020"

    }
  }
}
