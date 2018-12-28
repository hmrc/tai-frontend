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

package controllers

import builders.{AuthBuilder, RequestBuilder}
import mocks.MockTemplateRenderer
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.service.{AuditService, PersonService, SessionService}

import scala.concurrent.Future
import scala.util.Random

class ExternalServiceRedirectControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "External Service Redirect controller" must {
    "redirect to external url" when {
      "a valid service and i-form name has been passed" in {
        val sut = createSut
        when(sut.auditService.sendAuditEventAndGetRedirectUri(Matchers.eq(nino), Matchers.eq("Test"))(any(), any())).thenReturn(Future.successful(redirectUri))
        when(sessionService.invalidateCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))

        val result = sut.auditInvalidateCacheAndRedirectService("Test")(RequestBuilder.buildFakeRequestWithAuth("GET").withHeaders("Referer" ->
          redirectUri))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe redirectUri
        verify(sessionService, times(1)).invalidateCache()(any())
      }
    }
  }

  private val redirectUri = "redirectUri"
  private implicit val hc = HeaderCarrier()
  private val nino = new Generator(new Random).nextNino

  def createSut = new SUT

  val personService: PersonService = mock[PersonService]
  val sessionService = mock[SessionService]

  class SUT extends ExternalServiceRedirectController(
    sessionService,
    personService,
    mock[AuditService],
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

    when(authConnector.currentAuthority(any(), any())).thenReturn(
      Future.successful(Some(AuthBuilder.createFakeAuthority(nino.nino))))
  }

}
