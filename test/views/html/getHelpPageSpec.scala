/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers

class getHelpPageSpec extends PlaySpec with FakeTaiPlayApplication with JsoupMatchers {

  implicit val request = FakeRequest("GET", "")
  implicit val user = UserBuilder()
  implicit val templateRenderer = MockTemplateRenderer
  implicit val partialRetriever = MockPartialRetriever

  "show get help page" must {
    "show the correct page title and content when the page is displayed and show the english contact URL" in {
      implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

      val html = views.html.help.getHelp()
      val doc = Jsoup.parseBodyFragment(html.toString)
      doc must haveHeadingWithText(messagesApi("tai.getHelp.h1"))
      doc must haveBackLink

      doc.toString must include(ApplicationConfig.contactHelplineUrl)
    }

    "show the welsh contact URL" in {
      implicit val messages = Messages.apply(Lang("cy"), app.injector.instanceOf[MessagesApi])

      val html = views.html.help.getHelp()(request, messages, user, templateRenderer, partialRetriever)
      val doc = Jsoup.parseBodyFragment(html.toString)
      doc.toString must include(ApplicationConfig.contactHelplineWelshUrl)
    }
  }
}
