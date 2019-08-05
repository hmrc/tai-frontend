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
import play.api.libs.json.Json
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

class TaxFreeAmountComparisonSpec extends PlaySpec {

  "TaxFreeAmountComparison Reads" must {
    "read the previous and current coding component sequences" when {
      "previous and current are non empty" in {
        val previousCodingComponent = Seq(CodingComponent(CarBenefit, Some(1), 1, "Car Benefit", Some(1)))
        val currentCodingComponent = Seq(CodingComponent(Mileage, Some(2), 100, "Mileage", Some(100)))

        val model = TaxFreeAmountComparison(previousCodingComponent, currentCodingComponent)

        val json = Json.obj(
          "previous" -> Json.arr(
            Json.obj(
              "componentType" -> CarBenefit,
              "employmentId"  -> 1,
              "amount"        -> 1,
              "description"   -> "Car Benefit",
              "iabdCategory"  -> "Benefit",
              "inputAmount"   -> 1
            )
          ),
          "current" -> Json.arr(
            Json.obj(
              "componentType" -> Mileage,
              "employmentId"  -> 2,
              "amount"        -> 100,
              "description"   -> "Mileage",
              "iabdCategory"  -> "Benefit",
              "inputAmount"   -> 100
            )
          )
        )

        json.as[TaxFreeAmountComparison] mustEqual model
      }

      "previous and current are empty" in {
        val model = TaxFreeAmountComparison(Seq.empty, Seq.empty)

        val json = Json.obj(
          "previous" -> Json.arr(),
          "current"  -> Json.arr()
        )

        json.as[TaxFreeAmountComparison] mustEqual model
      }
    }
  }
}
