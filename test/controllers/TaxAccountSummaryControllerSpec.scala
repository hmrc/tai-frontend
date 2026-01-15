/*
 * Copyright 2025 HM Revenue & Customs
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
import cats.data.EitherT
import cats.instances.future.*
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import play.api.test.Helpers.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.income.*
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.AuditConstants
import utils.{BaseSpec, TaxAccountSummaryTestData}
import views.html.IncomeTaxSummaryView

import scala.concurrent.Future

class TaxAccountSummaryControllerSpec extends BaseSpec with TaxAccountSummaryTestData {

  private val testAmount: BigDecimal              = 100
  override val nonTaxCodeIncome: NonTaxCodeIncome = NonTaxCodeIncome(
    Some(
      uk.gov.hmrc.tai.model.domain.income.UntaxedInterest(UntaxedInterestIncome, None, testAmount, "Untaxed Interest")
    ),
    Seq(OtherNonTaxCodeIncome(Profit, None, testAmount, "Profit"))
  )

  val mockAuditService: AuditService           = mock[AuditService]
  val mockEmploymentService: EmploymentService = mock[EmploymentService]
  val mockTaxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockTrackingService: TrackingService     = mock[TrackingService]
  val mockIabdService: IabdService             = mock[IabdService]

  def sut: TaxAccountSummaryController = new TaxAccountSummaryController(
    mockTaxAccountService,
    mockEmploymentService,
    mockIabdService,
    mockAuditService,
    mockAuthJourney,
    appConfig,
    mcc,
    inject[IncomeTaxSummaryView],
    mockTrackingService,
    inject[ErrorPagesHandler]
  )

  val defaultUserAnswers: UserAnswers =
    UserAnswers("testSessionId", nino.nino)

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers("testSessionId", nino.nino))
    Mockito.reset(mockAuditService, mockEmploymentService, mockTaxAccountService, mockTrackingService)

    when(mockEmploymentService.employments(any(), any())(any()))
      .thenReturn(EitherT.right(Future.successful(Seq(employment))))

    when(mockTaxAccountService.newNonTaxCodeIncomes(any(), any())(any()))
      .thenReturn(EitherT.rightT(None))

    when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
      .thenReturn(EitherT.rightT(taxAccountSummary))

    when(mockTaxAccountService.newTaxCodeIncomes(any(), any())(any()))
      .thenReturn(EitherT.rightT(Seq.empty))

    when(mockTrackingService.isAnyIFormInProgress(any())(any(), any(), any()))
      .thenReturn(Future.successful(NoTimeToProcess))

    when(mockIabdService.getIabds(any(), any())(any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty[IabdDetails]))
  }

  "onPageLoad" must {

    "display the income tax summary page" in {
      setup(UserAnswers("testSessionId", nino.nino))

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
        .thenReturn(EitherT.rightT(taxAccountSummary))

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc           = Jsoup.parse(contentAsString(result))
      val expectedTitle =
        s"${messagesApi("tai.incomeTaxSummary.heading.part1", TaxYearRangeUtil.currentTaxYearRangeBreak)}"
      doc.title() must include(expectedTitle)
    }

    "after recovering from a 404 from tax account service" in {
      when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
        .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
    }

    "raise an audit event" in {
      when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
        .thenReturn(EitherT.rightT(taxAccountSummary))

      when(
        mockAuditService.createAndSendAuditEvent(
          meq(AuditConstants.TaxAccountSummaryUserEntersSummaryPage),
          meq(Map("nino" -> nino.nino))
        )(any(), any())
      ).thenReturn(Future.successful(Success))

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      verify(mockAuditService, times(1))
        .createAndSendAuditEvent(
          meq(AuditConstants.TaxAccountSummaryUserEntersSummaryPage),
          meq(Map("nino" -> nino.nino))
        )(any(), any())
    }

    "display an error page" when {
      "a downstream error has occurred in one of the TaiResponse responding service methods" in {
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream unauthorised exception has occurred in the tax account service" in {
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("Unauthorised", UNAUTHORIZED)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream error has occurred in the tax code income service (which does not reply with TaiResponse type)" in {
        when(mockTaxAccountService.newTaxCodeIncomes(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("Unauthorised", UNAUTHORIZED)))

        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(nonTaxCodeIncome)
        )
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT(taxAccountSummary)
        )
        when(mockEmploymentService.employments(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq(employment)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream error has occurred in one of the TaiResponse responding service methods due to not being authorised" in {
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", UNAUTHORIZED)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
