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

package uk.gov.hmrc.tai.model.domain

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, JsResultException, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.time.TaxYearResolver

import scala.util.Random

class TaxCodePairsSpec extends PlaySpec{

  "TaxCodePairs" should {
    "return the primary pair when only one pair exists" in {
      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode),
        Seq(primaryFullYearTaxCode)
      )
      model.pairs mustEqual Seq(TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)))
    }

    "return the primary pair first when multiple pairs exist" in {
      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode, previousTaxCodeRecord1, fullYearTaxCode),
        Seq(primaryFullYearTaxCode, currentTaxCodeRecord1, fullYearTaxCode)
      )
      model.pairs(0) mustEqual TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode))
    }

    "return the primary pair first when unmatched pairs exist" in {
      val model = TaxCodePairs(
        Seq(primaryFullYearTaxCode, previousTaxCodeRecord1, fullYearTaxCode),
        Seq(primaryFullYearTaxCode, currentTaxCodeRecord1, fullYearTaxCode)
      )
      model.pairs(0) mustEqual TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode))
    }

    "return the primary pairs first when multiple primaries exist" in {
      val previousPrimary = previousTaxCodeRecord1.copy(primary = true)
      val currentPrimary = currentTaxCodeRecord1.copy(primary = true)
      val model = TaxCodePairs(
        Seq(previousPrimary, primaryFullYearTaxCode),
        Seq(currentPrimary, primaryFullYearTaxCode)
      )
      model.pairs.take(2) mustEqual Seq(
        TaxCodePair(Some(previousPrimary), Some(currentPrimary)),
        TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode))
      )
    }

    "return the secondary pairs after the primary pairs" in {
      val model = TaxCodePairs(
        Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
        Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
      )

      model.pairs(1) mustEqual TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1))
      model.pairs(2) mustEqual TaxCodePair(Some(fullYearTaxCode), Some(fullYearTaxCode))
    }

    "return the unmatched current and previous taxCodes given one in current and one in previous" in {
      val model = TaxCodePairs(
        Seq(unmatchedPreviousTaxCode),
        Seq(unmatchedCurrentTaxCode)
      )
      model.pairs mustEqual Seq(
        TaxCodePair(Some(unmatchedPreviousTaxCode), None),
        TaxCodePair(None, Some(unmatchedCurrentTaxCode))
      )
    }

    "return all tax codes pairs ordered by primary, secondary, unmatched previous, unmatched current" in {
      val model = TaxCodePairs(
        Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
        Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
      )
      model.pairs mustEqual Seq(
        TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)),
        TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1)),
        TaxCodePair(Some(fullYearTaxCode), Some(fullYearTaxCode)),
        TaxCodePair(Some(unmatchedPreviousTaxCode), None),
        TaxCodePair(None, Some(unmatchedCurrentTaxCode))
      )
    }
  }

  val nino = generateNino
  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val previousTaxCodeRecord1 = TaxCodeRecord("code", startDate, startDate.plusMonths(1),"A Employer 1", 1, "1234", false)
  val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)
  val fullYearTaxCode = TaxCodeRecord("code", startDate, TaxYearResolver.endOfCurrentTaxYear, "B Employer 1", 2, "12345", false)
  val primaryFullYearTaxCode = fullYearTaxCode.copy(employerName = "C", employmentId = 3, primary = true)
  val unmatchedPreviousTaxCode = TaxCodeRecord("Unmatched Previous", startDate, startDate.plusMonths(1),"D", 4, "D Id", false)
  val unmatchedCurrentTaxCode = TaxCodeRecord("Unmatched Current", startDate.plusMonths(1), TaxYearResolver.endOfCurrentTaxYear,"E", 5, "E id", false)

  private def generateNino: Nino = new Generator(new Random).nextNino
}
