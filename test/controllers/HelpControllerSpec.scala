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

package controllers

import builders.{AuthBuilder, RequestBuilder}
import data.TaiData
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.tai.config.WSHttpProxy
import uk.gov.hmrc.tai.model.{TaiRoot, TaxSummaryDetails}
import uk.gov.hmrc.tai.service.TaiService
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
      val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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

  class SUT extends HelpController {
    override val taiService = mock[TaiService]

    override val httpGet = mock[WSHttpProxy]
    override val webChatURL = ""

    override val auditConnector = mock[AuditConnector]
    override val authConnector = mock[AuthConnector]
    override val delegationConnector = mock[DelegationConnector]
    
    override implicit val templateRenderer = MockTemplateRenderer
    override implicit val partialRetriever = MockPartialRetriever

    val partialHttpGet: HttpGet = mock[HttpGet]

    val nino: Nino = new Generator().nextNino

    val fakeTaiRoot = TaiRoot(nino.nino, 0, "Mr", "name", None, "surname", "name surname", false, Some(false))

    when(authConnector.currentAuthority(any(), any())).thenReturn(Future.successful(
      Some( AuthBuilder.createFakeAuthority(nino.nino))))

    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(fakeTaiRoot))

    val response = HttpResponse(1, None, Map("a" -> List("1", "2", "3")), None)

    when(httpGet.GET[HttpResponse](any())(any(), any(), any()))
      .thenReturn(Future.successful(response))

    when(partialHttpGet.GET[HttpResponse](any())(any(), any(), any()))
      .thenReturn(Future.successful(response))

  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val testTaxSummary: TaxSummaryDetails = TaiData.getEverything
}
