/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.libs.json.*

import java.time.LocalDate

class TaxAccountSummarySpec extends PlaySpec {

  "TaxAccountSummary Reads" must {

    "read all fields when values are present" in {

      val expectedModel = TaxAccountSummary(
        totalEstimatedTax = BigDecimal(1000),
        taxFreeAmount = BigDecimal(12570),
        totalInYearAdjustmentIntoCY = BigDecimal(100),
        totalInYearAdjustment = BigDecimal(200),
        totalInYearAdjustmentIntoCYPlusOne = BigDecimal(300),
        totalEstimatedIncome = BigDecimal(50000),
        taxFreeAllowance = BigDecimal(12570),
        date = Some(LocalDate.of(2026, 1, 1))
      )

      val json = Json.obj(
        "totalEstimatedTax"                  -> 1000,
        "taxFreeAmount"                      -> 12570,
        "totalInYearAdjustmentIntoCY"        -> 100,
        "totalInYearAdjustment"              -> 200,
        "totalInYearAdjustmentIntoCYPlusOne" -> 300,
        "totalEstimatedIncome"               -> 50000,
        "taxFreeAllowance"                   -> 12570,
        "date"                               -> "2026-01-01"
      )

      json.as[TaxAccountSummary] mustEqual expectedModel
    }

    "default nullable fields to zero when they are null" in {

      val expectedModel = TaxAccountSummary(
        totalEstimatedTax = BigDecimal(0),
        taxFreeAmount = BigDecimal(0),
        totalInYearAdjustmentIntoCY = BigDecimal(100),
        totalInYearAdjustment = BigDecimal(200),
        totalInYearAdjustmentIntoCYPlusOne = BigDecimal(300),
        totalEstimatedIncome = BigDecimal(0),
        taxFreeAllowance = BigDecimal(0),
        date = None
      )

      val json = Json.obj(
        "totalEstimatedTax"                  -> JsNull,
        "taxFreeAmount"                      -> JsNull,
        "totalInYearAdjustmentIntoCY"        -> 100,
        "totalInYearAdjustment"              -> 200,
        "totalInYearAdjustmentIntoCYPlusOne" -> 300,
        "totalEstimatedIncome"               -> JsNull,
        "taxFreeAllowance"                   -> JsNull,
        "date"                               -> JsNull
      )

      json.as[TaxAccountSummary] mustEqual expectedModel
    }

    "default nullable fields to zero when they are missing" in {

      val expectedModel = TaxAccountSummary(
        totalEstimatedTax = BigDecimal(0),
        taxFreeAmount = BigDecimal(0),
        totalInYearAdjustmentIntoCY = BigDecimal(100),
        totalInYearAdjustment = BigDecimal(200),
        totalInYearAdjustmentIntoCYPlusOne = BigDecimal(300),
        totalEstimatedIncome = BigDecimal(0),
        taxFreeAllowance = BigDecimal(0),
        date = None
      )

      val json = Json.obj(
        "totalInYearAdjustmentIntoCY"        -> 100,
        "totalInYearAdjustment"              -> 200,
        "totalInYearAdjustmentIntoCYPlusOne" -> 300
      )

      json.as[TaxAccountSummary] mustEqual expectedModel
    }
  }
}
