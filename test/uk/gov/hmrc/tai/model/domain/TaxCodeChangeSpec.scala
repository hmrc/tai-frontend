/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation

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

    "mostRecentTaxCodeChangeDate" should {
      "return the latest tax code change date from a sequence of tax code records" in {
        val model = TaxCodeChange(Seq(previousTaxCodeRecord1, fullYearTaxCode), Seq(currentTaxCodeRecord1, fullYearTaxCode))

        model.mostRecentTaxCodeChangeDate mustEqual startDate.plusMonths(1).plusDays(1)
      }
    }


    "mostRecentPreviousTaxCodeChangeDate" should {
      "return the latest tax code change date from a sequence of tax code records" in {
        val model = TaxCodeChange(Seq(previousTaxCodeRecord1, fullYearTaxCode), Seq(currentTaxCodeRecord1, fullYearTaxCode))

        model.mostRecentPreviousTaxCodeChangeDate mustEqual startDate
      }
    }

    "uniqueTaxCodes" should {
      "return a seq of unique tax codes found in the previous and current lists" in {
        val model = TaxCodeChange(Seq(previousTaxCodeRecord1, fullYearTaxCode), Seq(currentTaxCodeRecord1, fullYearTaxCode))

        model.uniqueTaxCodes mustEqual Seq("1185L", "OT")
      }
    }
  }

  val nino = generateNino
  val startDate = TaxYear().start
  val previousTaxCodeRecord1 = TaxCodeRecord("1185L", startDate, startDate.plusMonths(1), OtherBasisOfOperation,"A Employer 1", false, Some("1234"), false)
  val currentTaxCodeRecord1 = previousTaxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYear().end)
  val fullYearTaxCode = TaxCodeRecord("OT", startDate, TaxYear().end, OtherBasisOfOperation, "B Employer 1", false, Some("12345"), false)
  val primaryFullYearTaxCode = fullYearTaxCode.copy(employerName = "C", pensionIndicator = false, primary = true)


  val taxCodeChangeJson = Json.obj(
    "previous" -> Json.arr(
      Json.obj(
        "taxCode" -> "1185L",
        "startDate" -> startDate.toString,
        "endDate" -> startDate.plusMonths(1).toString,
        "basisOfOperation" -> "Cumulative",
        "employerName" -> "A Employer 1",
        "pensionIndicator" -> false,
        "payrollNumber" -> "1234",
        "primary" -> false
      )
    ),
    "current" -> Json.arr(
      Json.obj(
        "taxCode" -> "1185L",
        "startDate" -> startDate.plusMonths(1).plusDays(1).toString,
        "endDate" -> TaxYear().end.toString,
        "basisOfOperation" -> "Cumulative",
        "employerName" -> "A Employer 1",
        "pensionIndicator" -> false,
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
