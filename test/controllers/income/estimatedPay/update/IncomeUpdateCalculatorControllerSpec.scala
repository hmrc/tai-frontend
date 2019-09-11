/*
 * Copyright 2019 HM Revenue & Customs
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
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.{Matchers, Mockito}
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, _}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.{EditIncomePayPeriodConstants, _}
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{PaySlipAmountViewModel, TaxablePaySlipAmountViewModel}
import views.html.incomes.{bonusPaymentAmount, bonusPayments, payslipAmount, taxablePayslipAmount}
import org.mockito.Matchers.{eq => eqTo}
import play.api.test.FakeRequest

import scala.concurrent.Future

class IncomeUpdateCalculatorControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JsoupMatchers with JourneyCacheConstants
    with EditIncomeIrregularPayConstants with FormValuesConstants with ControllerViewTestHelper
    with EditIncomePayPeriodConstants with ScalaFutures {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val employer = IncomeSource(id = 1, name = "sample employer")
  val defaultEmployment = Employment("company", Some("123"), new LocalDate("2016-05-26"), None, Nil, "", "", 1, None, false, false)

  val incomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService = mock[EstimatedPayJourneyCompletionService]

  def BuildEmploymentAmount(isLive: Boolean = false, isOccupationPension: Boolean = true) =
    EmploymentAmount(
      name = "name",
      description = "description",
      employmentId = employer.id,
      newAmount = 200,
      oldAmount = 200,
      isLive = isLive,
      isOccupationalPension = isOccupationPension)

  def BuildTaxCodeIncomes(incomeCount: Int) = {

    val taxCodeIncome1 =
      TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
    val taxCodeIncome2 =
      TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)

    incomeCount match {
      case 2 => Seq(taxCodeIncome1, taxCodeIncome2)
      case 1 => Seq(taxCodeIncome1)
      case 0 => Nil
    }
  }


  class TestIncomeUpdateCalculatorController
      extends IncomeUpdateCalculatorController(
        incomeService,
        employmentService,
        taxAccountService,
        estimatedPayJourneyCompletionService,
        FakeAuthAction,
        FakeValidatePerson,
        journeyCacheService,
        mock[FormPartialRetriever],
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

        def onPageLoad(employerId: Int = 1): Future[Result] = new TestIncomeUpdateCalculatorController()
          .onPageLoad(employerId)(RequestBuilder.buildFakeGetRequestWithAuth)
      }
      def setup(hasJourneyCompleted: Boolean, returnedEmployment: Option[Employment]): OnPageLoadHarness =
        new OnPageLoadHarness(hasJourneyCompleted, returnedEmployment)
    }

    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {
        val result = OnPageLoadHarness.setup(true, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .duplicateSubmissionWarningPage()
          .url
      }
    }

    "redirect to the estimatedPayLanding url" when {
      "an income update has already been performed" in {
        val result = OnPageLoadHarness.setup(false, Some(defaultEmployment))
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .estimatedPayLandingPage()
          .url
      }
    }

    "generate an internal server error " when {
      "no employments are found" in {

        val result = OnPageLoadHarness.setup(false, None)
          .onPageLoad()

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "estimatedPayLandingPage" must {
    object EstimatedPayLandingPageHarness {
      sealed class EstimatedPayLandingPageHarness() {

        when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(Seq(employer.name, employer.id.toString, TaiConstants.IncomeTypeEmployment)))

        def estimatedPayLandingPage(): Future[Result] = new TestIncomeUpdateCalculatorController()
          .estimatedPayLandingPage()(RequestBuilder.buildFakeGetRequestWithAuth())
      }

      def setup(): EstimatedPayLandingPageHarness = new EstimatedPayLandingPageHarness()
    }
    "display the estimatedPayLandingPage view" in {
      val result = EstimatedPayLandingPageHarness.setup()
          .estimatedPayLandingPage()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
    }
  }

  "duplicateSubmissionWarning" must {
    object DuplicateSubmissionWarningHarness {
      sealed class DuplicateSubmissionWarningHarness() {
        when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(Seq(employer.name, employer.id.toString, "123456", TaiConstants.IncomeTypeEmployment)))

        def duplicateSubmissionWarning(): Future[Result] = new TestIncomeUpdateCalculatorController()
          .duplicateSubmissionWarningPage()(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(): DuplicateSubmissionWarningHarness = new DuplicateSubmissionWarningHarness()
    }

    "show employment duplicateSubmissionWarning view" in {
      val result = DuplicateSubmissionWarningHarness.setup()
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
          .thenReturn(Future.successful(Seq(employer.name, employer.id.toString, "123456", TaiConstants.IncomeTypeEmployment)))

        def submitDuplicateSubmissionWarning(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .submitDuplicateSubmissionWarning()(request)
      }

      def setup(): SubmitDuplicateSubmissionWarningHarness = new SubmitDuplicateSubmissionWarningHarness()
    }

    "redirect to the estimatedPayLandingPage url when yes is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness.setup()
        .submitDuplicateSubmissionWarning(RequestBuilder
        .buildFakePostRequestWithAuth(YesNoChoice -> YesValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
        .estimatedPayLandingPage()
        .url
    }

    "redirect to the IncomeSourceSummaryPage url when no is selected" in {
      val result = SubmitDuplicateSubmissionWarningHarness.setup()
        .submitDuplicateSubmissionWarning(RequestBuilder
          .buildFakePostRequestWithAuth(YesNoChoice -> NoValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
    }
  }

  "howToUpdatePage" must {
    object HowToUpdatePageHarness {
      sealed class HowToUpdatePageHarness(cacheMap: Map[String, String], employment: Option[Employment]) {

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome])))

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(employment))

        when(incomeService.employmentAmount(any(), any())(any(), any()))
          .thenReturn(Future.successful(BuildEmploymentAmount()))

        Mockito.reset(journeyCacheService)

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(cacheMap))

        def howToUpdatePage(): Future[Result] = new TestIncomeUpdateCalculatorController()
          .howToUpdatePage(1)(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(cacheMap: Map[String, String], employment: Option[Employment] = Some(defaultEmployment)): HowToUpdatePageHarness =
        new HowToUpdatePageHarness(cacheMap, employment)
    }

    "render the right response to the user" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypePension)

      val result = HowToUpdatePageHarness.setup(cacheMap)
        .howToUpdatePage()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
    }

    "cache the employer details" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypeEmployment)

      val result = HowToUpdatePageHarness.setup(cacheMap)
        .howToUpdatePage()

      status(result) mustBe SEE_OTHER

      verify(journeyCacheService, times(1)).cache(Matchers.eq(cacheMap))(any())
    }

    "employments return empty income is none" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypePension)

      val result = HowToUpdatePageHarness.setup(cacheMap, None)
        .howToUpdatePage()

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "processHowToUpdatePage" must {
    object ProcessHowToUpdatePageHarness {
      sealed class ProcessHowToUpdatePageHarness(incomeCount: Int, currentValue: Option[String]) {

          if(incomeCount >= 0) {
            when(incomeService.editableIncomes(any()))
              .thenReturn(BuildTaxCodeIncomes(incomeCount))
          }

          if(incomeCount == 1) {
            when(incomeService.singularIncomeId(any())).thenReturn(Some(1))
          }

          if(incomeCount == 0) {
            when(incomeService.singularIncomeId(any())).thenReturn(None)
          }

          currentValue match {
            case Some(x) =>
              when(journeyCacheService.currentValue(eqTo(UpdateIncome_HowToUpdateKey))(any()))
                .thenReturn(Future.successful(Some(x)))
            case None =>
          }

        def processHowToUpdatePage(employmentAmount: EmploymentAmount): Future[Result] = new TestIncomeUpdateCalculatorController()
          .processHowToUpdatePage(
            1,
            "name",
            employmentAmount,
            TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeGetRequestWithAuth(), FakeAuthAction.user)
      }

      def setup(incomeCount: Int = -1, currentValue: Option[String] = None): ProcessHowToUpdatePageHarness =
        new ProcessHowToUpdatePageHarness(incomeCount, currentValue)
    }

    "redirect user for non live employment " when {
      "employment amount is occupation income" in {
        val employmentAmount = BuildEmploymentAmount()

        val result = ProcessHowToUpdatePageHarness.setup()
          .processHowToUpdatePage(employmentAmount)

        whenReady(result) { r =>
          r.header.status mustBe SEE_OTHER
          r.header.headers.get(LOCATION) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
        }
      }

      "employment amount is not occupation income" in {
        val employmentAmount = BuildEmploymentAmount(false, false)
        val result = ProcessHowToUpdatePageHarness.setup()
          .processHowToUpdatePage(employmentAmount)

        whenReady(result) { r =>
          r.header.status mustBe SEE_OTHER
          r.header.headers.get(LOCATION) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
        }
      }
    }

    "redirect user for is live employment " when {
      "editable incomes are greater than one and UpdateIncome_HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness.setup(2, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable incomes are greater than one and no cached UpdateIncome_HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness.setup(2)
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is singular and UpdateIncome_HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness.setup(1, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is singular and no cached UpdateIncome_HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness.setup(1)
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is none and UpdateIncome_HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness.setup(0, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }

      "editable income is none and no cached UpdateIncome_HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness.setup(0, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }
    }
  }

  "handleChooseHowToUpdate" must {
    object HandleChooseHowToUpdateHarness {
      sealed class HandleChooseHowToUpdateHarness() {

        when(journeyCacheService.cache(Matchers.eq(UpdateIncome_HowToUpdateKey), any())(any()))
          .thenReturn(Future.successful(Map.empty[String,String]))

        def handleChooseHowToUpdate(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handleChooseHowToUpdate()(request)
      }

      def setup(): HandleChooseHowToUpdateHarness =
        new HandleChooseHowToUpdateHarness()
    }

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val result = HandleChooseHowToUpdateHarness.setup()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakePostRequestWithAuth("howToUpdate" -> "incomeCalculator"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.workingHoursPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val result = HandleChooseHowToUpdateHarness.setup()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakePostRequestWithAuth("howToUpdate" -> "income"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val result = HandleChooseHowToUpdateHarness.setup()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakePostRequestWithAuth("howToUpdate" -> ""))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", ""))
      }
    }
  }

  "workingHoursPage" must {

    object WorkingHoursPageHarness {
      sealed class WorkingHoursPageHarness() {

        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_WorkingHoursKey))(any()))
          .thenReturn(Future.successful(Option(REGULAR_HOURS)))

        def workingHoursPage(): Future[Result] = new TestIncomeUpdateCalculatorController()
          .workingHoursPage()(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(): WorkingHoursPageHarness =
        new WorkingHoursPageHarness()
    }

    "display workingHours page" when {
      "journey cache returns employment name and id" in {

        val result = WorkingHoursPageHarness.setup()
          .workingHoursPage()

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }
  }

  "handleWorkingHours" must {

    object HandleWorkingHoursHarness {
      sealed class HandleWorkingHoursHarness() {

        when(journeyCacheService.cache(eqTo(UpdateIncome_WorkingHoursKey), any())(any()))
          .thenReturn(Future.successful(Map.empty[String,String]))

        when(journeyCacheService.mandatoryJourneyValueAsInt(eqTo(UpdateIncome_IdKey))(any()))
          .thenReturn(Future.successful(Right(1)))

        def handleWorkingHours(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handleWorkingHours()(request)
      }

      def setup(): HandleWorkingHoursHarness =
        new HandleWorkingHoursHarness()
    }

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {

        val result = HandleWorkingHoursHarness.setup()
          .handleWorkingHours(RequestBuilder.buildFakePostRequestWithAuth("workingHours" -> REGULAR_HOURS))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payPeriodPage().url)
      }
    }

    "redirect user back to workingHours page" when {
      "user input has error" in {

        val result = HandleWorkingHoursHarness.setup()
          .handleWorkingHours(RequestBuilder.buildFakePostRequestWithAuth("workingHours" -> ""))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }

    "redirect user back to workingHours page" when {
      "bad data submitted in form" in {

        val result = HandleWorkingHoursHarness.setup()
          .handleWorkingHours(RequestBuilder.buildFakePostRequestWithAuth("workingHours" -> "anything"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }
  }

  "payPeriodPage" must {
    object PayPeriodPageHarness {
      sealed class PayPeriodPageHarness() {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayPeriodKey))(any()))
          .thenReturn(Future.successful(Some(MONTHLY)))
        when(journeyCacheService.currentValue(eqTo(UpdateIncome_OtherInDaysKey))(any()))
          .thenReturn(Future.successful(None))

        def payPeriodPage(): Future[Result] = new TestIncomeUpdateCalculatorController()
          .payPeriodPage()(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(): PayPeriodPageHarness =
        new PayPeriodPageHarness()
    }

    "display payPeriod page" when {
      "journey cache returns employment name and id" in {

        val result = PayPeriodPageHarness.setup()
          .payPeriodPage()

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }
  }

  "handlePayPeriod" must {
    object HandlePayPeriodHarness {
      sealed class HandlePayPeriodHarness() {

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        def handlePayPeriod(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handlePayPeriod()(request)
      }

      def setup(): HandlePayPeriodHarness =
        new HandlePayPeriodHarness()
    }
    "redirect the user to payslipAmountPage page" when {
      "user selected monthly" in {

        val result = HandlePayPeriodHarness.setup()
          .handlePayPeriod(RequestBuilder.buildFakePostRequestWithAuth("payPeriod" -> "monthly"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipAmountPage().url)
      }
    }

    "redirect user back to how to payPeriod page" when {
      "user input has error" in {

        val result = HandlePayPeriodHarness.setup()
          .handlePayPeriod(RequestBuilder.buildFakePostRequestWithAuth("payPeriod" -> "nonsense"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }
  }

  "payslipAmountPage" must {
    object PayslipAmountPageHarness {
      sealed class PayslipAmountPageHarness(payPeriod: Option[String], cachedAmount: Option[String]) {
        when(journeyCacheService.collectedValues(any(), any())(any()))
          .thenReturn(Future.successful(
            Seq[String](employer.id.toString, employer.name), Seq[Option[String]](payPeriod, None, cachedAmount)))

        def payslipAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .payslipAmountPage()(request)
      }

      def setup(payPeriod: Option[String], cachedAmount: Option[String]): PayslipAmountPageHarness =
        new PayslipAmountPageHarness(payPeriod, cachedAmount)
    }

    "display payslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {

        val cachedAmount = None
        val payPeriod = Some(MONTHLY)

        val result = PayslipAmountPageHarness.setup(payPeriod, cachedAmount)
          .payslipAmountPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslip.title.month"))
      }

      "journey cache returns a prepopulated pay slip amount" in {
        val cachedAmount = Some("998787")
        val payPeriod = Some(MONTHLY)

        implicit val request = RequestBuilder.buildFakeGetRequestWithAuth()

        val result = PayslipAmountPageHarness.setup(payPeriod, cachedAmount)
          .payslipAmountPage(request)

        status(result) mustBe OK

        val expectedForm = PayslipForm
          .createForm(messages("tai.payslip.error.form.totalPay.input.mandatory"))
          .fill(PayslipForm(cachedAmount))

        val expectedViewModel = PaySlipAmountViewModel(expectedForm, payPeriod, None, employer)
        val expectedView = payslipAmount(expectedViewModel)

        result rendersTheSameViewAs expectedView
      }
    }
  }

  "handlePayslipAmount" must {
    object HandlePayslipAmountHarness {
      sealed class HandlePayslipAmountHarness(salary: String) {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayPeriodKey))(any()))
          .thenReturn(Future.successful(Some(MONTHLY)))

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_OtherInDaysKey))(any()))
          .thenReturn(Future.successful(None))

        when(journeyCacheService.cache(Matchers.eq[Map[String,String]](Map(UpdateIncome_TotalSalaryKey -> salary)))(any()))
          .thenReturn(Future.successful(Map.empty[String,String]))

        def handlePayslipAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handlePayslipAmount()(request)
      }

      def setup(salary: String = ""): HandlePayslipAmountHarness =
        new HandlePayslipAmountHarness(salary)
    }

    "redirect the user to payslipDeductionsPage page" when {
      "user entered valid pay" in {

        val salary = "£3,000"
        val result = HandlePayslipAmountHarness.setup(salary)
          .handlePayslipAmount(RequestBuilder.buildFakePostRequestWithAuth("totalSalary" -> salary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url)
      }
    }

    "redirect user back to how to payslip page with an error form" when {
      "user input has error" in {

        val result = HandlePayslipAmountHarness.setup()
          .handlePayslipAmount(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().text must include(messages("tai.income.error.form.summary"))
        doc.body().text must include(messages("tai.payslip.title.month"))
        doc.title() must include(messages("tai.payslip.title.month"))
      }
    }
  }

  "taxablePayslipAmountPage" must {
    object TaxablePayslipAmountPageHarness {
      sealed class TaxablePayslipAmountPageHarness(payPeriod: Option[String], cachedAmount: Option[String]) {

        val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
        val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TaxablePayKey)

        when(journeyCacheService.collectedValues(Matchers.eq(mandatoryKeys), Matchers.eq(optionalKeys))(any()))
          .thenReturn(
            Future.successful(
              (Seq[String](employer.id.toString, employer.name), Seq[Option[String]](payPeriod, None, cachedAmount))
            )
          )

        def taxablePayslipAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .taxablePayslipAmountPage()(request)
      }

      def setup(payPeriod: Option[String], cachedAmount: Option[String]): TaxablePayslipAmountPageHarness =
        new TaxablePayslipAmountPageHarness(payPeriod, cachedAmount)
    }
    "display taxablePayslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {

        implicit val request = RequestBuilder.buildFakeGetRequestWithAuth()

        val cachedAmount = Some("9888787")
        val payPeriod = Some(MONTHLY)

        val result = TaxablePayslipAmountPageHarness.setup(payPeriod, cachedAmount)
          .taxablePayslipAmountPage(request)

        status(result) mustBe OK

        val expectedForm = TaxablePayslipForm.createForm().fill(TaxablePayslipForm(cachedAmount))
        val expectedViewModel = TaxablePaySlipAmountViewModel(expectedForm, payPeriod, None, employer)
        result rendersTheSameViewAs taxablePayslipAmount(expectedViewModel)
      }
    }
  }

  "handleTaxablePayslipAmount" must {
    object HandleTaxablePayslipAmountPageHarness {
      sealed class HandleTaxablePayslipAmountPageHarness() {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_TotalSalaryKey))(any()))
          .thenReturn(Future.successful(Some("4000")))

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayPeriodKey))(any()))
          .thenReturn(Future.successful(Some(MONTHLY)))

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_OtherInDaysKey))(any()))
          .thenReturn(Future.successful(None))

        when(journeyCacheService.cache(eqTo[Map[String,String]](Map(UpdateIncome_TaxablePayKey -> "3000")))(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String](employer.id.toString, employer.name),
            Seq[Option[String]](Some(MONTHLY), None))
          ))

        def handleTaxablePayslipAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handleTaxablePayslipAmount()(request)
      }

      def setup(): HandleTaxablePayslipAmountPageHarness =
        new HandleTaxablePayslipAmountPageHarness()
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user entered valid taxable pay" in {

        val result = HandleTaxablePayslipAmountPageHarness.setup()
          .handleTaxablePayslipAmount(RequestBuilder.buildFakePostRequestWithAuth("taxablePay" -> "3000"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to taxablePayslip page" when {
      "user input has error" in {

        val result = HandleTaxablePayslipAmountPageHarness.setup()
          .handleTaxablePayslipAmount(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.taxablePayslip.title.month"))
      }
    }
  }

  "payslipDeductionsPage" must {
    object PayslipDeductionsPageHarness {
      sealed class PayslipDeductionsPageHarness() {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayslipDeductionsKey))(any()))
          .thenReturn(Future.successful(Some("Yes")))

        def payslipDeductionsPage(): Future[Result] = new TestIncomeUpdateCalculatorController()
          .payslipDeductionsPage()(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(): PayslipDeductionsPageHarness =
        new PayslipDeductionsPageHarness()
    }
    "display payslipDeductions" when {
      "journey cache returns employment name and id" in {
        val result = PayslipDeductionsPageHarness.setup()
          .payslipDeductionsPage()

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }

  "handlePayslipDeductions" must {
    object HandlePayslipDeductionsHarness {
      sealed class HandlePayslipDeductionsHarness() {

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(Map.empty[String,String]))

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map.empty[String,String]))

        when(journeyCacheService.flush()(any()))
          .thenReturn(Future.successful(TaiSuccessResponse))

        def handlePayslipDeductions(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handlePayslipDeductions()(request)
      }

      def setup(): HandlePayslipDeductionsHarness =
        new HandlePayslipDeductionsHarness()
    }

    "redirect the user to taxablePayslipAmountPage page" when {
      "user selected yes" in {

        val result = HandlePayslipDeductionsHarness.setup()
          .handlePayslipDeductions(RequestBuilder.buildFakePostRequestWithAuth("payslipDeductions" -> "Yes"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url)
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val result = HandlePayslipDeductionsHarness.setup()
          .handlePayslipDeductions(RequestBuilder.buildFakePostRequestWithAuth("payslipDeductions" -> "No"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to payslipDeductions page" when {
      "user input has error" in {
        val result = HandlePayslipDeductionsHarness.setup()
          .handlePayslipDeductions(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }

  "bonusPaymentsPage" must {
    object BonusPaymentsPageHarness {
      sealed class BonusPaymentsPageHarness(cachedAmount: String) {
        when(journeyCacheService.currentValue(eqTo(UpdateIncome_BonusPaymentsKey))(any()))
          .thenReturn(Future.successful(Some(cachedAmount)))

        def bonusPaymentsPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .bonusPaymentsPage()(request)
      }

      def setup(cachedAmount: String): BonusPaymentsPageHarness =
        new BonusPaymentsPageHarness(cachedAmount)
    }
    "display bonusPayments" in {

      implicit val fakeRequest = RequestBuilder.buildFakeGetRequestWithAuth()
      val cachedAmount = "1231231"

      val result = BonusPaymentsPageHarness.setup(cachedAmount)
        .bonusPaymentsPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusPaymentsForm.createForm.fill(YesNoForm(Some(cachedAmount)))
      val expectedView = bonusPayments(expectedForm, employer)

      result rendersTheSameViewAs expectedView
    }
  }

  "handleBonusPayments" must {
    object HandleBonusPaymentsHarness {
      sealed class HandleBonusPaymentsHarness() {

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(Map.empty[String,String]))
        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))
        when(journeyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))
        when(journeyCacheService.flush()(any()))
          .thenReturn(Future.successful(TaiSuccessResponse))
        when(journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq(employer.id.toString, employer.name)))

        def handleBonusPayments(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handleBonusPayments()(request)
      }

      def setup(): HandleBonusPaymentsHarness =
        new HandleBonusPaymentsHarness()
    }

    "redirect the user to bonusOvertimeAmountPage page" when {
      "user selected yes" in {

        val result = HandleBonusPaymentsHarness.setup()
          .handleBonusPayments(RequestBuilder.buildFakePostRequestWithAuth(YesNoChoice -> YesValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url)
      }
    }

    "redirect the user to checkYourAnswers page" when {
      "user selected no" in {
        val result = HandleBonusPaymentsHarness.setup()
          .handleBonusPayments(RequestBuilder.buildFakePostRequestWithAuth(YesNoChoice -> NoValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
      }
    }

    "redirect user back to how to bonusPayments page" when {
      "user input has error" in {

        implicit val fakeRequest = RequestBuilder.buildFakePostRequestWithAuth()

        val result = HandleBonusPaymentsHarness.setup()
          .handleBonusPayments(fakeRequest)

        status(result) mustBe BAD_REQUEST
        result rendersTheSameViewAs bonusPayments(BonusPaymentsForm.createForm.bindFromRequest()(fakeRequest), employer)
      }
    }
  }

  "bonusOvertimeAmountPage" must {
    object BonusOvertimeAmountPageHarness {
      sealed class BonusOvertimeAmountPageHarness() {

        when(journeyCacheService.currentValue(any())(any()))
          .thenReturn(Future.successful(Some("313321")))

        def bonusOvertimeAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .bonusOvertimeAmountPage()(request)
      }

      def setup(): BonusOvertimeAmountPageHarness =
        new BonusOvertimeAmountPageHarness()
    }

    "display bonusPaymentAmount" in {
      val cachedAmount = "313321"
      implicit val fakeRequest = RequestBuilder.buildFakeGetRequestWithAuth()

      val result = BonusOvertimeAmountPageHarness.setup()
        .bonusOvertimeAmountPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusOvertimeAmountForm.createForm().fill(BonusOvertimeAmountForm(Some(cachedAmount)))
      result rendersTheSameViewAs bonusPaymentAmount(expectedForm, employer)
    }
  }

  "handleBonusOvertimeAmount" must {
    object HandleBonusOvertimeAmountHarness {
      sealed class HandleBonusOvertimeAmountHarness() {

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(Map.empty[String,String]))

        when(journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq(employer.id.toString, employer.name)))

        def handleBonusOvertimeAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handleBonusOvertimeAmount()(request)
      }

      def setup(): HandleBonusOvertimeAmountHarness =
        new HandleBonusOvertimeAmountHarness()
    }

    "redirect the user to checkYourAnswers page" in {

      val result = HandleBonusOvertimeAmountHarness.setup()
        .handleBonusOvertimeAmount(
          RequestBuilder.buildFakePostRequestWithAuth("amount" -> "£3,000"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
    }

    "redirect the user to bonusPaymentAmount page" when {
      "user input has error" in {

        implicit val fakeRequest =
          RequestBuilder.buildFakePostRequestWithAuth("amount" -> "")

        val result = HandleBonusOvertimeAmountHarness.setup()
          .handleBonusOvertimeAmount(fakeRequest)

        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs bonusPaymentAmount(
          BonusOvertimeAmountForm.createForm().bindFromRequest()(fakeRequest),
          employer)
      }
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
          .thenReturn(Future.successful((
            Seq[String](employerName, payFrequency, totalSalary, payslipDeductions, bonusPayments, employerId),
            Seq[Option[String]](Some(taxablePay),Some(bonusAmount), Some(payPeriodInDays)))))

        def checkYourAnswersPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .checkYourAnswersPage()(request)
      }

      def setup(): CheckYourAnswersPageHarness =
        new CheckYourAnswersPageHarness()
    }
    "display check your answers containing populated values from the journey cache" in {

      val result = CheckYourAnswersPageHarness.setup()
        .checkYourAnswersPage(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.checkYourAnswers.title"))
    }
  }

  "estimatedPayPage" must {
    object EstimatedPayPageHarness {
      sealed class EstimatedPayPageHarness(payment: Option[Payment], currentCache: Map[String, String]) {

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))
        when(incomeService.latestPayment(any(), any())(any()))
          .thenReturn(Future.successful(payment))
        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(currentCache))
        when(incomeService.employmentAmount(any(), any())(any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1,1,1)))
        when(incomeService.calculateEstimatedPay(any(), any())(any()))
          .thenReturn(Future.successful(
            CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))

        def estimatedPayPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .estimatedPayPage()(request)
      }

      def setup(payment: Option[Payment] = Some(Payment(new LocalDate(), 200, 50, 25, 100, 50, 25, Monthly)), currentCache: Map[String, String] = Map.empty[String, String]): EstimatedPayPageHarness =
        new EstimatedPayPageHarness(payment, currentCache)
    }
    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay" in {

        val result = EstimatedPayPageHarness.setup(Some(Payment(new LocalDate(), 50, 1, 1, 1, 1, 1, Monthly)))
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "payYearToDate is None" in {

        val result = EstimatedPayPageHarness.setup(None)
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }

    "display incorrectTaxableIncome page" when {
      "payYearToDate is greater than gross annual pay" in {

        val result = EstimatedPayPageHarness.setup()
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))
      }
    }

    "redirect to sameEstimatedPay page" when {
      "the pay is the same" in {

        val result = EstimatedPayPageHarness.setup(currentCache = Map(UpdateIncome_ConfirmedNewAmountKey -> "100"))
          .estimatedPayPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }
    }
  }

  "handleCalculationResult" must {
    object HandleCalculationResultHarness {
      sealed class HandleCalculationResultHarness(currentValue: Option[String]) {

          when(incomeService.employmentAmount(any(), any())(any(), any()))
            .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))

          when(journeyCacheService.currentValue(eqTo(UpdateIncome_NewAmountKey))(any()))
            .thenReturn(Future.successful(currentValue))

        def handleCalculationResult(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handleCalculationResult()(request)
      }

      def setup(currentValue: Option[String]): HandleCalculationResultHarness =
        new HandleCalculationResultHarness(currentValue)
    }
    "display confirm_save_Income page" when {
      "journey cache returns employment name, net amount and id" in {

        val result = HandleCalculationResultHarness.setup(Some("100"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "journey cache returns employment name, net amount with large decimal value and id" in {

        val result = HandleCalculationResultHarness.setup(Some("4632.460273972602739726027397260273"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "redirects to the same amount entered page" ignore {

        val result = HandleCalculationResultHarness.setup(Some("1"))
          .handleCalculationResult(RequestBuilder.buildFakeGetRequestWithAuth())
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }
  }

  "editIncomeIrregularHours" must {
    object EditIncomeIrregularHoursHarness {
      sealed class EditIncomeIrregularHoursHarness(taxCodeIncome: Option[TaxCodeIncome]) {

        when(incomeService.latestPayment(any(), any())(any()))
          .thenReturn(Future.successful(Some(Payment(LocalDate.now().minusDays(1), 0, 0, 0, 0, 0, 0, Monthly))))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
          .thenReturn(Future.successful(taxCodeIncome))

        def editIncomeIrregularHours(incomeNumber: Int, request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .editIncomeIrregularHours(incomeNumber)(request)
      }

      def setup(taxCodeIncome: Option[TaxCodeIncome]): EditIncomeIrregularHoursHarness =
        new EditIncomeIrregularHoursHarness(taxCodeIncome)
    }
    "respond with OK and show the irregular hours edit page" in {

      val result = EditIncomeIrregularHoursHarness.setup(Some(TaxCodeIncome(EmploymentIncome, Some(1), 123, "description", "taxCode", "name", OtherBasisOfOperation, Live)))
        .editIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.irregular.heading"))
    }

    "respond with INTERNAL_SERVER_ERROR" when {
      "the employment income cannot be found" in {

        val result = EditIncomeIrregularHoursHarness.setup(None)
          .editIncomeIrregularHours(2, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "handleIncomeIrregularHours" must {
    object HandleIncomeIrregularHoursHarness {
      sealed class HandleIncomeIrregularHoursHarness() {

        val cacheMap = Map(
          UpdateIncome_NameKey      -> "name",
          UpdateIncome_PayToDateKey -> "123",
          UpdateIncome_DateKey      -> LocalDate.now().toString(TaiConstants.MONTH_AND_YEAR)
        )

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(Seq("name", "123")))

        when(journeyCacheService.cache(eqTo(UpdateIncome_IrregularAnnualPayKey), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(cacheMap))
        def handleIncomeIrregularHours(employmentId: Int, request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .handleIncomeIrregularHours(employmentId)(request)
      }

      def setup(): HandleIncomeIrregularHoursHarness =
        new HandleIncomeIrregularHoursHarness()
    }
    "respond with Redirect to Confirm page" in {
      val result = HandleIncomeIrregularHoursHarness.setup()
        .handleIncomeIrregularHours(1,
          RequestBuilder.buildFakePostRequestWithAuth("income" -> "999"))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(
        routes.IncomeUpdateCalculatorController.confirmIncomeIrregularHours(1).url.toString)
    }

    "respond with BAD_REQUEST" when {
      "given an input which is less than the current amount" in {
        val result = HandleIncomeIrregularHoursHarness.setup()
        .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth("income" -> "122"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(
          messages("tai.irregular.error.error.incorrectTaxableIncome", 123, LocalDate.now().toString(TaiConstants.MONTH_AND_YEAR), "name"))
      }

      "given invalid form data of invalid currency" in {
        val result = HandleIncomeIrregularHoursHarness.setup()
          .handleIncomeIrregularHours(1,
            RequestBuilder.buildFakePostRequestWithAuth("income" -> "ABC"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(messages("tai.irregular.instruction.wholePounds"))
      }

      "given invalid form data of no input" in {
        val result = HandleIncomeIrregularHoursHarness.setup()
              .handleIncomeIrregularHours(1,
                RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("error.tai.updateDataEmployment.blankValue"))

      }

      "given invalid form data of more than 9 numbers" in {
        val result = HandleIncomeIrregularHoursHarness.setup()
          .handleIncomeIrregularHours(1,
            RequestBuilder.buildFakePostRequestWithAuth("income" -> "1234567890"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("error.tai.updateDataEmployment.maxLength"))

      }
    }
  }

  "confirmIncomeIrregularHours" must {
    object ConfirmIncomeIrregularHoursHarness {
      sealed class ConfirmIncomeIrregularHoursHarness(failure: Boolean, newAmount: Int, confirmedNewAmount: Int, payToDate: Int) {

        val future =
          if(failure)  {
            Future.failed(new Exception)
          }
          else {
            Future.successful((Seq(employer.name, newAmount.toString, payToDate.toString), Seq(Some(confirmedNewAmount.toString))))
          }

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(future)

        def confirmIncomeIrregularHours(employmentId: Int, request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .confirmIncomeIrregularHours(employmentId)(request)
      }
      def setup(failure: Boolean = false, newAmount: Int = 1235, confirmedNewAmount: Int = 1234, payToDate: Int = 123): ConfirmIncomeIrregularHoursHarness =
        new ConfirmIncomeIrregularHoursHarness(failure, newAmount, confirmedNewAmount, payToDate)
    }

    "respond with Ok" in {

      val result = ConfirmIncomeIrregularHoursHarness.setup()
        .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(
        messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
    }

    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {

      val result = ConfirmIncomeIrregularHoursHarness.setup(failure = true)
        .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "redirect to SameEstimatedPayPage" when {
      "the same amount of pay has been entered" in {

        val result = ConfirmIncomeIrregularHoursHarness.setup(confirmedNewAmount = 123, newAmount = 123)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }
    }

    "redirect to IrregularSameEstimatedPayPage" when {
      "the same amount of payment to date has been entered" in {
        val result = ConfirmIncomeIrregularHoursHarness.setup(newAmount = 123)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }
  }

  "submitIncomeIrregularHours" must {
    object SubmitIncomeIrregularHoursHarness {
      sealed class SubmitIncomeIrregularHoursHarness(mandatoryValuesFuture: Future[Seq[String]]) {

        when(
          journeyCacheService.mandatoryValues(any())(any())
        ).thenReturn(mandatoryValuesFuture)
        when(
          taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())
        ).thenReturn(
          Future.successful(TaiSuccessResponse)
        )
        when(estimatedPayJourneyCompletionService.journeyCompleted(Matchers.eq(employer.id.toString))(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))


        def submitIncomeIrregularHours(employmentId: Int, request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] = new TestIncomeUpdateCalculatorController()
          .submitIncomeIrregularHours(employmentId)(request)
      }
      def setup(mandatoryValuesFuture: Future[Seq[String]]): SubmitIncomeIrregularHoursHarness =
        new SubmitIncomeIrregularHoursHarness(mandatoryValuesFuture)
    }
    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {

      val result: Future[Result] = SubmitIncomeIrregularHoursHarness.setup(Future.failed(new Exception))
        .submitIncomeIrregularHours(1,
        RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "sends Ok on successful submit" in {

      val result = SubmitIncomeIrregularHoursHarness.setup(Future.successful(Seq(employer.name, "123", employer.id.toString)))
          .submitIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      doc.title() must include(messages("tai.incomes.updated.check.title", employer.name))
    }
  }
}