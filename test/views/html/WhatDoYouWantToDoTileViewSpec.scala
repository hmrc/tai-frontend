/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.{WhatDoYouWantToDoForm, WhatDoYouWantToDoFormData}
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel

import java.time.LocalDate
import scala.util.Random

class WhatDoYouWantToDoTileViewSpec extends TaiViewSpec {

  val modelWithiFormNoCyPlus1: WhatDoYouWantToDoViewModel = createViewModel()

  private val whatDoYouWantToDoTileView = inject[WhatDoYouWantToDoTileView]

  "whatDoYouWantTodo Page" should {

    behave like pageWithTitle(messages("your.paye.income.tax.overview"))
    behave like pageWithHeader(messages("your.paye.income.tax.overview"))
    behave like haveInternalLink(
      appConfig.taxReliefExpenseClaimLink,
      messages("claim.tax.relief.claimOtherExpense"),
      "other-expense-link"
    )

    "display cards correctly" when {
      "CY+1 is not enabled" in {
        val view: Html = whatDoYouWantToDoTileView(
          form,
          modelNoiFormNoCyPlus1,
          appConfig,
          incomeTaxHistoryEnabled = true,
          cyPlusOneEnabled = false
        )

        val cards = doc(view).getElementsByClass("card")

        cards.size mustBe 2
        cards.toString must include(Messages("current.tax.year"))
        doc(view) must haveParagraphWithText(
          Messages("check.current.income", TaxYearRangeUtil.currentTaxYearRangeBreak)
        )
        cards.toString mustNot include(Messages("next.year"))
        cards.toString mustNot include(Messages("check.estimated.income"))
        cards.toString must include(Messages("income.tax.history"))
        cards.toString must include(
          Messages("income.tax.history.content", appConfig.numberOfPreviousYearsToShowIncomeTaxHistory)
        )
      }

      "CY+1 is enabled" in {
        val modelNoiFormWithCyPlus1 = createViewModel()

        val nextYearView: Html = whatDoYouWantToDoTileView(
          form,
          modelNoiFormWithCyPlus1,
          appConfig,
          incomeTaxHistoryEnabled = true,
          cyPlusOneEnabled = true
        )
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 3
        cards.toString must include(Messages("current.tax.year"))
        doc(nextYearView) must haveParagraphWithText(
          Messages("check.current.income", TaxYearRangeUtil.currentTaxYearRangeBreak)
        )
        cards.toString must include(Messages("next.year"))
        doc(nextYearView) must haveParagraphWithText(
          Messages("check.estimated.income", TaxYearRangeUtil.futureTaxYearRange(yearsFromNow = 1)).replaceU00A0
        )
      }

      "Tax Code Change is disabled" in {
        val modelNoiFormWithCyPlus1 = createViewModel()

        val nextYearView: Html = whatDoYouWantToDoTileView(
          form,
          modelNoiFormWithCyPlus1,
          appConfig,
          incomeTaxHistoryEnabled = true,
          cyPlusOneEnabled = true
        )
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 3
        cards.toString mustNot include("Check your latest tax code change")
        cards.toString mustNot include("Find out what has changed and what happens next")
      }
    }

    "display tax code change banner correctly" when {
      "Tax Code Change is enabled" in {

        val localDate = LocalDate.now()

        val modeWithCyPlus1TaxCodeChange =
          createViewModel(maybeMostRecentTaxCodeChangeDate = Some(localDate))

        val nextYearView: Html = whatDoYouWantToDoTileView(
          form,
          modeWithCyPlus1TaxCodeChange,
          appConfig,
          incomeTaxHistoryEnabled = true,
          cyPlusOneEnabled = true
        )

        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 3
        doc(nextYearView).toString must include(Messages("tai.WhatDoYouWantToDo.ViewChangedTaxCode"))
        doc(nextYearView).toString must include(
          Messages(
            "tai.WhatDoYouWantToDo.ChangedTaxCode",
            TaxYearRangeUtil.formatDate(localDate).replace(" ", "&nbsp;")
          )
        )
      }
    }

    "JrsClaimTile is enabled" in {
      val modelJrsTileEnabled = createViewModel(showJrsLink = true)

      val jrsClaimView: Html = whatDoYouWantToDoTileView(
        form,
        modelJrsTileEnabled,
        appConfig,
        incomeTaxHistoryEnabled = true,
        cyPlusOneEnabled = false
      )
      val cards = doc(jrsClaimView).getElementsByClass("card")

      cards.size mustBe 2

      cards.toString must include(Messages("current.tax.year"))
      doc(view) must haveParagraphWithText(Messages("check.current.income", TaxYearRangeUtil.currentTaxYearRangeBreak))
      cards.toString mustNot include(Messages("next.year"))
      cards.toString mustNot include(Messages("check.estimated.income"))

      doc(jrsClaimView) must haveLinkWithUrlWithID(
        "jrs-link",
        s"${controllers.routes.JrsClaimsController.onPageLoad()}"
      )
      assertThrows[NullPointerException](
        doc(view) must haveLinkWithUrlWithID("jrs-link", s"${controllers.routes.JrsClaimsController.onPageLoad()}")
      )
    }

    "IncomeTaxHistory enabled" in {
      val view: Html = whatDoYouWantToDoTileView(
        form,
        modelNoiFormNoCyPlus1,
        appConfig,
        incomeTaxHistoryEnabled = true,
        cyPlusOneEnabled = false
      )

      val cards = doc(view).getElementsByClass("card")

      cards.size mustBe 2
      cards.toString must include(Messages("income.tax.history"))
      cards.toString must include(
        Messages("income.tax.history.content", appConfig.numberOfPreviousYearsToShowIncomeTaxHistory)
      )
    }

    "IncomeTaxHistory disabled" in {
      val view: Html = whatDoYouWantToDoTileView(
        form,
        modelNoiFormNoCyPlus1,
        appConfig,
        incomeTaxHistoryEnabled = false,
        cyPlusOneEnabled = false
      )

      val cards = doc(view).getElementsByClass("card")

      cards.size mustBe 1
      cards.toString mustNot include(Messages("income.tax.history"))
      cards.toString mustNot include(
        Messages("income.tax.history.content", appConfig.numberOfPreviousYearsToShowIncomeTaxHistory)
      )
    }

    "show the unread messages indicator when user has unread messages" in {
      val messageCount = Random.nextInt(100) + 1

      implicit val authedUser: AuthedUser = UserBuilder().copy(messageCount = Some(messageCount))

      val view: Html = whatDoYouWantToDoTileView(
        form,
        modelNoiFormNoCyPlus1,
        appConfig,
        incomeTaxHistoryEnabled = true,
        cyPlusOneEnabled = false
      )

      view.body must include(s"""<span class="hmrc-notification-badge">$messageCount</span>""")
    }
  }

  def createViewModel(
    showJrsLink: Boolean = false,
    maybeMostRecentTaxCodeChangeDate: Option[LocalDate] = None
  ): WhatDoYouWantToDoViewModel =
    WhatDoYouWantToDoViewModel(cyPlusOneDataAvailable = true, showJrsLink, maybeMostRecentTaxCodeChangeDate)

  def form: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))

  private lazy val modelNoiFormNoCyPlus1 = createViewModel()

  override def view: Html = whatDoYouWantToDoTileView(
    form,
    modelNoiFormNoCyPlus1,
    appConfig,
    incomeTaxHistoryEnabled = false,
    cyPlusOneEnabled = false
  )
}
