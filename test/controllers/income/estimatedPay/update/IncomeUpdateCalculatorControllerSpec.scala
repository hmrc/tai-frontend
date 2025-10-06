/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.{ControllerViewTestHelper, ErrorPagesHandler}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.income._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.BaseSpec
import views.html.incomes.DuplicateSubmissionWarningView
import views.html.incomes.estimatedPayment.update.CheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Random

class IncomeUpdateCalculatorControllerSpec
    extends BaseSpec
    with JsoupMatchers
    with ControllerViewTestHelper
    with ScalaFutures {

  def randomNino(): Nino = new Generator(new Random()).nextNino

  val employerId             = 1
  val sessionId: String      = "testSessionId"
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
      receivingOccupationalPension = false,
      EmploymentIncome
    )

  val mockIncomeService: IncomeService     = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]

  class SUT
      extends IncomeUpdateCalculatorController(
        mockIncomeService,
        employmentService,
        mockAuthJourney,
        mcc,
        inject[DuplicateSubmissionWarningView],
        inject[CheckYourAnswersView],
        inject[ErrorPagesHandler]
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(employmentService, mockIncomeService)
  }

  "onPageLoad" must {
    object OnPageLoadHarness {
      sealed class OnPageLoadHarness(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]) {

        private val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(employerId), hasJourneyCompleted)
        setup(mockUserAnswers)

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(returnedEmployment))

        def onPageLoad(id: Int = employerId): Future[Result] =
          new SUT().onPageLoad(id)(RequestBuilder.buildFakeGetRequestWithAuth())
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
        redirectLocation(result).get mustBe
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
            .duplicateSubmissionWarningPage(employerId)
            .url
      }
    }

    "redirect to the estimatedPayLanding url" when {
      "an income update has not been performed" in {
        val result = OnPageLoadHarness
          .harnessSetup(hasJourneyCompleted = false, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe
          controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
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
      sealed class DuplicateSubmissionWarningHarness {

        private val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), "123456")
        setup(mockUserAnswers)

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(defaultEmployment)))

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
      doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", defaultEmployment.name))
    }
  }

  "submitDuplicateSubmissionWarning" must {
    object SubmitDuplicateSubmissionWarningHarness {
      sealed class SubmitDuplicateSubmissionWarningHarness(isPension: Boolean) {

        private val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), "123456")
        setup(mockUserAnswers)

        private val emp: Employment = defaultEmployment.copy(receivingOccupationalPension = isPension)
        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(emp)))

        def submitDuplicateSubmissionWarning(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new SUT()
            .submitDuplicateSubmissionWarning(employerId)(request)
      }

      def harnessSetup(isPension: Boolean): SubmitDuplicateSubmissionWarningHarness =
        new SubmitDuplicateSubmissionWarningHarness(isPension)
    }

    "redirect to the estimatedPayLandingPage url when yes is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .harnessSetup(isPension = false)
        .submitDuplicateSubmissionWarning(
          RequestBuilder
            .buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
        )

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe
        controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
          .estimatedPayLandingPage(employerId)
          .url
    }

    "redirect to the IncomeSourceSummaryPage url when no is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .harnessSetup(isPension = false)
        .submitDuplicateSubmissionWarning(
          RequestBuilder
            .buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue)
        )

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
    }

    "use pension vm" when {
      "employment is a pension" in {
        val result = SubmitDuplicateSubmissionWarningHarness
          .harnessSetup(isPension = true)
          .submitDuplicateSubmissionWarning(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc must haveHeadingWithText(messages("tai.incomes.warning.pension.heading", defaultEmployment.name))
      }
    }

    "use employment vm" when {
      "employment is not a pension" in {
        val result = SubmitDuplicateSubmissionWarningHarness
          .harnessSetup(isPension = false)
          .submitDuplicateSubmissionWarning(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", defaultEmployment.name))
      }
    }
  }

  "checkYourAnswersPage" must {
    object CheckYourAnswersPageHarness {
      sealed class CheckYourAnswersPageHarness {

        private val employerName      = "Employer1"
        private val payFrequency      = "monthly"
        private val totalSalary       = "10000"
        private val payslipDeductions = "yes"
        private val bonusPayments     = "yes"
        private val taxablePay        = "8000"
        private val bonusAmount       = "1000"
        private val payPeriodInDays   = "3"

        private val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomePayPeriodPage, payFrequency)
          .setOrException(UpdateIncomeTotalSalaryPage, totalSalary)
          .setOrException(UpdateIncomePayslipDeductionsPage, payslipDeductions)
          .setOrException(UpdateIncomeBonusPaymentsPage, bonusPayments)
          .setOrException(UpdateIncomeTaxablePayPage, taxablePay)
          .setOrException(UpdateIncomeBonusOvertimeAmountPage, bonusAmount)
          .setOrException(UpdateIncomeOtherInDaysPage, payPeriodInDays)
        setup(mockUserAnswers)

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(defaultEmployment.copy(name = employerName))))

        def checkYourAnswersPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new SUT().checkYourAnswersPage(employerId)(request)
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
        "mandatory values are missing" in {
          setup(UserAnswers(sessionId, randomNino().nino))

          val result =
            new SUT().checkYourAnswersPage(employerId)(RequestBuilder.buildFakeGetRequestWithAuth())

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
      sealed class HandleCalculationResultHarness(currentValue: String, empId: Int) {

        private val mockUserAnswers: UserAnswers =
          UserAnswers(sessionId, randomNino().nino).setOrException(UpdateIncomeNewAmountPage, currentValue)

        setup(mockUserAnswers)

        when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, Some(1))))

        def handleCalculationResult(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new SUT().handleCalculationResult(empId)(request)
      }

      def harnessSetup(currentValue: String, empId: Int = 1): HandleCalculationResultHarness =
        new HandleCalculationResultHarness(currentValue, empId)
    }

    "redirect to update estimated income page" when {
      "journey cache has a new amount and it differs from the current amount" in {
        val empId  = 123
        val result = HandleCalculationResultHarness
          .harnessSetup("100", empId)
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.updateEstimatedIncome(empId).url)
      }

      "journey cache has a large decimal new amount and it differs from the current amount" in {
        val empId  = 456
        val result = HandleCalculationResultHarness
          .harnessSetup("4632.460273972602739726027397260273", empId)
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.updateEstimatedIncome(empId).url)
      }
    }

    "redirect to the same amount entered page" when {
      "new estimated income is equal to old income" in {
        val result = HandleCalculationResultHarness
          .harnessSetup("1")
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay(employerId).url)
      }
    }

    "redirect to /income-details" when {
      "no new amount found in user answers" in {
        setup(UserAnswers(sessionId, randomNino().nino))

        val result = new SUT().handleCalculationResult(employerId)(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
        )
      }
    }

    "redirect to update estimated income page" when {
      "income.oldAmount is None (missing old amount)" in {
        when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, None)))

        val result = HandleCalculationResultHarness
          .harnessSetup("500")
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.updateEstimatedIncome(1).url)
      }
    }
  }
}
