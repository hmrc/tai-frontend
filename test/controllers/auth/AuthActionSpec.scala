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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Controller
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientConfidenceLevel, UnsupportedAffinityGroup}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.PersonService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad() = authAction { request => Ok }
  }

  class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
    val serviceUrl: String = ""

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exceptionToReturn)
  }

  "Auth Action" when {
    "the user has insufficient confidence level" must {
      "redirect the user to an unauthorised page " in {
        val authAction = new AuthActionImpl(mock[PersonService], new FakeFailingAuthConnector(new InsufficientConfidenceLevel))
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().toString)
      }
    }
  }
}

