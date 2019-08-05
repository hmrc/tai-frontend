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
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.HtmlFormatter
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{TaxCodeComparisonViewModel, TaxCodeDetail}
import uk.gov.hmrc.urls.Link

class TaxCodesSpec extends TaiViewSpec {
  "Cy plus one tax codes view" must {

    "show the tax codes section with heading" in {
      doc must haveH2HeadingWithText(messages("tai.incomeTaxComparison.taxCodes.subHeading"))
    }

    "display scottish tax code information" in {
      doc.select("#scottishTaxCodeInfo").html() mustBe Html(
        messages(
          "tai.incomeTaxComparison.taxCodes.scottishInfo",
          Link
            .toExternalPage(
              url = ApplicationConfig.scottishRateIncomeTaxUrl,
              value = Some(messages("tai.taxCode.scottishIncomeText.link")))
            .toHtml
        )).body
    }

    "display tax code comparision table" in {
      doc must haveTdWithText("Employer name EMPLOYER")
      doc must haveTdWithText(taxYearEnds + " 1115L")
      doc must haveTdWithText(taxYearStarts + " S975L")

      doc must haveTdWithText("Pension name PENSION")
      doc must haveTdWithText(taxYearEnds + " 1150L")
      doc must haveTdWithText(taxYearStarts + " 1250L")
    }

  }

  private lazy val employmentTaxCode = Seq(TaxCodeDetail("EMPLOYER", Seq("1115L", "S975L")))
  private lazy val pensionTaxCode = Seq(TaxCodeDetail("PENSION", Seq("1150L", "1250L")))
  private lazy val model = TaxCodeComparisonViewModel(employmentTaxCode, pensionTaxCode)
  val taxYearEnds = "Current tax year ends " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear() end))
  val taxYearStarts = "Next tax year from " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().next.start))
  override def view: Html = views.html.incomeTaxComparison.TaxCodes(model)
}
