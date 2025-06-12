/*
 * Copyright 2024 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{UnauthorizedException, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.{IncomeSources, UserAnswers}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.{ApiBackendChoice, TaxYearRangeUtil}
import uk.gov.hmrc.tai.util.constants.AuditConstants
import uk.gov.hmrc.tai.viewModels.TaxAccountSummaryViewModel
import utils.{BaseSpec, TaxAccountSummaryTestData}
import views.html.IncomeTaxSummaryView

import scala.concurrent.Future

class TaxAccountSummaryControllerSpec extends BaseSpec with TaxAccountSummaryTestData {

  private val testAmount: BigDecimal              = 100
  override val nonTaxCodeIncome: NonTaxCodeIncome = NonTaxCodeIncome(
    Some(
      uk.gov.hmrc.tai.model.domain.income
        .UntaxedInterest(UntaxedInterestIncome, None, testAmount, "Untaxed Interest")
    ),
    Seq(
      OtherNonTaxCodeIncome(Profit, None, testAmount, "Profit")
    )
  )

  val auditService: AuditService                         = mock[AuditService]
  val employmentService: EmploymentService               = mock[EmploymentService]
  val taxAccountService: TaxAccountService               = mock[TaxAccountService]
  val taxAccountSummaryService: TaxAccountSummaryService = mock[TaxAccountSummaryService]
  val mockTrackingService: TrackingService               = mock[TrackingService]
  val mockEmploymentService: EmploymentService           = mock[EmploymentService]
  val mockApiBackendChoice: ApiBackendChoice             = mock[ApiBackendChoice]

  def sut: TaxAccountSummaryController = new TaxAccountSummaryController(
    taxAccountService,
    taxAccountSummaryService,
    mockEmploymentService,
    auditService,
    mockAuthJourney,
    appConfig,
    mcc,
    inject[IncomeTaxSummaryView],
    mockTrackingService,
    mockApiBackendChoice,
    inject[ErrorPagesHandler]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers("testSessionId", nino.nino))
    Mockito.reset(auditService)
  }

  "onPageLoad" must {

    "display the income tax summary page" in {
      when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        EitherT.rightT(taxAccountSummary)
      )

      when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
        Future.successful(
          TaxAccountSummaryViewModel(
            taxAccountSummary,
            ThreeWeeks,
            nonTaxCodeIncome,
            IncomeSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
            nonMatchedEmployments
          )
        )
      )

      when(taxAccountService.scottishBandRates(any(), any(), any())(any())).thenReturn(
        Future.successful(
          Map.empty[String, BigDecimal]
        )
      )

      when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(Right(Nil)))

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      val expectedTitle =
        s"${messagesApi("tai.incomeTaxSummary.heading.part1", TaxYearRangeUtil.currentTaxYearRangeBreak)}"
      doc.title() must include(expectedTitle)
    }

    "raise an audit event" in {
      when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        EitherT.rightT(taxAccountSummary)
      )

      when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
        Future.successful(
          TaxAccountSummaryViewModel(
            taxAccountSummary,
            ThreeWeeks,
            nonTaxCodeIncome,
            IncomeSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
            nonMatchedEmployments
          )
        )
      )

      when(
        auditService.createAndSendAuditEvent(
          meq(AuditConstants.TaxAccountSummaryUserEntersSummaryPage),
          meq(Map("nino" -> nino.nino))
        )(any(), any())
      )
        .thenReturn(Future.successful(Success))

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      verify(auditService, times(1))
        .createAndSendAuditEvent(
          meq(AuditConstants.TaxAccountSummaryUserEntersSummaryPage),
          meq(Map("nino" -> nino.nino))
        )(any(), any())
    }

    "display an error page" when {
      "a downstream error has occurred in one of the TaiResponse responding service methods" in {
        when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
          Future.successful(
            TaxAccountSummaryViewModel(
              taxAccountSummary,
              ThreeWeeks,
              nonTaxCodeIncome,
              IncomeSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
              nonMatchedEmployments
            )
          )
        )

        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream unauthenticated error has occurred in one of the TaiResponse responding service methods" in {
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("Unauthorised", UNAUTHORIZED)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream error has occurred in the tax account service (which does not reply with TaiResponse type)" in {
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT(taxAccountSummary)
        )

        when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
          Future.failed(new RuntimeException("Failed to fetch income details"))
        )

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream unauthorised exception has occurred in the tax account service" in {
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT(taxAccountSummary)
        )

        when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
          Future.failed(new UnauthorizedException("unauthorised"))
        )

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream error has occurred in the tax code income service (which does not reply with TaiResponse type)" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(nonTaxCodeIncome)
        )
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          EitherT.rightT(taxAccountSummary)
        )
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))

        when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
          Future.failed(new RuntimeException("Failed to fetch income details"))
        )
        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream error has occurred in one of the TaiResponse responding service methods due to no found primary employment information" in {

        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)

      }

      "a downstream error has occurred in one of the TaiResponse responding service methods due to no employments recorded for current tax year" in {

        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)
      }

      "a downstream error has occurred in one of the TaiResponse responding service methods due to not being authorised" in {

        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", UNAUTHORIZED)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "there is a TaiNotFoundResponse because there is no tax account information found" in {

        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("not found", NOT_FOUND)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)
      }
    }

  }

}
