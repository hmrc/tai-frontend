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

package controllers

import builders.{UserBuilder, RequestBuilder}
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.service.{AuditService, SessionService}

import scala.concurrent.Future
import scala.util.Random

class ExternalServiceRedirectControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "External Service Redirect controller" must {
    "redirect to external url" when {
      "a valid service and i-form name has been passed" in {
        val sut = createSut

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET").withHeaders("Referer" -> redirectUri)

        when(auditService.sendAuditEventAndGetRedirectUri(any(), Matchers.eq("Test"))(any(), any())).thenReturn(Future.successful(redirectUri))
        when(sessionService.invalidateCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))

        val result = sut.auditInvalidateCacheAndRedirectService("Test")(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe redirectUri
        verify(sessionService, times(1)).invalidateCache()(any())
      }
    }
  }

  private val redirectUri = "redirectUri"
  private implicit val hc = HeaderCarrier()

  def createSut = new SUT

  val sessionService = mock[SessionService]
  val auditService = mock[AuditService]

  class SUT extends ExternalServiceRedirectController(
    sessionService,
    auditService,
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  )
}
