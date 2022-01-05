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

package uk.gov.hmrc.tai.util

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

class TaxAccountCalculatorSpec extends PlaySpec {

  val taxAccountCalculator: TaxAccountCalculator = new TaxAccountCalculatorImpl

  "taxFreeAmount" must {
    "return zero" when {
      "there is no codingComponent" in {
        val codingComponents = Seq.empty[CodingComponent]
        taxAccountCalculator.taxFreeAmount(codingComponents) mustBe 0
      }
    }
  }

  "taxFreeAmount" must {
    "add allowances to taxFreeAmount value" when {
      "there are allowances in the coding component list" in {
        val codingComponents = Seq(
          CodingComponent(PersonalAllowancePA, Some(234), 11500, "PersonalAllowancePA"),
          CodingComponent(MarriageAllowanceReceived, Some(234), 200, "MarriageAllowanceReceived")
        )

        taxAccountCalculator.taxFreeAmount(codingComponents) mustBe 11700
      }
    }
    "subtract all the other coding componentTypes from taxFreeAmount value" when {
      "there are other coding Components in the tax component list" in {
        val codingComponents = Seq(
          CodingComponent(PersonalAllowancePA, Some(234), 10000, "PersonalAllowancePA"),
          CodingComponent(EmployerProvidedServices, Some(12), 10000, "EmployerProvidedServices"),
          CodingComponent(BenefitInKind, Some(12), 100, "EmployerProvidedServices"),
          CodingComponent(ForeignDividendIncome, Some(12), 200, "ForeignDividendIncome"),
          CodingComponent(Commission, Some(12), 300, "ForeignDividendIncome"),
          CodingComponent(MarriageAllowanceTransferred, Some(31), 10, "MarriageAllowanceTransferred"),
          CodingComponent(UnderPaymentFromPreviousYear, Some(31), 10, "MarriageAllowanceTransferred")
        )

        taxAccountCalculator.taxFreeAmount(codingComponents) mustBe -620
      }
    }
  }
}
