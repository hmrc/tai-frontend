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

class TaxCodeHistorySpec extends PlaySpec{

  "TaxCodeHistory" should {
    "return a valid TaxCodeHistory object when given valid Json" in {
      val expectedModel = TaxCodeHistory(
        taxCodeRecord1,
        taxCodeRecord2
      )

      taxCodeHistoryJson.as[TaxCodeHistory] mustEqual expectedModel
    }

    "throw a JsError given an empty Seq of TaxCodeRecords" in {
      an [JsResultException] should be thrownBy emptyTaxCodeRecordsJson.as[TaxCodeHistory]
    }

    "return the latest tax code change date from a sequence of tax code records" in {
      val expectedModel = TaxCodeHistory(taxCodeRecord1, taxCodeRecord2)

      expectedModel.mostRecentTaxCodeChangeDate mustEqual startDate.plusDays(2)

    }
  }

  val nino = generateNino
  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("code", startDate, startDate.plusDays(1),"Employer 1")
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(2), endDate = TaxYearResolver.endOfCurrentTaxYear)

  val taxCodeHistoryJson = Json.obj(
    "previous" -> Json.obj(
      "taxCode" -> "code",
      "startDate" -> startDate.toString,
      "endDate" -> startDate.plusDays(1).toString,
      "employerName" -> "Employer 1"
    ),
    "current" -> Json.obj(
      "taxCode" -> "code",
      "startDate" -> startDate.plusDays(2).toString,
      "endDate" -> TaxYearResolver.endOfCurrentTaxYear.toString,
      "employerName" -> "Employer 1"
    )
  )

  val emptyTaxCodeRecordsJson = Json.obj(
    "nino" -> nino.nino,
    "taxCodeRecord" -> Seq(JsNull)
  )

  private def generateNino: Nino = new Generator(new Random).nextNino
}
