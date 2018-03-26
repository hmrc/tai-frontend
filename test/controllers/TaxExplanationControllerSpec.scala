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
import uk.gov.hmrc.tai.model.SessionData
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.tai.model.{SessionData, TaxCalculation}

import scala.concurrent.Future

class TaxExplanationControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  def createTestController(sessionData: Option[SessionData] = None,
                           taxCalculation: Option[TaxCalculation] = None) = {

    new TaxExplanationController {

      override val taiService: TaiService = mock[TaiService]
      override lazy val authConnector: AuthConnector = mock[AuthConnector]
      override lazy val auditConnector: AuditConnector = mock[AuditConnector]
      override lazy val delegationConnector: DelegationConnector = mock[DelegationConnector]
      override implicit def templateRenderer = MockTemplateRenderer
      override implicit def partialRetriever = MockPartialRetriever

      val ad = AuthBuilder.createFakeAuthData
      when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

      val sd = sessionData.getOrElse(AuthBuilder.createFakeSessionDataWithPY)
      when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sd))

      override val scottishTaxRateEnabled: Boolean = false
    }
  }

  "Calling the TaxExplanationController should" should {
    "call taxExplanationPage() successfully with an authorised session" in {
      val testController = createTestController()
      val result = testController.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe 200
      doc.title() must include(Messages("tai.incomeTax.calculated.heading"))
    }
  }

}
