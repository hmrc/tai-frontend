/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.tai.model.domain.income.{OtherBasisOfOperation, Week1Month1BasisOfOperation}

class TaxCodeRecordSpec extends PlaySpec {

  "TaxCodeRecord" should {
    "return a valid Emergency TaxCodeRecord object when given valid Json" in {
      val expectedModel = TaxCodeRecord(
        "code",
        new LocalDate(2018, 7, 11),
        new LocalDate(2018, 7, 11),
        Week1Month1BasisOfOperation,
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
        new LocalDate(2018, 7, 11),
        new LocalDate(2018, 7, 11),
        OtherBasisOfOperation,
        "Employer name",
        false,
        Some("1234"),
        true
      )
      taxCodeRecordJson.as[TaxCodeRecord] mustEqual expectedModel
    }

    "throw an exception when an invalid basis of operation is returned" in {
      intercept[IllegalArgumentException] {
        invalidBasisOfOperation.as[TaxCodeRecord]
      }.getMessage mustEqual "Invalid basis of operation"
    }
  }

  private val emergencyTaxCodeRecordJson =
    Json.obj(
      "taxCode"          -> "code",
      "startDate"        -> "2018-07-11",
      "endDate"          -> "2018-07-11",
      "basisOfOperation" -> "Week1/Month1",
      "employerName"     -> "Employer name",
      "pensionIndicator" -> false,
      "payrollNumber"    -> "1234",
      "primary"          -> true
    )

  private val taxCodeRecordJson =
    Json.obj(
      "taxCode"          -> "code",
      "startDate"        -> "2018-07-11",
      "endDate"          -> "2018-07-11",
      "basisOfOperation" -> "Cumulative",
      "employerName"     -> "Employer name",
      "pensionIndicator" -> false,
      "payrollNumber"    -> "1234",
      "primary"          -> true
    )

  private val invalidBasisOfOperation =
    Json.obj(
      "taxCode"          -> "code",
      "startDate"        -> "2018-07-11",
      "endDate"          -> "2018-07-11",
      "basisOfOperation" -> "some invalid string",
      "employerName"     -> "Employer name",
      "pensionIndicator" -> false,
      "payrollNumber"    -> "1234",
      "primary"          -> true
    )
}
