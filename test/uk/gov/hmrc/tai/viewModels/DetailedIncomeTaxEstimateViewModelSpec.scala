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
import uk.gov.hmrc.tai.model.domain.EmploymentIncome
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.util.BandTypesConstants
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.DetailedIncomeTaxEstimateViewModel

class DetailedIncomeTaxEstimateViewModelSpec extends PlaySpec with BandTypesConstants {

  "DetailedIncomeTaxEstimateViewModel" must {
//
//    "return the total income from dividends " in {
//
//    }

    "return the total income from dividends" in {
      val taxBand = Seq(TaxBand(bandType = "", code = "", income = 100, tax = 0, lowerBand = None, upperBand = None, rate = 20))

      val incomeCategories = Seq(
                IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 10, taxBand),
                IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 20, taxBand),
                IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 4000, taxBand),
                IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 30, taxBand),
                IncomeCategory(BankInterestIncomeCategory, 0, 5000, 40, taxBand),
                IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 5000, taxBand)
              )

      DetailedIncomeTaxEstimateViewModel.totalDividendIncome(incomeCategories) mustEqual 9000

    }

    "return the tax free dividend allowance when there is one" in {

      val taxBand = Seq(
        TaxBand(bandType = DividendZeroRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand(bandType = PersonalSavingsRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand(bandType = StarterSavingsRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20)
      )

      val incomeCategories = Seq(
        IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 10, taxBand),
        IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 20, taxBand),
        IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 4000, taxBand),
        IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 30, taxBand),
        IncomeCategory(BankInterestIncomeCategory, 0, 5000, 40, taxBand),
        IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 5000, taxBand)
      )

      DetailedIncomeTaxEstimateViewModel.taxFreeDividendAllowance(incomeCategories) mustEqual 5000

    }

    "return zero free dividend allowance when there is not one" in {

      val taxBand = Seq(
        TaxBand(bandType = PersonalSavingsRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand(bandType = StarterSavingsRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20)
      )

      val incomeCategories = Seq(
        IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 5000, taxBand)
      )

      DetailedIncomeTaxEstimateViewModel.taxFreeDividendAllowance(incomeCategories) mustEqual 0

    }



//    "return tax bands from all categories" in {
//      val taxBand = Seq(TaxBand(bandType = "", code = "", income = 100, tax = 0, lowerBand = None, upperBand = None, rate = 20))
//      val incomeCategories = Seq(
//        IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
//        IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
//        IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
//        IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
//        IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
//        IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
//      )
//      val totalTax = TotalTax(100, incomeCategories, None, None, None, None)
//
//      val model = TaxExplanationViewModel(totalTax, Seq.empty[TaxCodeIncome])
//
//      model.nonSavings mustBe taxBand
//      model.savings mustBe  taxBand ++ taxBand ++ taxBand
//      model.dividends mustBe taxBand ++ taxBand
//    }
//
//    "return empty tax bands"  when {
//      "only zero rate bands are present" in {
//        val taxBand = Seq(TaxBand(bandType = "", code = "", income = 100, tax = 0, lowerBand = None, upperBand = None, rate = 0))
//        val incomeCategories = Seq(
//          IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
//          IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
//          IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
//          IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
//          IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
//          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
//        )
//        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)
//
//        val model = TaxExplanationViewModel(totalTax, Seq.empty[TaxCodeIncome])
//
//        model.nonSavings mustBe Seq.empty[TaxBand]
//        model.savings mustBe  Seq.empty[TaxBand]
//        model.dividends mustBe Seq.empty[TaxBand]
//      }
//
//      "income is zero" in {
//        val taxBand = Seq(TaxBand(bandType = "", code = "", income = 0, tax = 0, lowerBand = None, upperBand = None, rate = 20))
//        val incomeCategories = Seq(
//          IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
//          IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
//          IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
//          IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
//          IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
//          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
//        )
//        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)
//
//        val model = TaxExplanationViewModel(totalTax, Seq.empty[TaxCodeIncome])
//
//        model.nonSavings mustBe Seq.empty[TaxBand]
//        model.savings mustBe  Seq.empty[TaxBand]
//        model.dividends mustBe Seq.empty[TaxBand]
//      }
//    }
//
//    "return band type" when {
//      "tax code contains S" in {
//        val taxCodeIncomes = Seq(
//          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOperation, Live),
//          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1150L", "employer", OtherBasisOperation, Live)
//        )
//
//        val model = TaxExplanationViewModel(TotalTax(100, Seq.empty[IncomeCategory], None, None, None), taxCodeIncomes)
//
//        model.bandType mustBe ScottishBands
//      }
//
//      "tax code doesn't contains S" in {
//        val taxCodeIncomes = Seq(
//          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1050L", "employer", OtherBasisOperation, Live),
//          TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "1150L", "employer", OtherBasisOperation, Live)
//        )
//
//        val model = TaxExplanationViewModel(TotalTax(100, Seq.empty[IncomeCategory], None, None, None), taxCodeIncomes)
//
//        model.bandType mustBe UkBands
//      }
//    }
  }


}
