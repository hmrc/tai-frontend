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

import scala.util.Random

class TaxCodeHistorySpec extends PlaySpec{

  "TaxCodeHistory" should {
    "return a valid TaxCodeHistory object when given valid Json" in {
      val expectedModel = TaxCodeHistory(nino,
        Some(Seq(TaxCodeRecord("1185L","Employer 1","operated","2017-06-23"))))

      taxCodeHistoryJson.as[TaxCodeHistory] mustEqual expectedModel

    }

    "throw a JsError given an empty Seq of TaxCodeRecords" in {
      an [JsResultException] should be thrownBy emptyTaxCodeRecordsJson.as[TaxCodeHistory]
    }
  }

  val nino = generateNino

  val taxCodeHistoryJson = Json.obj(
    "nino" -> nino.nino,
      "taxCodeRecords" -> Seq(
        Json.obj(
          "taxCode" -> "1185L",
          "employerName" -> "Employer 1",
          "operatedTaxCode" -> "operated",
          "p2Date" -> "2017-06-23"
        )
      )
    )

  val emptyTaxCodeRecordsJson = Json.obj(
    "nino" -> nino.nino,
    "taxCodeRecords" -> Seq(JsNull)
  )

  private def generateNino: Nino = new Generator(new Random).nextNino
}
