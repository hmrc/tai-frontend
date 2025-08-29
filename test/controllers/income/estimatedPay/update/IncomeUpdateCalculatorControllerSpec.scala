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
import pages.income.*
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.*
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.util.constants.*
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

  val pensionEmployment: Employment = defaultEmployment.copy(
    name = "pension co",
    receivingOccupationalPension = true
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
      ) {}

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeService, employmentService)
  }

  "onPageLoad" must {
    object OnPageLoadHarness {
      sealed class OnPageLoadHarness(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]) {

        val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
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

    "redirect to the duplicateSubmissionWarning url when an income update has already been performed" in {
      val result = OnPageLoadHarness
        .harnessSetup(hasJourneyCompleted = true, Some(defaultEmployment))
        .onPageLoad()

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe
        controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .duplicateSubmissionWarningPage(employerId)
          .url
    }

    "redirect to the estimatedPayLanding url when no update has yet been performed" in {
      val result = OnPageLoadHarness
        .harnessSetup(hasJourneyCompleted = false, Some(defaultEmployment))
        .onPageLoad()

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe
        controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
          .estimatedPayLandingPage(employerId)
          .url
    }

    "return internal server error when no employment is found" in {
      val result = OnPageLoadHarness
        .harnessSetup(hasJourneyCompleted = false, None)
        .onPageLoad()

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "duplicateSubmissionWarningPage" must {
    object DuplicateSubmissionWarningHarness {
      sealed class DuplicateSubmissionWarningHarness(emp: Employment, confirmedAmount: Option[Int]) {

        val mockUserAnswers: UserAnswers = {
          val base = UserAnswers(sessionId, randomNino().nino)
          confirmedAmount
            .map(a => base.setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), a.toString))
            .getOrElse(base)
        }

        setup(mockUserAnswers)

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(emp)))

        def run(): Future[Result] =
          new SUT().duplicateSubmissionWarningPage(employerId)(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def harness(emp: Employment, confirmedAmount: Option[Int] = Some(123456)): DuplicateSubmissionWarningHarness =
        new DuplicateSubmissionWarningHarness(emp, confirmedAmount)
    }

    "show employment duplicateSubmissionWarning view" in {
      val result = DuplicateSubmissionWarningHarness
        .harness(defaultEmployment, Some(123456))
        .run()

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", defaultEmployment.name))
    }

    "show pension duplicateSubmissionWarning view when receivingOccupationalPension is true" in {
      val result = DuplicateSubmissionWarningHarness
        .harness(pensionEmployment, Some(999))
        .run()

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc must haveHeadingWithText(messages("tai.incomes.warning.pension.heading", pensionEmployment.name))
    }

    "return internal server error when mandatory values are missing (e.g. no confirmed amount in UA)" in {
      val result = DuplicateSubmissionWarningHarness
        .harness(defaultEmployment, None)
        .run()

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "submitDuplicateSubmissionWarning" must {
    object SubmitDuplicateSubmissionWarningHarness {
      sealed class SubmitDuplicateSubmissionWarningHarness(emp: Employment, confirmedAmount: Int) {

        val mockUserAnswers: UserAnswers =
          UserAnswers(sessionId, randomNino().nino)
            .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), confirmedAmount.toString)

        setup(mockUserAnswers)

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(emp)))

        def post(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new SUT().submitDuplicateSubmissionWarning(employerId)(request)
      }

      def harness(emp: Employment = defaultEmployment, confirmedAmount: Int = 123456) =
        new SubmitDuplicateSubmissionWarningHarness(emp, confirmedAmount)
    }

    "redirect to the estimatedPayLandingPage url when yes is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .harness()
        .post(
          RequestBuilder.buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
        )

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe
        controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
          .estimatedPayLandingPage(employerId)
          .url
    }

    "redirect to the IncomeSourceSummaryPage url when no is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .harness()
        .post(
          RequestBuilder.buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue)
        )

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
    }

    "re-render page with pension vm when no choice provided and employment is pension" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .harness(emp = pensionEmployment)
        .post(RequestBuilder.buildFakePostRequestWithAuth())

      status(result) mustBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc must haveHeadingWithText(messages("tai.incomes.warning.pension.heading", pensionEmployment.name))
    }

    "re-render page with employment vm when no choice provided and employment is NOT pension" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .harness(emp = defaultEmployment)
        .post(RequestBuilder.buildFakePostRequestWithAuth())

      status(result) mustBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", defaultEmployment.name))
    }
  }

  "checkYourAnswersPage" must {
    object CheckYourAnswersPageHarness {
      sealed class CheckYourAnswersPageHarness(returnedEmployment: Option[Employment], withMandatory: Boolean) {

        val uaBase = UserAnswers(sessionId, randomNino().nino)

        val mockUserAnswers: UserAnswers =
          if (withMandatory) {
            uaBase
              .setOrException(UpdateIncomePayPeriodPage, "monthly")
              .setOrException(UpdateIncomeTotalSalaryPage, "10000")
              .setOrException(UpdateIncomePayslipDeductionsPage, "yes")
              .setOrException(UpdateIncomeBonusPaymentsPage, "yes")
              .setOrException(UpdateIncomeTaxablePayPage, "8000")
              .setOrException(UpdateIncomeBonusOvertimeAmountPage, "1000")
              .setOrException(UpdateIncomeOtherInDaysPage, "3")
          } else {
            uaBase
          }

        setup(mockUserAnswers)

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(returnedEmployment))

        def run(): Future[Result] =
          new SUT().checkYourAnswersPage(employerId)(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def harness(returnedEmployment: Option[Employment], withMandatory: Boolean) =
        new CheckYourAnswersPageHarness(returnedEmployment, withMandatory)
    }

    "display check your answers containing populated values from the journey cache" in {
      val result = CheckYourAnswersPageHarness
        .harness(returnedEmployment = Some(defaultEmployment), withMandatory = true)
        .run()

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.checkYourAnswers.title"))
    }

    "Redirect to /Income-details when mandatory journey values are missing" in {
      val result = CheckYourAnswersPageHarness
        .harness(returnedEmployment = Some(defaultEmployment), withMandatory = false)
        .run()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
      )
    }

    "Redirect to /Income-details when employment not found" in {
      val result = CheckYourAnswersPageHarness
        .harness(returnedEmployment = None, withMandatory = true)
        .run()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
      )
    }
  }

  "handleCalculationResult" must {
    object HandleCalculationResultHarness {
      sealed class HandleCalculationResultHarness(currentValue: String, cacheEmpty: Boolean) {

        val baseUA: UserAnswers          = UserAnswers(sessionId, randomNino().nino)
        val mockUserAnswers: UserAnswers =
          if (!cacheEmpty) baseUA.setOrException(UpdateIncomeNewAmountPage, currentValue) else baseUA

        setup(mockUserAnswers)

        when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", employerId, Some(1))))

        def run(empId: Int = employerId): Future[Result] =
          new SUT().handleCalculationResult(empId)(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def harness(currentValue: String, cacheEmpty: Boolean = false): HandleCalculationResultHarness =
        new HandleCalculationResultHarness(currentValue, cacheEmpty)
    }

    "redirect to the same amount entered page when new estimated income equals old income" in {
      val result = HandleCalculationResultHarness
        .harness("1")
        .run()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
    }

    "redirect to update estimated income page when new differs from old (or old missing)" in {
      val diffResult = HandleCalculationResultHarness
        .harness("500")
        .run()

      status(diffResult) mustBe SEE_OTHER
      redirectLocation(diffResult) mustBe Some(
        controllers.routes.IncomeController.updateEstimatedIncome(employerId).url
      )

      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(EmploymentAmount("", "", employerId, None)))

      val missingOld = HandleCalculationResultHarness
        .harness("500")
        .run()

      status(missingOld) mustBe SEE_OTHER
      redirectLocation(missingOld) mustBe Some(
        controllers.routes.IncomeController.updateEstimatedIncome(employerId).url
      )
    }

    "redirect to /income-details when cache is empty" in {
      val result = HandleCalculationResultHarness
        .harness("", cacheEmpty = true)
        .run()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
      )
    }
  }
}
