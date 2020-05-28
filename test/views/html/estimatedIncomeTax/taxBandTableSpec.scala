/*
 * Copyright 2020 HM Revenue & Customs
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

package views.html.estimatedIncomeTax

import play.twirl.api.Html
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.constants.{BandTypesConstants, TaxRegionConstants}

class taxBandTableSpec extends TaiViewSpec with BandTypesConstants with TaxRegionConstants {

  "taxBandTable template" must {
    "display the given column headings" in {
      doc(view).select(".table-id").size() mustBe 1
      doc(view) must haveTableThWithClassAndText("taxBand", messages("tai.incomeTaxBand"))
      doc(view) must haveTableThWithClassAndText("numeric taxAmount", messages("tai.amount"))
      doc(view) must haveTableThWithClassAndText("numeric taxRate", messages("tai.taxRate"))
      doc(view) must haveTableThWithClassAndText("numeric tax", messages("tai.tax"))
    }
    "display the correct number of rows" in {
      doc(view).getElementsByTag("tr").size mustBe 5
    }
    "display the correct number of columns" in {
      doc(view).getElementsByTag("th").size mustBe 4
    }

    "display the given tax band types" in {
      doc(view).select(".bandType0").text() mustBe messages("estimate.uk.bandtype.SDR")
      doc(view).select(".bandType1").text() mustBe messages("estimate.uk.bandtype.LDR")
      doc(view).select(".bandType2").text() mustBe messages("estimate.uk.bandtype.HDR1")
      doc(view).select(".bandType3").text() mustBe messages("estimate.uk.bandtype.HDR2")
    }
    "display the given amounts" in {
      doc(view).select(".income0").text() mustBe "£11,500"
      doc(view).select(".income1").text() mustBe "£32,010"
      doc(view).select(".income2").text() mustBe "£36,466"
      doc(view).select(".income3").text() mustBe "£40,000"
    }
    "display the given tax rates" in {
      doc(view).select(".taxRate0").text() mustBe "0%"
      doc(view).select(".taxRate1").text() mustBe "20%"
      doc(view).select(".taxRate2").text() mustBe "40%"
      doc(view).select(".taxRate3").text() mustBe "50%"
    }
    "display the given tax paid" in {
      doc(view).select(".tax0").text() mustBe "£0"
      doc(view).select(".tax1").text() mustBe "£6,402"
      doc(view).select(".tax2").text() mustBe "£14,586"
      doc(view).select(".tax3").text() mustBe "£15,000"
    }
  }

  val taxBands = List(
    TaxBand(DividendZeroRate, "", 11500, 0, None, None, 0),
    TaxBand(DividendBasicRate, "", 32010, 6402, None, None, 20),
    TaxBand(DividendHigherRate, "", 36466, 14586.4, None, None, 40),
    TaxBand(DividendAdditionalRate, "", 40000, 15000, None, None, 50)
  )

  override def view: Html = views.html.estimatedIncomeTax.taxBandTable("table-id", taxBands, UkTaxRegion)

}
