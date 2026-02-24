/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec

class PathPatternMatcherSpec extends PlaySpec with Matchers {

  "PathPatternMatcher.patternMatches" should {

    "return true for exact match without params" in {
      PathPatternMatcher.patternMatches(
        "/check-income-tax/tax-codes",
        "/check-income-tax/tax-codes"
      ) mustBe true
    }

    "return false when exact path differs" in {
      PathPatternMatcher.patternMatches(
        "/check-income-tax/tax-codes",
        "/check-income-tax/tax-codes/:year"
      ) mustBe false
    }

    "return true for one :param segment" in {
      PathPatternMatcher.patternMatches(
        "/check-income-tax/income-details/:empId",
        "/check-income-tax/income-details/123"
      ) mustBe true
    }

    "return false if missing parameter" in {
      PathPatternMatcher.patternMatches(
        "/check-income-tax/income-details/:empId",
        "/check-income-tax/income-details"
      ) mustBe false
    }

    "return false if there is and extra segment in the path" in {
      PathPatternMatcher.patternMatches(
        "/check-income-tax/income-details/:empId",
        "/check-income-tax/income-details/:empId/other"
      ) mustBe false
    }

    "return true for multiple params" in {
      PathPatternMatcher.patternMatches(
        "/check-income-tax/your-income-calculation-previous-year/:year/:empId",
        "/check-income-tax/your-income-calculation-previous-year/2025/123"
      ) mustBe true
    }

    "return false for multiple params when one param is missing" in {
      PathPatternMatcher.patternMatches(
        "/check-income-tax/your-income-calculation-previous-year/:year/:empId",
        "/check-income-tax/your-income-calculation-previous-year/2025"
      ) mustBe false
    }
  }
}
