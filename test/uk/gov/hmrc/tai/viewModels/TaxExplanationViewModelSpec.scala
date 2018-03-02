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

import hmrc.nps2.{TaxAccount, TaxBand, TaxObject}
import uk.gov.hmrc.tai.model.{TaxCodeDetails, TaxSummaryDetails}
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.{Employments, TaxCodeDetails, TaxSummaryDetails}
import uk.gov.hmrc.tai.model.nps2.TaxDetail
import uk.gov.hmrc.tai.model.tai.AnnualAccount

class TaxExplanationViewModelSpec extends PlaySpec {

  "Tax Explanation view model" must {
    "have non savings non zero bands" in {
      val nonSavingsTaxBand = List(zeroPercentBand, twentyPercentBand)
      val taxSummaryDetails = createTaxSummaryObject(Some(nonSavingsTaxBand))

      val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, false)

      taxExplanationViewModel.nonSavings mustBe List(twentyPercentBand)
    }

    "have savings non zero bands" in {
      val savingsTaxBand = List(zeroPercentBand, fortyFivePercentBand)
      val taxSummaryDetails = createTaxSummaryObject(savingsTaxBand = Some(savingsTaxBand))

      val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, false)

      taxExplanationViewModel.savings mustBe List(fortyFivePercentBand, fortyFivePercentBand, fortyFivePercentBand)
    }

    "have dividends non zero bands" in {
      val dividendsTaxBands = List(zeroPercentBand, fortyPercentBand)
      val taxSummaryDetails = createTaxSummaryObject(dividendsTaxBand = Some(dividendsTaxBands))

      val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, false)

      taxExplanationViewModel.dividends mustBe List(fortyPercentBand, fortyPercentBand)
    }

    "return total tax" in {
      val taxBands = Some(List(zeroPercentBand, twentyPercentBand))
      val taxSummaryDetails = createTaxSummaryObject(taxBands, taxBands, taxBands)

      val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, false)

      taxExplanationViewModel.totalTax mustBe 18000
    }

    "return uk bandType" when {
      "scottish rate is disabled" in {
        val taxBands = Some(List(zeroPercentBand, twentyPercentBand))
        val taxSummaryDetails = createTaxSummaryObject(taxBands, taxBands, taxBands)

        val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, scottishTaxRateEnabled = false)

        taxExplanationViewModel.bandType mustBe "uk.bandtype"
      }

      "scottish rate is enabled and taxCode details are none" in {
        val taxBands = Some(List(zeroPercentBand, twentyPercentBand))
        val taxSummaryDetails = createTaxSummaryObject(taxBands, taxBands, taxBands)

        val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, scottishTaxRateEnabled = true)

        taxExplanationViewModel.bandType mustBe "uk.bandtype"
      }

      "scottish rate is enabled and tax code doesn't start with S" in {
        val taxBands = Some(List(zeroPercentBand, twentyPercentBand))
        val employments = List(
          Employments(id = Some(1), taxCode = Some("1150L")),
          Employments(id = Some(2), taxCode = Some("1100L"))
        )
        val taxCodeDetails = TaxCodeDetails(employment = Some(employments), None, None, None, None, None)
        val taxSummaryDetails = createTaxSummaryObject(taxBands, taxBands, taxBands).copy(taxCodeDetails = Some(taxCodeDetails))
        val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, scottishTaxRateEnabled = true)

        taxExplanationViewModel.bandType mustBe "uk.bandtype"
      }

       "scottish rate is enabled and employments are Nil" in {
        val taxBands = Some(List(zeroPercentBand, twentyPercentBand))
        val employments = List.empty[Employments]
        val taxCodeDetails = TaxCodeDetails(employment = Some(employments), None, None, None, None, None)
        val taxSummaryDetails = createTaxSummaryObject(taxBands, taxBands, taxBands).copy(taxCodeDetails = Some(taxCodeDetails))
        val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, scottishTaxRateEnabled = true)

        taxExplanationViewModel.bandType mustBe "uk.bandtype"
      }
    }

    "return scottish tax band" when {
      "scottish rate is enabled and any tax code start with S" in {
        val taxBands = Some(List(zeroPercentBand, twentyPercentBand))
        val employments = List(
          Employments(id = Some(1), taxCode = Some("S1150L")),
          Employments(id = Some(2), taxCode = Some("1100L"))
        )
        val taxCodeDetails = TaxCodeDetails(employment = Some(employments), None, None, None, None, None)
        val taxSummaryDetails = createTaxSummaryObject(taxBands, taxBands, taxBands).copy(taxCodeDetails = Some(taxCodeDetails))
        val taxExplanationViewModel = TaxExplanationViewModel(taxSummaryDetails, scottishTaxRateEnabled = true)

        taxExplanationViewModel.bandType mustBe "scottish.bandtype"
      }
    }
  }

  private val zeroPercentBand = TaxBand(Some("pa"), None, income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0)
  private val twentyPercentBand = TaxBand(Some("B"), None, income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20)
  private val fortyPercentBand = TaxBand(Some("D0"), None, income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40)
  private val fortyFivePercentBand = TaxBand(Some("D1"), None, income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)

  private def createTaxSummaryObject(nonSavingsTaxBand: Option[List[TaxBand]] = None, savingsTaxBand: Option[List[TaxBand]] = None,
                                     dividendsTaxBand: Option[List[TaxBand]] = None) = {
    val taxObjects: Map[TaxObject.Type.Value, TaxDetail] = Map(
      TaxObject.Type.NonSavings -> TaxDetail(taxBands = nonSavingsTaxBand),
      TaxObject.Type.UntaxedInterest -> TaxDetail(taxBands = savingsTaxBand),
      TaxObject.Type.BankInterest -> TaxDetail(taxBands = savingsTaxBand),
      TaxObject.Type.ForeignInterest -> TaxDetail(taxBands = savingsTaxBand),
      TaxObject.Type.ForeignDividends -> TaxDetail(taxBands = dividendsTaxBand),
      TaxObject.Type.UkDividends -> TaxDetail(taxBands = dividendsTaxBand)
    )
    val taxAccount = TaxAccount(None, None, tax = 0, taxObjects = taxObjects)
    val accounts = List(AnnualAccount(uk.gov.hmrc.tai.model.tai.TaxYear(), Some(taxAccount)))
    TaxSummaryDetails(nino = "", version = 0, accounts = accounts)
  }

}
