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
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.HttpEntity
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Request, ResponseHeader, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.domain.{Address, Person}
import uk.gov.hmrc.tai.service.PersonService
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random

class WithAuthorisedForTaiLiteSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JsoupMatchers {

  private implicit val hc = HeaderCarrier()

  "WithAuthorisedForTaiLite" must {

    "fetch the AuthorisedByTai instance" when {
      "authorisedForTai for tai is called" in {
        val sut = createSUT
        val personService = mock[PersonService]
        val result = sut.authorisedForTai(personService)
        result.isInstanceOf[sut.AuthorisedByTai] mustBe true
      }
    }

    "be able to Authorize Tai user" when {

      val person = generatePerson(generateNino)

      "AuthorisedByTai async is called" in {
        val personService = mock[PersonService]
        when(personService.personDetailsNew(any())(any())).thenReturn(Future.successful(person))

        val responseBody = "my response"
        val sut = createSUT.authorisedForTai(personService)
        val action = sut.async(implicit user => implicit taiRoot => implicit request => Future.successful(Ok(responseBody)))
        val result = action.apply(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        contentAsString(result) mustBe responseBody
        verify(personService, times(1)).personDetailsNew(any())(any())
      }

      "AuthorisedByTai call is failed" in {
        val personService = mock[PersonService]
        when(personService.personDetailsNew(any())(any())).thenReturn(Future.successful(person))

        val sut = createSUT.authorisedForTai(personService)
        val action = sut.async(implicit user => implicit taiRoot => implicit request => Future.failed(new HttpException("Something failed at citizen details call", 500)))
        val result = action.apply(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse(contentAsString(result))
        doc.title must include(Messages("global.error.InternalServerError500.title"))
        verify(personService, times(1)).personDetailsNew(any())(any())
      }
    }

    "be able to form TaiUser" when {
      "getTaiUser is called" in {
        val personService = mock[PersonService]
        val authContext = AuthContext(AuthBuilder.createFakeAuthority(fakeAuthorityFixtureNino), governmentGatewayToken = Some("a token"))
        val sut = createSUT.authorisedForTai(personService)
        val request = RequestBuilder.buildFakeRequestWithAuth("GET")
        val person = Person(generateNino, "firstname", "surname", Some(new LocalDate(1985, 10, 10)), Address("l1", "l2", "l3", "pc", "country"), false, false)
        val user = Await.result(sut.getTaiUser(authContext, person)(request), 5 seconds)
        user.getNino mustBe fakeAuthorityFixtureNino
      }
    }
  }

  "authorisedForTai.async" must {

    "expose the authenticated & authorised user details through authorisedForTai.async" in {

      val fakeAuthority: Authority = Await.result(FakeAuthConnector.currentAuthority, 5 seconds).get
      val sut = createSUT
      when(sut.personService.personDetailsNew(any())(any())).thenReturn(Future.successful(generatePerson(generateNino)))

      val action = sut.exampleAuthorisedForTaiUsage()
      val res = Await.result(action.apply(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
      res.header.status mustBe OK

      sut.capturedUser match {

        case Some(user) => {
          user.getDisplayName mustBe "Kkk Sss"
          user.getNino mustBe fakeAuthorityFixtureNino
          user.authContext.user.userId mustBe fakeAuthority.uri
          user.authContext.principal.accounts mustBe fakeAuthority.accounts
        }
        case _ => fail("No TaiUser instance was present")
      }
    }

    "expose basic person details in TaiRoot form, for an authorised user through authorisedForTai.async" in {

      val person = generatePerson(generateNino)
      val sut = createSUT
      when(sut.personService.personDetailsNew(any())(any())).thenReturn(Future.successful(person))

      val action = sut.exampleAuthorisedForTaiUsage()
      val res = Await.result(action.apply(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
      res.header.status mustBe OK

      sut.capturedPerson mustBe Some(person)
      sut.capturedUser.get.person mustBe person
    }

    "expose the request object for an authorised user through authorisedForTai.async" in {

      val sut = createSUT
      when(sut.personService.personDetailsNew(any())(any())).thenReturn(Future.successful(generatePerson(generateNino)))

      val action = sut.exampleAuthorisedForTaiUsage()
      val request = RequestBuilder.buildFakeRequestWithAuth("GET")
      val res = Await.result(action.apply(request), 5 seconds)
      res.header.status mustBe OK

      sut.capturedRequest mustBe Some(request)
    }

    "return a redirect response to govt gateway sign in, for an unauthorised user" in {

      val sut = createSUT
      when(sut.personService.personDetailsNew(any())(any())).thenReturn(Future.successful(generatePerson(generateNino)))

      val action = sut.exampleAuthorisedForTaiUsage()
      val request = RequestBuilder.buildFakeRequestWithoutAuth("GET")
      val res = Await.result(action.apply(request), 5 seconds)

      res.header.status mustBe SEE_OTHER
      res.header.headers.getOrElse("Location", "") must include("gg/sign-in")

      sut.capturedPerson mustBe None
      sut.capturedRequest mustBe None
      sut.capturedUser mustBe None
    }

    "return the same error page in response to a failure when retrieving person details, regardless of the " +
      "underlying status code" in {

        val codes = List(INTERNAL_SERVER_ERROR, BAD_GATEWAY, BAD_REQUEST, FORBIDDEN, NOT_FOUND, MOVED_PERMANENTLY, SEE_OTHER)

        codes.foreach( code =>
          withClue(s"When testing error code $code :") { assertErrorPage(code) }
        )
    }
  }

  private def assertErrorPage(statusCode: Int) = {
    val sut = createSUT
    when(sut.personService.personDetailsNew(any())(any())).thenReturn(Future.failed(new HttpException("an example failure", statusCode)))

    val action = sut.exampleAuthorisedForTaiUsage()
    val request = RequestBuilder.buildFakeRequestWithAuth("GET")
    val res = action.apply(request)
    status(res) mustBe INTERNAL_SERVER_ERROR
    val doc: Document = Jsoup.parse(contentAsString(res))
    doc must haveHeadingWithText(Messages("tai.technical.error.heading"))
    doc must haveParagraphWithText(Messages("tai.technical.error.message"))
  }

  def generatePerson(nino:Nino) = Person(nino, "Kkk", "Sss", None, Address("l1", "l2", "l3", "pc", "country"), false, false)

  def generateNino: Nino = new Generator(new Random).nextNino

  val fakeAuthorityFixtureNino: String = FakeAuthConnector.nino.get
  val fakeAuthConnectorNino: String = generateNino.nino

  def createSUT = new SUT

  class SUT extends WithAuthorisedForTaiLite  with ErrorPagesHandler {

    override implicit def templateRenderer: TemplateRenderer = MockTemplateRenderer

    override implicit def partialRetriever: FormPartialRetriever = MockPartialRetriever

    override val delegationConnector: DelegationConnector = mock[DelegationConnector]

    override val authConnector: AuthConnector = FakeAuthConnector

    val personService = mock[PersonService]

    var capturedUser : Option[TaiUser] = None
    var capturedPerson: Option[Person] = None
    var capturedRequest: Option[Request[AnyContent]] = None

    def exampleAuthorisedForTaiUsage() = authorisedForTai(personService).async {

      user => person => request => {
        capturedUser = Some(user)
        capturedPerson = Some(person)
        capturedRequest = Some(request)
        Future.successful(Result(ResponseHeader(200), mock[HttpEntity]))
      }
    }
  }
}

