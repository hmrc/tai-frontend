/*
 * Copyright 2022 HM Revenue & Customs
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

import mocks.MockTemplateRenderer
import org.mockito.Mockito.when
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.forms.{WhatDoYouWantToDoForm, WhatDoYouWantToDoFormData}
import uk.gov.hmrc.tai.model.domain.TaxCodeMismatch
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel
import utils.factories.TaxCodeMismatchFactory

class WhatDoYouWantToDoTileViewSpec extends TaiViewSpec {

  val modelWithiFormNoCyPlus1 = createViewModel(false)

  private val whatDoYouWantToDoTileView = inject[WhatDoYouWantToDoTileView]

  override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

  "whatDoYouWantTodo Page" should {
    behave like pageWithTitle(messages("your.paye.income.tax.overview"))
    behave like pageWithHeader(messages("your.paye.income.tax.overview"))

    "display cards correctly" when {
      "CY+1 is not enabled" in {

        val cards = doc.getElementsByClass("card")

        cards.size mustBe 4
        cards.toString must include(Messages("current.tax.year"))
        doc(view) must haveParagraphWithText(Messages("check.current.income", TaxYearRangeUtil.currentTaxYearRange))
        cards.toString mustNot include(Messages("next.year"))
        cards.toString mustNot include(Messages("check.estimated.income"))
        cards.toString must include(Messages("earlier"))
        cards.toString must include(Messages("check.tax.previous.years"))
        cards.toString must include(Messages("claim.tax.relief.wfh"))
        cards.toString must include(Messages("income.tax.history"))
        cards.toString must include(Messages("income.tax.history.content"))
      }

      "CY+1 is enabled" in {

        val modelNoiFormWithCyPlus1 = createViewModel(true)

        val nextYearView: Html = whatDoYouWantToDoTileView(form, modelNoiFormWithCyPlus1, appConfig)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 5
        cards.toString must include(Messages("current.tax.year"))
        doc(nextYearView) must haveParagraphWithText(
          Messages("check.current.income", TaxYearRangeUtil.currentTaxYearRange))
        cards.toString must include(Messages("next.year"))
        doc(nextYearView) must haveParagraphWithText(
          Messages("check.estimated.income", TaxYearRangeUtil.futureTaxYearRange(yearsFromNow = 1)))
        cards.toString must include(Messages("earlier"))
        cards.toString must include(Messages("check.tax.previous.years"))
        cards.toString must include(Messages("claim.tax.relief.wfh"))
      }

      "Tax Code Change is enabled" in {

        val taxCodeMatched = TaxCodeMismatchFactory.matchedTaxCode
        val modeWithCyPlus1TaxCodeChange = createViewModel(true, true, taxCodeMismatch = Some(taxCodeMatched))

        val nextYearView: Html = whatDoYouWantToDoTileView(form, modeWithCyPlus1TaxCodeChange, appConfig)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 6
        cards.toString must include("Check your latest tax code change")
        cards.toString must include("Find out what has changed and what happens next")
        cards.toString must include(Messages("claim.tax.relief.wfh"))
      }

      "Tax Code Change is disabled" in {

        val modelNoiFormWithCyPlus1 = createViewModel(true)

        val nextYearView: Html = whatDoYouWantToDoTileView(form, modelNoiFormWithCyPlus1, appConfig)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 5
        cards.toString mustNot include("Check your latest tax code change")
        cards.toString mustNot include("Find out what has changed and what happens next")
        cards.toString must include(Messages("claim.tax.relief.wfh"))
      }
    }

    "JrsClaimTile is enabled" in {

      val modelJrsTileEnabled = createViewModel(isCyPlusOneEnabled = false, showJrsTile = true)

      val jrsClaimView: Html = whatDoYouWantToDoTileView(form, modelJrsTileEnabled, appConfig)
      val cards = doc(jrsClaimView).getElementsByClass("card")

      cards.size mustBe 5

      cards.toString must include(Messages("current.tax.year"))
      doc(view) must haveParagraphWithText(Messages("check.current.income", TaxYearRangeUtil.currentTaxYearRange))
      cards.toString mustNot include(Messages("next.year"))
      cards.toString mustNot include(Messages("check.estimated.income"))
      cards.toString must include(Messages("earlier"))
      cards.toString must include(Messages("check.tax.previous.years"))
      cards.toString must include(Messages("check.jrs.claims"))

    }

    "IncomeTaxHistory enabled" in {
      val appConfig = mock[ApplicationConfig]
      when(appConfig.incomeTaxHistoryEnabled).thenReturn(true)

      val view: Html = whatDoYouWantToDoTileView(form, modelNoiFormNoCyPlus1, appConfig)

      val cards = doc(view).getElementsByClass("card")

      cards.size mustBe 4
      cards.toString must include(Messages("income.tax.history"))
      cards.toString must include(Messages("income.tax.history.content"))
    }

    "IncomeTaxHistory disabled" in {
      val appConfig = mock[ApplicationConfig]
      when(appConfig.incomeTaxHistoryEnabled).thenReturn(false)

      val view: Html = whatDoYouWantToDoTileView(form, modelNoiFormNoCyPlus1, appConfig)

      val cards = doc(view).getElementsByClass("card")

      cards.size mustBe 3
      cards.toString mustNot include(Messages("income.tax.history"))
      cards.toString mustNot include(Messages("income.tax.history.content"))
    }
  }

  def createViewModel(
    isCyPlusOneEnabled: Boolean,
    hasTaxCodeChanged: Boolean = false,
    showJrsTile: Boolean = false,
    taxCodeMismatch: Option[TaxCodeMismatch] = None): WhatDoYouWantToDoViewModel =
    WhatDoYouWantToDoViewModel(isCyPlusOneEnabled, hasTaxCodeChanged, showJrsTile, taxCodeMismatch)

  def form: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))

  private lazy val modelNoiFormNoCyPlus1 = createViewModel(false)

  override def view: Html = whatDoYouWantToDoTileView(form, modelNoiFormNoCyPlus1, appConfig)
}
