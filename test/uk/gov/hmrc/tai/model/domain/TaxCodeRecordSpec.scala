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

class TaxCodeRecordSpec extends PlaySpec {

  "TaxCodeRecord" should {
    "return a valid TaxCodeRecord object when given valid Json" in {
      val expectedModel = TaxCodeRecord(
        "code",
        new LocalDate(2018, 7,11),
        new LocalDate(2018, 7, 11),
        "Employer name",
        false,
        "1234",
        true
      )
      taxCodeRecordJson.as[TaxCodeRecord] mustEqual expectedModel

    }
  }

  private val taxCodeRecordJson =
    Json.obj(
      "taxCode" -> "code",
      "startDate" -> "2018-07-11",
      "endDate" -> "2018-07-11",
      "employerName" -> "Employer name",
      "pensionIndicator" -> false,
      "payrollNumber" -> "1234",
      "primary" -> true
    )
}
