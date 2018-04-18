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

import builders.UserBuilder
import controllers.FakeTaiPlayApplication
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers

class getHelpPageSpec extends UnitSpec
  with FakeTaiPlayApplication
  with JsoupMatchers{

  implicit val request = FakeRequest("GET", "")
  implicit val user = UserBuilder.apply()
  implicit val templateRenderer = MockTemplateRenderer
  implicit val partialRetriever = MockPartialRetriever
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]


  "show get help page" should {

    "show the correct page title and content when the page is displayed and dynamic web chat status is webchat_available" in {

      val html = views.html.help.getHelp(Some("0"))

      val doc = Jsoup.parseBodyFragment(html.toString)
      doc should haveHeadingWithText( messagesApi("tai.getHelp.h1"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.p1"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.p2"))
      doc should haveHeadingH2WithText(messagesApi("tai.getHelp.webchat.h2"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.webchat.available.p1", messagesApi("tai.getHelp.webchat.link")))
      doc should haveHeadingH2WithText(messagesApi("tai.getHelp.form.h2"))
      val getHelpPara = messagesApi("tai.getHelp.form.p1.text.with.also", messagesApi("tai.getHelp.form.link.title"))
      doc should haveParagraphWithText(getHelpPara)
      val iformLinks = doc.select("#get-help-iform-link")
      iformLinks.size() shouldBe 1
      iformLinks.first should haveLinkURL("/forms/form/tell-us-how-you-want-to-pay-estimated-tax/guide")
    }

    "show the correct page title and content when the page is displayed and dynamic web chat status is webchat_closed" in {
      val html = views.html.help.getHelp(Some("1"))

      val doc = Jsoup.parseBodyFragment(html.toString)
      doc should haveHeadingWithText( messagesApi("tai.getHelp.h1"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.p1.with.acronym"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.webchat.closed.p2"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.webchat.p2"))
      doc should haveBulletPointWithText(messagesApi("tai.getHelp.webchat.li1"))
      doc should haveBulletPointWithText(messagesApi("tai.getHelp.webchat.li2"))

      val getHelpPara = messagesApi("tai.getHelp.form.p1.text.with.also", messagesApi("tai.getHelp.form.link.title"))
      doc should haveParagraphWithText(getHelpPara)
      val iformLinks = doc.select("#get-help-iform-link")
      iformLinks.size() shouldBe 1
      iformLinks.first should haveLinkURL("/forms/form/tell-us-how-you-want-to-pay-estimated-tax/guide")
    }

    "show the correct page title and content when the page is displayed and dynamic web chat status is webchat_busy" in {
      val html = views.html.help.getHelp(Some("2"))
      val doc = Jsoup.parseBodyFragment(html.toString)
      doc should haveHeadingWithText( messagesApi("tai.getHelp.h1"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.p1.with.acronym"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.webchat.busy.p2"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.webchat.busy.error.p2"))
      doc should haveBulletPointWithText(messagesApi("tai.getHelp.webchat.busy.error.li1"))
      doc should haveBulletPointWithText(messagesApi("tai.getHelp.webchat.busy.error.li2"))

      val getHelpPara = messagesApi("tai.getHelp.form.p1", messagesApi("tai.getHelp.form.link.title"))
      doc should haveParagraphWithText(getHelpPara)
      val iformLinks = doc.select("#get-help-iform-link")
      iformLinks.size() shouldBe 1
      iformLinks.first should haveLinkURL("/forms/form/tell-us-how-you-want-to-pay-estimated-tax/guide")
    }

    "show the correct page title and content when the page is displayed and dynamic web chat status is webchat_error" in {
      val html = views.html.help.getHelp(None)
      val doc = Jsoup.parseBodyFragment(html.toString)
      doc should haveHeadingWithText( messagesApi("tai.getHelp.h1"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.p1.with.acronym"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.webchat.error.p2"))
      doc should haveParagraphWithText(messagesApi("tai.getHelp.webchat.busy.error.p2"))
      doc should haveBulletPointWithText(messagesApi("tai.getHelp.webchat.busy.error.li1"))
      doc should haveBulletPointWithText(messagesApi("tai.getHelp.webchat.busy.error.li2"))

      val getHelpPara = messagesApi("tai.getHelp.form.p1", messagesApi("tai.getHelp.form.link.title"))
      doc should haveParagraphWithText(getHelpPara)
      val iformLinks = doc.select("#get-help-iform-link")
      iformLinks.size() shouldBe 1
      iformLinks.first should haveLinkURL("/forms/form/tell-us-how-you-want-to-pay-estimated-tax/guide")
    }
  }

}
