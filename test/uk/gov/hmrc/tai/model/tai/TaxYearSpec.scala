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

package uk.gov.hmrc.tai.model.tai

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.tai.model.TaxYear


class TaxYearSpec extends PlaySpec {

  "Tax Year" should {

    "un-marshall Tax year Json" when {
      "given a TaxYear object" in {
        Json.toJson(TaxYear(2017)) mustBe Json.parse("2017")
      }
    }

    "marshall valid TaxYear object" when {
      "given a valid json value" in {
        Json.parse("2017").as[TaxYear] mustBe TaxYear(2017)
      }
    }

    "thrown an exception when year is an invalid" in {
      val ex = the[IllegalArgumentException] thrownBy TaxYear(17)
      ex.getMessage mustBe "requirement failed: Invalid year"
    }

    "not thrown an exception when year is valid" in{
      TaxYear("17") mustBe TaxYear(2017)
    }

  }

}
