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

package controllers

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{ApplicationConfig, ProxyHttpClient}
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers

import scala.concurrent.Future

class HelpControllerSpec extends PlaySpec
  with JsoupMatchers
  with MockitoSugar
  with OneServerPerSuite {

  "show help page" must {
    "call getHelpPage() successfully with an authorized session" in {
      val sut = createSut
      val result = sut.helpPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messagesApi("tai.getHelp.h1"))

    }

    "successfully receive valid eligibility status" in {
      val sut = createSut
      val xml =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
          <checkEligibility responseType="1"/>"""
      val response = HttpResponse(1, None, Map("a" -> List("1", "2", "3")), Some(xml.toString))

      when(sut.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result = sut.helpPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveParagraphWithText(messagesApi("tai.getHelp.webchat.closed.p2"))
    }

    "successfully receive None as eligibility status when http response does not have responseType attribute" in {
      val sut = createSut
      val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?> <checkEligibility />"""
      val response = HttpResponse(1, None, Map("a" -> List("1", "2", "3")), Some(xml.toString))

      when(sut.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result = sut.helpPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveParagraphWithText(messagesApi("tai.getHelp.webchat.error.p2"))
    }

    "successfully receive None as eligibility status when http response is empty" in {
      val sut = createSut
      val result = sut.helpPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveParagraphWithText(messagesApi("tai.getHelp.webchat.error.p2"))
    }

    "successfully receive None as eligibility status when there is a failure" in {
      val sut = createSut
      when(sut.httpGet.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("message")))

      val result = sut.helpPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveParagraphWithText(messagesApi("tai.getHelp.webchat.error.p2"))
    }
  }

  def createSut = new SUT

  val proxyHttpClient = mock[ProxyHttpClient]

  class SUT extends HelpController(
    mock[ApplicationConfig],
    proxyHttpClient,
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    override val webChatURL = ""

    val partialHttpGet: HttpGet = mock[HttpGet]

    val nino: Nino = new Generator().nextNino

    val fakePerson = Person(nino, "firstname", "surname", false, false)

    val response = HttpResponse(1, None, Map("a" -> List("1", "2", "3")), None)

    when(httpGet.GET[HttpResponse](any())(any(), any(), any()))
      .thenReturn(Future.successful(response))

    when(partialHttpGet.GET[HttpResponse](any())(any(), any(), any()))
      .thenReturn(Future.successful(response))

  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val hc: HeaderCarrier = HeaderCarrier()
}
