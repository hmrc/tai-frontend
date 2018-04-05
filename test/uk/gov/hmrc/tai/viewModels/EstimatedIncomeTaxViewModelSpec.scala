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
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, TaxBand, TotalTax}

import scala.collection.immutable.Seq

class EstimatedIncomeTaxViewModelSpec extends PlaySpec with FakeTaiPlayApplication {

  "Estimated Income Tax View Model" must {
    val totalTax = TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
    "return tax free allowance and totalTaxableIncome" in {
      val model = EstimatedIncomeTaxViewModel(Seq.empty[CodingComponent], TaxAccountSummary(100, 0, 0, 0, 0, 100, 100),
        totalTax)

      model.taxFreeEstimate mustBe 100
      model.incomeEstimate mustBe 100
    }

    "return totalEstimatedTax" in {
      val model = EstimatedIncomeTaxViewModel(Seq.empty[CodingComponent], TaxAccountSummary(100, 0, 0, 0, 0), totalTax)

      model.incomeTaxEstimate mustBe 100
    }
  }


  "merge Tax bands" must {

    "return None when an empty list is supplied." in {
      val result = EstimatedIncomeTaxViewModel.mergedBands(Nil)
      result mustBe None
    }

    "return empty string in table percentage when tax explanation link is not coming " in {
      val taxBand = List(TaxBand("", "", income = 1000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 20),
        TaxBand("", "", income = 2500, tax = 40, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand("", "", income = 1000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 40),
        TaxBand("", "", income = 2000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 40))

      val dataF = EstimatedIncomeTaxViewModel.mergedBands(taxBand)
      dataF.get mustBe Band("Band", 100, "Check in more detail", 6500, 100, "TaxedIncome")
    }

    "return only one merged tax band for other than zero% rate band" in {
      val taxBand = List(TaxBand("", "", income = 1000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 20),
        TaxBand("", "", income = 2500, tax = 40, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand("", "", income = 1000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 40),
        TaxBand("", "", income = 2000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 40))

      val dataF = EstimatedIncomeTaxViewModel.mergedBands(taxBand)
      dataF.get mustBe Band("Band", 100, "Check in more detail", 6500, 100, "TaxedIncome")
    }
  }

  "individual Tax bands" must {

    "return an empty list when an empty list is supplied." in {
      val result = EstimatedIncomeTaxViewModel.individualBands(Nil)
      result mustBe Nil
    }

    "return two tax bands for 0% rate" in {
      val taxBand = List(TaxBand("PSA", "", income = 1000, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 0),
        TaxBand("B", "", income = 2000, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 0))

      val dataF = EstimatedIncomeTaxViewModel.individualBands(taxBand)
      dataF mustBe List(Band("TaxFree", 20, "0%", 1000, 0, "PSA"), Band("TaxFree", 40, "0%", 2000, 0, "B"))
    }
  }

  "individual other rate Tax bands" must {

    "return an empty list when an empty list is supplied." in {
      val result = EstimatedIncomeTaxViewModel.individualOtherRateBands(Nil)
      result mustBe Nil

    }

    "return two tax bands for 20% and 40% rate" in {
      val taxBand = List(TaxBand("B", "", income = 1000, tax = 200, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand("D0", "", income = 2000, tax = 800, lowerBand = None, upperBand = Some(5000), rate = 40))

      val dataF = EstimatedIncomeTaxViewModel.individualOtherRateBands(taxBand)
      dataF mustBe List(Band("Band", 20, "20%", 1000, 200, "B"), Band("Band", 40, "40%", 2000, 800, "D0"))
    }
  }

  "getUpperBand" must {

    "return 0 when empty list" in {
      val taxBands = Nil
      val upperBand = EstimatedIncomeTaxViewModel.getUpperBand(taxBands)
      upperBand mustBe 0
    }

    "return default value when only pa band has been passed" in {
      val taxBands: List[TaxBand] = List(TaxBand("pa", "", income = 11500, tax = 0, lowerBand = None, upperBand = None, rate = 0))
      val upperBand = EstimatedIncomeTaxViewModel.getUpperBand(taxBands)
      upperBand mustBe 11500
    }

    "return proper upperBand when passed two bands (0&20)" in {
      val taxBands: List[TaxBand] = List(
        TaxBand("PSR", "", income = 3200, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 16000, tax = 5000, lowerBand = Some(11000), upperBand = Some(28800), rate = 20)
      )

      val upperBand = EstimatedIncomeTaxViewModel.getUpperBand(taxBands, taxFreeAllowanceBandSum = 3200)

      upperBand mustBe 32000
    }

    "return income as upperBand when income is greater than upper band" in {
      val taxBands: List[TaxBand] = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val upperBand = EstimatedIncomeTaxViewModel.getUpperBand(taxBands, taxFreeAllowanceBandSum = 5000)

      upperBand mustBe 200000
    }

    "deduct personal allowance from tax-free allowances to get the upper-band if user is in higher rate" in {
      val taxBands = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 100, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40)
      )

      val upperBand = EstimatedIncomeTaxViewModel.getUpperBand(taxBands = taxBands, personalAllowance = Some(5000), taxFreeAllowanceBandSum = 5000)

      upperBand mustBe 150000
    }

    "not deduct personal allowance from tax-free allowances to get the upper-band if user is not in higher rate" in {
      val taxBands = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20)
      )

      val upperBand = EstimatedIncomeTaxViewModel.getUpperBand(taxBands = taxBands, personalAllowance = Some(5000), taxFreeAllowanceBandSum = 5000)

      upperBand mustBe 37000
    }

  }

  "calculate bar percentage" must {

    "return value 0 when no band has passed" in {
      val percentage = EstimatedIncomeTaxViewModel.calcBarPercentage(20000, Nil)

      percentage mustBe 0
    }

    "return valid percentage for first income when two bands has passed" in {
      val taxBands = List(
        TaxBand("PSR", "", income = 3200, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 16000, tax = 5000, lowerBand = Some(11000), upperBand = Some(28800), rate = 20)
      )

      val percentage = EstimatedIncomeTaxViewModel.calcBarPercentage(3200, taxBands, taxFreeAllowanceBandSum = 3200)

      percentage mustBe 10
    }

    "return valid percentage for second income when two bands has passed" in {
      val taxBands = List(
        TaxBand("PSR", "", income = 3200, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 16000, tax = 5000, lowerBand = Some(11000), upperBand = Some(28800), rate = 20)
      )

      val percentage = EstimatedIncomeTaxViewModel.calcBarPercentage(16000, taxBands, taxFreeAllowanceBandSum = 3200)

      percentage mustBe 50
    }

    "return value till two decimal" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 4000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 4000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("D0", "", income = 15000, tax = 3000, lowerBand = Some(14000), upperBand = Some(32000), rate = 20)
      )

      val percentage = EstimatedIncomeTaxViewModel.calcBarPercentage(15000, taxBand, taxFreeAllowanceBandSum = 4000)

      percentage mustBe 41.66

    }

  }

  "retrieve tax bands" must {

    "return tax bands" in {
      val taxBand = List(
        TaxBand("pa", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )


      val taxBands = EstimatedIncomeTaxViewModel.retrieveTaxBands(taxBand)

      taxBands mustBe taxBand

    }

    "return sorted tax bands" in {
      val taxBand = List(
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40)
      )

      val taxBands = EstimatedIncomeTaxViewModel.retrieveTaxBands(taxBand)

      taxBands mustBe List(
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )
    }

    "return merged tax bands for multiple PSR bands" in {
      val bankIntTaxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

      val untaxedTaxBand = List(TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0),
        upperBand = Some(11000), rate = 0))


      val taxBands = EstimatedIncomeTaxViewModel.retrieveTaxBands(bankIntTaxBand ::: untaxedTaxBand)

      taxBands mustBe List(TaxBand("PSR", "", 10000, 0, Some(0), Some(11000), 0),
        TaxBand("B", "", 15000, 3000, Some(11000), Some(32000), 20))
    }

    "return ordered tax bands for multiple PSR SR SDR bands" in {
      val bankIntTaxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20))

      val untaxedTaxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SDR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0))


      val taxBands = EstimatedIncomeTaxViewModel.retrieveTaxBands(bankIntTaxBand ::: untaxedTaxBand)

      val resBands = List(TaxBand("SR", "", 5000, 0, Some(0), Some(11000), 0),
        TaxBand("PSR", "", 10000, 0, Some(0), Some(11000), 0),
        TaxBand("SDR", "", 5000, 0, Some(0), Some(11000), 0),
        TaxBand("B", "", 15000, 3000, Some(11000), Some(32000), 20))

      taxBands mustBe resBands
    }
  }

  "bandedGraph" must {

    "return an empty BandedGraph with Nil bands and values set to zero when an empty list is supplied." in {

      val result = EstimatedIncomeTaxViewModel.createBandedGraph(Nil)

      result mustBe BandedGraph("taxGraph", Nil, 0, 0, 0, 0, 0, 0, 0, None)

    }

    "have two bands(0&20) to display in graph" in {

      val taxBand = List(
        TaxBand("pa", "", income = 3200, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 16000, tax = 5000, lowerBand = Some(11000), upperBand = Some(28800), rate = 20)
      )

      val bands = List(Band("TaxFree", 10.00, "0%", 3200, 0, "pa"),
        Band("Band", 50.00, "20%", 16000, 5000, "B"))

      val nextBandMessage = Some("You can have £12,800 more before your income reaches the next tax band.")

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 3200)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 32000, 19200, 10.00, 3200, 60.00, 5000, nextBandMessage)
    }

    "have two bands(0 & Taxed Income) to display in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 3000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 30000, tax = 12000, lowerBand = Some(32000), upperBand = Some(147000), rate = 40)
      )

      val bands = List(
        Band("TaxFree", 2.00, "0%", 3000, 0, "PSR"),
        Band("Band", 30.00, "Check in more detail", 45000, 15000, "TaxedIncome")
      )

      val nextBandMessage = Some("You can have £102,000 more before your income reaches the next tax band.")
      val links = Map("taxExplanationScreen" -> "Check in more detail")
      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 3000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 150000, 48000, 2.00, 3000, 32.00, 15000, nextBandMessage)
    }

    "have two bands(0 & Taxed Income) for multiple other band to display in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val bands = List(
        Band("TaxFree", 2.5, "0%", 5000, 0, "PSR"),
        Band("Band", 97.5, "Check in more detail", 195000, 65250, "TaxedIncome")
      )

      val links = Map("taxExplanationScreen" -> "Check in more detail")
      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 5000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 200000, 200000, 2.5, 5000, 100, 65250)
    }

    "have three bands as 20% 40% 45% for three other rate bands to display in graph" in {

      val taxBand = List(
        TaxBand("B", "", income = 20000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val bands = List(
        Band("Band", 10.00, "20%", 20000, 3000, "B"),
        Band("Band", 75.00, "40%", 150000, 60000, "D0"),
        Band("Band", 15.00, "45%", 30000, 2250, "D1")
      )

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand)

      dataF mustBe BandedGraph("taxGraph", bands, 0, 200000, 200000, 0, 0, 100.00, 65250)
    }

    "have four bands as 20% 40% 45% 45% for four other rate bands to display in graph" in {

      val taxBand = List(
        TaxBand("B", "", income = 20000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45),
        TaxBand("HSR2", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val bands = List(
        Band("Band", 8.69, "20%", 20000, 3000, "B"),
        Band("Band", 65.21, "40%", 150000, 60000, "D0"),
        Band("Band", 13.04, "45%", 30000, 2250, "D1"),
        Band("Band", 13.04, "45%", 30000, 2250, "HSR2")
      )

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand)

      dataF mustBe BandedGraph("taxGraph", bands, 0, 230000, 230000, 0, 0, 99.98, 67500)
    }

    "have two bands as 20% 40% for two other rate bands to display in graph" in {
      val taxBand = List(
        TaxBand("B", "", income = 33500, tax = 6700, lowerBand = Some(11000), upperBand = Some(33500), rate = 20),
        TaxBand("D0", "", income = 91500, tax = 36600, lowerBand = Some(33500), upperBand = Some(150000), rate = 40)
      )

      val nextBandMessage = Some("You can have £25,000 more before your income reaches the next tax band.")

      val bands = List(
        Band("Band", 22.33, "20%", 33500, 6700, "B"),
        Band("Band", 61.00, "40%", 91500, 36600, "D0")
      )

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand)

      dataF mustBe BandedGraph("taxGraph", bands, 0, 150000, 125000, 0, 0, 83.33, 43300, nextBandMessage)
    }

    "have two 0 % band and one 20% band in graph" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 4000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 4000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("D0", "", income = 15000, tax = 3000, lowerBand = Some(14000), upperBand = Some(32000), rate = 20)
      )

      val bands = List(
        Band("TaxFree", 11.11, "0%", 4000, 0, "PSR"),
        Band("TaxFree", 11.11, "0%", 4000, 0, "SR"),
        Band("Band", 41.66, "20%", 15000, 3000, "D0")
      )

      val nextBandMessage = Some("You can have £13,000 more before your income reaches the next tax band.")

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 4000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 36000, 23000, 22.22, 8000, 63.88, 3000, nextBandMessage)
    }

    "have two 0 % band and one Taxed Income band in graph" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 10000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 10000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("B", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(32000), rate = 20),
        TaxBand("B", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(30000), rate = 20)
      )

      val bands = List(
        Band("TaxFree", 25, "0%", 10000, 0, "PSR"),
        Band("TaxFree", 25, "0%", 10000, 0, "SR"),
        Band("Band", 50, "Check in more detail", 20000, 6000, "TaxedIncome")
      )

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 10000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 40000, 40000, 50, 20000, 100, 6000)
    }

    "have two 0 % band and one 7.5% band in graph" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 11000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 3000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("SDR", "", income = 15000, tax = 2000, lowerBand = Some(14000), upperBand = Some(18000), rate = 7.5)
      )

      val bands = List(
        Band("TaxFree", 37.93, "0%", 11000, 0, "PSR"),
        Band("TaxFree", 10.34, "0%", 3000, 0, "SR"),
        Band("Band", 51.72, "7.5%", 15000, 2000, "SDR")
      )

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 11000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 29000, 29000, 48.27, 14000, 99.99, 2000)
    }

    "have three 0 % band and zero other band in graph" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 11000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 3000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("SDR", "", income = 15000, tax = 0, lowerBand = Some(14000), upperBand = Some(32000), rate = 0)
      )

      val bands = List(
        Band("TaxFree", 25.58, "0%", 11000, 0, "PSR"),
        Band("TaxFree", 6.97, "0%", 3000, 0, "SR"),
        Band("TaxFree", 34.88, "0%", 15000, 0, "SDR")
      )

      val nextBandMessage = Some("You can have £14,000 more before your income reaches the next tax band.")

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 11000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 43000, 29000, 67.43, 29000, 67.43, 0, nextBandMessage)
    }

    "have two 0 % band and one Taxed Income band(7.5 & 20 ) in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 10000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 10000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("SDR", "", income = 10000, tax = 750, lowerBand = Some(14000), upperBand = Some(32000), rate = 7.5),
        TaxBand("B", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(30000), rate = 20)
      )

      val bands = List(
        Band("TaxFree", 25.00, "0%", 10000, 0, "PSR"),
        Band("TaxFree", 25.00, "0%", 10000, 0, "SR"),
        Band("Band", 50.00, "Check in more detail", 20000, 3750, "TaxedIncome")
      )

      val links = Map("taxExplanationScreen" -> "Check in more detail")
      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 10000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 40000, 40000, 50.00, 20000, 100.00, 3750)
    }

    "have two 0 % band and one Taxed Income band(7.5 & 20 & 45) in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 10000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 10000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("B", "", income = 10000, tax = 750, lowerBand = Some(14000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(100000), rate = 40),
        TaxBand("D1", "", income = 20000, tax = 3000, lowerBand = Some(100000), upperBand = Some(0), rate = 45)
      )

      val bands = List(
        Band("TaxFree", 9.09, "0%", 10000, 0, "PSR"),
        Band("TaxFree", 9.09, "0%", 10000, 0, "SR"),
        Band("Band", 36.36, "Check in more detail", 40000, 6750, "TaxedIncome")
      )

      val nextBandMessage = Some("You can have £50,000 more before your income reaches the next tax band.")

      val links = Map("taxExplanationScreen" -> "Check in more detail")
      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 10000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 110000, 60000, 18.18, 20000, 54.54, 6750, nextBandMessage)
    }

    "have two 0 % band and one Taxed Income band(7.5 & 20 & 45 & 60) in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 10000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 10000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("D0", "", income = 10000, tax = 750, lowerBand = Some(14000), upperBand = Some(32000), rate = 20),
        TaxBand("D1", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(100000), rate = 40),
        TaxBand("D2", "", income = 40000, tax = 3000, lowerBand = Some(100000), upperBand = Some(200000), rate = 45),
        TaxBand("D3", "", income = 40000, tax = 3000, lowerBand = Some(200000), upperBand = Some(0), rate = 60)
      )

      val bands = List(
        Band("TaxFree", 4.76, "0%", 10000, 0, "PSR"),
        Band("TaxFree", 4.76, "0%", 10000, 0, "SR"),
        Band("Band", 47.61, "Check in more detail", 100000, 9750, "TaxedIncome")
      )

      val nextBandMessage = Some("You can have £90,000 more before your income reaches the next tax band.")

      val links = Map("taxExplanationScreen" -> "Check in more detail")
      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(taxBand, taxFreeAllowanceBandSum = 10000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 210000, 120000, 9.52, 20000, 57.13, 9750, nextBandMessage)
    }

    "have tax-free allowance on the top in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 10000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("D0", "", income = 10000, tax = 750, lowerBand = Some(14000), upperBand = Some(32000), rate = 20),
        TaxBand("D1", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(100000), rate = 40),
        TaxBand("D2", "", income = 40000, tax = 3000, lowerBand = Some(100000), upperBand = Some(200000), rate = 45),
        TaxBand("D3", "", income = 40000, tax = 3000, lowerBand = Some(200000), upperBand = Some(0), rate = 60)
      )

      val bands = List(
        Band("TaxFree", 4.76, "0%", 10000, 0, "SR"),
        Band("TaxFree", 4.76, "0%", 10000, 0, "PSR"),
        Band("Band", 47.61, "Check in more detail", 100000, 9750, "TaxedIncome")
      )

      val nextBandMessage = Some("You can have £90,000 more before your income reaches the next tax band.")

      val dataF = EstimatedIncomeTaxViewModel.createBandedGraph(EstimatedIncomeTaxViewModel.retrieveTaxBands(taxBand), taxFreeAllowanceBandSum = 10000)
      dataF mustBe BandedGraph("taxGraph", bands, 0, 210000, 120000, 9.52, 20000, 57.13, 9750, nextBandMessage)
    }


  }


}
