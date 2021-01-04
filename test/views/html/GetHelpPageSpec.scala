/*
 * Copyright 2021 HM Revenue & Customs
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
import org.jsoup.Jsoup
import play.api.i18n.Lang
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class GetHelpPageSpec extends TaiViewSpec {

  implicit val user = UserBuilder()
  implicit val request = FakeRequest()
  override def view: Html = views.html.help.getHelp(appConfig)

  "show get help page" must {
    "show the correct page title and content when the page is displayed and show the english contact URL" in {
      val doc = Jsoup.parseBodyFragment(view.toString)
      doc must haveHeadingWithText(messagesApi("tai.getHelp.h1"))
      doc must haveBackLink
      doc.toString must include(appConfig.contactHelplineUrl)
    }

    "show the welsh contact URL" in {
      val messages = messagesApi.preferred(Seq(Lang("cy")))
      val html = views.html.help.getHelp(appConfig)(request, messages, user, templateRenderer, partialRetriever)
      val doc = Jsoup.parseBodyFragment(html.toString)
      doc.toString must include(appConfig.contactHelplineWelshUrl)
    }
  }
}
