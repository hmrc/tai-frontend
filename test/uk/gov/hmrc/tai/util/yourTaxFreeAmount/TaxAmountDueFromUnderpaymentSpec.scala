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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.tax._

class TaxAmountDueFromUnderpaymentSpec extends PlaySpec {

  val totalTaxOwed = 100

  "amountDue" must {
    "Give basic rate" in {
      val taxRate = 20

      val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), taxRate)
      val incomeCategories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
      val totalTax: TotalTax = TotalTax(1, Seq(incomeCategories), None, None, None)

      val actual = TaxAmountDueFromUnderpayment.amountDue(totalTaxOwed, totalTax)
      actual mustBe 20.0
    }

    "Give higher rate" in {
      val taxRate = 40

      val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), taxRate)
      val incomeCategories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
      val totalTax: TotalTax = TotalTax(1, Seq(incomeCategories), None, None, None)

      val actual = TaxAmountDueFromUnderpayment.amountDue(totalTaxOwed, totalTax)
      actual mustBe 40.0
    }

    "throw an exception" when {
      "the tax band is not BasicRate" in {
        val taxBand = TaxBand("SR", "BR", 16500, 1000, Some(0), Some(16500), 20)
        val incomeCategories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
        val totalTax: TotalTax = TotalTax(1, Seq(incomeCategories), None, None, None)

        val exception = the[RuntimeException] thrownBy TaxAmountDueFromUnderpayment.amountDue(totalTaxOwed, totalTax)
        exception.getMessage mustBe "Failed to calculate the tax amount due"
      }

      "the income category is not a NonSavingsIncomeCategory" in {
        val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
        val incomeCategories = IncomeCategory(UntaxedInterestIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
        val totalTax: TotalTax = TotalTax(1, Seq(incomeCategories), None, None, None)

        val exception = the[RuntimeException] thrownBy TaxAmountDueFromUnderpayment.amountDue(totalTaxOwed, totalTax)
        exception.getMessage mustBe "Failed to calculate the tax amount due"
      }
    }
  }
}
