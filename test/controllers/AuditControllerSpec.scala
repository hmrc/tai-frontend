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
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.tai.service.{AuditService, TaiService}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AuditControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {

  "Audit Controller" must {
    "send specific audit event and redirect" when {
      "triggered from any page" which {
        "redirects to appropriate url " in {
          val sut = createSut
          when(sut.auditService.sendAuditEventAndGetRedirectUri(any(), any())(any(), any())).thenReturn(Future.successful(redirectUri))

          val result = Await.result(sut.auditLinksToIForm("any-iform")(RequestBuilder.buildFakeRequestWithAuth("GET").withHeaders("Referer" ->
            redirectUri)), 5.seconds)

          result.header.status mustBe 303
          verify(sut.auditService, times(1)).sendAuditEventAndGetRedirectUri(Matchers.eq(Nino(nino)),
            Matchers.eq("any-iform"))(any(), any())
        }
      }
    }
  }

  private val nino = AuthBuilder.nino.nino
  private val taiRoot = TaiRoot(nino = nino)
  private val redirectUri = "redirectUri"

  def createSut = new SUT

  class SUT extends AuditController {
    override val taiService: TaiService = mock[TaiService]

    override val auditService: AuditService = mock[AuditService]

    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever

    override protected val authConnector: AuthConnector = mock[AuthConnector]

    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]

    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(taiRoot))

    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData)
  }
}
