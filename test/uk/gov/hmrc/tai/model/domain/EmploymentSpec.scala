/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json._

import java.time.LocalDate

class EmploymentSpec extends PlaySpec {

  "AddEmployment JSON" must {
    "include payeRef when writing" in {
      val model = AddEmployment(
        employerName = "ACME Ltd",
        startDate = LocalDate.of(2024, 1, 2),
        payrollNumber = "PR-123",
        payeRef = "123/AB456",
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("0123456789")
      )

      val json = Json.toJson(model)

      (json \ "employerName").as[String] mustBe "ACME Ltd"
      (json \ "startDate").isDefined mustBe true
      (json \ "payrollNumber").as[String] mustBe "PR-123"
      (json \ "payeRef").as[String] mustBe "123/AB456"
      (json \ "telephoneContactAllowed").as[String] mustBe "Yes"
      (json \ "telephoneNumber").asOpt[String] mustBe Some("0123456789")
    }

    "round-trip (write then read) with payeRef" in {
      val original = AddEmployment(
        employerName = "Beta Corp",
        startDate = LocalDate.of(2023, 12, 31),
        payrollNumber = "9999",
        payeRef = "321/ZZ999",
        telephoneContactAllowed = "No",
        telephoneNumber = None
      )

      val json   = Json.toJson(original)
      val parsed = json.as[AddEmployment]

      parsed mustBe original
    }
  }
}
