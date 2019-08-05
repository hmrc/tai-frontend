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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import controllers.{FakeTaiPlayApplication, routes}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, TaxAdjustmentComponent, TaxBand, TotalTax}
import uk.gov.hmrc.tai.util.constants.{BandTypesConstants, TaxRegionConstants}
import uk.gov.hmrc.urls.Link
import views.html.includes.link

import scala.collection.immutable.Seq
import scala.language.postfixOps

class EstimatedIncomeViewModelSpec
    extends PlaySpec with FakeTaiPlayApplication with I18nSupport with TaxRegionConstants with BandTypesConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Banded Graph" must {

    "return an empty BandedGraph with Nil bands and values set to zero when an empty list is supplied." in {

      val result = BandedGraph(Seq.empty[CodingComponent], List.empty[TaxBand], 0, 0, 0, taxViewType = ZeroTaxView)

      result mustBe BandedGraph("taxGraph", Nil, 0, 0, 0, 0, 0, 0, 0, None, None)

    }

    "have two bands(0&NextBand) to display in graph" in {

      val taxBand = List(
        TaxBand("pa", "", income = 11500, tax = 0, lowerBand = Some(0), upperBand = None, rate = 0)
      )

      val bands = List(Band("TaxFree", 78.26, 11500, 0, "pa"))

      val graph = BandedGraph(Seq.empty[CodingComponent], taxBand, 11500, 0, 9000, taxViewType = ZeroTaxView)

      graph mustBe BandedGraph("taxGraph", bands, 0, 11500, 11500, 78.26, 11500, 78.26, 0, None, None)

    }

    "have two bands(0&20) to display in graph" in {

      val taxBand = List(
        TaxBand("pa", "", income = 3200, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 16000, tax = 5000, lowerBand = Some(11000), upperBand = Some(28800), rate = 20)
      )

      val bands = List(Band("TaxFree", 16.66, 3200, 0, ZeroBand), Band("Band", 83.33, 16000, 5000, NonZeroBand))

      val nextBandMessage = Some(Messages("tai.taxCalc.nextTaxBand", 12800))

      val dataF = BandedGraph(Seq.empty[CodingComponent], taxBand, 3200, 5000, taxViewType = SimpleTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        32000,
        19200,
        16.66,
        3200,
        99.99,
        5000,
        nextBandMessage,
        Some(Swatch(26.04, 5000)))
    }

    "have two bands(0 & Taxed Income) to display in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 3000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 30000, tax = 12000, lowerBand = Some(32000), upperBand = Some(147000), rate = 40)
      )

      val bands = List(
        Band("TaxFree", 6.25, 3000, 0, ZeroBand),
        Band("Band", 93.75, 45000, 15000, NonZeroBand)
      )

      val nextBandMessage = Some(Messages("tai.taxCalc.nextTaxBand", 102000))
      val dataF = BandedGraph(Seq.empty[CodingComponent], taxBand, 3000, 15000, taxViewType = ComplexTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        150000,
        48000,
        6.25,
        3000,
        100.00,
        15000,
        nextBandMessage,
        Some(Swatch(31.25, 15000)))
    }

    "have two bands(0 & Taxed Income) for multiple other band to display in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val bands = List(
        Band("TaxFree", 2.5, 5000, 0, ZeroBand),
        Band("Band", 97.5, 195000, 65250, NonZeroBand)
      )

      val dataF = BandedGraph(Seq.empty[CodingComponent], taxBand, 5000, 65250, taxViewType = ComplexTaxView)

      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        200000,
        200000,
        2.5,
        5000,
        100,
        65250,
        None,
        Some(Swatch(32.62, 65250)))
    }

    "have three bands as 20% 40% 45% for three other rate bands to display in graph" in {

      val taxBand = List(
        TaxBand("B", "", income = 20000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val bands = List(
        Band("Band", 10.00, 20000, 3000, "B"),
        Band("Band", 75.00, 150000, 60000, "D0"),
        Band("Band", 15.00, 30000, 2250, "D1")
      )

      val dataF = BandedGraph(Seq.empty[CodingComponent], taxBand, 0, 65250, taxViewType = SimpleTaxView)

      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        200000,
        200000,
        0,
        0,
        100.00,
        65250,
        None,
        Some(Swatch(32.62, 65250)))
    }

    "have four bands as 20% 40% 45% 45% for four other rate bands to display in graph" in {

      val taxBand = List(
        TaxBand("B", "", income = 20000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45),
        TaxBand("HSR2", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val bands = List(
        Band("Band", 8.69, 20000, 3000, "B"),
        Band("Band", 65.21, 150000, 60000, "D0"),
        Band("Band", 13.04, 30000, 2250, "D1"),
        Band("Band", 13.04, 30000, 2250, "HSR2")
      )

      val dataF = BandedGraph(Seq.empty[CodingComponent], taxBand, 0, 67500, taxViewType = ComplexTaxView)

      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        230000,
        230000,
        0,
        0,
        99.98,
        67500,
        None,
        Some(Swatch(29.34, 67500)))
    }

    "have two bands as 20% 40% for two other rate bands to display in graph" in {
      val taxBand = List(
        TaxBand("B", "", income = 33500, tax = 6700, lowerBand = Some(11000), upperBand = Some(33500), rate = 20),
        TaxBand("D0", "", income = 91500, tax = 36600, lowerBand = Some(33500), upperBand = Some(150000), rate = 40)
      )

      val nextBandMessage = Some(Messages("tai.taxCalc.nextTaxBand", 25000))

      val bands = List(
        Band("Band", 26.80, 33500, 6700, "B"),
        Band("Band", 73.20, 91500, 36600, "D0")
      )

      val dataF =
        BandedGraph(Seq.empty[CodingComponent], taxBand, totalEstimatedTax = 43300, taxViewType = SimpleTaxView)

      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        150000,
        125000,
        0,
        0,
        100.00,
        43300,
        nextBandMessage,
        Some(Swatch(34.64, 43300)))
    }

    "have two 0 % band and one 20% band in graph" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 4000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 4000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("D0", "", income = 15000, tax = 3000, lowerBand = Some(14000), upperBand = Some(32000), rate = 20)
      )

      val bands = List(
        Band("TaxFree", 34.78, 8000, 0, ZeroBand),
        Band("Band", 65.21, 15000, 3000, NonZeroBand)
      )

      val nextBandMessage = Some(Messages("tai.taxCalc.nextTaxBand", 13000))

      val dataF = BandedGraph(
        Seq.empty[CodingComponent],
        taxBand,
        taxFreeAllowanceBandSum = 4000,
        totalEstimatedTax = 3000,
        taxViewType = ComplexTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        36000,
        23000,
        34.78,
        8000,
        99.99,
        3000,
        nextBandMessage,
        Some(Swatch(13.04, 3000)))
    }

    "have two 0 % band and one Taxed Income band in graph" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 10000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 10000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("B", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(32000), rate = 20),
        TaxBand("B", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(30000), rate = 20)
      )

      val bands = List(
        Band("TaxFree", 50, 20000, 0, ZeroBand),
        Band("Band", 50, 20000, 6000, NonZeroBand)
      )

      val dataF = BandedGraph(
        Seq.empty[CodingComponent],
        taxBand,
        taxFreeAllowanceBandSum = 10000,
        totalEstimatedTax = 6000,
        taxViewType = ComplexTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        40000,
        40000,
        50,
        20000,
        100,
        6000,
        None,
        Some(Swatch(15.00, 6000)))
    }

    "have two 0 % band and one 7.5% band in graph" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 11000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 3000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("SDR", "", income = 15000, tax = 2000, lowerBand = Some(14000), upperBand = Some(18000), rate = 7.5)
      )

      val bands = List(
        Band("TaxFree", 48.27, 14000, 0, ZeroBand),
        Band("Band", 51.72, 15000, 2000, NonZeroBand)
      )

      val dataF = BandedGraph(
        Seq.empty[CodingComponent],
        taxBand,
        taxFreeAllowanceBandSum = 11000,
        totalEstimatedTax = 2000,
        taxViewType = ComplexTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        29000,
        29000,
        48.27,
        14000,
        99.99,
        2000,
        None,
        Some(Swatch(6.89, 2000)))
    }

    "have three 0 % band and zero other band in graph" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 11000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 3000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("SDR", "", income = 15000, tax = 0, lowerBand = Some(14000), upperBand = Some(32000), rate = 0)
      )

      val bands = List(
        Band("TaxFree", 37.93, 11000, 0, "PSR"),
        Band("TaxFree", 10.34, 3000, 0, "SR"),
        Band("TaxFree", 51.72, 15000, 0, "SDR")
      )

      val nextBandMessage = Some(Messages("tai.taxCalc.nextTaxBand", 14000))

      val dataF = BandedGraph(
        Seq.empty[CodingComponent],
        taxBand,
        taxFreeAllowanceBandSum = 11000,
        totalEstimatedTax = 0,
        taxViewType = ComplexTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        43000,
        29000,
        99.99,
        29000,
        99.99,
        0,
        nextBandMessage,
        Some(Swatch(0, 0)))
    }

    "have two 0 % band and one Taxed Income band(7.5 & 20 ) in graph" in {

      val taxBand = List(
        TaxBand("PSR", "", income = 10000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 10000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("SDR", "", income = 10000, tax = 750, lowerBand = Some(14000), upperBand = Some(32000), rate = 7.5),
        TaxBand("B", "", income = 10000, tax = 3000, lowerBand = Some(14000), upperBand = Some(30000), rate = 20)
      )

      val bands = List(
        Band("TaxFree", 50.00, 20000, 0, ZeroBand),
        Band("Band", 50.00, 20000, 3750, NonZeroBand)
      )

      val dataF = BandedGraph(
        Seq.empty[CodingComponent],
        taxBand,
        taxFreeAllowanceBandSum = 10000,
        totalEstimatedTax = 3750,
        taxViewType = ComplexTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        40000,
        40000,
        50.00,
        20000,
        100.00,
        3750,
        None,
        Some(Swatch(9.37, 3750)))
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
        Band("TaxFree", 33.32, 20000, 0, ZeroBand),
        Band("Band", 66.66, 40000, 6750, NonZeroBand)
      )

      val nextBandMessage = Some(Messages("tai.taxCalc.nextTaxBand", 50000))

      val dataF = BandedGraph(
        Seq.empty[CodingComponent],
        taxBand,
        taxFreeAllowanceBandSum = 10000,
        totalEstimatedTax = 6750,
        taxViewType = ComplexTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        110000,
        60000,
        33.32,
        20000,
        99.98,
        6750,
        nextBandMessage,
        Some(Swatch(11.25, 6750)))
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
        Band("TaxFree", 16.66, 20000, 0, ZeroBand),
        Band("Band", 83.33, 100000, 9750, NonZeroBand)
      )

      val nextBandMessage = Some(Messages("tai.taxCalc.nextTaxBand", 90000))

      val dataF = BandedGraph(
        Seq.empty[CodingComponent],
        taxBand,
        taxFreeAllowanceBandSum = 10000,
        totalEstimatedTax = 9750,
        taxViewType = ComplexTaxView)
      dataF mustBe BandedGraph(
        "taxGraph",
        bands,
        0,
        210000,
        120000,
        16.66,
        20000,
        99.99,
        9750,
        nextBandMessage,
        Some(Swatch(8.12, 9750)))
    }

  }

  "merge Zero bands" must {
    "return an empty list when an empty list is supplied" in {
      val result = BandedGraph.mergeZeroBands(List())
      result mustBe List()
    }

    "return a merged list when two individual lists are supplied " in {
      val individualList = List(Band("TaxFree", 50, 1000, 0, "ZeroBand"), Band("TaxFree", 50, 1000, 0, "ZeroBand"))

      val mergedIndividualList = BandedGraph.mergeZeroBands(individualList)
      mergedIndividualList mustBe List(Band("TaxFree", 100, 2000, 0, "ZeroBand"))
    }
  }

  "merge Tax bands" must {

    "return None when an empty list is supplied." in {
      val result = BandedGraph.mergedBands(Nil, totalTaxBandIncome = 10000, taxViewType = SimpleTaxView)
      result mustBe None
    }

    "return empty string in table percentage when tax explanation link is not coming " in {
      val taxBand = List(
        TaxBand("", "", income = 1000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 20),
        TaxBand("", "", income = 2500, tax = 40, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand("", "", income = 1000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 40),
        TaxBand("", "", income = 2000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 40)
      )

      val dataF = BandedGraph.mergedBands(taxBand, totalTaxBandIncome = 6500, taxViewType = SimpleTaxView)
      dataF.get mustBe Band("Band", 100, 6500, 100, NonZeroBand)
    }

    "return only one merged tax band for other than zero% rate band" in {
      val taxBand = List(
        TaxBand("", "", income = 1000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 20),
        TaxBand("", "", income = 2500, tax = 40, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand("", "", income = 1000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 40),
        TaxBand("", "", income = 2000, tax = 20, lowerBand = None, upperBand = Some(4000), rate = 40)
      )

      val dataF = BandedGraph.mergedBands(taxBand, totalTaxBandIncome = 6500, taxViewType = SimpleTaxView)
      dataF.get mustBe Band("Band", 100, 6500, 100, NonZeroBand)
    }
  }

  "individual Tax bands" must {

    "return an empty list when an empty list is supplied." in {
      val result = BandedGraph.individualBands(Nil, totalTaxBandIncome = 10000, taxViewType = SimpleTaxView)
      result mustBe Nil
    }

    "return two tax bands for 0% rate" in {
      val taxBand = List(
        TaxBand("PSA", "", income = 1000, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 0),
        TaxBand("B", "", income = 2000, tax = 0, lowerBand = None, upperBand = Some(5000), rate = 0)
      )

      val dataF = BandedGraph.individualBands(taxBand, totalTaxBandIncome = 3000, taxViewType = ComplexTaxView)
      dataF mustBe List(Band("TaxFree", 33.33, 1000, 0, "PSA"), Band("TaxFree", 66.66, 2000, 0, "B"))
    }
  }

  "individual other rate Tax bands" must {

    "return an empty list when an empty list is supplied." in {
      val result = BandedGraph.individualOtherRateBands(Nil, totalTaxBandIncome = 10000, taxViewType = SimpleTaxView)
      result mustBe Nil

    }

    "return two tax bands for 20% and 40% rate" in {
      val taxBand = List(
        TaxBand("B", "", income = 1000, tax = 200, lowerBand = None, upperBand = Some(5000), rate = 20),
        TaxBand("D0", "", income = 2000, tax = 800, lowerBand = None, upperBand = Some(5000), rate = 40)
      )

      val dataF = BandedGraph.individualOtherRateBands(taxBand, totalTaxBandIncome = 3000, taxViewType = SimpleTaxView)
      dataF mustBe List(Band("Band", 33.33, 1000, 200, "B"), Band("Band", 66.66, 2000, 800, "D0"))
    }
  }

  "create swatch" must {

    "return a swatch when given a total estimated tax and total estimated income is provided" in {
      val totalEstimatedIncome = 20000
      val totalEstimatedTax = 1700
      BandedGraph.createSwatch(totalEstimatedTax, totalEstimatedIncome) mustEqual Swatch(8.5, totalEstimatedTax)
    }

  }

  "getUpperBand" must {

    "return 0 when empty list" in {
      val taxBands = Nil
      val upperBand = BandedGraph.getUpperBand(taxBands)
      upperBand mustBe 0
    }

    "return default value when only pa band has been passed" in {
      val taxBands: List[TaxBand] =
        List(TaxBand("pa", "", income = 11500, tax = 0, lowerBand = None, upperBand = None, rate = 0))
      val upperBand = BandedGraph.getUpperBand(taxBands)
      upperBand mustBe 11500
    }

    "return proper upperBand when passed two bands (0&20)" in {
      val taxBands: List[TaxBand] = List(
        TaxBand("PSR", "", income = 3200, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 16000, tax = 5000, lowerBand = Some(11000), upperBand = Some(28800), rate = 20)
      )

      val upperBand = BandedGraph.getUpperBand(taxBands, taxFreeAllowanceBandSum = 3200)

      upperBand mustBe 32000
    }

    "return income as upperBand when income is greater than upper band" in {
      val taxBands: List[TaxBand] = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 150000, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40),
        TaxBand("D1", "", income = 30000, tax = 2250, lowerBand = Some(150000), upperBand = Some(0), rate = 45)
      )

      val upperBand = BandedGraph.getUpperBand(taxBands, taxFreeAllowanceBandSum = 5000)

      upperBand mustBe 200000
    }

    "deduct personal allowance from tax-free allowances to get the upper-band if user is in higher rate" in {
      val taxBands = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20),
        TaxBand("D0", "", income = 100, tax = 60000, lowerBand = Some(32000), upperBand = Some(150000), rate = 40)
      )

      val upperBand =
        BandedGraph.getUpperBand(taxBands = taxBands, personalAllowance = Some(5000), taxFreeAllowanceBandSum = 5000)

      upperBand mustBe 150000
    }

    "not deduct personal allowance from tax-free allowances to get the upper-band if user is not in higher rate" in {
      val taxBands = List(
        TaxBand("PSR", "", income = 5000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 15000, tax = 3000, lowerBand = Some(11000), upperBand = Some(32000), rate = 20)
      )

      val upperBand =
        BandedGraph.getUpperBand(taxBands = taxBands, personalAllowance = Some(5000), taxFreeAllowanceBandSum = 5000)

      upperBand mustBe 37000
    }

  }

  "calculate bar percentage" must {

    "return value 0 when no band has passed" in {
      val percentage =
        BandedGraph.calcBarPercentage(20000, Nil, totalTaxBandIncome = 10000, taxViewType = SimpleTaxView)

      percentage mustBe 0
    }

    "return valid percentage when a user earns less than their tax free amount" in {

      val personalAllowance = 11500

      val taxBands = List(
        TaxBand("pa", "", income = personalAllowance, tax = 0, lowerBand = Some(0), upperBand = None, rate = 0)
      )

      val percentage = BandedGraph.calcBarPercentage(
        personalAllowance,
        taxBands,
        personalAllowance = Some(personalAllowance),
        taxFreeAllowanceBandSum = personalAllowance,
        totalTaxBandIncome = personalAllowance,
        totalEstimatedIncome = 9000,
        taxViewType = ZeroTaxView
      )

      percentage mustBe 78.26

    }

    "return valid percentage for first income when two bands has passed" in {
      val taxBands = List(
        TaxBand("PSR", "", income = 3200, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 16000, tax = 5000, lowerBand = Some(11000), upperBand = Some(28800), rate = 20)
      )

      val percentage = BandedGraph.calcBarPercentage(
        3200,
        taxBands,
        taxFreeAllowanceBandSum = 3200,
        totalTaxBandIncome = 19200,
        taxViewType = ComplexTaxView)

      percentage mustBe 16.66
    }

    "return valid percentage for second income when two bands has passed" in {
      val taxBands = List(
        TaxBand("PSR", "", income = 3200, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("B", "", income = 16000, tax = 5000, lowerBand = Some(11000), upperBand = Some(28800), rate = 20)
      )

      val percentage = BandedGraph.calcBarPercentage(
        16000,
        taxBands,
        taxFreeAllowanceBandSum = 3200,
        totalTaxBandIncome = 19200,
        taxViewType = ComplexTaxView)

      percentage mustBe 83.33
    }

    "return value till two decimal" in {
      val taxBand = List(
        TaxBand("PSR", "", income = 4000, tax = 0, lowerBand = Some(0), upperBand = Some(11000), rate = 0),
        TaxBand("SR", "", income = 4000, tax = 0, lowerBand = Some(11000), upperBand = Some(14000), rate = 0),
        TaxBand("D0", "", income = 15000, tax = 3000, lowerBand = Some(14000), upperBand = Some(32000), rate = 20)
      )

      val percentage = BandedGraph.calcBarPercentage(
        15000,
        taxBand,
        taxFreeAllowanceBandSum = 4000,
        totalTaxBandIncome = 23000,
        taxViewType = ComplexTaxView)

      percentage mustBe 65.21

    }

  }

  "personalAllowanceAmount" must {

    "return None when no personal allowance component type is present" in {

      val codingComponents = Seq(
        CodingComponent(CarBenefit, Some(1), 8026, "Car Benefit", None),
        CodingComponent(MedicalInsurance, Some(1), 637, "Medical Insurance", None),
        CodingComponent(OtherItems, Some(1), 65, "Other Items", None)
      )

      BandedGraph.personalAllowanceAmount(codingComponents) mustBe None
    }

    "return an amount for each given personal allowance component type" when {

      "personal allowance is present" in {
        val codingComponents = Seq(
          CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500)),
          CodingComponent(CarBenefit, Some(1), 8026, "Car Benefit", None)
        )

        BandedGraph.personalAllowanceAmount(codingComponents) mustBe Some(11500)
      }

      "Personal Allowance Aged PPA is present" in {
        val codingComponents = Seq(
          CodingComponent(PersonalAllowanceAgedPAA, None, 11500, "Personal Allowance Aged PPA", Some(11500)),
          CodingComponent(CarBenefit, Some(1), 8026, "Car Benefit", None)
        )

        BandedGraph.personalAllowanceAmount(codingComponents) mustBe Some(11500)
      }

      "Personal Allowance Elderly PAE is present" in {
        val codingComponents = Seq(
          CodingComponent(PersonalAllowanceElderlyPAE, None, 11500, "Personal Allowance Elderly PAE", Some(11500)),
          CodingComponent(CarBenefit, Some(1), 8026, "Car Benefit", None)
        )

        BandedGraph.personalAllowanceAmount(codingComponents) mustBe Some(11500)
      }

    }

  }
}
