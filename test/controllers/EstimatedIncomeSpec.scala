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

import TestConnectors.FakeAuthConnector
import builders.{RequestBuilder, UserBuilder}
import controllers.viewModels.EstimatedIncomePageVMBuilder
import data.TaiData
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.RequestHeader
import testServices.{FakeActivityLoggerService, FakeHasFormPartialService}
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{CorePut, HeaderCarrier}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.config.{FrontEndDelegationConnector, AuditConnector => TaiAuditConnector}
import uk.gov.hmrc.tai.connectors.PreferencesFrontendConnector.ActivatePaperlessResponse
import uk.gov.hmrc.tai.connectors.PreferencesFrontendConnector.ActivatePaperlessResponse.ActivationNotAllowed
import uk.gov.hmrc.tai.connectors.{DomainConnector, PreferencesFrontendConnector}
import uk.gov.hmrc.tai.model.{SessionData, TaiRoot, TaxSummaryDetails}
import uk.gov.hmrc.tai.service.{ActivityLoggerService, TaiService}
import uk.gov.hmrc.tai.viewModels.{BandedGraph, EstimatedIncomeViewModel}

import scala.concurrent.Future

class EstimatedIncomeSpec extends UnitSpec with MockitoSugar with FakeTaiPlayApplication {

  val mockDomainConnector = mock[DomainConnector]
  val mockEstimatedIncomePageVMBuilder = mock[EstimatedIncomePageVMBuilder]
  val expectedEstimatedIncomeViewModel = EstimatedIncomeViewModel(newGraph = BandedGraph(id = "id"), ukDividends = None, taxBands = None,
    increasesTax = true, incomeTaxReducedToZeroMessage = None,
    incomeTaxEstimate = 14514.6, incomeEstimate = 62219, taxFreeEstimate = 10000, taxRegion = "")

  "Estimated income page" should {
    "have the correct estimated income tax, income and tax free amount" in {
      val testTaxSummary = TaiData.getEverything
      val controller  = buildEstimatedIncomeController(testTaxSummary,Some(testTaxSummary))

      when(mockEstimatedIncomePageVMBuilder.createObject(any(), any())(any(), any())).thenReturn(Future.successful(expectedEstimatedIncomeViewModel))

      val result = controller.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) shouldBe 200
    }
  }

  def buildEstimatedIncomeController(mockDetails: TaxSummaryDetails,
                                     mockCachedDetails: Option[TaxSummaryDetails]=None,
                                     returnCode:Int=200,
                                     activatePaperlessEnabled: Boolean = true,
                                     activatePaperlessEvenIfGatekeeperFailsEnabled: Boolean = false,
                                     mockPaperlessResponse: => ActivatePaperlessResponse = ActivationNotAllowed) = new EstimatedIncomeTaxController {

    override implicit def auditConnector: AuditConnector = TaiAuditConnector
    override implicit def authConnector: AuthConnector = FakeAuthConnector
    override implicit def delegationConnector: DelegationConnector = FrontEndDelegationConnector
    override def activityLoggerService : ActivityLoggerService = FakeActivityLoggerService
    override def activatePaperless: Boolean = activatePaperlessEnabled
    override def activatePaperlessEvenIfGatekeeperFails: Boolean = activatePaperlessEvenIfGatekeeperFailsEnabled
    override implicit def templateRenderer: MockTemplateRenderer.type = MockTemplateRenderer
    override implicit def partialRetriever: MockPartialRetriever.type = MockPartialRetriever
    override def taxPlatformTaiRootLandingPageUri: String = ???
    override val partialService: FakeHasFormPartialService.type = FakeHasFormPartialService
    val nino = new Generator().nextNino

    override val taiService: TaiService = mock[TaiService]

    when(taiService.taiSession(any(), any(), any())(any()))
      .thenReturn(Future.successful(SessionData(nino.nino, Some(TaiRoot(nino.nino, 1, "Mr", "Monkey", None, "Great Sage Equal of Heaven", "name", false, Some(false))), mockDetails, None, None)))

    final implicit val hc: HeaderCarrier = HeaderCarrier()
    val user = UserBuilder.apply()

    var attemptedToActivatePaperless = false

    override def preferencesFrontendConnector: PreferencesFrontendConnector = new PreferencesFrontendConnector {
      override def http: CorePut = ???
      override def baseUrl: String = ???
      override def returnUrl: String = ???
      override def queryParamEcrypter: Encrypter = ???
      override def returnLinkText: String = ???
      override def crypto: (String) => String = ???

      override def activatePaperless(nino: Nino)(implicit request: RequestHeader): Future[ActivatePaperlessResponse] = {
        attemptedToActivatePaperless = true
        Future(mockPaperlessResponse)
      }
    }

    override val estimatedIncomePageVM: EstimatedIncomePageVMBuilder = mockEstimatedIncomePageVMBuilder
  }
}
