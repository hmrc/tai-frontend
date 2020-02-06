/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.{FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants._

import scala.concurrent.Future

class IncomeUpdateEstimatedPayControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JourneyCacheConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val employer = IncomeSource(id = 1, name = "sample employer")

  val incomeService: IncomeService = mock[IncomeService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  class TestIncomeUpdateEstimatedPayController
      extends IncomeUpdateEstimatedPayController(
        FakeAuthAction,
        FakeValidatePerson,
        incomeService,
        journeyCacheService,
        mock[FormPartialRetriever],
        MockTemplateRenderer) {
    when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
      .thenReturn(Future.successful(Right(employer.id)))
    when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncome_NameKey))(any()))
      .thenReturn(Future.successful(Right(employer.name)))
  }

  "estimatedPayLandingPage" must {
    object EstimatedPayLandingPageHarness {
      sealed class EstimatedPayLandingPageHarness() {

        when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(Seq(employer.name, employer.id.toString, TaiConstants.IncomeTypeEmployment)))

        def estimatedPayLandingPage(): Future[Result] =
          new TestIncomeUpdateEstimatedPayController()
            .estimatedPayLandingPage()(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def setup(): EstimatedPayLandingPageHarness = new EstimatedPayLandingPageHarness()
    }
    "display the estimatedPayLandingPage view" in {
      val result = EstimatedPayLandingPageHarness
        .setup()
        .estimatedPayLandingPage()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
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
            .estimatedPayPage()(request)
      }

      def setup(
        payment: Option[Payment] = Some(Payment(new LocalDate(), 200, 50, 25, 100, 50, 25, Monthly)),
        currentCache: Map[String, String] = Map.empty[String, String]): EstimatedPayPageHarness =
        new EstimatedPayPageHarness(payment, currentCache)
    }
    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay" in {

        val result = EstimatedPayPageHarness
          .setup(Some(Payment(new LocalDate(), 50, 1, 1, 1, 1, 1, Monthly)))
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "payYearToDate is None" in {

        val result = EstimatedPayPageHarness
          .setup(None)
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
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
          .setup(currentCache = Map(UpdateIncome_ConfirmedNewAmountKey -> "100"))
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }
    }
    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        val controller = new TestIncomeUpdateEstimatedPayController

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))

        val result = controller.estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
