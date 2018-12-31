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
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.{AuditService, PersonService}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AuditControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {

  "Audit Controller" must {
    "send specific audit event and redirect" when {
      "triggered from any page" which {
        "redirects to appropriate url " in {
          val sut = createSut
          when(auditService.sendAuditEventAndGetRedirectUri(any(), any())(any(), any())).thenReturn(Future.successful(redirectUri))

          val result = Await.result(sut.auditLinksToIForm("any-iform")(RequestBuilder.buildFakeRequestWithAuth("GET").withHeaders("Referer" ->
            redirectUri)), 5.seconds)

          result.header.status mustBe 303
          verify(auditService, times(1)).sendAuditEventAndGetRedirectUri(Matchers.eq(Nino(nino)),
            Matchers.eq("any-iform"))(any(), any())
        }
      }
    }
  }

  private val nino = AuthBuilder.nino.nino
  private val person = Person(Nino(nino), "firstname", "surname", false, false)
  private val redirectUri = "redirectUri"

  def createSut = new SUT

  val personService: PersonService = mock[PersonService]
  val auditService = mock[AuditService]

  class SUT extends AuditController(
    personService,
    auditService,
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(person))

    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData)
  }

}
