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
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import pages.income._
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, Request, Result}
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

  val mockIncomeService: IncomeService = mock[IncomeService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]
  val mockTaxAccountService: TaxAccountService = mock[TaxAccountService]

  class SUT
      extends IncomeUpdateEstimatedPayController(
        mockAuthJourney,
        mockIncomeService,
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

  "estimatedPayLandingPage" must {

    val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
      .setOrException(UpdateIncomeNamePage, employer.name)
      .setOrException(UpdateIncomeIdPage, employer.id)
      .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)

    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(mockUserAnswers)))

    def estimatedPayLandingPage(): Future[Result] =
      new SUT()
        .estimatedPayLandingPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

    "display the estimatedPayLandingPage view" in {

      setup(mockUserAnswers)

      val taxAccountSummary = TaxAccountSummary(0, 0, 0, 0, 0)
      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future(taxAccountSummary)

      val result = estimatedPayLandingPage()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
    }

    "return INTERNAL_SERVER_ERROR when TaiNotFoundResponse is returned from the service" in {

      setup(mockUserAnswers)

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new NotFoundException("")
      )

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
    "return INTERNAL_SERVER_ERROR when TaiUnauthorisedResponse is returned from the service" in {

      setup(mockUserAnswers)

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new NotFoundException("")
      )

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR when TaiTaxAccountFailureResponse is returned from the service" in {

      setup(mockUserAnswers)

      when(mockTaxAccountService.taxAccountSummary(any(), any())(any())) thenReturn Future.failed(
        new RuntimeException("")
      )

      val result = estimatedPayLandingPage()
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
    "return to /income-details when nothing is present in the cache" in {

      setup(UserAnswers(sessionId, randomNino().nino))

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(None))

      val result = estimatedPayLandingPage()

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url)
    }
  }

  "estimatedPayPage" must {
    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay" in {

        val payment = Some(Payment(LocalDate.now, 50, 1, 1, 1, 1, 1, Monthly))

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)

        val controller = createSUT

        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(payment))
        when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))
        when(mockIncomeService.calculateEstimatedPay(any(), any())(any()))
          .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))

        val result = controller.estimatedPayPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeBreak.replace("\u00A0", " "))
        )
      }

      "payYearToDate is None" in {

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)

        val controller = createSUT

        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))
        when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(None))

        val result = controller.estimatedPayPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
      }
    }

    "display incorrectTaxableIncome page" when {
      "payYearToDate is greater than gross annual pay" in {

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)

        val controller = createSUT

        setup(mockUserAnswers)

        val payment = Some(Payment(LocalDate.now, 200, 50, 25, 100, 50, 25, Monthly))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))
        when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(payment))
        when(mockIncomeService.calculateEstimatedPay(any(), any())(any()))
          .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))

        val result = controller.estimatedPayPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))
      }
    }

    "redirect to sameEstimatedPay page" when {
      "the pay is the same" in {

        val payment = Some(Payment(LocalDate.now, 200, 50, 25, 100, 50, 25, Monthly))
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomeGrossAnnualPayPage, "100")
          .setOrException(UpdateIncomeNewAmountPage, "123")
          .setOrException(UpdateIncomeBonusPaymentsPage, "")
          .setOrException(UpdateIncomeConfirmedNewAmountPage(empId), "150")

        val controller = createSUT

        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))
        when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(payment))
        when(mockIncomeService.calculateEstimatedPay(any(), any())(any()))
          .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(150)), Some(BigDecimal(100)))))

        val result = controller.estimatedPayPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeController.sameEstimatedPayInCache(employer.id).url
        )
      }
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        val controller = createSUT

        setup(UserAnswers(sessionId, randomNino().nino))

        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(None))

        val result = controller.estimatedPayPage(employer.id)(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
