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

import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.{WhatDoYouWantToDoForm, WhatDoYouWantToDoFormData}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel
import uk.gov.hmrc.time.TaxYearResolver


class WhatDoYouWantToDoTileViewSpec extends TaiViewSpec {
  "whatDoYouWantTodo Page" should {
    behave like pageWithTitle(messages("your.paye.income.tax.overview"))
    behave like pageWithHeader(messages("your.paye.income.tax.overview"))

    "display iForms status message when an iForm has not been fully processed" in{
      def view: Html = views.html.whatDoYouWantToDoTileView(form, modelWithiFormNoCyPlus1)
      val paragraphs = doc(view).select(".panel-indent > p")
      paragraphs.get(0).text mustBe Messages("tai.whatDoYouWantToDo.iformPanel.p1")
      paragraphs.get(1).text mustBe Messages("tai.whatDoYouWantToDo.iformPanel.p2")
    }

    "not display iForms status message when no iForms are in progress" in{
      doc(view).select(".panel-indent").size() mustBe 0
    }


    "display cards correctly" when {
      "CY+1 is not enabled" in {

        val cards = doc.getElementsByClass("card")

        cards.size mustBe 2
        cards.toString must include(Messages("current.tax.year"))
        cards.toString must include(Messages("check.current.income", (TaxYearResolver.currentTaxYear-1).toString, TaxYearResolver.currentTaxYear.toString))
        cards.toString mustNot include(Messages("next.year"))
        cards.toString mustNot include(Messages("check.estimated.income"))
        cards.toString must include(Messages("earlier"))
        cards.toString must include(Messages("check.tax.previous.years"))

      }

      "CY+1 is enabled" in {

        val nextYearView: Html = views.html.whatDoYouWantToDoTileView(form, modelNoiFormWithCyPlus1)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 3
        cards.toString must include(Messages("current.tax.year"))
        cards.toString must include(Messages("check.current.income", (TaxYearResolver.currentTaxYear-1).toString, TaxYearResolver.currentTaxYear.toString))
        cards.toString must include(Messages("next.year"))
        cards.toString must include(Messages("check.estimated.income"))
        cards.toString must include(Messages("earlier"))
        cards.toString must include(Messages("check.tax.previous.years"))
      }

      "Tax Code Change is enabled" in {

        val nextYearView: Html = views.html.whatDoYouWantToDoTileView(form, modeWithCyPlus1TaxCodeChange)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 4
        cards.toString must include("Check your latest tax code change")
        cards.toString must include("Find out what has changed and what happens next")
      }

      "Tax Code Change is disabled" in {

        val nextYearView: Html = views.html.whatDoYouWantToDoTileView(form, modelNoiFormWithCyPlus1)
        val cards = doc(nextYearView).getElementsByClass("card")

        cards.size mustBe 3
        cards.toString mustNot include("Check your latest tax code change")
        cards.toString mustNot include("Find out what has changed and what happens next")
      }
    }
  }

  def form: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))

  private lazy val modelNoiFormNoCyPlus1 = WhatDoYouWantToDoViewModel(false, false)
  private lazy val modelNoiFormWithCyPlus1 = WhatDoYouWantToDoViewModel(false, true)
  private lazy val modelWithiFormNoCyPlus1 = WhatDoYouWantToDoViewModel(true, false)
  private lazy val modeWithCyPlus1TaxCodeChange = WhatDoYouWantToDoViewModel(false, true, true)
  override def view: Html = views.html.whatDoYouWantToDoTileView(form, modelNoiFormNoCyPlus1)
}
