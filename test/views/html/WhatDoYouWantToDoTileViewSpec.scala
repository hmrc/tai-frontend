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

import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.forms.{WhatDoYouWantToDoForm, WhatDoYouWantToDoFormData}
import uk.gov.hmrc.tai.model.domain.TaxCodeMismatch
import uk.gov.hmrc.tai.service.{NoTimeToProcess, SevenDays, ThreeWeeks, TimeToProcess}
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel
import utils.factories.TaxCodeMismatchFactory
import play.api.mvc.Cookie

class WhatDoYouWantToDoTileViewSpec extends TaiViewSpec {

  val modelWithiFormNoCyPlus1 = createViewModel(ThreeWeeks, false)

  "whatDoYouWantTodo Page" should {
    behave like pageWithTitle(messages("your.paye.income.tax.overview"))
    behave like pageWithHeader(messages("your.paye.income.tax.overview"))

    "when confirmedAPIEnabled is set to false" should {
      "display iForms status message with three weeks when an iForm has not been fully processed" in {

        val threeWeekDoc = doc(views.html.whatDoYouWantToDoTileView(form, modelWithiFormNoCyPlus1))
        threeWeekDoc must haveH2HeadingWithText(messages("tai.whatDoYouWantToDo.iformPanel.p1"))
        threeWeekDoc must haveParagraphWithText(messages("tai.whatDoYouWantToDo.iformPanel.threeWeeks.p2"))
        threeWeekDoc must haveLinkElement("checkProgressLink", ApplicationConfig.checkUpdateProgressLinkUrl, messages("checkProgress.link"))
      }


      "display iForms status message with seven days when an iForm has not been fully processed" in {

        val modelWithiFormNoCyPlus1ForSevenDays = createViewModel(SevenDays, false)

        val sevenDaysDoc = doc(views.html.whatDoYouWantToDoTileView(form, modelWithiFormNoCyPlus1ForSevenDays))
        sevenDaysDoc must haveH2HeadingWithText(messages("tai.whatDoYouWantToDo.iformPanel.p1"))
        sevenDaysDoc must haveParagraphWithText(messages("tai.whatDoYouWantToDo.iformPanel.sevenDays.p2"))
        sevenDaysDoc must haveLinkElement("checkProgressLink", ApplicationConfig.checkUpdateProgressLinkUrl, messages("checkProgress.link"))

      }

      "not display iForms status message when no iForms are in progress" in {
        doc(view).select(".tai-progress-panel").size() mustBe 0
      }
    }

    "when confirmedAPIEnabled is set to true" should {
      "not display iForms status message" in {

        val threeWeeksViewModel = createViewModel(ThreeWeeks, false, isConfirmedAPI = true)
        val confirmedAPIEnabledDoc = doc(views.html.whatDoYouWantToDoTileView(form, threeWeeksViewModel))

        confirmedAPIEnabledDoc must not(haveH2HeadingWithText(messages("tai.whatDoYouWantToDo.iformPanel.p1")))
        confirmedAPIEnabledDoc must not(haveParagraphWithText(messages("tai.whatDoYouWantToDo.iformPanel.threeWeeks.p2")))
        confirmedAPIEnabledDoc.select("#checkProgressLink").size() mustBe 0

      }
    }

    "display cards correctly" when {
      "CY+1 is not enabled" in {

        val cards = doc.getElementsByClass("card")

        cards.size mustBe 2
        cards.toString must include(Messages("current.tax.year"))
        doc(view) must haveParagraphWithText(Messages("check.current.income", TaxYearRangeUtil.currentTaxYearRange))
        cards.toString mustNot include(Messages("next.year"))
        cards.toString mustNot include(Messages("check.estimated.income"))
        cards.toString must include(Messages("earlier"))
        cards.toString must include(Messages("check.tax.previous.years"))

      }

      "CY+1 is enabled" in {

        val modelNoiFormWithCyPlus1 = createViewModel(NoTimeToProcess, true)

        val nextYearView: Html = views.html.whatDoYouWantToDoTileView(form, modelNoiFormWithCyPlus1)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 3
        cards.toString must include(Messages("current.tax.year"))
        doc(nextYearView) must haveParagraphWithText(Messages("check.current.income", TaxYearRangeUtil.currentTaxYearRange))
        cards.toString must include(Messages("next.year"))
        doc(nextYearView) must haveParagraphWithText(Messages("check.estimated.income", TaxYearRangeUtil.futureTaxYearRangeHtmlNonBreak(yearsFromNow = 1)))
        cards.toString must include(Messages("earlier"))
        cards.toString must include(Messages("check.tax.previous.years"))
      }

      "Tax Code Change is enabled" in {

        val taxCodeMatched = TaxCodeMismatchFactory.matchedTaxCode
        val modeWithCyPlus1TaxCodeChange = createViewModel(NoTimeToProcess, true, true, Some(taxCodeMatched))

        val nextYearView: Html = views.html.whatDoYouWantToDoTileView(form, modeWithCyPlus1TaxCodeChange)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 4
        cards.toString must include("Check your latest tax code change")
        cards.toString must include("Find out what has changed and what happens next")
      }

      "Tax Code Change is disabled" in {

        val modelNoiFormWithCyPlus1 = createViewModel(NoTimeToProcess, true)

        val nextYearView: Html = views.html.whatDoYouWantToDoTileView(form, modelNoiFormWithCyPlus1)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 3
        cards.toString mustNot include("Check your latest tax code change")
        cards.toString mustNot include("Find out what has changed and what happens next")
      }
    }

    "display UR banner" in {
      val document: Html = views.html.whatDoYouWantToDoTileView(form, modelWithiFormNoCyPlus1)
      val urBanner = doc(document).getElementsByAttributeValue("id", "full-width-banner")
      val urDismissedText = doc(document).getElementsByAttributeValue("id", "fullWidthBannerDismissText")
      val urBannerHref = doc(document).getElementsByAttributeValue("id", "fullWidthBannerLink")
      urBanner mustNot be(null)
      urBanner.text() startsWith Messages("tai.urbanner.title")
      urDismissedText.text() must include(Messages("tai.urbanner.reject"))
      urBanner.text() must include(Messages("tai.urbanner.text"))
      urBannerHref.text() must include(ApplicationConfig.urBannerLink)
    }

  }

  def createViewModel(isAnyIFormInProgress: TimeToProcess,
                      isCyPlusOneEnabled: Boolean,
                      hasTaxCodeChanged: Boolean = false,
                      taxCodeMismatch: Option[TaxCodeMismatch] = None,
                      isConfirmedAPI: Boolean = false): WhatDoYouWantToDoViewModel = {
    WhatDoYouWantToDoViewModel(isAnyIFormInProgress, isCyPlusOneEnabled, hasTaxCodeChanged, taxCodeMismatch, isConfirmedAPI)
  }

  def form: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))

  private lazy val modelNoiFormNoCyPlus1 = createViewModel(NoTimeToProcess, false)

  override def view: Html = views.html.whatDoYouWantToDoTileView(form, modelNoiFormNoCyPlus1)
}
