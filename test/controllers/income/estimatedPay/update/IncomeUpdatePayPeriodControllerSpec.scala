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
import controllers.{FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
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
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants._

import scala.concurrent.Future

class IncomeUpdatePayPeriodControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JourneyCacheConstants
    with EditIncomePayPeriodConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val employer = IncomeSource(id = 1, name = "sample employer")

  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  class TestIncomeUpdatePayPeriodController
      extends IncomeUpdatePayPeriodController(
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

  "payPeriodPage" must {
    object PayPeriodPageHarness {
      sealed class PayPeriodPageHarness() {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayPeriodKey))(any()))
          .thenReturn(Future.successful(Some(MONTHLY)))
        when(journeyCacheService.currentValue(eqTo(UpdateIncome_OtherInDaysKey))(any()))
          .thenReturn(Future.successful(None))

        def payPeriodPage(): Future[Result] =
          new TestIncomeUpdatePayPeriodController()
            .payPeriodPage()(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(): PayPeriodPageHarness =
        new PayPeriodPageHarness()
    }

    "display payPeriod page" when {
      "journey cache returns employment name and id" in {

        val result = PayPeriodPageHarness
          .setup()
          .payPeriodPage()

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }
  }

  "handlePayPeriod" must {
    object HandlePayPeriodHarness {
      sealed class HandlePayPeriodHarness() {

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        def handlePayPeriod(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdatePayPeriodController()
            .handlePayPeriod()(request)
      }

      def setup(): HandlePayPeriodHarness =
        new HandlePayPeriodHarness()
    }
    "redirect the user to payslipAmountPage page" when {
      "user selected monthly" in {

        val result = HandlePayPeriodHarness
          .setup()
          .handlePayPeriod(RequestBuilder.buildFakePostRequestWithAuth("payPeriod" -> "monthly"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipAmountPage().url)
      }
    }

    "redirect user back to how to payPeriod page" when {
      "user input has error" in {

        val result = HandlePayPeriodHarness
          .setup()
          .handlePayPeriod(RequestBuilder.buildFakePostRequestWithAuth("payPeriod" -> "nonsense"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }
  }

}
