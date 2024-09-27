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
import controllers.auth.{AuthedUser, DataRequest}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import pages.income.{UpdateIncomeIdPage, UpdateIncomeNamePage, UpdateIncomeOtherInDaysPage, UpdateIncomePayPeriodPage}
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, Request, Result}
import play.api.test.Helpers._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.PayPeriodConstants.Monthly
import utils.BaseSpec
import views.html.incomes.PayPeriodView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class IncomeUpdatePayPeriodControllerSpec extends BaseSpec {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val sessionId = "testSessionId"

  def randomNino(): Nino = new Generator(new Random()).nextNino
  def createSUT = new SUT

  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  class SUT
      extends IncomeUpdatePayPeriodController(
        mockAuthJourney,
        mcc,
        inject[PayPeriodView],
        mockJourneyCacheNewRepository
      ) {
    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  private def setup(ua: UserAnswers): OngoingStubbing[ActionBuilder[DataRequest, AnyContent]] =
    when(mockAuthJourney.authWithDataRetrieval) thenReturn new ActionBuilder[DataRequest, AnyContent] {
      override def invokeBlock[A](
        request: Request[A],
        block: DataRequest[A] => Future[Result]
      ): Future[Result] =
        block(
          DataRequest(
            request,
            taiUser = AuthedUser(
              Nino(nino.toString()),
              Some("saUtr"),
              None
            ),
            fullName = "",
            userAnswers = ua
          )
        )

      override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = ec
    }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockJourneyCacheNewRepository)
  }

  "payPeriodPage" must {
    "display payPeriod page" when {
      "journey cache returns employment name and id" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, Monthly)
          .setOrException(UpdateIncomeOtherInDaysPage, "123")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.payPeriodPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(None))

        val SUT = createSUT
        setup(mockUserAnswers)

        val result =
          SUT.payPeriodPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handlePayPeriod" must {
    "redirect the user to payslipAmountPage page" when {
      "user selected monthly" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomePayPeriodPage, Monthly)
          .setOrException(UpdateIncomeOtherInDaysPage, "123")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.handlePayPeriod(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payPeriod" -> "monthly")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipAmountPage().url
        )
      }
    }

    "redirect user back to how to payPeriod page" when {
      "user input has error" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, "nonsense")
          .setOrException(UpdateIncomeOtherInDaysPage, "123")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handlePayPeriod(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payPeriod" -> "nonsense")
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomePayPeriodPage, "nonsense")
          .setOrException(UpdateIncomeOtherInDaysPage, "123")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handlePayPeriod(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payPeriod" -> "nonsense")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
