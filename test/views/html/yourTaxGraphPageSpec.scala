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

package views.html

import builders.UserBuilder
import controllers.FakeTaiPlayApplication
import controllers.auth.TaiUser
import uk.gov.hmrc.tai.viewModels.{Band, BandedGraph}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.util.TaxRegionConstants


class yourTaxGraphPageSpec extends UnitSpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with TaxRegionConstants {

  implicit val request = FakeRequest("GET", "")
  implicit val user: TaiUser = UserBuilder.apply()

  "Graph" should {

    "show Tax-Free Allowance and SSR and DIV" in {

      val bands = List(
        Band("TaxFree", 25.58, "0%", 11000, 0, "pa"),
        Band("TaxFree", 6.97, "0%", 3000, 0, "SR"),
        Band("TaxFree", 34.88, "0%", 15000, 0, "SDR")
      )
      val nextBandMessage = "You can have £14,000 more before your income reaches the next tax band."
      val graphData = BandedGraph("taxGraph", bands, 0, 43000, 29000, 67.43, 29000, 67.43, 0, Some(nextBandMessage))

      val doc = Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion).toString())
      doc.select("#bandType0").text() shouldBe "Tax-free allowance"
      doc.select("#bandType1").text() shouldBe "Starting rate for savings"
      doc.select("#bandType2").text() shouldBe "Dividend Allowance"
      doc.select("#nextBand").text() shouldBe nextBandMessage
      doc.select("#incomeTotal").text() shouldBe "29,000"
      doc.select("#taxTotal").text() shouldBe "0.00"
      doc.select("#bandType3").size() shouldBe 0
      doc.select("#zeroIncomeTotal").text() shouldBe "£29,000"
      doc.select("#totalIncome").text() shouldBe empty

    }

    "show Tax-Free Allowance and Income basic rate" in {
      val bands = List(
        Band("TaxFree", 10.00, "0%", 3200, 0, "pa"),
        Band("Band", 50.00, "20%", 16000, 5000, "B")
      )
      val nextBandMessage = "You can have £12,800 more before your income reaches the next tax band."
      val graphData = BandedGraph("taxGraph", bands, 0, 32000, 19200, 10.00, 3200, 60.00, 5000, Some(nextBandMessage))

      val doc = Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion).toString())
      doc.select("#bandType0").text() shouldBe "Tax-free allowance"
      doc.select("#bandType1").text() shouldBe "Income basic rate"
      doc.select("#bandType1").text() shouldBe "Income basic rate"
      doc.select("#nextBand").text() shouldBe nextBandMessage
      doc.select("#incomeTotal").text() shouldBe "19,200"
      doc.select("#taxTotal").text() shouldBe "5,000.00"
      doc.select("#bandType2").size() shouldBe 0
      doc.select("#zeroIncomeTotal").text() shouldBe "£3,200"
      doc.select("#totalIncome").text() shouldBe "£19,200"
    }

    "show Tax-Free Allowance and Taxed Income" in {
      val bands = List(
        Band("TaxFree", 2.00, "0%", 3000, 0, "pa"),
        Band("Band", 30.00, "Check in more detail", 45000, 15000, "TaxedIncome")
      )
      val nextBandMessage = "You can have £102,000 more before your income reaches the next tax band."
      val graphData = BandedGraph("taxGraph", bands, 0, 150000, 48000, 2.00, 3000, 32.00, 15000, Some(nextBandMessage))

      val doc = Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion).toString())
      doc.select("#bandType0").text() shouldBe "Tax-free allowance"
      doc.select("#bandType1").text() shouldBe "Taxed income"
      doc.select("#nextBand").text() shouldBe nextBandMessage
      doc.select("#incomeTotal").text() shouldBe "48,000"
      doc.select("#taxTotal").text() shouldBe "15,000.00"
      doc.select("#bandType2").size() shouldBe 0
      doc.select("#zeroIncomeTotal").text() shouldBe "£3,000"
      doc.select("#totalIncome").text() shouldBe "£48,000"
    }

    "show Tax-Free Allowance and PSA and 7.5% DIV" in {

      val bands = List(
        Band("TaxFree", 37.93, "0%", 11000, 0, "pa"),
        Band("TaxFree", 10.34, "0%", 3000, 0, "PSR"),
        Band("Band", 51.72, "7.5%", 15000, 2000, "LDR")
      )

      val graphData = BandedGraph("taxGraph", bands, 0, 29000, 29000, 48.27, 14000, 99.99, 2000)

      val doc = Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion).toString())

      doc.select("#bandType0").text() shouldBe "Tax-free allowance"
      doc.select("#bandType1").text() shouldBe "Personal Savings Allowance"
      doc.select("#bandType2").text() shouldBe "Dividend basic rate"
      doc.select("#nextBand").size() shouldBe 0
      doc.select("#incomeTotal").text() shouldBe "29,000"
      doc.select("#taxTotal").text() shouldBe "2,000.00"
      doc.select("#bandType3").size() shouldBe 0
      doc.select("#zeroIncomeTotal").text() shouldBe "£14,000"
      doc.select("#totalIncome").text() shouldBe "£29,000"
    }

    "show tax rate bands for other rate bands " when {
      "no Tax-Free Allowance, PSA, DIV and SSR bands available" in {

        val bands = List(
          Band("Band", 37.93, "20%", 33500, 6700, "B"),
          Band("Band", 10.34, "40%", 116500, 46600, "D0"),
          Band("Band", 25.86, "45%", 15000, 6750, "D1"),
          Band("Band", 25.86, "45%", 1500, 675, "HSR2")
        )

        val graphData = BandedGraph("taxGraph", bands, 0, 166500, 166500, 0, 0, 99.99, 60725)

        val doc = Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion).toString())

        doc.select("#bandType0").text() shouldBe "Income basic rate"
        doc.select("#bandType1").text() shouldBe "Income higher rate"
        doc.select("#bandType2").text() shouldBe "Income additional rate"
        doc.select("#bandType3").text() shouldBe "Savings additional rate"
      }
    }

    "show tax rate bands for 20% and 40% bands" when {
      "no Tax-Free Allowance, PSA, DIV and SSR bands available" in {

        val bands = List(
          Band("Band", 37.93, "20%", 33500, 6700, "B"),
          Band("Band", 40.34, "40%", 91500, 36600, "D0")
        )

        val graphData = BandedGraph("taxGraph", bands, 0, 150000, 125000, 0, 0, 78.27, 43300)

        val doc = Jsoup.parseBodyFragment(views.html.includes.yourTaxGraph(graphData, UkTaxRegion).toString())

        doc.select("#bandType0").text() shouldBe "Income basic rate"
        doc.select("#bandType1").text() shouldBe "Income higher rate"
      }
    }

  }

}
