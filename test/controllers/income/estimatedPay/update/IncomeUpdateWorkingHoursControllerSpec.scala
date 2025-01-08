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

package controllers.income.estimatedPay.update

import builders.RequestBuilder
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import pages.income.{UpdateIncomeIdPage, UpdateIncomeIrregularAnnualPayPage, UpdateIncomeNamePage, UpdateIncomeWorkingHoursPage}
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants._
import utils.BaseSpec
import views.html.incomes.WorkingHoursView

import scala.concurrent.Future
import scala.util.Random

class IncomeUpdateWorkingHoursControllerSpec extends BaseSpec {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val sessionId = "testSessionId"

  def randomNino(): Nino = new Generator(new Random()).nextNino
  def createSUT = new SUT

  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  class SUT
      extends IncomeUpdateWorkingHoursController(
        mockAuthJourney,
        mcc,
        inject[WorkingHoursView],
        mockJourneyCacheRepository
      ) {
    when(mockJourneyCacheRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheRepository)
  }

  "workingHoursPage" must {

    "display workingHours page" when {
      "journey cache returns employment name and id" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomeWorkingHoursPage, "work")
          .setOrException(UpdateIncomeIrregularAnnualPayPage, "123")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result =
          SUT.workingHoursPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(None))

        val result =
          SUT.workingHoursPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handleWorkingHours" must {

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomeWorkingHoursPage, "work")
          .setOrException(UpdateIncomeIrregularAnnualPayPage, "workingHours")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handleWorkingHours(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("workingHours" -> EditIncomeIrregularPayConstants.RegularHours)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayPeriodController.payPeriodPage().url
        )
      }
    }

    "redirect user back to workingHours page" when {
      "user input has error" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomeWorkingHoursPage, "work")
          .setOrException(UpdateIncomeIrregularAnnualPayPage, "workingHours")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handleWorkingHours(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("workingHours" -> "")
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }

    "redirect user back to workingHours page" when {
      "bad data submitted in form" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomeWorkingHoursPage, "work")
          .setOrException(UpdateIncomeIrregularAnnualPayPage, "workingHours")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handleWorkingHours(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("workingHours" -> "anything")
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }
  }

  "Redirect to /income-summary page" when {
    "IncomeSource.create returns a left" in {
      val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeWorkingHoursPage, "work")
        .setOrException(UpdateIncomeIrregularAnnualPayPage, "workingHours")

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheRepository.get(any(), any()))
        .thenReturn(Future.successful(None))

      val result = SUT.handleWorkingHours(RequestBuilder.buildFakePostRequestWithAuth())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
    }
  }
}
