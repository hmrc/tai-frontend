/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.income.estimatedPay.update

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import controllers.{ErrorPagesHandler, FakeAuthAction}
import mocks.MockTemplateRenderer

import java.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse, TaiUnauthorisedResponse}
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateIncomeConstants
import utils.BaseSpec
import views.html.incomes.{EstimatedPayLandingPageView, EstimatedPayView, IncorrectTaxableIncomeView}

import scala.concurrent.Future

class IncomeUpdateEstimatedPayControllerSpec extends BaseSpec {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")

  val incomeService: IncomeService = mock[IncomeService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val mockTaxAccountService: TaxAccountService = mock[TaxAccountService]

  class TestIncomeUpdateEstimatedPayController
      extends IncomeUpdateEstimatedPayController(
        FakeAuthAction,
        FakeValidatePerson,
        incomeService,
        appConfig,
        mcc,
        mockTaxAccountService,
        inject[EstimatedPayLandingPageView],
        inject[EstimatedPayView],
        inject[IncorrectTaxableIncomeView],
        journeyCacheService,
        MockTemplateRenderer,
        inject[ErrorPagesHandler]
      ) {
    when(journeyCacheService.mandatoryJourneyValues(Matchers.anyVararg[String])(any()))
      .thenReturn(Future.successful(Right(Seq(employer.id.toString, employer.name))))
  }

  "estimatedPayLandingPage" must {

    val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)

    when(journeyCacheService.mandatoryJourneyValues(Matchers.anyVararg[String])(any()))
      .thenReturn(Future.successful(Right(Seq(employer.name, employer.id.toString, TaiConstants.IncomeTypeEmployment))))
    when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future(taxAccountSummary)

    def estimatedPayLandingPage(): Future[Result] =
      new TestIncomeUpdateEstimatedPayController()
        .estimatedPayLandingPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

    "display the estimatedPayLandingPage view" in {

      val result = estimatedPayLandingPage()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
    }

    "return INTERNAL_SERVER_ERROR when TaiNotFoundResponse is returned from the service" in {

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new NotFoundException(""))

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR

    }
    "return INTERNAL_SERVER_ERROR when TaiUnauthorisedResponse is returned from the service" in {

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new NotFoundException(""))

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR

    }

    "return INTERNAL_SERVER_ERROR when TaiTaxAccountFailureResponse is returned from the service" in {

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new RuntimeException(""))

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR

    }
    "return to /income-details when nothing is present in the cache" in {
      val result = estimatedPayLandingPage()

      when(journeyCacheService.mandatoryJourneyValues(any())(any())).thenReturn(Future.successful(Left("empty cache")))

      redirectLocation(result) mustBe Some(controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url)
    }
  }

  "estimatedPayPage" must {
    object EstimatedPayPageHarness {
      sealed class EstimatedPayPageHarness(payment: Option[Payment], currentCache: Map[String, String]) {

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))
        when(incomeService.latestPayment(any(), any())(any()))
          .thenReturn(Future.successful(payment))
        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(currentCache))
        when(incomeService.employmentAmount(any(), any())(any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))
        when(incomeService.calculateEstimatedPay(any(), any())(any()))
          .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))

        def estimatedPayPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateEstimatedPayController()
            .estimatedPayPage(employer.id)(request)
      }

      def setup(
        payment: Option[Payment] = Some(Payment(LocalDate.now, 200, 50, 25, 100, 50, 25, Monthly)),
        currentCache: Map[String, String] = Map.empty[String, String]): EstimatedPayPageHarness =
        new EstimatedPayPageHarness(payment, currentCache)
    }
    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay" in {

        val result = EstimatedPayPageHarness
          .setup(Some(Payment(LocalDate.now, 50, 1, 1, 1, 1, 1, Monthly)))
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRange))
      }

      "payYearToDate is None" in {

        val result = EstimatedPayPageHarness
          .setup(None)
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRange))
      }
    }

    "display incorrectTaxableIncome page" when {
      "payYearToDate is greater than gross annual pay" in {

        val result = EstimatedPayPageHarness
          .setup()
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))
      }
    }

    "redirect to sameEstimatedPay page" when {
      "the pay is the same" in {

        val result = EstimatedPayPageHarness
          .setup(currentCache = Map(s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-${employer.id}" -> "100"))
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeController.sameEstimatedPayInCache(employer.id).url)
      }
    }
    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        val controller = new TestIncomeUpdateEstimatedPayController

        when(journeyCacheService.mandatoryJourneyValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(Left("empty cache")))

        val result = controller.estimatedPayPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
