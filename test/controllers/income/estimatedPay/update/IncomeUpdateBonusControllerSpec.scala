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

package controllers.income.estimatedPay.update

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import controllers.{ControllerViewTestHelper, FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants._
import views.html.incomes.{bonusPaymentAmount, bonusPayments}

import scala.concurrent.Future

class IncomeUpdateBonusControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JourneyCacheConstants
    with ControllerViewTestHelper with FormValuesConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val employer = IncomeSource(id = 1, name = "sample employer")

  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  class TestIncomeUpdateBonusController
      extends IncomeUpdateBonusController(
        FakeAuthAction,
        FakeValidatePerson,
        journeyCacheService,
        mock[FormPartialRetriever],
        MockTemplateRenderer) {
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

      implicit val fakeRequest = RequestBuilder.buildFakeGetRequestWithAuth()
      val cachedAmount = "1231231"

      val result = BonusPaymentsPageHarness
        .setup(cachedAmount)
        .bonusPaymentsPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusPaymentsForm.createForm.fill(YesNoForm(Some(cachedAmount)))
      val expectedView = bonusPayments(expectedForm, employer)

      result rendersTheSameViewAs expectedView
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
        result rendersTheSameViewAs bonusPayments(BonusPaymentsForm.createForm.bindFromRequest()(fakeRequest), employer)
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
      result rendersTheSameViewAs bonusPaymentAmount(expectedForm, employer)
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

        result rendersTheSameViewAs bonusPaymentAmount(
          BonusOvertimeAmountForm.createForm().bindFromRequest()(fakeRequest),
          employer)
      }
    }
  }

}
