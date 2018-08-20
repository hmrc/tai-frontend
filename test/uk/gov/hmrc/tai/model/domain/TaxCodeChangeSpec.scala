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
import play.api.libs.json.{JsArray, JsNull, JsResultException, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.time.TaxYearResolver

import scala.util.Random

class TaxCodeChangeSpec extends PlaySpec{

  "TaxCodeChange" when {
    "parsing JSON" should {
      "return a valid TaxCodeChange object when given valid Json" in {
        val expectedModel = TaxCodeChange(
          Seq(previousTaxCodeRecord1),
          Seq(currentTaxCodeRecord1)
        )

        taxCodeChangeJson.as[TaxCodeChange] mustEqual expectedModel
      }


      "throw a JsError given an empty Seq of TaxCodeRecords" in {
        an[JsResultException] should be thrownBy emptyTaxCodeRecordsJson.as[TaxCodeChange]
      }
    }

    "calling mostRecentTaxCodeChangeDate" should {
      "return the latest tax code change date from a sequence of tax code records" in {
        val model = TaxCodeChange(Seq(previousTaxCodeRecord1, fullYearTaxCode), Seq(currentTaxCodeRecord1, fullYearTaxCode))

        model.mostRecentTaxCodeChangeDate mustEqual startDate.plusMonths(1).plusDays(1)
      }
    }

    "allTaxCodePairsOrdered is called" should {
      "return the primary pair when only one pair exists" in {
        val model = TaxCodeChange(
          Seq(primaryFullYearTaxCode),
          Seq(primaryFullYearTaxCode)
        )
        model.allTaxCodePairsOrdered mustEqual Seq(TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)))
      }

      "return the primary pair when multiple pairs exist" in {
        val model = TaxCodeChange(
          Seq(primaryFullYearTaxCode, previousTaxCodeRecord1, fullYearTaxCode),
          Seq(primaryFullYearTaxCode, currentTaxCodeRecord1, fullYearTaxCode)
        )
        model.primaryPairs mustEqual Seq(TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)))
      }

      "return the primary pair when unmatched pairs exist" in {
        val model = TaxCodeChange(
          Seq(primaryFullYearTaxCode, previousTaxCodeRecord1, fullYearTaxCode),
          Seq(primaryFullYearTaxCode, currentTaxCodeRecord1, fullYearTaxCode)
        )
        model.primaryPairs mustEqual Seq(TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)))
      }

      "return the primary pairs when multiple primaries exist" in {
        val previousPrimary = previousTaxCodeRecord1.copy(primary = true)
        val currentPrimary = currentTaxCodeRecord1.copy(primary = true)
        val model = TaxCodeChange(
          Seq(previousPrimary, primaryFullYearTaxCode),
          Seq(currentPrimary, primaryFullYearTaxCode)
        )
        model.primaryPairs mustEqual Seq(
          TaxCodePair(Some(previousPrimary), Some(currentPrimary)),
          TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode))
        )
      }

      "return the secondary pairs" in {
        val model = TaxCodeChange(
          Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
          Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
        )

        model.secondaryPairs mustEqual Seq(
          TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1)),
          TaxCodePair(Some(fullYearTaxCode), Some(fullYearTaxCode))
        )
      }

      "return the unmatched current taxCodes given one in current and one in previous" in {
        val model = TaxCodeChange(
          Seq(unmatchedPreviousTaxCode),
          Seq(unmatchedCurrentTaxCode)
        )
        model.unpairedCurrentCodes mustEqual Seq(TaxCodePair(None, Some(unmatchedCurrentTaxCode)))
      }

      "return the unmatched current taxCodes given a matching pair" in {
        val model = TaxCodeChange(
          Seq(previousTaxCodeRecord1),
          Seq(currentTaxCodeRecord1)
        )
        model.unpairedCurrentCodes mustEqual Seq.empty
      }

      "return the unmatched current taxCodes" in {
        val model = TaxCodeChange(
          Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
          Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
        )
        model.unpairedCurrentCodes mustEqual Seq(TaxCodePair(None, Some(unmatchedCurrentTaxCode)))
      }

      "return the unmatched previous taxCodes given one in current and one in previous" in {
        val model = TaxCodeChange(
          Seq(unmatchedPreviousTaxCode),
          Seq(unmatchedCurrentTaxCode)
        )
        model.unpairedPreviousCodes mustEqual Seq(TaxCodePair(Some(unmatchedPreviousTaxCode), None))
      }

      "return the unmatched previous taxCodes" in {
        val model = TaxCodeChange(
          Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
          Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
        )
        model.unpairedPreviousCodes mustEqual Seq(TaxCodePair(Some(unmatchedPreviousTaxCode), None))
      }

      "return the unmatched previous taxCodes given a matching pair" in {
        val model = TaxCodeChange(
          Seq(previousTaxCodeRecord1),
          Seq(currentTaxCodeRecord1)
        )
        model.unpairedPreviousCodes mustEqual Seq.empty
      }

      "return all tax codes pairs ordered by primary, secondary, unmatched previous, unmatched current" in {
        val model = TaxCodeChange(
          Seq(previousTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedPreviousTaxCode),
          Seq(currentTaxCodeRecord1, fullYearTaxCode, primaryFullYearTaxCode, unmatchedCurrentTaxCode)
        )
        model.allTaxCodePairsOrdered mustEqual Seq(
          TaxCodePair(Some(primaryFullYearTaxCode), Some(primaryFullYearTaxCode)),
          TaxCodePair(Some(previousTaxCodeRecord1), Some(currentTaxCodeRecord1)),
          TaxCodePair(Some(fullYearTaxCode), Some(fullYearTaxCode)),
          TaxCodePair(Some(unmatchedPreviousTaxCode), None),
          TaxCodePair(None, Some(unmatchedCurrentTaxCode))
        )
      }
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


  val taxCodeChangeJson = Json.obj(
    "previous" -> Json.arr(
      Json.obj(
        "taxCode" -> "code",
        "startDate" -> startDate.toString,
        "endDate" -> startDate.plusMonths(1).toString,
        "employerName" -> "A Employer 1",
        "employmentId" -> 1,
        "payrollNumber" -> "1234",
        "primary" -> false
      )
    ),
    "current" -> Json.arr(
      Json.obj(
        "taxCode" -> "code",
        "startDate" -> startDate.plusMonths(1).plusDays(1).toString,
        "endDate" -> TaxYearResolver.endOfCurrentTaxYear.toString,
        "employerName" -> "A Employer 1",
        "employmentId" -> 1,
        "payrollNumber" -> "1234",
        "primary" -> false
      )
    )
  )

  val emptyTaxCodeRecordsJson = Json.obj(
    "nino" -> nino.nino,
    "taxCodeRecord" -> Seq(JsNull)
  )

  private def generateNino: Nino = new Generator(new Random).nextNino
}
