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

  "TaxCodeChange" should {
    "return a valid TaxCodeChange object when given valid Json" in {
      val expectedModel = TaxCodeChange(
        Seq(taxCodeRecord1),
        Seq(taxCodeRecord2)
      )

      taxCodeChangeJson.as[TaxCodeChange] mustEqual expectedModel
    }

    "throw a JsError given an empty Seq of TaxCodeRecords" in {
      an [JsResultException] should be thrownBy emptyTaxCodeRecordsJson.as[TaxCodeChange]
    }

    "return the latest tax code change date from a sequence of tax code records" in {
      val model = TaxCodeChange(Seq(taxCodeRecord1, taxCodeRecord3), Seq(taxCodeRecord2, taxCodeRecord3))

      model.mostRecentTaxCodeChangeDate mustEqual startDate.plusMonths(1).plusDays(1)
    }

    "generate pairs of taxCodeChanges based on employmentId" in {
      val model = TaxCodeChange(Seq(taxCodeRecord1, taxCodeRecord3), Seq(taxCodeRecord2, taxCodeRecord3, unmatchedCurrentTaxCode))
      val generatedPairs = Seq(model.TaxCodePair(Some(taxCodeRecord1), Some(taxCodeRecord2)), model.TaxCodePair(Some(taxCodeRecord3), Some(taxCodeRecord3)))

      model.taxCodePairs mustEqual generatedPairs
    }

    "return the primary pairs" in {
      val model = TaxCodeChange(
        Seq(taxCodeRecord1, taxCodeRecord3, taxCodeRecord4, unmatchedPreviousTaxCode),
        Seq(taxCodeRecord2, taxCodeRecord3, taxCodeRecord4, unmatchedCurrentTaxCode)
      )
      model.primaryPairs mustEqual Seq(model.TaxCodePair(Some(taxCodeRecord4), Some(taxCodeRecord4)))
    }

    "return the secondary pairs" in {
      val model = TaxCodeChange(
        Seq(taxCodeRecord1, taxCodeRecord3, taxCodeRecord4, unmatchedPreviousTaxCode),
        Seq(taxCodeRecord2, taxCodeRecord3, taxCodeRecord4, unmatchedCurrentTaxCode)
      )

      model.secondaryPairs mustEqual Seq(
        model.TaxCodePair(Some(taxCodeRecord1), Some(taxCodeRecord2)),
        model.TaxCodePair(Some(taxCodeRecord3), Some(taxCodeRecord3))
      )
    }

    "return the unmatched current taxCodes given one in current and one in previous" in {
      val model = TaxCodeChange(
        Seq(unmatchedPreviousTaxCode),
        Seq(unmatchedCurrentTaxCode)
      )
      model.unpairedCurrentCodes mustEqual Seq(model.TaxCodePair(None, Some(unmatchedCurrentTaxCode)))
    }

    "return the unmatched current taxCodes given a matching pair" in {
      val model = TaxCodeChange(
        Seq(taxCodeRecord1),
        Seq(taxCodeRecord2)
      )
      model.unpairedCurrentCodes mustEqual Seq.empty
    }

    "return the unmatched current taxCodes" in {
      val model = TaxCodeChange(
        Seq(taxCodeRecord1, taxCodeRecord3, taxCodeRecord4, unmatchedPreviousTaxCode),
        Seq(taxCodeRecord2, taxCodeRecord3, taxCodeRecord4, unmatchedCurrentTaxCode)
      )
      model.unpairedCurrentCodes mustEqual Seq(model.TaxCodePair(None, Some(unmatchedCurrentTaxCode)))
    }

    "return the unmatched previous taxCodes given one in current and one in previous" in {
      val model = TaxCodeChange(
        Seq(unmatchedPreviousTaxCode),
        Seq(unmatchedCurrentTaxCode)
      )
      model.unpairedPreviousCodes mustEqual Seq(model.TaxCodePair(Some(unmatchedPreviousTaxCode), None))
    }

    "return the unmatched previous taxCodes" in {
      val model = TaxCodeChange(
        Seq(taxCodeRecord1, taxCodeRecord3, taxCodeRecord4, unmatchedPreviousTaxCode),
        Seq(taxCodeRecord2, taxCodeRecord3, taxCodeRecord4, unmatchedCurrentTaxCode)
      )
      model.unpairedPreviousCodes mustEqual Seq(model.TaxCodePair(Some(unmatchedPreviousTaxCode), None))
    }

    "return the unmatched previous taxCodes given a matching pair" in {
      val model = TaxCodeChange(
        Seq(taxCodeRecord1),
        Seq(taxCodeRecord2)
      )
      model.unpairedPreviousCodes mustEqual Seq.empty
    }
  }

  val nino = generateNino
  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("code", startDate, startDate.plusMonths(1),"A Employer 1", 1, "1234", false)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)
  val taxCodeRecord3 = taxCodeRecord1.copy(startDate = startDate.plusDays(3), endDate = TaxYearResolver.endOfCurrentTaxYear, employerName = "B", employmentId = 2, primary = false)
  val taxCodeRecord4 = taxCodeRecord3.copy(employerName = "C", employmentId = 3, primary = true)
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
