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
import controllers.ErrorPagesHandler
import controllers.auth.{AuthedUser, DataRequest}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.ScalaOngoingStubbing
import pages.income._
import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsFormUrlEncoded, BodyParser, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants._
import utils.BaseSpec
import views.html.incomes.{EstimatedPayLandingPageView, EstimatedPayView, IncorrectTaxableIncomeView}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class IncomeUpdateEstimatedPayControllerSpec extends BaseSpec {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val empId: Int = 1
  val sessionId: String = "testSessionId"

  def randomNino(): Nino = new Generator(new Random()).nextNino
  def createSUT = new SUT

  val incomeService: IncomeService = mock[IncomeService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]
  val mockTaxAccountService: TaxAccountService = mock[TaxAccountService]

  class SUT
      extends IncomeUpdateEstimatedPayController(
        mockAuthJourney,
        incomeService,
        appConfig,
        mcc,
        mockTaxAccountService,
        inject[EstimatedPayLandingPageView],
        inject[EstimatedPayView],
        inject[IncorrectTaxableIncomeView],
        mockJourneyCacheNewRepository,
        inject[ErrorPagesHandler]
      ) {
    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  private def setup(ua: UserAnswers): ScalaOngoingStubbing[ActionBuilder[DataRequest, AnyContent]] =
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
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheNewRepository)
  }

  "estimatedPayLandingPage" must {
    reset(mockJourneyCacheNewRepository)

    val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
      .setOrException(UpdateIncomeNamePage, employer.name)
      .setOrException(UpdateIncomeIdPage, employer.id)
      .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)

    setup(mockUserAnswers)

    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(mockUserAnswers)))

    val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)

    when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future(taxAccountSummary)

    def estimatedPayLandingPage(): Future[Result] =
      new SUT()
        .estimatedPayLandingPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

    "display the estimatedPayLandingPage view" in {

      val result = estimatedPayLandingPage()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
    }

    "return INTERNAL_SERVER_ERROR when TaiNotFoundResponse is returned from the service" in {

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new NotFoundException("")
      )

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR

    }
    "return INTERNAL_SERVER_ERROR when TaiUnauthorisedResponse is returned from the service" in {

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new NotFoundException("")
      )

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR

    }

    "return INTERNAL_SERVER_ERROR when TaiTaxAccountFailureResponse is returned from the service" in {

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new RuntimeException("")
      )

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR

    }
    "return to /income-details when nothing is present in the cache" in {
      val testController = new IncomeUpdateEstimatedPayController(
        mockAuthJourney,
        incomeService,
        appConfig,
        mcc,
        mockTaxAccountService,
        inject[EstimatedPayLandingPageView],
        inject[EstimatedPayView],
        inject[IncorrectTaxableIncomeView],
        mockJourneyCacheNewRepository,
        inject[ErrorPagesHandler]
      )

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(None))

      val result = testController.estimatedPayLandingPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url)
    }
  }

  "estimatedPayPage" must {
    object EstimatedPayPageHarness {
      sealed class EstimatedPayPageHarness(payment: Option[Payment], userAnswers: Option[UserAnswers]) {
        reset(mockJourneyCacheNewRepository)

        setup(userAnswers.getOrElse(UserAnswers("testSessionId", randomNino().nino)))

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(userAnswers))
        when(mockJourneyCacheNewRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(payment))
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))
        when(incomeService.calculateEstimatedPay(any(), any())(any()))
          .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))

        def estimatedPayPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new SUT()
            .estimatedPayPage(employer.id)(request)
      }

      def harnessSetup(
        payment: Option[Payment] = Some(Payment(LocalDate.now, 200, 50, 25, 100, 50, 25, Monthly)),
        userAnswers: Option[UserAnswers] = None
      ): EstimatedPayPageHarness =
        new EstimatedPayPageHarness(payment, userAnswers)
    }

    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay" in {

        val result = EstimatedPayPageHarness
          .harnessSetup(Some(Payment(LocalDate.now, 50, 1, 1, 1, 1, 1, Monthly)))
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeBreak.replace("\u00A0", " "))
        )
      }

      "payYearToDate is None" in {

        val result = EstimatedPayPageHarness
          .harnessSetup(None)
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
      }
    }

    "display incorrectTaxableIncome page" when {
      "payYearToDate is greater than gross annual pay" in {

        val result = EstimatedPayPageHarness
          .harnessSetup()
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))
      }
    }

    "redirect to sameEstimatedPay page" when {
      "the pay is the same" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdateIncomeGrossAnnualPayPage, "company")
          .setOrException(UpdateIncomeNewAmountPage, "123")
          .setOrException(UpdateIncomeBonusPaymentsPage, "")
          .setOrException(UpdateIncomeConfirmedNewAmountPage(empId), "100")

        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val payment = Some(Payment(LocalDate.now, 200, 50, 25, 100, 50, 25, Monthly))

        val result = EstimatedPayPageHarness
          .harnessSetup(payment, Some(mockUserAnswers))
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeController.sameEstimatedPayInCache(employer.id).url
        )
      }
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        val controller = new SUT

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.estimatedPayPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
