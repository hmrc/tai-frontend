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
import controllers.{FakeAuthAction, FakeTaiPlayApplication, routes}
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.{Matchers, Mockito}
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

class IncomeUpdateWorkingHoursControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JourneyCacheConstants
    with EditIncomeIrregularPayConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val employer = IncomeSource(id = 1, name = "sample employer")

  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  class TestIncomeUpdateWorkingHoursController
      extends IncomeUpdateWorkingHoursController(
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

  "workingHoursPage" must {

    object WorkingHoursPageHarness {
      sealed class WorkingHoursPageHarness() {

        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_WorkingHoursKey))(any()))
          .thenReturn(Future.successful(Option(REGULAR_HOURS)))

        def workingHoursPage(): Future[Result] =
          new TestIncomeUpdateWorkingHoursController()
            .workingHoursPage()(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(): WorkingHoursPageHarness =
        new WorkingHoursPageHarness()
    }

    "display workingHours page" when {
      "journey cache returns employment name and id" in {

        val result = WorkingHoursPageHarness
          .setup()
          .workingHoursPage()

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        val result = WorkingHoursPageHarness
          .setup()
          .workingHoursPage()

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.any())(any()))
          .thenReturn(Future.successful(Left("empty cache")))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }

  }

  "handleWorkingHours" must {

    object HandleWorkingHoursHarness {
      sealed class HandleWorkingHoursHarness() {

        when(journeyCacheService.cache(eqTo(UpdateIncome_WorkingHoursKey), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.mandatoryJourneyValueAsInt(eqTo(UpdateIncome_IdKey))(any()))
          .thenReturn(Future.successful(Right(1)))

        def handleWorkingHours(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateWorkingHoursController()
            .handleWorkingHours()(request)
      }

      def setup(): HandleWorkingHoursHarness =
        new HandleWorkingHoursHarness()
    }

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {

        val result = HandleWorkingHoursHarness
          .setup()
          .handleWorkingHours(RequestBuilder.buildFakePostRequestWithAuth("workingHours" -> REGULAR_HOURS))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayPeriodController.payPeriodPage().url)
      }
    }

    "redirect user back to workingHours page" when {
      "user input has error" in {

        val result = HandleWorkingHoursHarness
          .setup()
          .handleWorkingHours(RequestBuilder.buildFakePostRequestWithAuth("workingHours" -> ""))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }

    "redirect user back to workingHours page" when {
      "bad data submitted in form" in {

        val result = HandleWorkingHoursHarness
          .setup()
          .handleWorkingHours(RequestBuilder.buildFakePostRequestWithAuth("workingHours" -> "anything"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }
    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {

        val result = HandleWorkingHoursHarness
          .setup()
          .handleWorkingHours(RequestBuilder.buildFakePostRequestWithAuth())

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
          .thenReturn(Future.successful(Left("")))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncome_NameKey))(any()))
          .thenReturn(Future.successful(Left("")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)

      }
    }
  }
}
