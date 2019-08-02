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

package views.html

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.constants.TaxRegionConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.{Band, BandedGraph, SimpleTaxView}

class YourTaxGraphSpec extends TaiViewSpec with TaxRegionConstants {

  "Your tax graph" should {

    "display number, chart and tax bars" in {
      doc must haveSpanWithText("£0")
      doc must haveSpanWithText("£48,000")
      doc must haveSpanWithText("£3,000")
    }

    "display graph table header" in {
      doc must haveThWithText(messages("tai.key"))
      doc must haveThWithText(messages("tai.item"))
      doc must haveThWithText(messages("tai.amount"))
    }

    "display next band message" in {
      doc must haveSpanWithText(nextBandMessage)
    }
  }

  private lazy val bands = List(
    Band("TaxFree", 2.00, 3000, 0, "S"),
    Band("TaxFree", 2.00, 3000, 0, "PSR"),
    Band("TaxFree", 2.00, 3000, 0, "SR"),
    Band("Band", 30.00, 45000, 15000, "TaxedIncome")
  )
  private lazy val nextBandMessage = "You can have £102,000 more before your income reaches the next tax band."
  private lazy val graphData =
    BandedGraph("taxGraph", bands, 0, 150000, 48000, 2.00, 3000, 32.00, 15000, Some(nextBandMessage), None)

  override def view: Html = views.html.includes.yourTaxGraph(graphData, ScottishTaxRegion, SimpleTaxView)
}
