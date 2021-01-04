/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.domain.tax

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json}

class TotalTaxSpec extends PlaySpec {

  "IncomeCategoryType" must {
    "be able to parse json" when {
      "given a valid json with NonSavingsIncomeCategory" in {
        incomeCategoryJson("NonSavingsIncomeCategory")
          .as[IncomeCategory] mustBe IncomeCategory(NonSavingsIncomeCategory, 10, 10, 10, Nil)
      }

      "given a valid json with UntaxedInterestIncomeCategory" in {
        incomeCategoryJson("UntaxedInterestIncomeCategory")
          .as[IncomeCategory] mustBe IncomeCategory(UntaxedInterestIncomeCategory, 10, 10, 10, Nil)
      }

      "given a valid json with BankInterestIncomeCategory" in {
        incomeCategoryJson("BankInterestIncomeCategory")
          .as[IncomeCategory] mustBe IncomeCategory(BankInterestIncomeCategory, 10, 10, 10, Nil)
      }

      "given a valid json with UkDividendsIncomeCategory" in {
        incomeCategoryJson("UkDividendsIncomeCategory")
          .as[IncomeCategory] mustBe IncomeCategory(UkDividendsIncomeCategory, 10, 10, 10, Nil)
      }

      "given a valid json with ForeignInterestIncomeCategory" in {
        incomeCategoryJson("ForeignInterestIncomeCategory")
          .as[IncomeCategory] mustBe IncomeCategory(ForeignInterestIncomeCategory, 10, 10, 10, Nil)
      }

      "given a valid json with ForeignDividendsIncomeCategory" in {
        incomeCategoryJson("ForeignDividendsIncomeCategory")
          .as[IncomeCategory] mustBe IncomeCategory(ForeignDividendsIncomeCategory, 10, 10, 10, Nil)
      }

      "throw an exception" when {
        "given an invalid json value" in {
          val exception = the[IllegalArgumentException] thrownBy incomeCategoryJson("invalid").as[IncomeCategory]
          exception.getMessage mustBe "Invalid income category type"
        }
      }
    }
  }

  private def incomeCategoryJson(incomeCategoryType: String) =
    Json.obj(
      "incomeCategoryType" -> incomeCategoryType,
      "totalTax"           -> 10,
      "totalTaxableIncome" -> 10,
      "totalIncome"        -> 10,
      "taxBands"           -> Json.arr()
    )

}
