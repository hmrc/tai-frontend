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

package views.html.estimatedIncomeTax

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.SimpleEstimatedIncomeTaxViewModel
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link

class taxOnIncomeSpec extends TaiViewSpec {

  "tax on income template" must {

    "have tax on your employment income section" in {

      doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.taxOnEmploymentIncome.subHeading"))
      doc(view) must haveParagraphWithText(Html(messages("tai.estimatedIncome.desc",
        "£68,476",
        messages("tai.estimatedIncome.taxFree.link"),
        "£11,500")).body)

      doc(view).select("#taxOnEmploymentIncomeDesc").html() mustBe Html(Messages("tai.estimatedIncome.desc",
        "£68,476",
        Link.toInternalPage(
          id = Some("taxFreeAmountLink"),
          url = routes.TaxFreeAmountController.taxFreeAmount.url.toString,
          value = Some("tai.estimatedIncome.taxFree.link")
        ).toHtml,
        "£11,500")).body
      doc(view).select("#employmentIncomeTaxDetails").size() mustBe 1
      doc(view) must haveTableThWithIdAndText("incomeTaxBand", messages("tai.incomeTaxBand"))
      doc(view) must haveTableThWithIdAndText("taxAmount", messages("tai.amount"))
      doc(view) must haveTableThWithIdAndText("taxRate", messages("tai.taxRate"))
      doc(view) must haveTableThWithIdAndText("tax", messages("tai.tax"))
      doc(view).select("#bandType0").text() mustBe messages("uk.bandtype.pa")
      doc(view).select("#bandType1").text() mustBe messages("uk.bandtype.B")
      doc(view).select("#bandType2").text() mustBe messages("uk.bandtype.D0")
      doc(view).select("#income0").text() mustBe "£11,500"
      doc(view).select("#taxRate0").text() mustBe "0%"
      doc(view).select("#tax0").text() mustBe "£0"
      doc(view).select("#income1").text() mustBe "£32,010"
      doc(view).select("#taxRate1").text() mustBe "20%"
      doc(view).select("#tax1").text() mustBe "£6,402"
      doc(view).select("#income2").text() mustBe "£36,466"
      doc(view).select("#taxRate2").text() mustBe "40%"
      doc(view).select("#tax2").text() mustBe "£14,586"

    }

  }

  val bandedGraph = BandedGraph("taxGraph", Nil, 0, 0, 0, 0, 0, 0, 0, None)

  val ukTaxBands = List(
    TaxBand("pa", "", 11500, 0, None, None, 0),
    TaxBand("B", "", 32010, 6402, None, None, 20),
    TaxBand("D0", "", 36466, 14586.4, None, None, 40))

  override def view: Html = views.html.estimatedIncomeTax.taxOnIncome(68476,11500,ukTaxBands,"UK")
}
