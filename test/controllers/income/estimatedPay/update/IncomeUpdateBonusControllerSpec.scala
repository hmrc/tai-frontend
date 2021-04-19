/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.{ControllerViewTestHelper, FakeAuthAction}
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants._
import utils.BaseSpec
import views.html.incomes.{bonusPaymentAmount, bonusPayments}

import scala.concurrent.Future

class IncomeUpdateBonusControllerSpec
    extends BaseSpec with JourneyCacheConstants with ControllerViewTestHelper with FormValuesConstants {

  val employer = IncomeSource(id = 1, name = "sample employer")

  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  override implicit val fakeRequest: FakeRequest[AnyContent] = RequestBuilder.buildFakeGetRequestWithAuth()

  private val bonusPaymentsView = inject[bonusPayments]

  private val bonusPaymentAmountView = inject[bonusPaymentAmount]

  class TestIncomeUpdateBonusController
      extends IncomeUpdateBonusController(
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        bonusPaymentsView,
        bonusPaymentAmountView,
        journeyCacheService,
        error_template_noauth,
        error_no_primary,
        MockPartialRetriever,
        MockTemplateRenderer
      ) {
    when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
      .thenReturn(Future.successful(Right(employer.id)))
    when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncome_NameKey))(any()))
      .thenReturn(Future.successful(Right(employer.name)))
  }

  "bonusPaymentsPage" must {
    object BonusPaymentsPageHarness {
      sealed class BonusPaymentsPageHarness(cachedAmount: String) {
        when(journeyCacheService.currentValue(eqTo(UpdateIncome_BonusPaymentsKey))(any()))
          .thenReturn(Future.successful(Some(cachedAmount)))

        def bonusPaymentsPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateBonusController()
            .bonusPaymentsPage()(request)
      }

      def setup(cachedAmount: String): BonusPaymentsPageHarness =
        new BonusPaymentsPageHarness(cachedAmount)
    }
    "display bonusPayments" in {

      val cachedAmount = "1231231"

      val result = BonusPaymentsPageHarness
        .setup(cachedAmount)
        .bonusPaymentsPage(fakeRequest.asInstanceOf[FakeRequest[AnyContentAsFormUrlEncoded]])

      status(result) mustBe OK

      val expectedForm = BonusPaymentsForm.createForm.fill(YesNoForm(Some(cachedAmount)))
      val expectedView =
        bonusPaymentsView(expectedForm, employer)(fakeRequest, messages, authedUser, templateRenderer, partialRetriever)

      result rendersTheSameViewAs expectedView
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        val controller = new TestIncomeUpdateBonusController

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))

        val result = controller.bonusPaymentsPage(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handleBonusPayments" must {
    object HandleBonusPaymentsHarness {
      sealed class HandleBonusPaymentsHarness() {

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))
        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))
        when(journeyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))
        when(journeyCacheService.flush()(any()))
          .thenReturn(Future.successful(TaiSuccessResponse))
        when(journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq(employer.id.toString, employer.name)))

        def handleBonusPayments(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateBonusController()
            .handleBonusPayments()(request)
      }

      def setup(): HandleBonusPaymentsHarness =
        new HandleBonusPaymentsHarness()
    }

    "redirect the user to bonusOvertimeAmountPage page" when {
      "user selected yes" in {

        val result = HandleBonusPaymentsHarness
          .setup()
          .handleBonusPayments(RequestBuilder.buildFakePostRequestWithAuth(YesNoChoice -> YesValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusOvertimeAmountPage().url)
      }
    }

    "redirect the user to checkYourAnswers page" when {
      "user selected no" in {
        val result = HandleBonusPaymentsHarness
          .setup()
          .handleBonusPayments(RequestBuilder.buildFakePostRequestWithAuth(YesNoChoice -> NoValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
      }
    }

    "redirect user back to how to bonusPayments page" when {
      "user input has error" in {

        implicit val fakeRequest = RequestBuilder.buildFakePostRequestWithAuth()

        val result = HandleBonusPaymentsHarness
          .setup()
          .handleBonusPayments(fakeRequest)

        status(result) mustBe BAD_REQUEST
        result rendersTheSameViewAs bonusPaymentsView(
          BonusPaymentsForm.createForm.bindFromRequest()(fakeRequest),
          employer)(
          fakeRequest,
          messages,
          authedUser,
          templateRenderer,
          partialRetriever
        )
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {

        implicit val fakeRequest = RequestBuilder.buildFakeGetRequestWithAuth()

        val controller = new TestIncomeUpdateBonusController

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))

        val result = controller.handleBonusPayments(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "bonusOvertimeAmountPage" must {
    object BonusOvertimeAmountPageHarness {
      sealed class BonusOvertimeAmountPageHarness() {

        when(journeyCacheService.currentValue(any())(any()))
          .thenReturn(Future.successful(Some("313321")))

        def bonusOvertimeAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateBonusController()
            .bonusOvertimeAmountPage()(request)
      }

      def setup(): BonusOvertimeAmountPageHarness =
        new BonusOvertimeAmountPageHarness()
    }

    "display bonusPaymentAmount" in {
      val cachedAmount = "313321"
      implicit val fakeRequest = RequestBuilder.buildFakeGetRequestWithAuth()

      val result = BonusOvertimeAmountPageHarness
        .setup()
        .bonusOvertimeAmountPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusOvertimeAmountForm.createForm().fill(BonusOvertimeAmountForm(Some(cachedAmount)))
      result rendersTheSameViewAs bonusPaymentAmountView(expectedForm, employer)(
        fakeRequest,
        messages,
        authedUser,
        templateRenderer,
        partialRetriever
      )
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        implicit val fakeRequest = RequestBuilder.buildFakeGetRequestWithAuth()

        val controller = new TestIncomeUpdateBonusController

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))

        val result = controller.bonusOvertimeAmountPage(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handleBonusOvertimeAmount" must {
    object HandleBonusOvertimeAmountHarness {

      sealed class HandleBonusOvertimeAmountHarness() {

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq(employer.id.toString, employer.name)))

        def handleBonusOvertimeAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateBonusController()
            .handleBonusOvertimeAmount()(request)
      }

      def setup(): HandleBonusOvertimeAmountHarness =
        new HandleBonusOvertimeAmountHarness()
    }

    "redirect the user to checkYourAnswers page" in {

      val result = HandleBonusOvertimeAmountHarness
        .setup()
        .handleBonusOvertimeAmount(RequestBuilder.buildFakePostRequestWithAuth("amount" -> "£3,000"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
    }

    "redirect the user to bonusPaymentAmount page" when {
      "user input has error" in {

        implicit val fakeRequest =
          RequestBuilder.buildFakePostRequestWithAuth("amount" -> "")

        val result = HandleBonusOvertimeAmountHarness
          .setup()
          .handleBonusOvertimeAmount(fakeRequest)

        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs bonusPaymentAmountView(
          BonusOvertimeAmountForm.createForm().bindFromRequest()(fakeRequest),
          employer)(fakeRequest, messages, authedUser, templateRenderer, partialRetriever)
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {

        implicit val fakeRequest =
          RequestBuilder.buildFakePostRequestWithAuth("" -> "")

        val controller = new TestIncomeUpdateBonusController

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
          .thenReturn(Future.successful(Left("")))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncome_NameKey))(any()))
          .thenReturn(Future.successful(Left("")))

        val result = controller.handleBonusOvertimeAmount(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
