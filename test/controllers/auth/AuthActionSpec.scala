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

package controllers.auth

import controllers.{FakeTaiPlayApplication, routes}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Controller
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
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

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad() = authAction { request => BadRequest }
  }

  class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
    val serviceUrl: String = ""

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exceptionToReturn)
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
          val authAction = new AuthActionImpl(new FakeFailingAuthConnector(error))
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().toString)
        }
      }
    })
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
        s"there is an ${
          error.toString
        }" in {
          val authAction = new AuthActionImpl(new FakeFailingAuthConnector(error))
          val controller = new Harness(authAction)

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
        s"there is an ${
          error.toString
        }" in {
          val authAction = new AuthActionImpl(new FakeFailingAuthConnector(error))
          val controller = new Harness(authAction)

          val result = controller.onPageLoad()(fakeVerifyRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.loginVerify().toString)
        }
      })
    }
  }

  "Given an unexpected exception occurred" should {
    "return the exception" in {
      val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new Exception("Help")))
      val controller = new Harness(authAction)

      val ex: Exception = the[Exception] thrownBy Await.result(controller.onPageLoad()(fakeRequest), 5.seconds)
      ex.getMessage mustBe "Help"
    }
  }
}

