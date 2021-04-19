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

package controllers

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.BaseSpec
import views.html.help.getHelp

class HelpControllerSpec extends BaseSpec with JsoupMatchers {

  "show help page" must {
    "call getHelpPage() successfully with an authorized session" in {
      val result = sut.helpPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messagesApi("tai.getHelp.h1"))
    }
  }

  def sut = new HelpController(
    FakeAuthAction,
    FakeValidatePerson,
    appConfig,
    mcc,
    inject[getHelp],
    error_template_noauth,
    error_no_primary,
    MockPartialRetriever,
    MockTemplateRenderer
  )
}
