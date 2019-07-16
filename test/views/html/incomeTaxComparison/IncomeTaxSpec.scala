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

package views.html.incomeTaxComparison

import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.HtmlFormatter
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.incomeTaxComparison.{EstimatedIncomeTaxComparisonItem, EstimatedIncomeTaxComparisonViewModel}

class IncomeTaxSpec extends TaiViewSpec {

  "Income Tax Comparison view - Estimated Income tax section" must {
    "display a higher estimated tax amount for next year" when {
      "a view model is supplied to the view with appropriate data" in {

        val currentYearItem = EstimatedIncomeTaxComparisonItem(TaxYear(),100)
        val nextYearItem = EstimatedIncomeTaxComparisonItem(TaxYear().next,201.83)

        val viewmodel = EstimatedIncomeTaxComparisonViewModel(Seq(currentYearItem, nextYearItem))
        def view: Html = views.html.incomeTaxComparison.IncomeTax(viewmodel)

        doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.more", "&pound;101"))
      }
    }

    "display a lower estimated tax amount for next year" when {
      "a view model is supplied to the view with appropriate data" in {

        val currentYearItem = EstimatedIncomeTaxComparisonItem(TaxYear(),200.83)
        val nextYearItem = EstimatedIncomeTaxComparisonItem(TaxYear().next,100)

        val viewmodel = EstimatedIncomeTaxComparisonViewModel(Seq(currentYearItem, nextYearItem))
        def view: Html = views.html.incomeTaxComparison.IncomeTax(viewmodel)

        doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.less", "&pound;100"))
      }
    }

    "display that tax amount for next year is the same" when {
      "a view model is supplied to the view with appropriate data" in {

        doc must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.same"))
      }
    }

    "display the description for estimated income tax comparison" when {
      "a view model is supplied to the view with appropriate data" in {

        doc must haveParagraphWithText(messages("tai.incomeTaxComparison.incomeTax.description"))
      }
    }

    "display the comparison table with correct row text" when {
      "a view model is supplied to the view with appropriate data" in {

        doc must haveTdWithText(messages("tai.incomeTaxComparison.incomeTax.estimate"))
      }
    }

    "display the comparison table with correct row values for cy and cy+1" when {
      "a view model is supplied to the view with appropriate data" in {
        doc must haveTdWithText(yourPAYEIncomeTaxEstimate)
        doc must haveTdWithText(taxYearEnds + "£100")
        doc must haveTdWithText(taxYearStarts + "£100")
      }
    }

    "display the comparison table column headers with correct values" ignore {
      "a view model is supplied to the view with appropriate data" in {

        doc must haveThWithText(s"${messages("tai.CurrentTaxYear")} ${messages("tai.incomeTaxComparison.incomeTax.column1",
          TaxYear().end.toString("d MMMM"))}")

        doc must haveThWithText(s"${messages("tai.NextTaxYear")} ${messages("tai.incomeTaxComparison.incomeTax.column2",
          TaxYear().next.start.toString("d MMMM yyyy"))}")
      }
    }
  }

  private val yourPAYEIncomeTaxEstimate = "Your PAYE Income Tax estimate"
  val taxYearEnds = "Current tax year ends " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().end)) + " "
  val taxYearStarts = "Next tax year from " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().next.start)) + " "
  private val currentYearItem = EstimatedIncomeTaxComparisonItem(TaxYear(),100.83)
  private val nextYearItem = EstimatedIncomeTaxComparisonItem(TaxYear().next,100.83)

  private val viewmodel = EstimatedIncomeTaxComparisonViewModel(Seq(currentYearItem, nextYearItem))
  override def view: Html = views.html.incomeTaxComparison.IncomeTax(viewmodel)
}
