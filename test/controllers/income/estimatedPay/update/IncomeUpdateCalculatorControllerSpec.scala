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
import controllers.{ControllerViewTestHelper, FakeAuthAction, FakeTaiPlayApplication}
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.model.domain.{Employment, _}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.{EditIncomePayPeriodConstants, _}
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import org.mockito.Matchers.{eq => eqTo}
import play.api.test.FakeRequest

import scala.concurrent.Future

class IncomeUpdateCalculatorControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JsoupMatchers with JourneyCacheConstants
    with EditIncomeIrregularPayConstants with FormValuesConstants with ControllerViewTestHelper
    with EditIncomePayPeriodConstants with ScalaFutures {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val employer = IncomeSource(id = 1, name = "sample employer")
  val defaultEmployment =
    Employment("company", Some("123"), new LocalDate("2016-05-26"), None, Nil, "", "", 1, None, false, false)

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
        app.injector.instanceOf[MessagesApi],
        journeyCacheService,
        MockPartialRetriever,
        MockTemplateRenderer
      ) {
    when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
      .thenReturn(Future.successful(employer.id))
    when(journeyCacheService.mandatoryValue(Matchers.eq(UpdateIncome_NameKey))(any()))
      .thenReturn(Future.successful(employer.name))
  }

  "onPageLoad" must {
    object OnPageLoadHarness {
      sealed class OnPageLoadHarness(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]) {
        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(returnedEmployment))

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(eqTo("1"))(any()))
          .thenReturn(Future.successful(hasJourneyCompleted))

        def onPageLoad(employerId: Int = 1): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .onPageLoad(employerId)(RequestBuilder.buildFakeGetRequestWithAuth)
      }
      def setup(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]): OnPageLoadHarness =
        new OnPageLoadHarness(hasJourneyCompleted, returnedEmployment)
    }

    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {
        val result = OnPageLoadHarness
          .setup(true, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .duplicateSubmissionWarningPage()
          .url
      }
    }

    "redirect to the estimatedPayLanding url" when {
      "an income update has already been performed" in {
        val result = OnPageLoadHarness
          .setup(false, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
          .estimatedPayLandingPage()
          .url
      }
    }

    "generate an internal server error " when {
      "no employments are found" in {

        val result = OnPageLoadHarness
          .setup(false, None)
          .onPageLoad()

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "duplicateSubmissionWarning" must {
    object DuplicateSubmissionWarningHarness {
      sealed class DuplicateSubmissionWarningHarness() {
        when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
          .thenReturn(
            Future.successful(Seq(employer.name, employer.id.toString, "123456", TaiConstants.IncomeTypeEmployment)))

        def duplicateSubmissionWarning(): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .duplicateSubmissionWarningPage()(RequestBuilder.buildFakeGetRequestWithAuth)
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
      sealed class SubmitDuplicateSubmissionWarningHarness() {
        when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
          .thenReturn(
            Future.successful(Seq(employer.name, employer.id.toString, "123456", TaiConstants.IncomeTypeEmployment)))

        def submitDuplicateSubmissionWarning(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .submitDuplicateSubmissionWarning()(request)
      }

      def setup(): SubmitDuplicateSubmissionWarningHarness = new SubmitDuplicateSubmissionWarningHarness()
    }

    "redirect to the estimatedPayLandingPage url when yes is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .setup()
        .submitDuplicateSubmissionWarning(RequestBuilder
          .buildFakePostRequestWithAuth(YesNoChoice -> YesValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController
        .estimatedPayLandingPage()
        .url
    }

    "redirect to the IncomeSourceSummaryPage url when no is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness
        .setup()
        .submitDuplicateSubmissionWarning(RequestBuilder
          .buildFakePostRequestWithAuth(YesNoChoice -> NoValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
    }
  }

  "checkYourAnswersPage" must {
    object CheckYourAnswersPageHarness {
      sealed class CheckYourAnswersPageHarness() {

        val employerName = "Employer1"
        val payFrequency = "monthly"
        val totalSalary = "10000"
        val payslipDeductions = "yes"
        val bonusPayments = "yes"
        val taxablePay = "8000"
        val bonusAmount = "1000"
        val payPeriodInDays = "3"
        val employerId = "1"

        when(journeyCacheService.collectedValues(any(), any())(any()))
          .thenReturn(
            Future.successful(
              (
                Seq[String](employerName, payFrequency, totalSalary, payslipDeductions, bonusPayments, employerId),
                Seq[Option[String]](Some(taxablePay), Some(bonusAmount), Some(payPeriodInDays)))))

        def checkYourAnswersPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .checkYourAnswersPage()(request)
      }

      def setup(): CheckYourAnswersPageHarness =
        new CheckYourAnswersPageHarness()
    }
    "display check your answers containing populated values from the journey cache" in {

      val result = CheckYourAnswersPageHarness
        .setup()
        .checkYourAnswersPage(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.checkYourAnswers.title"))
    }
  }

  "handleCalculationResult" must {
    object HandleCalculationResultHarness {
      sealed class HandleCalculationResultHarness(currentValue: Option[String]) {

        when(incomeService.employmentAmount(any(), any())(any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_NewAmountKey))(any()))
          .thenReturn(Future.successful(currentValue))

        def handleCalculationResult(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateCalculatorController()
            .handleCalculationResult()(request)
      }

      def setup(currentValue: Option[String]): HandleCalculationResultHarness =
        new HandleCalculationResultHarness(currentValue)
    }
    "display confirm_save_Income page" when {
      "journey cache returns employment name, net amount and id" in {

        val result = HandleCalculationResultHarness
          .setup(Some("100"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "journey cache returns employment name, net amount with large decimal value and id" in {

        val result = HandleCalculationResultHarness
          .setup(Some("4632.460273972602739726027397260273"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "redirects to the same amount entered page" ignore {

        val result = HandleCalculationResultHarness
          .setup(Some("1"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }
  }
}
