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

package controllers.auth

import controllers.{FakeTaiPlayApplication, routes}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Controller
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{Nino => _, _}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, Retrieval, ~}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.tai.util.constants.TaiConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class AuthActionSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy val fakeVerifyRequest = FakeRequest("GET", "/").withSession(
    SessionKeys.authProvider -> TaiConstants.AuthProviderVerify
  )

  abstract class Harness(authAction: AuthAction) extends Controller {

    def onPageLoad() = authAction { request =>
      Ok(request.taiUser.toString)
    }
  }

  object Harness {

    def fromAction(authAction: AuthAction): Harness =
      new Harness(authAction) {}

    def successful[A](a: A): Harness = {
      val mocked = mock[AuthConnector]
      when(mocked.authorise[A](any(), any())(any(), any())).thenReturn(Future.successful(a))

      fromAction(new AuthActionImpl(mocked))
    }

    def failure(ex: Throwable): Harness =
      fromAction(new AuthActionImpl(new FakeFailingAuthConnector(ex)))
  }

  class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[A] =
      Future.failed(exceptionToReturn)
  }

  private implicit class HelperOps[A](a: A) {
    def ~[B](b: B) = new ~(a, b)
  }

  "Auth Action" when {
    val authErrors = Seq[AuthorisationException](
      new InsufficientConfidenceLevel,
      new InsufficientEnrolments,
      new UnsupportedAffinityGroup,
      new UnsupportedCredentialRole,
      new UnsupportedAuthProvider,
      new IncorrectCredentialStrength,
      new InternalError
    )

    authErrors.foreach(error => {
      s"the user has ${error.toString}" must {
        "redirect the user to an unauthorised page " in {
          val controller = Harness.failure(error)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().toString)
        }
      }
    })
  }

  "Given the user is authorised" should {

    "return the users nino in an Ok response" when {

      val authProviderGG = Some(TaiConstants.AuthProviderGG)
      val creds = Some(Credentials("GG", TaiConstants.AuthProviderGG))
      val saUtr = Some("000111222")
      val nino = new Generator().nextNino.nino
      val baseRetrieval =
        creds ~ Some(nino) ~ Some(Name(Some("mainUser"), Some(""))) ~ saUtr ~ ConfidenceLevel.L200

      "no trusted helper data is returned" in {

        val controller = Harness.successful(baseRetrieval ~ None)
        val result = controller.onPageLoad()(fakeRequest)

        val expectedTaiUser = AuthedUser("mainUser", nino, saUtr, authProviderGG, ConfidenceLevel.L200, None)

        contentAsString(result) mustBe expectedTaiUser.toString
      }

      "trusted helper data is returned" in {

        val nino = new Generator().nextNino
        val trustedHelper = TrustedHelper("principalName", "attorneyName", "returnLinkUrl", nino.nino)
        val controller = Harness.successful(baseRetrieval ~ Some(trustedHelper))
        val result = controller.onPageLoad()(fakeRequest)

        val expectedTaiUser =
          AuthedUser(
            "principalName",
            nino.nino,
            Some("000111222"),
            authProviderGG,
            ConfidenceLevel.L200,
            Some(trustedHelper))

        contentAsString(result) mustBe expectedTaiUser.toString
      }
    }

  }

  "Given the user is unauthorised" should {
    "redirect the user to the GG log in page" when {

      val authErrors = Seq[RuntimeException](
        new InvalidBearerToken,
        new BearerTokenExpired,
        new MissingBearerToken,
        new SessionRecordNotFound
      )

      authErrors.foreach(error => {
        s"there is an ${error.toString}" in {
          val controller = Harness.failure(error)

          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.loginGG().toString)
        }
      })
    }

    "redirect the user to the Verify log in page" when {
      val authErrors = Seq[RuntimeException](
        new InvalidBearerToken,
        new BearerTokenExpired,
        new MissingBearerToken,
        new SessionRecordNotFound
      )

      authErrors.foreach(error => {
        s"there is an ${error.toString}" in {
          val controller = Harness.failure(error)

          val result = controller.onPageLoad()(fakeVerifyRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.loginVerify().toString)
        }
      })
    }
  }

  "Given an unexpected exception occurred" should {
    "return the exception" in {
      val controller = Harness.failure(new Exception("Help"))

      val ex: Exception = the[Exception] thrownBy Await.result(controller.onPageLoad()(fakeRequest), 5.seconds)
      ex.getMessage mustBe "Help"
    }
  }
}
