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

import java.util.UUID

import uk.gov.hmrc.tai.service.PersonService
import builders.{AuthBuilder, RequestBuilder}
import uk.gov.hmrc.tai.connectors._
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsArray, JsValue}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.config.{ApplicationConfig, WSHttp}
import uk.gov.hmrc.tai.model.{TaiRoot, UserDetails}
import uk.gov.hmrc.tai.util.TaiConstants

import scala.concurrent.Future

class ServiceControllerSpec extends UnitSpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val hc = HeaderCarrier()

  "Time Out page" should {
    "return page when called" in  {
      val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody()
      val sut = createSut
      val result = sut.timeoutPage()(fakeRequest)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() should include(Messages("tai.timeout.title"))
    }
  }

  "Sign Out" should {
    "redirect to company auth frontend if it is a GG user" in  {
      val sut = createSut

      when(sut.userDetailsConnector.userDetails(any[AuthContext](any()))).thenReturn(Future.successful(UserDetails("GovernmentGateway")))


      val result = sut.serviceSignout()(authorisedRequest)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(ApplicationConfig.companyAuthFrontendSignOutUrl)
    }
    "redirect to citizen auth frontend if it is a Verify user" in  {
      val sut = createSut

      when(sut.userDetailsConnector.userDetails(any[AuthContext])(any()))
        .thenReturn(Future.successful(UserDetails("Verify")))

      val result = sut.serviceSignout()(authorisedRequest)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(ApplicationConfig.citizenAuthFrontendSignOutUrl)
      session(result).get(TaiConstants.SessionPostLogoutPage) shouldBe Some(ApplicationConfig.feedbackSurveyUrl)
    }
    "return 500 when userDetails returns failed" in  {
      val request = RequestBuilder.buildFakeRequestWithAuth("GET")
      val sut = createSut
      when(sut.userDetailsConnector.userDetails(any[AuthContext])(any())).thenReturn(Future.failed(new RuntimeException))
      val result = sut.serviceSignout()(request)
      status(result) shouldBe 500
    }
  }


  //create TaxSummaryDetail from Json
  class TestTaiConnector extends TaiConnector {
    override def http = WSHttp
    override def serviceUrl: String = "test/tai"

    def get(key: String)(implicit hc: HeaderCarrier) =
      Future.successful(Some(JsArray()))

    def put(key: String,data: JsValue)(implicit hc: HeaderCarrier) =
      Future.successful(())

  }

  def createSut = new SUT
  class SUT extends ServiceController {
    override val authConnector = mock[AuthConnector]
    override val auditConnector = mock[AuditConnector]
    override val delegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer = MockTemplateRenderer
    override implicit val partialRetriever = MockPartialRetriever
    override val personService = mock[PersonService]
    override val userDetailsConnector = mock[UserDetailsConnector]

    val taiRoot = TaiRoot(nino, 0, "Mr", "Kkk", None, "Sss", "Kkk Sss", false, Some(false))
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(taiRoot))

    when(authConnector.currentAuthority(any(), any())).thenReturn(
      Future.successful(Some(AuthBuilder.createFakeAuthority(nino))))
  }

  private val nino = new Generator().nextNino.nino

  lazy val authorisedRequest = FakeRequest("GET", path = "").withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.authProvider -> "IDA",
    SessionKeys.userId -> s"/path/to/authority",
    SessionKeys.authToken -> "a token")

}

