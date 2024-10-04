/*
 * Copyright 2024 HM Revenue & Customs
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

/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.auth.core.retrieve.{Name, Retrieval, ~}
import uk.gov.hmrc.auth.core.{Nino => _, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class AuthRetrievalsSpec extends BaseSpec {

  val cc: ControllerComponents = stubControllerComponents()

  abstract class Harness(AuthRetrievals: AuthRetrievals) extends AbstractController(cc) {

    def onPageLoad(): Action[AnyContent] = AuthRetrievals { request =>
      Ok(request.taiUser.toString)
    }
  }

  object Harness {

    def fromAction(AuthRetrievals: AuthRetrievals): Harness =
      new Harness(AuthRetrievals) {}

    def successful[A](a: A): Harness = {
      val mocked = mock[AuthConnector]
      when(mocked.authorise[A](any(), any())(any(), any())).thenReturn(Future.successful(a))

      fromAction(new AuthRetrievalsImpl(mocked, mcc))
    }

    def failure(ex: Throwable): Harness =
      fromAction(new AuthRetrievalsImpl(new FakeFailingAuthConnector(ex), mcc))
  }

  class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
    ): Future[A] =
      Future.failed(exceptionToReturn)
  }

  private implicit class HelperOps[A](a: A) {
    def ~[B](b: B) = new ~(a, b)
  }

  "Auth Action" when {
    s"is not logged in" must {
      "throw an exception" in {
        val controller = Harness.failure(MissingBearerToken())
        val result = controller.onPageLoad()(fakeRequest)

        whenReady(result.failed) { e =>
          e mustBe a[MissingBearerToken]
        }
      }
    }
  }

  "Given the user is authorised" should {

    "return the users nino & name in an Ok response" when {
      val saUtr = Some("000111222")
      val nino = Nino(new Generator().nextNino.nino)
      val baseRetrieval =
        Some(nino.nino) ~ saUtr

      "no trusted helper data is returned" in {
        val controller = Harness.successful(baseRetrieval ~ None ~ Some(Name(Some("first"), Some("last"))))
        val result = controller.onPageLoad()(fakeRequest)
        val expectedTaiUser = AuthedUserWithName(nino, saUtr, None, Some("first"), Some("last"))

        contentAsString(result) mustBe expectedTaiUser.toString
      }

      "trusted helper data is returned" in {

        val nino = new Generator().nextNino
        val trustedHelper = TrustedHelper("principalName", "attorneyName", "returnLinkUrl", Some(nino.nino))
        val controller =
          Harness.successful(baseRetrieval ~ Some(trustedHelper) ~ Some(Name(Some("first"), Some("last"))))
        val result = controller.onPageLoad()(fakeRequest)

        val expectedTaiUser =
          AuthedUserWithName(nino, Some("000111222"), Some(trustedHelper), Some("first"), Some("last"))

        contentAsString(result) mustBe expectedTaiUser.toString
      }

    }
  }
}
