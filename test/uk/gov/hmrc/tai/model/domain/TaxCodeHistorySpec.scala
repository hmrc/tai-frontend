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

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, JsResultException, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.TaxYear

import scala.util.Random

class TaxCodeHistorySpec extends PlaySpec{

  "TaxCodeHistory" should {
    "return a valid TaxCodeHistory object when given valid Json" in {
      val expectedModel = TaxCodeHistory(nino.nino, Seq(TaxCodeRecord(TaxYear(2018), 1, "A1111", date, date.plusDays(1),"Employer 1")))

      taxCodeHistoryJson.as[TaxCodeHistory] mustEqual expectedModel

    }

    "throw a JsError given an empty Seq of TaxCodeRecords" in {
      an [JsResultException] should be thrownBy emptyTaxCodeRecordsJson.as[TaxCodeHistory]
    }

    "return the latest tax code change date from a sequence of tax code records" in {
      val expectedModel = TaxCodeHistory(nino.nino, Seq(TaxCodeRecord(TaxYear(2018), 1, "A1111", date, date.plusDays(1),"Employer 1"),
                                                        TaxCodeRecord(TaxYear(2018), 1, "A1111", date.plusMonths(1), date.plusMonths(1).plusDays(1),"Employer 1"),
                                                        TaxCodeRecord(TaxYear(2018), 1, "A1111", date.plusMonths(2), date.plusMonths(2).plusDays(1),"Employer 1")))

      expectedModel.mostRecentTaxCodeChangeDate mustEqual date.plusMonths(2)

    }
  }

  val nino = generateNino
  val date = new LocalDate(2018, 5, 23)

  val taxCodeHistoryJson = Json.obj(
    "nino" -> nino.nino,
      "taxCodeRecord" -> Seq(
        Json.obj(
          "taxYear" -> 2018,
          "taxCodeId" -> 1,
          "taxCode" -> "A1111",
          "startDate" -> "2018-05-23",
          "endDate" -> "2018-05-24",
          "employerName" -> "Employer 1"
        )
      )
    )

  val emptyTaxCodeRecordsJson = Json.obj(
    "nino" -> nino.nino,
    "taxCodeRecord" -> Seq(JsNull)
  )

  private def generateNino: Nino = new Generator(new Random).nextNino
}
