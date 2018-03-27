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
import data.TaiData
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testServices.FakeActivityLoggerService
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.tai.config.{FrontEndDelegationConnector, AuditConnector => TaiAuditConnector}
import uk.gov.hmrc.tai.connectors.PreferencesFrontendConnector
import uk.gov.hmrc.tai.connectors.PreferencesFrontendConnector.ActivatePaperlessResponse
import uk.gov.hmrc.tai.connectors.PreferencesFrontendConnector.ActivatePaperlessResponse._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{GiftAidPayments, MaintenancePayments}
import uk.gov.hmrc.tai.model.{SessionData, TaiRoot, TaxSummaryDetails}
import uk.gov.hmrc.tai.service.{ActivityLoggerService, AuditService, CodingComponentService, TaiService}
import uk.gov.hmrc.tai.util.AuditConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CurrentYearPageControllerSpec
  extends PlaySpec
    with FakeTaiPlayApplication
    with ScalaFutures
    with I18nSupport
    with MockitoSugar
    with AuditConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val nino = new Generator().nextNino

  val mockEmptyLandingDetails = TaxSummaryDetails(nino.nino, 99, None)

  implicit val hc = HeaderCarrier()

  "Calling the Current Year Page method" should {

    "display service unavailable error page when http response is 403" in {
      val testTaxSummary = TaiData.getPotentialUnderpaymentTaxSummary
      val testCurrentYearPageController = buildCurrentYearPageController(mockTaxSummaryDetails = testTaxSummary, responseStatus = BAD_REQUEST)
      val result = testCurrentYearPageController.reliefsPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe BAD_REQUEST
    }

    "call reliefsPage() successfully with an authorised session " in {
      val testTaxSummary = TaiData.getIncomesAndPensionsTaxSummary
      val testController = buildCurrentYearPageController(testTaxSummary)
      val result = testController.reliefsPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
    }
  }

  val codingComponents: Seq[CodingComponent] = Seq(CodingComponent(GiftAidPayments, None, 1000,  "GiftAidPayments description"),
    CodingComponent(MaintenancePayments, None, 100,  "MaintenancePayments description"))

  def buildCurrentYearPageController(mockTaxSummaryDetails: TaxSummaryDetails,
                                     mockCachedDetails: Option[TaxSummaryDetails] = None,
                                     activatePaperlessEnabled: Boolean = true,
                                     activatePaperlessEvenIfGatekeeperFailsEnabled: Boolean = false,
                                     mockPaperlessResponse: => ActivatePaperlessResponse = ActivationNotAllowed,
                                     manualCorrespondence: Boolean = false,
                                     responseStatus: Int = OK,
                                     newTaxCompPagesEnabled: Boolean = false) = new CurrentYearPageController {

    final implicit val hc = HeaderCarrier()

    override implicit def templateRenderer = MockTemplateRenderer

    override implicit def partialRetriever = MockPartialRetriever

    override implicit def auditConnector: AuditConnector = TaiAuditConnector

    override implicit def authConnector: AuthConnector = FakeAuthConnector

    override implicit def delegationConnector: DelegationConnector = FrontEndDelegationConnector

    override def activityLoggerService: ActivityLoggerService = FakeActivityLoggerService

    override def activatePaperless = activatePaperlessEnabled

    override def activatePaperlessEvenIfGatekeeperFails = activatePaperlessEvenIfGatekeeperFailsEnabled

    override def taiService = createMockTaiService(mockTaxSummaryDetails, responseStatus)

    override def codingComponentService = createMockCodingComponentService

    override val auditService: AuditService = mock[AuditService]

    var attemptedToActivatePaperless = false

    override def preferencesFrontendConnector: PreferencesFrontendConnector = new PreferencesFrontendConnector {
      override def http = ???

      override def baseUrl = ???

      override def returnUrl: String = ???

      override def queryParamEcrypter: Encrypter = ???

      override def returnLinkText: String = ???

      override def crypto: (String) => String = ???

      override def activatePaperless(nino: Nino)(implicit request: RequestHeader) = {
        attemptedToActivatePaperless = true
        Future(mockPaperlessResponse)
      }
    }
  }

  private def createMockCodingComponentService() = {
    val mockCodingComponentService = mock[CodingComponentService]
    when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.successful(codingComponents))
    mockCodingComponentService
  }

  private def createMockTaiService(taxSummaryDets: TaxSummaryDetails, responseStatus: Int) = {
    val mockTaiService = mock[TaiService]

    responseStatus match {
      case BAD_REQUEST =>
        val exception = new Upstream4xxResponse("Upstream4xxException", 403, 0) {
          override val upstreamResponseCode = 403
        }
        when(mockTaiService.taiSession(any(), any(), any())(any())).thenReturn(Future.failed(exception))
      case NOT_FOUND =>
        val exception = new NotFoundException("appStatusMessage: No Tax Account Information Found")
        when(mockTaiService.taiSession(any(), any(), any())(any())).thenReturn(Future.failed(exception))
      case _ =>
        val sessionData: SessionData = createMockSessionData(taxSummaryDets)
        when(mockTaiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sessionData))
    }
    mockTaiService
  }

  private def createMockSessionData(taxSummaryDets: TaxSummaryDetails): SessionData = {
    val msd = mock[SessionData]
    when(msd.taxSummaryDetailsCY).thenReturn(taxSummaryDets)
    when(msd.taiRoot).thenReturn(None)
    when(msd.nino).thenReturn(taxSummaryDets.nino)
    when(msd.taiRoot).thenReturn(Some(TaiRoot(taxSummaryDets.nino + "A")))
    msd
  }
}
