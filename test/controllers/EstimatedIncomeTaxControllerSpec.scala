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
import uk.gov.hmrc.tai.connectors.PreferencesFrontendConnector
import controllers.viewModels.EstimatedIncomePageVMBuilder
import mocks.MockTemplateRenderer
import uk.gov.hmrc.tai.viewModels.{BandedGraph, EstimatedIncomeViewModel}
import uk.gov.hmrc.tai.model.SessionData
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.twirl.api.Html
import uk.gov.hmrc.tai.service.{ActivityLoggerService, HasFormPartialService, TaiService}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.{HtmlPartial, PartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.{SessionData, TaiRoot, TaxSummaryDetails}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class EstimatedIncomeTaxControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "EstimatedIncomeTaxController" should {
    "load all the iForm links" when {
      "page has loaded" in {
        val sut = createSUT
        when(sut.estimatedIncomePageVM.createObject(any(), any())(any(), any())).thenReturn(Future.successful(viewModel))
        when(sut.taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sessionData))
        when(sut.partialService.getIncomeTaxPartial(any())).thenReturn(Future.successful[HtmlPartial]
          (HtmlPartial.Success(Some("title"), Html("<title/>"))))

        val result = Await.result(sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5.seconds)

        result.header.status mustBe 200

        verify(sut.partialService, times(1)).getIncomeTaxPartial(any())
      }
    }
  }

  private val nino = new Generator().nextNino.nino
  private val viewModel = EstimatedIncomeViewModel(newGraph = BandedGraph(id = "id"),
    ukDividends = None, taxBands = None,
    increasesTax = true, incomeTaxReducedToZeroMessage = None,
    incomeTaxEstimate = 14514.6, incomeEstimate = 62219, taxFreeEstimate = 10000, taxRegion = "UK")
  private val taxSummaryDetails = TaxSummaryDetails(nino = nino, version = 0)
  private val taiRoot = TaiRoot(nino = nino)
  private val sessionData = SessionData(nino = nino, taxSummaryDetailsCY = taxSummaryDetails, taiRoot = Some(taiRoot))

  def createSUT = new SUT

  class SUT extends EstimatedIncomeTaxController {

    override val estimatedIncomePageVM: EstimatedIncomePageVMBuilder = mock[EstimatedIncomePageVMBuilder]

    override val taiService: TaiService = mock[TaiService]

    override val preferencesFrontendConnector: PreferencesFrontendConnector = mock[PreferencesFrontendConnector]

    override val activatePaperless: Boolean = true

    override val activatePaperlessEvenIfGatekeeperFails: Boolean = true

    override val taxPlatformTaiRootLandingPageUri: String = ""

    override val activityLoggerService: ActivityLoggerService = mock[ActivityLoggerService]

    override val partialService: HasFormPartialService = mock[HasFormPartialService]

    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

    override implicit val partialRetriever: PartialRetriever = mock[PartialRetriever]

    override val auditConnector: AuditConnector = mock[AuditConnector]

    override protected val authConnector: AuthConnector = mock[AuthConnector]

    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]

    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData)
  }

}
