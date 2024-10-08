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

package controllers

import builders.RequestBuilder
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.test.Helpers._
import uk.gov.hmrc.tai.service.AuditService
import utils.BaseSpec

import scala.concurrent.Future

class AuditControllerSpec extends BaseSpec {

  private val redirectUri = "redirectUri"

  val auditService: AuditService = mock[AuditService]

  class TestAuditController
      extends AuditController(
        auditService,
        mockAuthJourney,
        mcc
      ) {

    when(auditService.sendAuditEventAndGetRedirectUri(any(), any())(any(), any()))
      .thenReturn(Future.successful(redirectUri))
  }

  "Audit Controller" must {
    "send specific audit event and redirect" when {
      "triggered from any page" which {
        "redirects to appropriate url " in {
          val testAuditController = new TestAuditController

          val result = testAuditController.auditLinksToIForm("any-iform")(
            RequestBuilder
              .buildFakeRequestWithAuth("GET")
              .withHeaders(
                "Referer" ->
                  redirectUri
              )
          )

          status(result) mustBe SEE_OTHER
          verify(auditService, times(1))
            .sendAuditEventAndGetRedirectUri(any(), any())(any(), any())
          redirectLocation(result) mustBe Some(redirectUri)
        }
      }
    }
  }

}
