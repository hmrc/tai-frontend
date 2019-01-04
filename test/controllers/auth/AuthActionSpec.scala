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
import controllers.routes
import controllers.FakeTaiPlayApplication
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Controller
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientConfidenceLevel, UnsupportedAffinityGroup}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.PersonService
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad() = authAction { request => Ok }
  }

  class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
    val serviceUrl: String = ""

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exceptionToReturn)
  }

  def fakeRequest = FakeRequest("", "")

  "Auth Action" when {
    "the user has insufficient confidence level" must {
      "redirect the user to an unauthorised page " in {
        val authAction = new AuthActionImpl(mock[PersonService], new FakeFailingAuthConnector(new InsufficientConfidenceLevel))
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().toString)
      }
    }

    "the user has an unsupported affinity group" must {
      "redirect the user to an unauthorised page " in {
        val authAction = new AuthActionImpl(mock[PersonService], new FakeFailingAuthConnector(new UnsupportedAffinityGroup))
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().toString)
      }
    }
  }
}

