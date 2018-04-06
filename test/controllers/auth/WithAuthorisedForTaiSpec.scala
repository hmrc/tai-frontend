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

package controllers.auth

import TestConnectors.FakeAuthConnector
import builders.{AuthBuilder, RequestBuilder}
import controllers.{ErrorPagesHandler, FakeTaiPlayApplication}
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import uk.gov.hmrc.tai.model.SessionData
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import uk.gov.hmrc.tai.service.TaiService
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.{SessionData, TaiRoot, TaxSummaryDetails}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class WithAuthorisedForTaiSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {

  "WithAuthorisedForTai" should {
    implicit val hc = HeaderCarrier()
    "fetch the AuthorisedByTai instance" when {
      "authorisedForTai for tai is called" in {
        val sut = createSUT
        val taiService = mock[TaiService]
        val result = sut.authorisedForTai(true)(taiService)
        result.isInstanceOf[sut.AuthorisedByTai] mustBe true
      }
    }

    "be able to Authorize Tai user" when {
      val taxSummaryDetails = TaxSummaryDetails(nino = nino, version = 0)
      val sessionData = SessionData(nino = nino, taiRoot = Some(TaiRoot()), taxSummaryDetailsCY = taxSummaryDetails)

      "AuthorisedByTai apply is called" in {
        val taiService = mock[TaiService]
        when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sessionData))

        val responseBody = "my response"
        val sut = createSUT.authorisedForTai(true)(taiService)
        val action = sut.apply(implicit user => implicit request => Ok(responseBody))
        val result = action.apply(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe 200
        contentAsString(result) mustBe responseBody
        verify(taiService, times(1)).taiSession(any(), any(), any())(any())
      }

      "AuthorisedByTai async is called" in {
        val taiService = mock[TaiService]
        when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sessionData))

        val responseBody = "my response"
        val sut = createSUT.authorisedForTai(true)(taiService)
        val action = sut.async(implicit user => implicit sessionData => implicit request => Future.successful(Ok(responseBody)))
        val result = action.apply(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe 200
        contentAsString(result) mustBe responseBody
        verify(taiService, times(1)).taiSession(any(), any(), any())(any())
      }

      "AuthorisedByTai call is failed" in {
        val taiService = mock[TaiService]
        when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sessionData))

        val responseBody = "Bad request - 400"
        val sut = createSUT.authorisedForTai(true)(taiService)
        val action = sut.async(implicit user => implicit sessionData => implicit request => Future.failed(new BadRequestException(responseBody)))
        val result = action.apply(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe 400
        val doc = Jsoup.parse(contentAsString(result))
        doc.title must include(responseBody)
        verify(taiService, times(1)).taiSession(any(), any(), any())(any())
      }
    }

    "be able to form TaiUser" when {
      "getTaiUser is called" in {
        val taiService = mock[TaiService]
        val authContext = AuthContext(AuthBuilder.createFakeAuthority(nino), governmentGatewayToken = Some("a token"))
        val sut = createSUT.authorisedForTai(true)(taiService)
        val request = RequestBuilder.buildFakeRequestWithAuth("GET")
        val user = Await.result(sut.getTaiUser(authContext, Nino(nino), TaiRoot())(request), 5 seconds)
        user.getNino mustBe nino
      }
    }
  }

  val nino: String = new Generator().nextNino.nino

  def createSUT = new SUT

  class SUT extends WithAuthorisedForTai  with ErrorPagesHandler {

    override implicit def templateRenderer: TemplateRenderer = MockTemplateRenderer

    override implicit def partialRetriever: FormPartialRetriever = MockPartialRetriever

    override def delegationConnector: DelegationConnector = mock[DelegationConnector]

    override def authConnector: AuthConnector = FakeAuthConnector
  }

}
