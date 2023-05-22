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

package controllers

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tai.service.{AuditService, SessionService}
import utils.BaseSpec

import scala.concurrent.Future

class ExternalServiceRedirectControllerSpec extends BaseSpec {

  "External Service Redirect controller - auditInvalidateCacheAndRedirectService" must {
    "redirect to external url" when {
      "a valid service and i-form name has been passed" in {
        val sut = createSut

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET").withHeaders("Referer" -> redirectUri)

        when(auditService.sendAuditEventAndGetRedirectUri(any(), meq("Test"))(any(), any()))
          .thenReturn(Future.successful(redirectUri))
        when(sessionService.invalidateCache()(any())).thenReturn(Future.successful(HttpResponse.apply(OK, "")))

        val result = sut.auditInvalidateCacheAndRedirectService("Test")(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe redirectUri
        verify(sessionService, times(1)).invalidateCache()(any())
      }
    }

    "give an internal server error" when {
      "an invalid service and i-form name has been passed" in {

        val sut = createSut

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET").withHeaders("Referer" -> redirectUri)

        when(auditService.sendAuditEventAndGetRedirectUri(any(), meq("Test"))(any(), any()))
          .thenReturn(Future.failed(new IllegalArgumentException))

        val result = sut.auditInvalidateCacheAndRedirectService("Test")(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "External Service Redirect controller - auditAndRedirectService" must {
    "redirect to external url - " when {
      "a valid service and i-form name has been passed" in {
        val sut = createSut

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET").withHeaders("Referer" -> redirectUri)

        when(auditService.sendAuditEventAndGetRedirectUri(any(), meq("Test"))(any(), any()))
          .thenReturn(Future.successful(redirectUri))

        val result = sut.auditAndRedirectService("Test")(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe redirectUri
      }
    }

    "give an internal server error" when {
      "an invalid service and i-form name has been passed" in {

        val sut = createSut

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET").withHeaders("Referer" -> redirectUri)

        when(auditService.sendAuditEventAndGetRedirectUri(any(), meq("Test"))(any(), any()))
          .thenReturn(Future.failed(new IllegalArgumentException))

        val result = sut.auditAndRedirectService("Test")(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  private val redirectUri = "redirectUri"

  def createSut = new SUT

  val sessionService = mock[SessionService]
  val auditService = mock[AuditService]

  class SUT
      extends ExternalServiceRedirectController(
        sessionService,
        auditService,
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        inject[ErrorPagesHandler]
      )
}
