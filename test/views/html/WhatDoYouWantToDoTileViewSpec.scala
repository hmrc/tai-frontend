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

import builders.UserBuilder
import controllers.auth.AuthedUser
import mocks.MockTemplateRenderer
import org.joda.time.DateTime
import org.mockito.Mockito.when
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.forms.{WhatDoYouWantToDoForm, WhatDoYouWantToDoFormData}
import uk.gov.hmrc.tai.util.DateHelper.dateTimeFormat
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel

import java.time.LocalDate

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

    "display tax code change banner correctly" when {
      "Tax Code Change is enabled" in {
        val localDate = LocalDate.now()

        val modeWithCyPlus1TaxCodeChange =
          createViewModel(isCyPlusOneEnabled = true, maybeMostRecentTaxCodeChangeDate = Some(localDate))

        val nextYearView: Html = whatDoYouWantToDoTileView(form, modeWithCyPlus1TaxCodeChange, appConfig)

        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 5
        doc(nextYearView).toString must include(Messages("tai.WhatDoYouWantToDo.ViewChangedTaxCode"))
        doc(nextYearView).toString must include(
          Messages(
            "tai.WhatDoYouWantToDo.ChangedTaxCode",
            TaxYearRangeUtil.formatDate(localDate).replace(" ", "&nbsp;")))
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

    "display last logged in message when we retrieve a last log in from auth" in {

      val dt = DateTime.now.minusDays(7)
      implicit val thisAuthedUser: AuthedUser =
        UserBuilder("utr", TaiConstants.AuthProviderGG, "test", Some(dt))

      val view: Html = whatDoYouWantToDoTileView(form, modelNoiFormNoCyPlus1, appConfig)(
        authRequest,
        messages,
        thisAuthedUser,
        templateRenderer,
        ec)

      val cards = doc(view).getElementsByClass("card")

      cards.size mustBe 4
      doc(view).toString must include(Messages("tai.WhatDoYouWantToDo.lastSignedIn", dateTimeFormat(dt)))

    }

    "not display last logged in message when we retrieve a last log in from auth" in {

      val dt = DateTime.now.minusDays(7)
      implicit val thisAuthedUser: AuthedUser =
        UserBuilder("utr", TaiConstants.AuthProviderGG, "test", None)

      val view: Html = whatDoYouWantToDoTileView(form, modelNoiFormNoCyPlus1, appConfig)(
        authRequest,
        messages,
        thisAuthedUser,
        templateRenderer,
        ec)

      val cards = doc(view).getElementsByClass("card")

      cards.size mustBe 4
      doc(view).toString mustNot include(Messages("tai.WhatDoYouWantToDo.lastSignedIn", dateTimeFormat(dt)))

    }
  }

  def createViewModel(
    isCyPlusOneEnabled: Boolean,
    showJrsTile: Boolean = false,
    maybeMostRecentTaxCodeChangeDate: Option[LocalDate] = None): WhatDoYouWantToDoViewModel =
    WhatDoYouWantToDoViewModel(isCyPlusOneEnabled, showJrsTile, maybeMostRecentTaxCodeChangeDate)

  def form: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))

  private lazy val modelNoiFormNoCyPlus1 = createViewModel(false)

  override def view: Html = whatDoYouWantToDoTileView(form, modelNoiFormNoCyPlus1, appConfig)
}
