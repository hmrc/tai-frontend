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


import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel
import uk.gov.hmrc.tai.forms.{WhatDoYouWantToDoForm, WhatDoYouWantToDoFormData}
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.config
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec


class WhatDoYouWantToDoSpec extends TaiViewSpec {
  "whatDoYouWantTodo Page" should {
    behave like pageWithTitle(messages("tai.whatDoYouWantToDo.heading"))
    behave like pageWithHeader(messages("tai.whatDoYouWantToDo.heading"))

    "have an error box on the top of the page with link a to error field" when {
      "a form with errors is passed into the view" in {
        val wdywtdForm: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))
        def view: Html = views.html.whatDoYouWantToDo(wdywtdForm, model)
        doc(view).select(".error-summary--show > ul > li > #taxYears-error-summary").text mustBe Messages("tai.whatDoYouWantToDo.error.selectOption")
      }
    }

    "display iForms status message when an iForm has not been fully processed" in{
      val wdywtdForm: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))
      def view: Html = views.html.whatDoYouWantToDo(wdywtdForm, WhatDoYouWantToDoViewModel(true, false))
      val paragraphs = doc(view).select(".panel-indent > p")
      paragraphs.get(0).text mustBe Messages("tai.whatDoYouWantToDo.iformPanel.p1")
      paragraphs.get(1).text mustBe Messages("tai.whatDoYouWantToDo.iformPanel.p2")
    }

    "not display iForms status message when no iForms are in progress" in{
      val wdywtdForm: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))
      def view: Html = views.html.whatDoYouWantToDo(wdywtdForm, model)
      doc(view).select(".panel-indent").size() mustBe 0
    }

    "have 'choose one option' legend" in {
      doc must haveElementAtPathWithText("legend span[id=radioGroupLegendMain", Messages("tai.whatDoYouWantToDo.chooseOneOption"))
    }

    "have 'I want to check:' legend hint" in {
      doc must haveElementAtPathWithText("legend div[id=radioGroupLegendHint", Messages("tai.whatDoYouWantToDo.iWantToCheck"))
    }

    "have two radio buttons with relevant text" in {
      doc must haveElementAtPathWithId("form fieldset input", "taxYears-currenttaxyear")
      doc must haveElementAtPathWithText("form fieldset label[for=taxYears-currenttaxyear]", Messages("tai.WhatDoYouWantToDo.radio1"))
      doc must haveElementAtPathWithId("form fieldset input", "taxYears-lasttaxyear")
      doc must haveElementAtPathWithText("form fieldset label[for=taxYears-lasttaxyear]", Messages("tai.WhatDoYouWantToDo.radio2"))
    }

    "have error message with the radio buttons" in {
      doc must haveElementAtPathWithText("form fieldset span.error-notification", Messages("tai.whatDoYouWantToDo.error.selectOption"))
    }

    "have 'continue' button" in {
      val continueButton = doc(view).select("form > p > input").attr("type", "submit").attr("value")

      continueButton mustBe Messages("tai.WhatDoYouWantToDo.submit")
    }

    "have next year radio button" when {
      "CY+1 is enabled" in {

        val nextYearView: Html = views.html.whatDoYouWantToDo(form, WhatDoYouWantToDoViewModel(false, true))
        val nextYearDoc = doc(nextYearView)

        nextYearDoc must haveElementAtPathWithId("form fieldset input", "taxYears-nexttaxyear")
        nextYearDoc must haveElementAtPathWithText("form fieldset label[for=taxYears-nexttaxyear]", Messages("tai.WhatDoYouWantToDo.radio3"))
      }
    }

   "not have next year radio button" when {
      "CY+1 is disabled" in {
        doc must not(haveElementAtPathWithId("form fieldset input", "taxYears-nexttaxyear"))
      }
    }
  }

  def form: Form[WhatDoYouWantToDoFormData] = WhatDoYouWantToDoForm.createForm.bind(Map("taxYears" -> ""))
  private lazy val model = WhatDoYouWantToDoViewModel(false, false)
  override def view: Html = views.html.whatDoYouWantToDo(form, model)
}
