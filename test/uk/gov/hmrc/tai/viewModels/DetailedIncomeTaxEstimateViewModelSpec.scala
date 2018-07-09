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

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, TaxAccountSummary}
import uk.gov.hmrc.tai.model.domain.income.{Live, NonTaxCodeIncome, OtherBasisOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax._
import uk.gov.hmrc.tai.util.BandTypesConstants
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{AdditionalTaxDetailRow, DetailedIncomeTaxEstimateViewModel, ReductionTaxRow}

class DetailedIncomeTaxEstimateViewModelSpec extends PlaySpec with FakeTaiPlayApplication with BandTypesConstants with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "DetailedIncomeTaxEstimateViewModel" when {

    "looking at dividends" when {

      "totalDividendIncome is called" must {

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
      }

      "taxFreeDividendAllowance is called" must {
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
      }

      "return all dividend bands that have an income" in {

        val taxBands = Seq(
          TaxBand(bandType = DividendZeroRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 0),
          TaxBand(bandType = DividendBasicRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 10),
          TaxBand(bandType = DividendHigherRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 20),
          TaxBand(bandType = DividendAdditionalRate, code = "", income = 100, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 30)
        )
        val incomeCategories = Seq(
          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBands)
        )
        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)
        val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

        model.dividends must contain theSameElementsAs(taxBands)

      }
    }



    "return tax bands from all categories" in {
      val taxBand = Seq(TaxBand(bandType = "", code = "", income = 100, tax = 0, lowerBand = None, upperBand = None, rate = 20))
      val incomeCategories = Seq(
        IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
        IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
        IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
        IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
        IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
        IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
      )
      val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

      val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

      model.nonSavings mustEqual Seq(TaxBand(TaxFreeAllowanceBand,"",0,0,Some(0),None,0)) ++ taxBand
      model.savings mustEqual  taxBand ++ taxBand ++ taxBand
      model.dividends mustEqual taxBand ++ taxBand
    }

    "return empty tax bands"  ignore {
      "only zero rate bands are present with no income" in {
        val taxBand = Seq(TaxBand(bandType = "", code = "", income = 100, tax = 0, lowerBand = None, upperBand = None, rate = 0))
        val incomeCategories = Seq(
          IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
          IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
          IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
          IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
          IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
        )
        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

        val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

        model.nonSavings mustBe Seq.empty[TaxBand]
        model.savings mustBe  Seq.empty[TaxBand]
        model.dividends mustBe Seq.empty[TaxBand]
      }

      "income is zero" in {
        val taxBand = Seq(TaxBand(bandType = "", code = "", income = 0, tax = 0, lowerBand = None, upperBand = None, rate = 20))
        val incomeCategories = Seq(
          IncomeCategory(NonSavingsIncomeCategory, 0, 1000, 0, taxBand),
          IncomeCategory(UntaxedInterestIncomeCategory, 0, 2000, 0, taxBand),
          IncomeCategory(ForeignDividendsIncomeCategory, 0, 3000, 0, taxBand),
          IncomeCategory(ForeignInterestIncomeCategory, 0, 4000, 0, taxBand),
          IncomeCategory(BankInterestIncomeCategory, 0, 5000, 0, taxBand),
          IncomeCategory(UkDividendsIncomeCategory, 0, 6000, 0, taxBand)
        )
        val totalTax = TotalTax(100, incomeCategories, None, None, None, None)

        val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

        model.nonSavings mustBe Seq.empty[TaxBand]
        model.savings mustBe  Seq.empty[TaxBand]
        model.dividends mustBe Seq.empty[TaxBand]
      }
    }
  }

  private val totalTax = TotalTax(100, Seq.empty[IncomeCategory], None, None, None)
  private val taxCodeIncomes = Seq.empty[TaxCodeIncome]
  private val taxCodeSummary = TaxAccountSummary(0,0,0,0,0,0,0)
  private val nonTaxCodeIncome = NonTaxCodeIncome(None, Seq.empty)

  val basicModel = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes,taxCodeSummary,Seq.empty,nonTaxCodeIncome)

}
