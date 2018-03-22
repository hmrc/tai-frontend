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

import controllers.routes
import hmrc.nps2.TaxBand
import play.api.i18n.Messages
import uk.gov.hmrc.tai.viewModels.TaxExplanationViewModel
import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.util.BandTypesConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.time.TaxYearResolver

class howIncomeTaxIsCalculatedSpec extends TaiViewSpec with BandTypesConstants {

  "view" must {
    behave like pageWithTitle(messages("tai.incomeTax.calculated.title"))
    behave like pageWithBackLink
  }

  "display headings" in {
    doc must haveHeadingH3WithText(messages("tai.incomeTax.calculated.subHeading"))
  }
  "show correct header content" in {

    val accessiblePreHeading = doc.select("""header span[class="visuallyhidden"]""")
    accessiblePreHeading.text mustBe Messages("tai.estimatedIncome.accessiblePreHeading")

    val expectedTaxYearString =  Messages("tai.taxYear",
      nonBreakable(Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear)),
      nonBreakable(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)) )

    val preHeading = doc.select("header p")
    preHeading.text mustBe s"${Messages("tai.estimatedIncome.accessiblePreHeading")} ${expectedTaxYearString}"
  }

  "display table headers" in {
    doc must haveThWithText(messages("tai.incomeTax.calculated.table.headingOne"))
    doc must haveThWithText(messages("tai.incomeTax.calculated.table.headingTwo"))
    doc must haveThWithText(messages("tai.incomeTax.calculated.table.headingThree"))
    doc must haveThWithText(messages("tai.incomeTax.calculated.table.headingFour"))
  }

  "display table body" when {
    "UK user have non-savings" in {
      val nonSavings = List(
        TaxBand(Some("B"), None, 32010, 6402, None, None, 20),
        TaxBand(Some("D0"), None, 36466, 36466, None, None, 40)
      )
      val viewWithSavings: Html = views.html.howIncomeTaxIsCalculated(new TaxExplanationViewModel(nonSavings, Nil, Nil, UkBands))
      doc(viewWithSavings) must haveTdWithText("32,010")
      doc(viewWithSavings) must haveTdWithText(messages("uk.bandtype.B"))
      doc(viewWithSavings) must haveTdWithText("20%")
      doc(viewWithSavings) must haveTdWithText("6,402.00")
      doc(viewWithSavings) must haveTdWithText("36,466")
      doc(viewWithSavings) must haveTdWithText(messages("uk.bandtype.D0"))
      doc(viewWithSavings) must haveTdWithText("40%")
      doc(viewWithSavings) must haveTdWithText("36,466.00")
    }

    "Scottish user have non-savings" in {
      val nonSavings = List(
        TaxBand(Some("B"), None, 32010, 6402, None, None, 20),
        TaxBand(Some("D0"), None, 36466, 36466, None, None, 40)
      )
      val viewWithSavings: Html = views.html.howIncomeTaxIsCalculated(new TaxExplanationViewModel(nonSavings, Nil, Nil, ScottishBands))
      doc(viewWithSavings) must haveTdWithText("32,010")
      doc(viewWithSavings) must haveTdWithText(messages("scottish.bandtype.B"))
      doc(viewWithSavings) must haveTdWithText("20%")
      doc(viewWithSavings) must haveTdWithText("6,402.00")
      doc(viewWithSavings) must haveTdWithText("36,466")
      doc(viewWithSavings) must haveTdWithText(messages("scottish.bandtype.D0"))
      doc(viewWithSavings) must haveTdWithText("40%")
      doc(viewWithSavings) must haveTdWithText("36,466.00")
    }

    "UK user have savings" in {
      val savings = List(
        TaxBand(Some("LSR"), None, 32010, 6402, None, None, 20),
        TaxBand(Some("HSR1"), None, 36466, 36466, None, None, 40)
      )
      val viewWithSavings: Html = views.html.howIncomeTaxIsCalculated(new TaxExplanationViewModel(Nil, savings, Nil, UkBands))
      doc(viewWithSavings) must haveTdWithText("32,010")
      doc(viewWithSavings) must haveTdWithText(messages("uk.bandtype.LSR"))
      doc(viewWithSavings) must haveTdWithText("20%")
      doc(viewWithSavings) must haveTdWithText("6,402.00")
      doc(viewWithSavings) must haveTdWithText("36,466")
      doc(viewWithSavings) must haveTdWithText(messages("uk.bandtype.HSR1"))
      doc(viewWithSavings) must haveTdWithText("40%")
      doc(viewWithSavings) must haveTdWithText("36,466.00")
    }

    "Scottish user have savings" in {
      val savings = List(
        TaxBand(Some("LSR"), None, 32010, 6402, None, None, 20),
        TaxBand(Some("HSR1"), None, 36466, 36466, None, None, 40)
      )
      val viewWithSavings: Html = views.html.howIncomeTaxIsCalculated(new TaxExplanationViewModel(Nil, savings, Nil, ScottishBands))
      doc(viewWithSavings) must haveTdWithText("32,010")
      doc(viewWithSavings) must haveTdWithText(messages("scottish.bandtype.LSR"))
      doc(viewWithSavings) must haveTdWithText("20%")
      doc(viewWithSavings) must haveTdWithText("6,402.00")
      doc(viewWithSavings) must haveTdWithText("36,466")
      doc(viewWithSavings) must haveTdWithText(messages("scottish.bandtype.HSR1"))
      doc(viewWithSavings) must haveTdWithText("40%")
      doc(viewWithSavings) must haveTdWithText("36,466.00")
    }

    "UK user have dividends" in {
      val dividends = List(
        TaxBand(Some("LDR"), None, 32010, 6402, None, None, 20),
        TaxBand(Some("HDR1"), None, 36466, 36466, None, None, 40)
      )
      val viewWithSavings: Html = views.html.howIncomeTaxIsCalculated(new TaxExplanationViewModel(Nil, Nil, dividends, UkBands))
      doc(viewWithSavings) must haveTdWithText("32,010")
      doc(viewWithSavings) must haveTdWithText(messages("uk.bandtype.LDR"))
      doc(viewWithSavings) must haveTdWithText("20%")
      doc(viewWithSavings) must haveTdWithText("6,402.00")
      doc(viewWithSavings) must haveTdWithText("36,466")
      doc(viewWithSavings) must haveTdWithText(messages("uk.bandtype.HDR1"))
      doc(viewWithSavings) must haveTdWithText("40%")
      doc(viewWithSavings) must haveTdWithText("36,466.00")
    }

    "scottish user have dividends" in {
      val dividends = List(
        TaxBand(Some("LDR"), None, 32010, 6402, None, None, 20),
        TaxBand(Some("HDR1"), None, 36466, 36466, None, None, 40)
      )
      val viewWithSavings: Html = views.html.howIncomeTaxIsCalculated(new TaxExplanationViewModel(Nil, Nil, dividends, UkBands))
      doc(viewWithSavings) must haveTdWithText("32,010")
      doc(viewWithSavings) must haveTdWithText(messages("scottish.bandtype.LDR"))
      doc(viewWithSavings) must haveTdWithText("20%")
      doc(viewWithSavings) must haveTdWithText("6,402.00")
      doc(viewWithSavings) must haveTdWithText("36,466")
      doc(viewWithSavings) must haveTdWithText(messages("scottish.bandtype.HDR1"))
      doc(viewWithSavings) must haveTdWithText("40%")
      doc(viewWithSavings) must haveTdWithText("36,466.00")
    }

  }
  override def view: Html = views.html.howIncomeTaxIsCalculated(new TaxExplanationViewModel(Nil, Nil, Nil, ""))
}