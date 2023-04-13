/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.{ControllerViewTestHelper, ErrorPagesHandler, FakeAuthAction}
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.BaseSpec
import views.html.incomes.estimatedPayment.update.CheckYourAnswersView
import views.html.incomes.{ConfirmAmountEnteredView, DuplicateSubmissionWarningView}

import java.time.LocalDate
import scala.concurrent.Future

class IncomeUpdateCalculatorControllerSpec
    extends BaseSpec with JsoupMatchers with ControllerViewTestHelper with ScalaFutures {

  val employerId = 1
  val employer: IncomeSource = IncomeSource(id = employerId, name = "sample employer")
  val defaultEmployment: Employment =
    Employment(
      "company",
      Live,
      Some("123"),
      LocalDate.parse("2016-05-26"),
      None,
      Nil,
      "",
      "",
      employerId,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false)

  val incomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService =
    mock[EstimatedPayJourneyCompletionService]

  class TestIncomeUpdateCalculatorController
      extends IncomeUpdateCalculatorController(
        incomeService,
        employmentService,
        taxAccountService,
        estimatedPayJourneyCompletionService,
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        inject[DuplicateSubmissionWarningView],
        inject[CheckYourAnswersView],
        inject[ConfirmAmountEnteredView],
        journeyCacheService,
        MockTemplateRenderer,
        inject[ErrorPagesHandler]
      ) {}

  "onPageLoad" must {
    object OnPageLoadHarness {
      sealed class OnPageLoadHarness(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]) {
        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(returnedEmployment))

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(eqTo("1"))(any()))
          .thenReturn(Future.successful(hasJourneyCompleted))

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncomeConstants.IdKey))(any()))
          .thenReturn(Future.successful(Right(employer.id)))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncomeConstants.NameKey))(any()))
          .thenReturn(Future.successful(Right(employer.name)))

        def onPageLoad(employerId: Int = employerId): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .onPageLoad(employerId)(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def setup(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]): OnPageLoadHarness =
        new OnPageLoadHarness(hasJourneyCompleted, returnedEmployment)
    }

    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {
        val result = OnPageLoadHarness
          .setup(hasJourneyCompleted = true, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .duplicateSubmissionWarningPage(employerId)
          .url
      }
    }

    "redirect to the estimatedPayLanding url" when {
      "an income update has already been performed" in {
        val result = OnPageLoadHarness
          .setup(hasJourneyCompleted = false, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
          .estimatedPayLandingPage(employerId)
          .url
      }
    }

    "generate an internal server error " when {
      "no employments are found" in {

        val result = OnPageLoadHarness
          .setup(hasJourneyCompleted = false, None)
          .onPageLoad()

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "duplicateSubmissionWarning" must {
    object DuplicateSubmissionWarningHarness {
      sealed class DuplicateSubmissionWarningHarness() {
        when(journeyCacheService.mandatoryJourneyValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(
            Right(Seq(employer.name, employer.id.toString, "123456", TaiConstants.IncomeTypeEmployment))))

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncomeConstants.IdKey))(any()))
          .thenReturn(Future.successful(Right(employer.id)))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncomeConstants.NameKey))(any()))
          .thenReturn(Future.successful(Right(employer.name)))

        def duplicateSubmissionWarning(): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .duplicateSubmissionWarningPage(employerId)(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def setup(): DuplicateSubmissionWarningHarness = new DuplicateSubmissionWarningHarness()
    }

    "show employment duplicateSubmissionWarning view" in {
      val result = DuplicateSubmissionWarningHarness
        .setup()
        .duplicateSubmissionWarning()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", employer.name))
    }
  }

  "submitDuplicateSubmissionWarning" must {
    object SubmitDuplicateSubmissionWarningHarness {
      sealed class SubmitDuplicateSubmissionWarningHarness(employmentType: String) {
        when(journeyCacheService.mandatoryJourneyValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(Right(Seq(employer.name, "123456", employmentType))))

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncomeConstants.IdKey))(any()))
          .thenReturn(Future.successful(Right(employer.id)))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncomeConstants.NameKey))(any()))
          .thenReturn(Future.successful(Right(employer.name)))

        def submitDuplicateSubmissionWarning(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .submitDuplicateSubmissionWarning(employerId)(request)
      }

      def setup(employmentType: String): SubmitDuplicateSubmissionWarningHarness =
        new SubmitDuplicateSubmissionWarningHarness(employmentType)
    }

    "redirect to the estimatedPayLandingPage url when yes is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .setup(TaiConstants.IncomeTypeEmployment)
        .submitDuplicateSubmissionWarning(RequestBuilder
          .buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
        .estimatedPayLandingPage(employerId)
        .url
    }

    "redirect to the IncomeSourceSummaryPage url when no is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .setup(TaiConstants.IncomeTypeEmployment)
        .submitDuplicateSubmissionWarning(RequestBuilder
          .buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
    }
    "use pension vm" when {
      "income type is pension" in {

        val result = SubmitDuplicateSubmissionWarningHarness
          .setup(TaiConstants.IncomeTypePension)
          .submitDuplicateSubmissionWarning(RequestBuilder
            .buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc must haveHeadingWithText(messages("tai.incomes.warning.pension.heading", employer.name))

      }
    }

    "use employment vm" when {
      "income type is employment" in {

        val result = SubmitDuplicateSubmissionWarningHarness
          .setup(TaiConstants.IncomeTypeEmployment)
          .submitDuplicateSubmissionWarning(RequestBuilder
            .buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", employer.name))

      }
    }
  }

  "checkYourAnswersPage" must {
    object CheckYourAnswersPageHarness {
      sealed class CheckYourAnswersPageHarness(cacheEmpty: Boolean) {

        val employerName = "Employer1"
        val payFrequency = "monthly"
        val totalSalary = "10000"
        val payslipDeductions = "yes"
        val bonusPayments = "yes"
        val taxablePay = "8000"
        val bonusAmount = "1000"
        val payPeriodInDays = "3"
        val employerId = "1"

        if (cacheEmpty) {
          when(journeyCacheService.collectedJourneyValues(any(), any())(any()))
            .thenReturn(Future.successful(Left("cacheEmpty")))
        } else {
          when(journeyCacheService.collectedJourneyValues(any(), any())(any()))
            .thenReturn(Future.successful(Right(
              Seq[String](employerName, payFrequency, totalSalary, payslipDeductions, bonusPayments, employerId),
              Seq[Option[String]](Some(taxablePay), Some(bonusAmount), Some(payPeriodInDays))
            )))
          when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncomeConstants.IdKey))(any()))
            .thenReturn(Future.successful(Right(employer.id)))
          when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncomeConstants.NameKey))(any()))
            .thenReturn(Future.successful(Right(employer.name)))
        }

        def checkYourAnswersPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .checkYourAnswersPage(employer.id)(request)
      }

      def setup(cacheEmpty: Boolean): CheckYourAnswersPageHarness =
        new CheckYourAnswersPageHarness(cacheEmpty)

      "display check your answers containing populated values from the journey cache" in {

        val result = CheckYourAnswersPageHarness
          .setup(false)
          .checkYourAnswersPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.checkYourAnswers.title"))
      }

      "Redirect to /Income-details" when {
        "the cache is empty" in {
          val result = CheckYourAnswersPageHarness
            .setup(true)
            .checkYourAnswersPage(RequestBuilder.buildFakeGetRequestWithAuth())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url)

        }
      }
    }
  }

  "handleCalculationResult" must {
    object HandleCalculationResultHarness {
      sealed class HandleCalculationResultHarness(currentValue: Option[String], cacheEmpty: Boolean) {

        when(incomeService.employmentAmount(any(), any())(any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))

        if (cacheEmpty) {
          when(journeyCacheService.mandatoryJourneyValues(any())(any()))
            .thenReturn(Future.successful(Left("empty cache")))
          when(journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))
        } else {
          when(journeyCacheService.currentValue(eqTo(UpdateIncomeConstants.NewAmountKey))(any()))
            .thenReturn(Future.successful(currentValue))
        }

        def handleCalculationResult(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .handleCalculationResult()(request)
      }

      def setup(currentValue: Option[String], cacheEmpty: Boolean = false): HandleCalculationResultHarness =
        new HandleCalculationResultHarness(currentValue, cacheEmpty)
    }
    "display ConfirmAmountEnteredView" when {
      "journey cache returns employment name, net amount and id" in {

        val result = HandleCalculationResultHarness
          .setup(Some("100"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
      }

      "journey cache returns employment name, net amount with large decimal value and id" in {

        val result = HandleCalculationResultHarness
          .setup(Some("4632.460273972602739726027397260273"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
      }

      "redirects to the same amount entered page" ignore {

        val result = HandleCalculationResultHarness
          .setup(Some("1"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay.url)

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
      }
    }
    "redirect to /income-details" when {
      "cache is empty" in {

        val result = HandleCalculationResultHarness
          .setup(Some("1"), true)
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url)

      }
    }
  }
}
