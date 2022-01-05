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

package uk.gov.hmrc.tai.model.tai

import org.joda.time.LocalDate
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

    "instantiate as the previous year" when {
      "the given date is before the start of the tax year" in {
        TaxYear(LocalDate.parse("2020-04-05")).year mustBe 2019
      }
    }

    "instantiate as the current year" when {
      "the given date is after the start of the tax year" in {
        TaxYear(LocalDate.parse("2020-04-06")).year mustBe 2020
      }
    }

    "thrown an exception when year is an invalid" in {
      val ex = the[IllegalArgumentException] thrownBy TaxYear(17)
      ex.getMessage mustBe "requirement failed: Invalid year"
    }

    "not thrown an exception when year is valid" in {
      TaxYear("17") mustBe TaxYear(2017)
    }

    "fromNow" must {
      "for 0" in {
        TaxYear.fromNow(1) mustBe TaxYear().next
      }

      "for > 0" in {
        TaxYear.fromNow(1) mustBe TaxYear().next
        TaxYear.fromNow(3) mustBe TaxYear().next.next.next
      }

      "for < 0" in {
        TaxYear.fromNow(-1) mustBe TaxYear().prev
        TaxYear.fromNow(-3) mustBe TaxYear().prev.prev.prev
      }
    }

    "within" should {

      "return true when given a date of the start of the current tax year" in {
        TaxYear().within(TaxYear().start) mustBe true
      }

      "return true when given a date after the start of the current tax year but before the end of the tax year" in {
        TaxYear().within(TaxYear().start.plusDays(1)) mustBe true
      }

      "return true when given a date on the end of the current tax year" in {
        TaxYear().within(TaxYear().end) mustBe true
      }

      "return false when given a date before the start of the current tax year" in {
        TaxYear().within(TaxYear().prev.end) mustBe false
      }
      "return false when given a date after the end of the current tax year" in {
        TaxYear().within(TaxYear().next.start) mustBe false
      }

    }

  }

}
