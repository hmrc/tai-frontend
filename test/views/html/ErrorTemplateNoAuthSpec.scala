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

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class ErrorTemplateNoAuthSpec extends TaiViewSpec {

  val pageTitle = "Unauthorised Title"
  val pageHeading = "You are unauthorised"
  val pageMessage = "You are not signed in so you cannot use this service"

  val additionalMessage = "Please sign in"
  val link = """<a id="sign-in-link" href="fake-path/fake-endpoint">Sign in</a>"""

  override def view: Html = error_template_noauth(
    pageTitle,
    pageHeading,
    pageMessage,
    List(additionalMessage, link)
  )

  "error_template_noauth view" should {

    pageWithTitle(pageTitle)

    "not render the PTA Account nav" in {
      doc.getElementsByClass("account-menu") mustBe empty
    }

    pageWithHeader(pageHeading)

    "render the correct message" in {
      doc.text() must include(pageMessage)
    }

    "display additional content if present" in {
      doc.text() must include(additionalMessage)
      doc.getElementById("sign-in-link").toString mustBe link
    }
  }
}
