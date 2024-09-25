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
import controllers.{ControllerViewTestHelper, ErrorPagesHandler}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatest.concurrent.ScalaFutures
import pages.TrackingJourneyConstantsEstimatedPayPage
import pages.income._
import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsFormUrlEncoded, BodyParser, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live}
import uk.gov.hmrc.tai.service._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.BaseSpec
import views.html.incomes.estimatedPayment.update.CheckYourAnswersView
import views.html.incomes.DuplicateSubmissionWarningView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class IncomeUpdateCalculatorControllerSpec
    extends BaseSpec with JsoupMatchers with ControllerViewTestHelper with ScalaFutures {

  def randomNino(): Nino = new Generator(new Random()).nextNino

  val employerId = 1
  val sessionId: String = "testSessionId"
  val employer: IncomeSource = IncomeSource(id = employerId, name = "sample employer")

  val defaultEmployment: Employment =
    Employment(
      "company",
      Live,
      Some("123"),
      Some(LocalDate.parse("2016-05-26")),
      None,
      Nil,
      "",
      "",
      employerId,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )

  val mockIncomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  class SUT
      extends IncomeUpdateCalculatorController(
        mockIncomeService,
        employmentService,
        mockAuthJourney,
        mcc,
        inject[DuplicateSubmissionWarningView],
        inject[CheckYourAnswersView],
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
    reset(mockJourneyCacheNewRepository)
  }

  "onPageLoad" must {
    object OnPageLoadHarness {
      sealed class OnPageLoadHarness(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]) {

        val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(TrackingJourneyConstantsEstimatedPayPage(employerId), hasJourneyCompleted.toString)
        setup(mockUserAnswers)

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(returnedEmployment))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        def onPageLoad(employerId: Int = employerId): Future[Result] =
          new SUT()
            .onPageLoad(employerId)(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def harnessSetup(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]): OnPageLoadHarness =
        new OnPageLoadHarness(hasJourneyCompleted, returnedEmployment)
    }

    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {
        val result = OnPageLoadHarness
          .harnessSetup(hasJourneyCompleted = true, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(
          result
        ).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .duplicateSubmissionWarningPage(employerId)
          .url
      }
    }

    "redirect to the estimatedPayLanding url" when {
      "an income update has already been performed" in {
        val result = OnPageLoadHarness
          .harnessSetup(hasJourneyCompleted = false, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(
          result
        ).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
          .estimatedPayLandingPage(employerId)
          .url
      }
    }

    "generate an internal server error " when {
      "no employments are found" in {

        val result = OnPageLoadHarness
          .harnessSetup(hasJourneyCompleted = false, None)
          .onPageLoad()

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "duplicateSubmissionWarning" must {
    object DuplicateSubmissionWarningHarness {
      sealed class DuplicateSubmissionWarningHarness() {

        val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), "123456")
          .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)

        setup(mockUserAnswers)

        def duplicateSubmissionWarning(): Future[Result] =
          new SUT()
            .duplicateSubmissionWarningPage(employerId)(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def harnessSetup(): DuplicateSubmissionWarningHarness = new DuplicateSubmissionWarningHarness()
    }

    "show employment duplicateSubmissionWarning view" in {
      val result = DuplicateSubmissionWarningHarness
        .harnessSetup()
        .duplicateSubmissionWarning()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", employer.name))
    }
  }

  "submitDuplicateSubmissionWarning" must {
    object SubmitDuplicateSubmissionWarningHarness {
      sealed class SubmitDuplicateSubmissionWarningHarness(employmentType: String) {

        val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), "123456")
          .setOrException(UpdateIncomeTypePage, employmentType)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        def submitDuplicateSubmissionWarning(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new SUT()
            .submitDuplicateSubmissionWarning(employerId)(request)
      }

      def harnessSetup(employmentType: String): SubmitDuplicateSubmissionWarningHarness =
        new SubmitDuplicateSubmissionWarningHarness(employmentType)
    }

    "redirect to the estimatedPayLandingPage url when yes is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .harnessSetup(TaiConstants.IncomeTypeEmployment)
        .submitDuplicateSubmissionWarning(
          RequestBuilder
            .buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
        )

      status(result) mustBe SEE_OTHER

      redirectLocation(
        result
      ).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
        .estimatedPayLandingPage(employerId)
        .url
    }

    "redirect to the IncomeSourceSummaryPage url when no is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .harnessSetup(TaiConstants.IncomeTypeEmployment)
        .submitDuplicateSubmissionWarning(
          RequestBuilder
            .buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue)
        )

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
    }
    "use pension vm" when {
      "income type is pension" in {

        val result = SubmitDuplicateSubmissionWarningHarness
          .harnessSetup(TaiConstants.IncomeTypePension)
          .submitDuplicateSubmissionWarning(
            RequestBuilder
              .buildFakePostRequestWithAuth()
          )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc must haveHeadingWithText(messages("tai.incomes.warning.pension.heading", employer.name))

      }
    }

    "use employment vm" when {
      "income type is employment" in {

        val result = SubmitDuplicateSubmissionWarningHarness
          .harnessSetup(TaiConstants.IncomeTypeEmployment)
          .submitDuplicateSubmissionWarning(
            RequestBuilder
              .buildFakePostRequestWithAuth()
          )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", employer.name))

      }
    }
  }

  "checkYourAnswersPage" must {
    object CheckYourAnswersPageHarness {
      sealed class CheckYourAnswersPageHarness {

        val employerName = "Employer1"
        val payFrequency = "monthly"
        val totalSalary = "10000"
        val payslipDeductions = "yes"
        val bonusPayments = "yes"
        val taxablePay = "8000"
        val bonusAmount = "1000"
        val payPeriodInDays = "3"
        val employerId = "1"

        val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdateIncomePayPeriodPage, payFrequency)
          .setOrException(UpdateIncomeTotalSalaryPage, totalSalary)
          .setOrException(UpdateIncomePayslipDeductionsPage, payslipDeductions)
          .setOrException(UpdateIncomeBonusPaymentsPage, bonusPayments)
          .setOrException(UpdateIncomeIdPage, employerId.toInt)
          .setOrException(UpdateIncomeTaxablePayPage, taxablePay)
          .setOrException(UpdateIncomeBonusOvertimeAmountPage, bonusAmount)
          .setOrException(UpdateIncomeOtherInDaysPage, payPeriodInDays)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        def checkYourAnswersPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new SUT()
            .checkYourAnswersPage(employer.id)(request)
      }

      def harnessSetup: CheckYourAnswersPageHarness =
        new CheckYourAnswersPageHarness

      "display check your answers containing populated values from the journey cache" in {

        val result = CheckYourAnswersPageHarness.harnessSetup
          .checkYourAnswersPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.checkYourAnswers.title"))
      }

      "Redirect to /Income-details" when {
        "the cache is empty" in {
          when(mockJourneyCacheNewRepository.get(any(), any()))
            .thenReturn(Future.successful(None))

          val result = CheckYourAnswersPageHarness.harnessSetup
            .checkYourAnswersPage(RequestBuilder.buildFakeGetRequestWithAuth())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
          )

        }
      }
    }
  }

  "handleCalculationResult" must {
    object HandleCalculationResultHarness {
      sealed class HandleCalculationResultHarness(currentValue: String, empId: Int, cacheEmpty: Boolean) {

        val baseUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
        val mockUserAnswers: UserAnswers = if (!cacheEmpty) {
          baseUserAnswers
            .setOrException(UpdateIncomeNamePage, "company")
            .setOrException(UpdateIncomeIdPage, empId)
            .setOrException(UpdateIncomeNewAmountPage, currentValue)
        } else {
          baseUserAnswers
        }

        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))

        def handleCalculationResult(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new SUT()
            .handleCalculationResult()(request)
      }

      def harnessSetup(
        currentValue: String,
        empId: Int = 1,
        cacheEmpty: Boolean = false
      ): HandleCalculationResultHarness =
        new HandleCalculationResultHarness(currentValue, empId, cacheEmpty)
    }
    "redirect to EditSuccessView" when {
      "journey cache returns employment name, net amount and id" in {
        val empId = 123
        val result = HandleCalculationResultHarness
          .harnessSetup("100", empId)
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/check-income-tax/update-income/success-page/$empId")
      }

      "journey cache returns employment name, net amount with large decimal value and id" in {
        val empId = 456
        val result = HandleCalculationResultHarness
          .harnessSetup("4632.460273972602739726027397260273", empId)
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/check-income-tax/update-income/success-page/$empId")
      }
    }

    "redirect to the same amount entered page" when {
      "new estimated income is equal to old income" in {

        val result = HandleCalculationResultHarness
          .harnessSetup("1")
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }

    "redirect to /income-details" when {
      "cache is empty" in {
        val result = HandleCalculationResultHarness
          .harnessSetup("", cacheEmpty = true)
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
        )
      }
    }
  }
}
