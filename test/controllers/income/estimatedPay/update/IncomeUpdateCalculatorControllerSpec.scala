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
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]
  val journeyCacheService = mock[JourneyCacheService]
  val estimatedPayJourneyCompletionService = mock[EstimatedPayJourneyCompletionService]

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

  object UnifiedHarness {
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

    def BuildEmploymentAmount(isLive:Boolean = false, isOccupationPension:Boolean = true) =
      EmploymentAmount(
        name = "name",
        description = "description",
        employmentId = employer.id,
        newAmount = 200,
        oldAmount = 200,
        isLive = false,
        isOccupationalPension = true)

    sealed class UnifiedHarness(controller: IncomeUpdateCalculatorController) {
      private def BuildFakeGet() = RequestBuilder.buildFakeRequestWithAuth("GET")

      //Requests
      def onPageLoad(employerId: Integer = 1) = controller.onPageLoad(employerId)(BuildFakeGet())

      def estimatedPayLandingPage() = controller.estimatedPayLandingPage()(BuildFakeGet())

      def duplicateSubmissionWarningPage() = controller.duplicateSubmissionWarningPage()(BuildFakeGet())

      def submitDuplicateSubmissionWarning(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.submitDuplicateSubmissionWarning()(request)

      def howToUpdatePage() = controller.howToUpdatePage(1)(BuildFakeGet())

      def processHowToUpdatePage(employmentAmount: EmploymentAmount) =
        controller.processHowToUpdatePage(
          1,
          "name",
          employmentAmount,
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(
          BuildFakeGet(),
          FakeAuthAction.user)

      def handleChooseHowToUpdate(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handleChooseHowToUpdate()(request)

      def payPeriodPage() = controller.payPeriodPage()(BuildFakeGet())

      def workingHoursPage() = controller.workingHoursPage()(BuildFakeGet())

      def handleWorkingHours(request: FakeRequest[AnyContentAsFormUrlEncoded])  = controller.handleWorkingHours()(request)

      def handlePayPeriod(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handlePayPeriod()(request)

      def payslipAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.payslipAmountPage()(request)

      def handlePayslipAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handlePayslipAmount()(request)

      def taxablePayslipAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.taxablePayslipAmountPage()(request)

      def handleTaxablePayslipAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handleTaxablePayslipAmount()(request)

      def payslipDeductionsPage() = controller.payslipDeductionsPage()(BuildFakeGet())

      def handlePayslipDeductions(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handlePayslipDeductions()(request)

      def bonusPaymentsPage(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.bonusPaymentsPage()(request)

      def handleBonusPayments(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handleBonusPayments()(request)

      def bonusOvertimeAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.bonusOvertimeAmountPage()(request)

      def handleBonusOvertimeAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handleBonusOvertimeAmount()(request)

      def checkYourAnswersPage(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.checkYourAnswersPage()(request)

      def estimatedPayPage(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.estimatedPayPage()(request)

      def handleCalculationResult(request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handleCalculationResult()(request)

      def editIncomeIrregularHours(incomeNumber: Int, request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.editIncomeIrregularHours(incomeNumber)(request)

      def handleIncomeIrregularHours(employmentId: Int, request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.handleIncomeIrregularHours(employmentId)(request)

      def confirmIncomeIrregularHours(employmentId: Int, request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.confirmIncomeIrregularHours(employmentId)(request)

      def submitIncomeIrregularHours(employmentId: Int, request: FakeRequest[AnyContentAsFormUrlEncoded]) = controller.submitIncomeIrregularHours(employmentId)(request)
    }

    def setup() = new UnifiedHarness(new TestIncomeUpdateCalculatorController)

    def setupOnPageLoad() = {

      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(defaultEmployment)))

      when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      setup()
    }

    def setupHowToUpdatePage() = {

      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome])))

      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(defaultEmployment)))

      when(incomeService.employmentAmount(any(), any())(any(), any()))
        .thenReturn(Future.successful(BuildEmploymentAmount()))

      setup()
    }

    def setupSubmitDuplicateSubmissionWarning() = {
      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
        .thenReturn(Future.successful(Seq(employer.name, employer.id.toString, "123456", TaiConstants.IncomeTypeEmployment)))

      setup()
    }

    def setupHandleIncomeIrregularHours() = {
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

      setup()
    }

    def setupConfirmIncomeIrregularHours(newAmount: Integer = 1235, confirmedNewAmount: Integer = 1234, payToDate: Integer = 123) = {

      when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
        Future.successful(
          (Seq(employer.name, newAmount.toString, payToDate.toString), Seq(Some(confirmedNewAmount.toString)))))

      setup()
    }

    def setupHandleChooseHowToUpdate() = {
      when(journeyCacheService.cache(Matchers.eq(UpdateIncome_HowToUpdateKey), any())(any()))
        .thenReturn(Future.successful(Map.empty[String,String]))

      setup()
    }

    def setupWorkingHoursPage() = {
      when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_WorkingHoursKey))(any()))
        .thenReturn(Future.successful(Option(REGULAR_HOURS)))

      setup()
    }

    def setupHandleWorkingHours() = {
      when(journeyCacheService.cache(eqTo(UpdateIncome_WorkingHoursKey), any())(any()))
        .thenReturn(Future.successful(Map.empty[String,String]))

      when(journeyCacheService.mandatoryJourneyValueAsInt(eqTo(UpdateIncome_IdKey))(any()))
        .thenReturn(Future.successful(Right(1)))

      setup()
    }

    def setupPayPeriod() = {
      when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayPeriodKey))(any()))
        .thenReturn(Future.successful(Some(MONTHLY)))
      when(journeyCacheService.currentValue(eqTo(UpdateIncome_OtherInDaysKey))(any()))
        .thenReturn(Future.successful(None))
      setup()
    }

    def setupHandlePayPeriod() = {
      when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      setup()
    }

    def setupPayslipAmountPage(payPeriod: Option[String], cachedAmount: Option[String]) = {
      when(journeyCacheService.collectedValues(any(), any())(any()))
        .thenReturn(Future.successful(
          Seq[String](employer.id.toString, employer.name), Seq[Option[String]](payPeriod, None, cachedAmount)))
      setup()
    }

    def setupHandlePayslipAmount() = {
      when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayPeriodKey))(any()))
        .thenReturn(Future.successful(Some(MONTHLY)))

      when(journeyCacheService.currentValue(eqTo(UpdateIncome_OtherInDaysKey))(any()))
        .thenReturn(Future.successful(None))

      setup()
    }

    def setupTaxablePayslipAmountPage(cachedAmount: Option[String], payPeriod: Option[String]) = {
      val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
      val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TaxablePayKey)

      when(journeyCacheService.collectedValues(Matchers.eq(mandatoryKeys), Matchers.eq(optionalKeys))(any()))
        .thenReturn(
          Future.successful(
            (Seq[String](employer.id.toString, employer.name), Seq[Option[String]](payPeriod, None, cachedAmount))
          )
        )

      setup()
    }

    def setupHandleTaxablePayslipAmount() = {
      when(journeyCacheService.currentValue(eqTo(UpdateIncome_TotalSalaryKey))(any()))
        .thenReturn(Future.successful(Some("4000")))

      when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayPeriodKey))(any()))
        .thenReturn(Future.successful(Some(MONTHLY)))

      when(journeyCacheService.currentValue(eqTo(UpdateIncome_OtherInDaysKey))(any()))
        .thenReturn(Future.successful(None))

      setup()
    }

    def setupHandlePayslipDeductions() = {
      when(journeyCacheService.cache(any())(any()))
        .thenReturn(Future.successful(Map.empty[String,String]))
      when(journeyCacheService.currentCache(any()))
        .thenReturn(Future.successful(Map.empty[String,String]))
      when(journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))
      setup()
    }

    def setupHandleBonusPayments() = {
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
      setup()
    }

    def setupBonusOvertimeAmountPage() = {
      when(journeyCacheService.currentValue(any())(any()))
        .thenReturn(Future.successful(Some("313321")))
      setup()
    }

    def setupHandleBonusOvertimeAmount() = {
      when(journeyCacheService.cache(any())(any()))
        .thenReturn(Future.successful(Map.empty[String,String]))

      when(journeyCacheService.mandatoryValues(any())(any()))
          .thenReturn(Future.successful(Seq(employer.id.toString, employer.name)))
      setup()
    }

    def setupCheckYourAnswers() = {
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

      setup()
    }

    def setupEstimatedPayPage() = {
      when(journeyCacheService.cache(any())(any()))
        .thenReturn(Future.successful(Map.empty[String, String]))
      when(incomeService.latestPayment(any(), any())(any()))
        .thenReturn(Future.successful(Some(Payment(new LocalDate(), 200, 50, 25, 100, 50, 25, Monthly))))
      when(journeyCacheService.currentCache(any()))
        .thenReturn(Future.successful(Map.empty[String,String]))
      when(incomeService.employmentAmount(any(), any())(any(), any()))
        .thenReturn(Future.successful(EmploymentAmount("", "", 1,1,1)))
      when(incomeService.calculateEstimatedPay(any(), any())(any()))
        .thenReturn(Future.successful(
          CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))
      setup()
    }

    def setupHandleCalculationResult() = {
      when(incomeService.employmentAmount(any(), any())(any(), any()))
          .thenReturn(Future.successful(EmploymentAmount("", "", 1, 1, 1)))
      setup()
    }

    def setupEditIncomeIrregularHours() = {
      when(incomeService.latestPayment(any(), any())(any()))
        .thenReturn(Future.successful(Some(Payment(LocalDate.now().minusDays(1), 0, 0, 0, 0, 0, 0, Monthly))))
      when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
      setup()
    }
  }

  "onPageLoad" must {
    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(eqTo("1"))(any()))
          .thenReturn(Future.successful(true))

        val result = UnifiedHarness.setupOnPageLoad()
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .duplicateSubmissionWarningPage()
          .url
      }
    }

    "redirect to the estimatedPayLanding url" when {
      "an income update has already been performed" in {
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(eqTo("1"))(any()))
          .thenReturn(Future.successful(false))

        val result = UnifiedHarness.setupOnPageLoad()
          .onPageLoad()

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .estimatedPayLandingPage()
          .url
      }
    }

    "generate an internal server error " when {
      "no employments are found" in {
        val harness = UnifiedHarness.setupOnPageLoad()

        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(eqTo("1"))(any()))
          .thenReturn(Future.successful(false))

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = harness.onPageLoad()

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "estimatedPayLandingPage" must {
    "display the estimatedPayLandingPage view" in {

      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
        .thenReturn(Future.successful(Seq(employer.name, employer.id.toString, TaiConstants.IncomeTypeEmployment)))

      val result = UnifiedHarness.setup()
        .estimatedPayLandingPage()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
    }
  }

  "duplicateSubmissionWarning" must {
    "show employment duplicateSubmissionWarning view" in {

      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
        .thenReturn(Future.successful(Seq(employer.name, employer.id.toString, "123456", TaiConstants.IncomeTypeEmployment)))

      val result = UnifiedHarness.setup()
        .duplicateSubmissionWarningPage()

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", employer.name))
    }
  }

  "submitDuplicateSubmissionWarning" must {
    "redirect to the estimatedPayLandingPage url when yes is selected" in {
      val result = UnifiedHarness.setupSubmitDuplicateSubmissionWarning()
        .submitDuplicateSubmissionWarning(RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(YesNoChoice -> YesValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
        .estimatedPayLandingPage()
        .url
    }

    "redirect to the IncomeSourceSummaryPage url when no is selected" in {
      val result = UnifiedHarness.setupSubmitDuplicateSubmissionWarning()
        .submitDuplicateSubmissionWarning(RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(YesNoChoice -> NoValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
    }
  }

  "howToUpdatePage" must {

    "render the right response to the user" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypePension)

      when(journeyCacheService.cache(any())(any()))
        .thenReturn(Future.successful(cacheMap))

      val result = UnifiedHarness.setupHowToUpdatePage()
        .howToUpdatePage()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
    }

    "cache the employer details" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypeEmployment)

      when(journeyCacheService.cache(any())(any()))
        .thenReturn(Future.successful(cacheMap))

      val result = UnifiedHarness.setupHowToUpdatePage()
        .howToUpdatePage()

      status(result) mustBe SEE_OTHER

      verify(journeyCacheService, times(1)).cache(Matchers.eq(cacheMap))(any())
    }

    "employments return empty income is none" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypePension)

      when(journeyCacheService.cache(any())(any()))
        .thenReturn(Future.successful(cacheMap))

      val harness = UnifiedHarness.setupHowToUpdatePage()

      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = harness.howToUpdatePage()

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "processHowToUpdatePage" must {

    "redirect user for non live employment " when {
      "employment amount is occupation income" in {

        val employmentAmount = UnifiedHarness.BuildEmploymentAmount()

        val result = UnifiedHarness.setup()
          .processHowToUpdatePage(employmentAmount)

        whenReady(result) { r =>
          r.header.status mustBe SEE_OTHER
          r.header.headers.get(LOCATION) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
        }
      }

      "employment amount is not occupation income" in {

        val result = UnifiedHarness.setup()
          .processHowToUpdatePage(UnifiedHarness.BuildEmploymentAmount(false, true))

        whenReady(result) { r =>
          r.header.status mustBe SEE_OTHER
          r.header.headers.get(LOCATION) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
        }
      }
    }

    "redirect user for is live employment " when {
      "editable incomes are greater than one and UpdateIncome_HowToUpdateKey has a cached value" in {

        when(incomeService.editableIncomes(any()))
          .thenReturn(UnifiedHarness.BuildTaxCodeIncomes(2))

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_HowToUpdateKey))(any()))
          .thenReturn(Future.successful(Some("incomeCalculator")))

        val result = UnifiedHarness.setup()
          .processHowToUpdatePage(UnifiedHarness.BuildEmploymentAmount())

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable incomes are greater than one and no cached UpdateIncome_HowToUpdateKey" in {

        when(incomeService.editableIncomes(any()))
          .thenReturn(UnifiedHarness.BuildTaxCodeIncomes(2))

        val result = UnifiedHarness
          .setup()
          .processHowToUpdatePage(UnifiedHarness.BuildEmploymentAmount())

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is singular and UpdateIncome_HowToUpdateKey has a cached value" in {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_HowToUpdateKey))(any()))
          .thenReturn(Future.successful(Some("incomeCalculator")))

        when(incomeService.editableIncomes(any()))
          .thenReturn(UnifiedHarness.BuildTaxCodeIncomes(1))

        val result = UnifiedHarness.setup()
          .processHowToUpdatePage(UnifiedHarness.BuildEmploymentAmount())

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is singular and no cached UpdateIncome_HowToUpdateKey" in {

        //In multiple of these tests we store a value when the description says we shouldn't. Do we need to flip this?
        when(incomeService.editableIncomes(any()))
          .thenReturn(UnifiedHarness.BuildTaxCodeIncomes(1))

  //      when(journeyCacheService.currentValue(eqTo(UpdateIncome_HowToUpdateKey))(any()))
//          .thenReturn(Future.successful(Some("incomeCalculator")))


        val result = UnifiedHarness
          .setup()
          .processHowToUpdatePage(UnifiedHarness.BuildEmploymentAmount())

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is none and UpdateIncome_HowToUpdateKey has a cached value" in {

        when(incomeService.editableIncomes(any()))
          .thenReturn(UnifiedHarness.BuildTaxCodeIncomes(0))

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_HowToUpdateKey))(any()))
          .thenReturn(Future.successful(Some("incomeCalculator")))

        val result = UnifiedHarness
          .setup()
          .processHowToUpdatePage(UnifiedHarness.BuildEmploymentAmount())

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }

      "editable income is none and no cached UpdateIncome_HowToUpdateKey" in {

        when(incomeService.editableIncomes(any()))
          .thenReturn(UnifiedHarness.BuildTaxCodeIncomes(0))

        val result = UnifiedHarness
          .setup()
          .processHowToUpdatePage(UnifiedHarness.BuildEmploymentAmount())

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }
    }
  }

  "handleChooseHowToUpdate" must {

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val result = UnifiedHarness
          .setupHandleChooseHowToUpdate()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("howToUpdate" -> "incomeCalculator"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.workingHoursPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val result = UnifiedHarness
          .setupHandleChooseHowToUpdate()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("howToUpdate" -> "income"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val result = UnifiedHarness
          .setupHandleChooseHowToUpdate()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("howToUpdate" -> ""))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", ""))
      }
    }
  }

  "workingHoursPage" must {
    "display workingHours page" when {
      "journey cache returns employment name and id" in {
        val result = UnifiedHarness
          .setupWorkingHoursPage()
          .workingHoursPage()

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }
  }

  "handleWorkingHours" must {

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val result = UnifiedHarness.setupHandleWorkingHours()
          .handleWorkingHours(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("workingHours" -> REGULAR_HOURS))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payPeriodPage().url)
      }
    }

    "redirect user back to workingHours page" when {
      "user input has error" in {

        val result = UnifiedHarness.setupHandleWorkingHours()
          .handleWorkingHours(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("workingHours" -> ""))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }

    "redirect user back to workingHours page" when {
      "bad data submitted in form" in {

        val result = UnifiedHarness.setupHandleWorkingHours()
          .handleWorkingHours(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("workingHours" -> "anything"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }
  }

  "payPeriodPage" must {
    "display payPeriod page" when {
      "journey cache returns employment name and id" in {

        val result = UnifiedHarness.setupPayPeriod()
          .payPeriodPage()

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }
  }

  "handlePayPeriod" must {
    "redirect the user to payslipAmountPage page" when {
      "user selected monthly" in {

        val result = UnifiedHarness.setupHandlePayPeriod()
          .handlePayPeriod(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payPeriod" -> "monthly"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipAmountPage().url)
      }
    }

    "redirect user back to how to payPeriod page" when {
      "user input has error" in {

        val result = UnifiedHarness.setupHandlePayPeriod()
          .handlePayPeriod(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payPeriod" -> "nonsense"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }
  }

  "payslipAmountPage" must {
    "display payslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {

        val cachedAmount = None
        val payPeriod = Some(MONTHLY)

        val result = UnifiedHarness
          .setupPayslipAmountPage(payPeriod, cachedAmount)
          .payslipAmountPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslip.title.month"))
      }

      "journey cache returns a prepopulated pay slip amount" in {
        val cachedAmount = Some("998787")
        val payPeriod = Some(MONTHLY)
        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = UnifiedHarness
          .setupPayslipAmountPage(payPeriod, cachedAmount)
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
    "redirect the user to payslipDeductionsPage page" when {
      "user entered valid pay" in {

        val salary = "£3,000"

        when(journeyCacheService.cache(Matchers.eq[Map[String,String]](Map(UpdateIncome_TotalSalaryKey -> salary)))(any()))
          .thenReturn(Future.successful(Map.empty[String,String]))

        val result = UnifiedHarness.setupHandlePayslipAmount()
          .handlePayslipAmount(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("totalSalary" -> salary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url)
      }
    }

    "redirect user back to how to payslip page with an error form" when {
      "user input has error" in {

        val result = UnifiedHarness.setupHandlePayslipAmount()
          .handlePayslipAmount(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().text must include(messages("tai.income.error.form.summary"))
        doc.body().text must include(messages("tai.payslip.title.month"))
        doc.title() must include(messages("tai.payslip.title.month"))
      }
    }
  }

  "taxablePayslipAmountPage" must {
    "display taxablePayslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val cachedAmount = Some("9888787")
        val payPeriod = Some(MONTHLY)

        val result = UnifiedHarness.setupTaxablePayslipAmountPage(cachedAmount, payPeriod)
          .taxablePayslipAmountPage(request)

        status(result) mustBe OK

        val expectedForm = TaxablePayslipForm.createForm().fill(TaxablePayslipForm(cachedAmount))
        val expectedViewModel = TaxablePaySlipAmountViewModel(expectedForm, payPeriod, None, employer)
        result rendersTheSameViewAs taxablePayslipAmount(expectedViewModel)
      }
    }
  }

  "handleTaxablePayslipAmount" must {
    "redirect the user to bonusPaymentsPage page" when {
      "user entered valid taxable pay" in {

        when(journeyCacheService.cache(eqTo[Map[String,String]](Map(UpdateIncome_TaxablePayKey -> "3000")))(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        val result = UnifiedHarness.setupHandleTaxablePayslipAmount()
          .handleTaxablePayslipAmount(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("taxablePay" -> "3000"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to taxablePayslip page" when {
      "user input has error" in {

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
              Seq[String](employer.id.toString, employer.name),
              Seq[Option[String]](Some(MONTHLY), None))
            ))

        val result = UnifiedHarness.setupHandleTaxablePayslipAmount()
          .handleTaxablePayslipAmount(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.taxablePayslip.title.month"))
      }
    }
  }

  "payslipDeductionsPage" must {
    "display payslipDeductions" when {
      "journey cache returns employment name and id" in {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_PayslipDeductionsKey))(any()))
          .thenReturn(Future.successful(Some("Yes")))

        val result = UnifiedHarness.setup()
          .payslipDeductionsPage()

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }

  "handlePayslipDeductions" must {
    "redirect the user to taxablePayslipAmountPage page" when {
      "user selected yes" in {

        val result = UnifiedHarness.setupHandlePayslipDeductions()
          .handlePayslipDeductions(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payslipDeductions" -> "Yes"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url)
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val result = UnifiedHarness.setupHandlePayslipDeductions()
          .handlePayslipDeductions(RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payslipDeductions" -> "No"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to payslipDeductions page" when {
      "user input has error" in {
        val result = UnifiedHarness.setupHandlePayslipDeductions()
          .handlePayslipDeductions(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }

  "bonusPaymentsPage" must {
    "display bonusPayments" in {

      implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
      val cachedAmount = "1231231"

      when(journeyCacheService.currentValue(eqTo(UpdateIncome_BonusPaymentsKey))(any()))
        .thenReturn(Future.successful(Some(cachedAmount)))

      val result = UnifiedHarness
        .setup()
        .bonusPaymentsPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusPaymentsForm.createForm.fill(YesNoForm(Some(cachedAmount)))
      val expectedView = bonusPayments(expectedForm, employer)

      result rendersTheSameViewAs expectedView
    }
  }

  "handleBonusPayments" must {
    "redirect the user to bonusOvertimeAmountPage page" when {
      "user selected yes" in {

        val result = UnifiedHarness
          .setupHandleBonusPayments()
          .handleBonusPayments(
            RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(YesNoChoice -> YesValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url)
      }
    }

    "redirect the user to checkYourAnswers page" when {
      "user selected no" in {
        val result = UnifiedHarness.setupHandleBonusPayments()
          .handleBonusPayments(
            RequestBuilder.buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(YesNoChoice -> NoValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
      }
    }

    "redirect user back to how to bonusPayments page" when {
      "user input has error" in {

        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> "")

        val result = UnifiedHarness.setupHandleBonusPayments()
          .handleBonusPayments(fakeRequest)

        status(result) mustBe BAD_REQUEST
        result rendersTheSameViewAs bonusPayments(BonusPaymentsForm.createForm.bindFromRequest()(fakeRequest), employer)
      }
    }
  }

  "bonusOvertimeAmountPage" must {
    "display bonusPaymentAmount" in {
      val cachedAmount = "313321"
      implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

      val result = UnifiedHarness.setupBonusOvertimeAmountPage()
        .bonusOvertimeAmountPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusOvertimeAmountForm.createForm().fill(BonusOvertimeAmountForm(Some(cachedAmount)))
      result rendersTheSameViewAs bonusPaymentAmount(expectedForm, employer)
    }
  }

  "handleBonusOvertimeAmount" must {
    "redirect the user to checkYourAnswers page" in {

      val result = UnifiedHarness.setupHandleBonusOvertimeAmount()
        .handleBonusOvertimeAmount(
          RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("amount" -> "£3,000"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
    }

    "redirect the user to bonusPaymentAmount page" when {
      "user input has error" in {

        implicit val fakeRequest =
          RequestBuilder.buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("amount" -> "")

        val result = UnifiedHarness.setupHandleBonusOvertimeAmount()
          .handleBonusOvertimeAmount(fakeRequest)

        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs bonusPaymentAmount(
          BonusOvertimeAmountForm.createForm().bindFromRequest()(fakeRequest),
          employer)
      }
    }
  }

  "checkYourAnswers page" must {
    "display check your answers containing populated values from the journey cache" in {

      val result = UnifiedHarness.setupCheckYourAnswers()
        .checkYourAnswersPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.checkYourAnswers.title"))
    }
  }

  "estimatedPayPage" must {
    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay" in {

        when(incomeService.latestPayment(any(), any())(any()))
          .thenReturn(Future.successful(Some(Payment(new LocalDate(), 50, 1, 1, 1, 1, 1, Monthly))))

        val result = UnifiedHarness.setupEstimatedPayPage()
          .estimatedPayPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "payYearToDate is None" in {

        val harness = UnifiedHarness.setupEstimatedPayPage()

        when(incomeService.latestPayment(any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = harness.estimatedPayPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }

    "display incorrectTaxableIncome page" when {
      "payYearToDate is greater than gross annual pay" in {

        val result = UnifiedHarness.setupEstimatedPayPage()
          .estimatedPayPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))
      }
    }

    "redirect to sameEstimatedPay page" when {
      "the pay is the same" in {

        val harness = UnifiedHarness.setupEstimatedPayPage()

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(UpdateIncome_ConfirmedNewAmountKey -> "100")))

        val result = harness.estimatedPayPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }
    }
  }

  "handleCalculationResult" must {
    "display confirm_save_Income page" when {
      "journey cache returns employment name, net amount and id" in {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_NewAmountKey))(any()))
          .thenReturn(Future.successful(Some("100")))

        val result = UnifiedHarness.setupHandleCalculationResult()
          .handleCalculationResult(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "journey cache returns employment name, net amount with large decimal value and id" in {

        when(journeyCacheService.currentValue(eqTo(UpdateIncome_NewAmountKey))(any()))
          .thenReturn(Future.successful(Some("4632.460273972602739726027397260273")))

        val result = UnifiedHarness.setupHandleCalculationResult()
          .handleCalculationResult(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "redirects to the same amount entered page" ignore {
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any()))
          .thenReturn(Future.successful(Some("1")))

        val result = UnifiedHarness.setupHandleCalculationResult()
          .handleCalculationResult(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }
  }

  "editIncomeIrregularHours" must {
    "respond with OK and show the irregular hours edit page" in {
      when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
        .thenReturn(Future.successful(
          Some(TaxCodeIncome(EmploymentIncome, Some(1), 123, "description", "taxCode", "name", OtherBasisOfOperation, Live))))

      val result = UnifiedHarness.setupEditIncomeIrregularHours()
        .editIncomeIrregularHours(1, RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.irregular.heading"))
    }

    "respond with INTERNAL_SERVER_ERROR" when {
      "the employment income cannot be found" in {
        when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = UnifiedHarness.setupEditIncomeIrregularHours()
          .editIncomeIrregularHours(2, RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "handleIncomeIrregularHours" must {
    "respond with Redirect to Confirm page" in {
      val result = UnifiedHarness
        .setupHandleIncomeIrregularHours()
        .handleIncomeIrregularHours(1,
          RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody("income" -> "999"))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(
        routes.IncomeUpdateCalculatorController.confirmIncomeIrregularHours(1).url.toString)
    }

    "respond with BAD_REQUEST" when {
      "given an input which is less than the current amount" in {
        val result = UnifiedHarness
          .setupHandleIncomeIrregularHours()
          .handleIncomeIrregularHours(1,
            RequestBuilder.buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody("income" -> "122"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(
          messages("tai.irregular.error.error.incorrectTaxableIncome", 123, LocalDate.now().toString(TaiConstants.MONTH_AND_YEAR), "name"))
      }

      "given invalid form data of invalid currency" in {
        val result = UnifiedHarness.setupHandleIncomeIrregularHours()
          .handleIncomeIrregularHours(1,
            RequestBuilder.buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody("income" -> "ABC"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(messages("tai.irregular.instruction.wholePounds"))
      }

      "given invalid form data of no input" in {
        val result = UnifiedHarness
            .setupHandleIncomeIrregularHours()
              .handleIncomeIrregularHours(1,
                RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("error.tai.updateDataEmployment.blankValue"))

      }

      "given invalid form data of more than 9 numbers" in {
        val result = UnifiedHarness
          .setupHandleIncomeIrregularHours()
          .handleIncomeIrregularHours(1,
            RequestBuilder.buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody("income" -> "1234567890"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("error.tai.updateDataEmployment.maxLength"))

      }
    }
  }

  "confirmIncomeIrregularHours" must {
    "respond with Ok" in {

      val result = UnifiedHarness
        .setupConfirmIncomeIrregularHours()
        .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(
        messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
    }

    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {

      val harness = UnifiedHarness.setup()

      when(journeyCacheService.collectedValues(any(), any())(any()))
        .thenReturn(Future.failed(new Exception))

      val result = harness.confirmIncomeIrregularHours(1, RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "redirect to SameEstimatedPayPage" when {
      "the same amount of pay has been entered" in {

        val result = UnifiedHarness.setupConfirmIncomeIrregularHours(confirmedNewAmount = 123, newAmount = 123)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }
    }

    "redirect to IrregularSameEstimatedPayPage" when {
      "the same amount of payment to date has been entered" in {
        val result = UnifiedHarness.setupConfirmIncomeIrregularHours(newAmount = 123)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }
  }

  "submitIncomeIrregularHours" must {
    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {

      val harness = UnifiedHarness.setup()

      when(
        journeyCacheService.mandatoryValues(any())(any())
      ).thenReturn(
        Future.failed(new Exception)
      )

      val result: Future[Result] = harness.submitIncomeIrregularHours(1,
        RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "sends Ok on successful submit" in {

      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
        .thenReturn(Future.successful(Seq(employer.name, "123", employer.id.toString)))

      val harness = UnifiedHarness.setup()

      when(
        taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())
      ).thenReturn(
        Future.successful(TaiSuccessResponse)
      )

      when(estimatedPayJourneyCompletionService.journeyCompleted(Matchers.eq(employer.id.toString))(any()))
        .thenReturn(Future.successful(Map.empty[String, String]))

      val result: Future[Result] = harness.submitIncomeIrregularHours(1,
        RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      doc.title() must include(messages("tai.incomes.updated.check.title", employer.name))
    }
  }
}