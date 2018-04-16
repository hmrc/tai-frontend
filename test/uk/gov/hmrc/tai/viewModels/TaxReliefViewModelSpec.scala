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

package uk.gov.hmrc.tai.viewModels

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.{BalancingCharge, LoanInterestAmount}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax._

class TaxReliefViewModelSpec extends PlaySpec {

  "Tax Relief Model" must {
    "return hasGiftAid as true and personalPension as false" when {
      "gift aid is present" in {
        val taxReliefComponents = Some(TaxAdjustment(100, Seq(
          TaxAdjustmentComponent(GiftAidPayments, 100)
        )))
        val totalTax = TotalTax(100, Seq.empty[IncomeCategory], None, None, None, None, taxReliefComponents)
        val model = TaxReliefViewModel(Seq.empty[CodingComponent], totalTax)

        model.hasGiftAid mustBe true
        model.hasPersonalPension mustBe false
        model.giftAid mustBe 100
        model.personalPension mustBe 0
        model.giftAidRelief mustBe 0
        model.personalPensionPaymentRelief mustBe 0
      }
    }

    "return hasGiftAid as false and personal pension flag as true" when {
      "gift aid is not present and personal pension is present" in {
        val taxReliefComponents = Some(TaxAdjustment(100, Seq(
          TaxAdjustmentComponent(PersonalPensionPayment, 100)
        )))
        val totalTax = TotalTax(100, Seq.empty[IncomeCategory], None, None, None, None, taxReliefComponents)
        val model = TaxReliefViewModel(Seq.empty[CodingComponent], totalTax)

        model.hasGiftAid mustBe false
        model.hasPersonalPension mustBe true
        model.giftAid mustBe 0
        model.personalPension mustBe 100
      }
    }

    "return gift aid and personal pension relief" in {
      val taxReliefComponents = Some(TaxAdjustment(100, Seq(
        TaxAdjustmentComponent(PersonalPensionPaymentRelief, 100),
        TaxAdjustmentComponent(GiftAidPaymentsRelief, 100)
      )))
      val totalTax = TotalTax(100, Seq.empty[IncomeCategory], None, None, None, None, taxReliefComponents)
      val model = TaxReliefViewModel(Seq.empty[CodingComponent], totalTax)

      model.giftAidRelief mustBe 100
      model.personalPensionPaymentRelief mustBe 100
    }

    "return hasTaxableIncome as true" when {
      "totalIncome is greater than totalAllowance" in {
        val taxBand = Seq(TaxBand(bandType = "", code = "", income = 0, tax = 0, lowerBand = None, upperBand = None, rate = 20))
        val incomeCategories = Seq(
          IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 100, taxBand),
          IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand)
        )
        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

        val codingComponent = CodingComponent(BalancingCharge, None, 10000, "BalancingCharge")
        val model = TaxReliefViewModel(Seq(codingComponent), totalTax)

        model.hasNoTaxableIncome mustBe true
      }
    }

    "return hasTaxableIncome as false" when {
      "totalIncome is less than totalAllowance" in {
        val taxBand = Seq(TaxBand(bandType = "", code = "", income = 0, tax = 0, lowerBand = None, upperBand = None, rate = 20))
        val incomeCategories = Seq(
          IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
        )
        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

        val codingComponent = CodingComponent(LoanInterestAmount, None, 10000, "LoanInterestAmount")

        val model = TaxReliefViewModel(Seq(codingComponent), totalTax)

        model.hasNoTaxableIncome mustBe false
      }
    }
  }

}
