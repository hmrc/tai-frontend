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

class EstimatedIncomeViewModelSpec extends PlaySpec {

  "EstimatedIncomeViewModel - ssrValue" should {
    "return 0" when {
      "an SR tax band isn't included with the supplied tax bands" in {

        val bands = List(
          Band("TaxFree", 25.58, "0%", 11000, 0, "pa"),
          Band("TaxFree", 34.88, "0%", 15000, 0, "SDR")
        )

        val graphData = BandedGraph("taxGraph", bands)

        val sut = EstimatedIncomeViewModel(newGraph = graphData,
          taxBands = None,
          ukDividends = None,
          incomeTaxReducedToZeroMessage = None, taxRegion = "")

        sut.ssrValue mustBe 0
      }
    }

    "return the correct SR value" when {
      "an SR tax band is included with the supplied tax bands" in {

        val srValue = 1500

        val bands = List(
          Band("TaxFree", 25.58, "0%", 11000, 0, "pa"),
          Band("TaxFree", 6.97, "0%", srValue, 0, "SR"),
          Band("TaxFree", 34.88, "0%", 15000, 0, "SDR")
        )

        val graphData = BandedGraph("taxGraph", bands)

        val sut = EstimatedIncomeViewModel(newGraph = graphData,
          taxBands = None,
          ukDividends = None,
          incomeTaxReducedToZeroMessage = None, taxRegion = "")

        sut.ssrValue mustBe srValue
      }
    }
  }

  "EstimatedIncomeViewModel - psaValue" should {
    "return 0" when {
      "an PSA tax band isn't included with the supplied tax bands" in {

        val bands = List(
          Band("TaxFree", 25.58, "0%", 11000, 0, "pa"),
          Band("TaxFree", 34.88, "0%", 15000, 0, "SDR")
        )

        val graphData = BandedGraph("taxGraph", bands)

        val sut = EstimatedIncomeViewModel(newGraph = graphData,
          taxBands = None,
          ukDividends = None,
          incomeTaxReducedToZeroMessage = None, taxRegion = "")

        sut.psaValue mustBe 0
      }
    }

    "return the correct PSA value" when {
      "an PSA tax band is included with the supplied tax bands" in {
        val psaValue = 1500

        val bands = List(
          Band("TaxFree", 25.58, "0%", 11000, 0, "pa"),
          Band("TaxFree", 6.97, "0%", psaValue, 0, "PSR"),
          Band("TaxFree", 34.88, "0%", 15000, 0, "SDR")
        )

        val graphData = BandedGraph("taxGraph", bands)

        val sut = EstimatedIncomeViewModel(newGraph = graphData,
          taxBands = None,
          ukDividends = None,
          incomeTaxReducedToZeroMessage = None, taxRegion = "")

        sut.psaValue mustBe psaValue
      }
    }
  }
}
