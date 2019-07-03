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

package controllers.i18n

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import controllers.{FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.partials.FormPartialRetriever

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class TaiLanguageControllerSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "switchLanguage method" must {

    "return a redirect to the what do you want to do page" when {
      "the request is authorised but no referrer header is present" in {
        val result = new SUT().switchToLanguage("english")(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include(controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
      }
    }

    "return a redirect to the referrer url" when {
      "the request is authorised and a referrer header is present" in {
        val result = new SUT().switchToLanguage("english")(RequestBuilder.buildFakeRequestWithAuth("GET", Map("Referer" -> "/fake/url")))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include("/fake/url")
      }
    }

    "return a header to set the PLAY_LANG cookie to the requested language code" when {
      "the requested language is supported" in {
        val result = Await.result(new SUT().switchToLanguage("english")(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
        result.header.headers.isDefinedAt("Set-Cookie") mustBe true
        result.header.headers("Set-Cookie") must include("PLAY_LANG=en;")

        val result2 = Await.result(new SUT().switchToLanguage("cymraeg")(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
        result2.header.headers.isDefinedAt("Set-Cookie") mustBe true
        result2.header.headers("Set-Cookie") must include("PLAY_LANG=cy;")
      }
    }

    "return a header to set the PLAY_LANG to the current language" when {
      "the requested language is not supported" in {
        val result = Await.result(new SUT().switchToLanguage("czech")(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
        result.header.headers.isDefinedAt("Set-Cookie") mustBe true
        result.header.headers("Set-Cookie") must include("PLAY_LANG=en;")
      }
      "the requested language is supported, but the welsh language feature toggle is not enabled" in {
        val result = Await.result(new SUT(false).switchToLanguage("english")(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
        result.header.headers.isDefinedAt("Set-Cookie") mustBe true
        result.header.headers("Set-Cookie") must include("PLAY_LANG=en;")
      }
    }
  }

  private val nino = new Generator(new Random).nextNino

  private class SUT(welshEnabled: Boolean = true) extends TaiLanguageController(
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
    override val isWelshEnabled = welshEnabled
  }

}
