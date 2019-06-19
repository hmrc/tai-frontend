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

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.{AuditConstants, TaiConstants}
import uk.gov.hmrc.tai.viewModels.{IncomesSources, TaxAccountSummaryViewModel}
import utils.TaxAccountSummaryTestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class TaxAccountSummaryControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with AuditConstants
  with BeforeAndAfterEach
  with TaxAccountSummaryTestData {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override def beforeEach: Unit = {
    Mockito.reset(auditService)
  }

  "onPageLoad" must {

    "display the income tax summary page" in {
      val sut = createSUT
      when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
      )

      when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
        Future.successful(TaxAccountSummaryViewModel(taxAccountSummary,
          ThreeWeeks,
          nonTaxCodeIncome,
          IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
          nonMatchedEmployments))
      )

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      val expectedTitle = s"${messagesApi("tai.incomeTaxSummary.heading.part1", TaxYearRangeUtil.currentTaxYearRangeSingleLine)}"
      doc.title() must include(expectedTitle)
    }

    "raise an audit event" in {
      val sut = createSUT
      when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
      )

      when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
        Future.successful(TaxAccountSummaryViewModel(taxAccountSummary,
          ThreeWeeks,
          nonTaxCodeIncome,
          IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
          nonMatchedEmployments))
      )

      when(auditService.createAndSendAuditEvent(Matchers.eq(TaxAccountSummary_UserEntersSummaryPage), Matchers.eq(Map("nino" -> nino.nino)))(any(), any()))
        .thenReturn(Future.successful(Success))


      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      verify(auditService, times(1))
        .createAndSendAuditEvent(Matchers.eq(TaxAccountSummary_UserEntersSummaryPage), Matchers.eq(Map("nino" -> nino.nino)))(Matchers.any(), Matchers.any())
    }

    "display an error page" when {
      "a downstream error has occurred in one of the TaiResponse responding service methods" in {
        val sut = createSUT
        when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
          Future.successful(TaxAccountSummaryViewModel(taxAccountSummary,
            ThreeWeeks,
            nonTaxCodeIncome,
            IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
            nonMatchedEmployments))
        )

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse("Data retrieval failure")))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "a downstream error has occurred in the employment service (which does not reply with TaiResponse type)" in {
        val sut = createSUT
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        when(taxAccountSummaryService.taxAccountSummaryViewModel(any(), any())(any(), any())).thenReturn(
          Future.failed(new RuntimeException("Failed to fetch income details"))
        )

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }


      "a downstream error has occurred in the tax code income service (which does not reply with TaiResponse type)" in {
        val sut = createSUT
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
        )
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }


      "a downstream error has occurred in one of the TaiResponse responding service methods due to no found primary employment information" in {
        val sut = createSUT

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse(TaiConstants.NpsTaxAccountDataAbsentMsg.toLowerCase)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)

      }
      "a downstream error has occurred in one of the TaiResponse responding service methods due to no employments recorded for current tax year" in {
        val sut = createSUT

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future(TaiTaxAccountFailureResponse(TaiConstants.NpsNoEmploymentForCurrentTaxYear.toLowerCase)))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url)

      }
    }

  }

   override val nonTaxCodeIncome = NonTaxCodeIncome(Some(uk.gov.hmrc.tai.model.domain.income.UntaxedInterest(UntaxedInterestIncome,
    None, 100, "Untaxed Interest", Seq.empty[BankAccount])), Seq(
    OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
  ))

  def createSUT = new SUT()

  val trackingService = mock[TrackingService]
  val auditService = mock[AuditService]
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]
  val taxAccountSummaryService = mock[TaxAccountSummaryService]

  class SUT() extends TaxAccountSummaryController(
    trackingService,
    employmentService,
    taxAccountService,
    taxAccountSummaryService,
    auditService,
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
    when(trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(ThreeWeeks))
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()


}
