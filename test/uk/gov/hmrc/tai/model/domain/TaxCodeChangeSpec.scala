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
      val expectedModel = TaxCodeChange(Seq(taxCodeRecord1, taxCodeRecord3), Seq(taxCodeRecord2, taxCodeRecord3))

      expectedModel.mostRecentTaxCodeChangeDate mustEqual startDate.plusMonths(1).plusDays(1)
    }

    "generates pairs of taxCodeChanges based on employmentId" in {
      val expectedModel = TaxCodeChange(Seq(taxCodeRecord1, taxCodeRecord3), Seq(taxCodeRecord2, taxCodeRecord3))
      val generatedPairs = Seq((taxCodeRecord1, taxCodeRecord2), (taxCodeRecord3, taxCodeRecord3))

      expectedModel.generatePairs mustEqual generatedPairs
    }
  }

  val nino = generateNino
  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("code", startDate, startDate.plusMonths(1),"Employer 1", 1, "1234", true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusMonths(1).plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)
  val taxCodeRecord3 = taxCodeRecord1.copy(startDate = startDate.plusDays(3), endDate = TaxYearResolver.endOfCurrentTaxYear, employmentId = 2)

  val taxCodeChangeJson = Json.obj(
    "previous" -> Json.arr(
      Json.obj(
        "taxCode" -> "code",
        "startDate" -> startDate.toString,
        "endDate" -> startDate.plusMonths(1).toString,
        "employerName" -> "Employer 1",
        "employmentId" -> 1,
        "payrollNumber" -> "1234",
        "primary" -> true
      )
    ),
    "current" -> Json.arr(
      Json.obj(
        "taxCode" -> "code",
        "startDate" -> startDate.plusMonths(1).plusDays(1).toString,
        "endDate" -> TaxYearResolver.endOfCurrentTaxYear.toString,
        "employerName" -> "Employer 1",
        "employmentId" -> 1,
        "payrollNumber" -> "1234",
        "primary" -> true
      )
    )
  )

  val emptyTaxCodeRecordsJson = Json.obj(
    "nino" -> nino.nino,
    "taxCodeRecord" -> Seq(JsNull)
  )

  private def generateNino: Nino = new Generator(new Random).nextNino
}
