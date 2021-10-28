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

package views.html

import builders.RequestBuilder
import org.jsoup.Jsoup
import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.constants.TaxRegionConstants
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{Band, BandedGraph, ComplexTaxView, SimpleTaxView}
import utils.BaseSpec

class YourTaxGraphPageSpec extends BaseSpec with TaxRegionConstants {

  implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

  "Graph" should {

    "show Tax-Free Allowance and SSR and DIV" in {

      val bands = List(
        Band("TaxFree", 99.99, 29000, 0, "ZeroBand")
      )
      val nextBandMessage = "You can have £14,000 more before your income reaches the next tax band."
      val graphData =
        BandedGraph("taxGraph", bands, 0, 43000, 29000, 99.99, 29000, 99.99, 0, Some(nextBandMessage), None)

      val doc =
        Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion, ComplexTaxView).toString())
      doc.select("#bandType0").text() mustBe Messages("tai.bandtype.zeroBand")
      doc.select("#totalIncome").text() mustBe empty

    }

    "show Tax-Free Allowance and Income basic rate" in {
      val bands = List(
        Band("TaxFree", 16.66, 3200, 0, "ZeroBand"),
        Band("Band", 83.33, 16000, 5000, "NonZeroBand")
      )
      val nextBandMessage = "You can have £12,800 more before your income reaches the next tax band."
      val graphData =
        BandedGraph("taxGraph", bands, 0, 32000, 19200, 16.66, 3200, 83.33, 5000, Some(nextBandMessage), None)

      val doc =
        Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion, SimpleTaxView).toString())
      doc.select("#bandType0").text() mustBe Messages("tai.bandtype.zeroBand")
      doc.select("#bandType1").text() mustBe Messages("tai.bandtype.nonZeroBand")
      doc.select("#bandType2").size() mustBe 0
      doc.select("#zeroIncomeTotal").text() mustBe "£3,200"
      doc.select("#totalIncome").text() mustBe "£19,200"
    }

    "show Tax-Free Allowance and Taxed Income" in {
      val bands = List(
        Band("TaxFree", 6.15, 3000, 0, "ZeroBand"),
        Band("Band", 93.75, 45000, 15000, "NonZeroBand")
      )
      val nextBandMessage = "You can have £102,000 more before your income reaches the next tax band."
      val graphData =
        BandedGraph("taxGraph", bands, 0, 150000, 48000, 6.15, 3000, 93.75, 15000, Some(nextBandMessage), None)

      val doc =
        Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion, SimpleTaxView).toString())
      doc.select("#bandType0").text() mustBe Messages("tai.bandtype.zeroBand")
      doc.select("#bandType1").text() mustBe Messages("tai.bandtype.nonZeroBand")
      doc.select("#bandType2").size() mustBe 0
      doc.select("#zeroIncomeTotal").text() mustBe "£3,000"
      doc.select("#totalIncome").text() mustBe "£48,000"
    }

    "show Tax-Free Allowance and PSA and 7.5% DIV" in {

      val bands = List(
        Band("TaxFree", 48.27, 14000, 0, "ZeroBand"),
        Band("Band", 51.72, 15000, 2000, "NonZeroBand")
      )

      val graphData = BandedGraph("taxGraph", bands, 0, 29000, 29000, 48.27, 14000, 51.72, 2000, None, None)

      val doc =
        Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion, ComplexTaxView).toString())

      doc.select("#bandType0").text() mustBe Messages("tai.bandtype.zeroBand")
      doc.select("#bandType1").text() mustBe Messages("tai.bandtype.nonZeroBand")
      doc.select("#bandType3").size() mustBe 0
      doc.select("#zeroIncomeTotal").text() mustBe "£14,000"
      doc.select("#totalIncome").text() mustBe "£29,000"
    }

    "show tax rate bands for other rate bands " when {
      "no Tax-Free Allowance, PSA, DIV and SSR bands available" in {

        val bands = List(
          Band("Band", 100.00, 33500, 6700, "NonZeroBand")
        )

        val graphData = BandedGraph("taxGraph", bands, 0, 33500, 33500, 0, 0, 99.99, 6700, None, None)

        val doc =
          Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion, ComplexTaxView).toString())

        doc.select("#bandType0").text() mustBe Messages("tai.bandtype.nonZeroBand")
      }
    }
  }

}
