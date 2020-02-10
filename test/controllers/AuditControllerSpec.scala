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

package controllers

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.service.AuditService

import scala.concurrent.Future

class AuditControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {

  "Audit Controller" must {
    "send specific audit event and redirect" when {
      "triggered from any page" which {
        "redirects to appropriate url " in {
          val testAuditController = new TestAuditController

          val result = testAuditController.auditLinksToIForm("any-iform")(
            RequestBuilder
              .buildFakeRequestWithAuth("GET")
              .withHeaders("Referer" ->
                redirectUri))

          status(result) mustBe SEE_OTHER
          verify(auditService, times(1))
            .sendAuditEventAndGetRedirectUri(any(), any())(any(), any())
          redirectLocation(result) mustEqual Some(redirectUri)
        }
      }
    }
  }

  private val redirectUri = "redirectUri"

  val auditService = mock[AuditService]

  class TestAuditController
      extends AuditController(
        auditService,
        FakeAuthAction,
        FakeValidatePerson,
        MockPartialRetriever,
        MockTemplateRenderer
      ) {

    when(auditService.sendAuditEventAndGetRedirectUri(any(), any())(any(), any()))
      .thenReturn(Future.successful(redirectUri))
  }

}
