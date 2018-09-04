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
import play.api.libs.json.Json
import uk.gov.hmrc.tai.model.domain.income.{OtherBasisOperation, Week1Month1BasisOperation}

class TaxCodeRecordSpec extends PlaySpec {

  "TaxCodeRecord" should {
    "return a valid Emergency TaxCodeRecord object when given valid Json" in {
      val expectedModel = TaxCodeRecord(
        "code",
        new LocalDate(2018, 7,11),
        new LocalDate(2018, 7, 11),
        Week1Month1BasisOperation,
        "Employer name",
        false,
        Some("1234"),
        true
      )
      emergencyTaxCodeRecordJson.as[TaxCodeRecord] mustEqual expectedModel
    }

    "return a valid TaxCodeRecord object when given valid Json" in {
      val expectedModel = TaxCodeRecord(
        "code",
        new LocalDate(2018, 7,11),
        new LocalDate(2018, 7, 11),
        OtherBasisOperation,
        "Employer name",
        false,
        Some("1234"),
        true
      )
      taxCodeRecordJson.as[TaxCodeRecord] mustEqual expectedModel
    }
  }

  private val emergencyTaxCodeRecordJson =
    Json.obj(
      "taxCode" -> "code",
      "startDate" -> "2018-07-11",
      "endDate" -> "2018-07-11",
      "basisOfOperation" -> "Week 1 Month 1",
      "employerName" -> "Employer name",
      "pensionIndicator" -> false,
      "payrollNumber" -> "1234",
      "primary" -> true
    )

  private val taxCodeRecordJson =
    Json.obj(
      "taxCode" -> "code",
      "startDate" -> "2018-07-11",
      "endDate" -> "2018-07-11",
      "basisOfOperation" -> "Cumulative",
      "employerName" -> "Employer name",
      "pensionIndicator" -> false,
      "payrollNumber" -> "1234",
      "primary" -> true
    )
}
