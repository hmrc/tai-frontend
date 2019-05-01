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

import builders.{AuthBuilder, RequestBuilder, UserBuilder}
import controllers.{ControllerViewTestHelper, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
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

import scala.concurrent.Future
import scala.util.Random

class IncomeUpdateCalculatorControllerSpec
  extends PlaySpec
    with FakeTaiPlayApplication
    with MockitoSugar
    with JsoupMatchers
    with JourneyCacheConstants
    with EditIncomeIrregularPayConstants
    with FormValuesConstants
    with ControllerViewTestHelper
    with EditIncomePayPeriodConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val employer = IncomeSource(id = 1, name = "sample employer")

  def fakeNino = new Generator(new Random).nextNino

  def createTestIncomeUpdateCalculatorController = new TestIncomeUpdateCalculatorController()

  val personService: PersonService = mock[PersonService]
  val incomeService: IncomeService = mock[IncomeService]
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]
  val journeyCacheService = mock[JourneyCacheService]
  val estimatedPayJourneyCompletionService = mock[EstimatedPayJourneyCompletionService]

  class TestIncomeUpdateCalculatorController extends IncomeUpdateCalculatorController(
    incomeService,
    employmentService,
    taxAccountService,
    personService,
    estimatedPayJourneyCompletionService,
    mock[AuditConnector],
    mock[DelegationConnector],
    mock[AuthConnector],
    journeyCacheService,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    val ad: Future[Some[Authority]] = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(fakeNino)))
    when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(employer.id))
    when(journeyCacheService.mandatoryValue(Matchers.eq(UpdateIncome_NameKey))(any())).thenReturn(Future.successful(employer.name))
  }

  val employment = Employment(employer.name, Some("123"), new LocalDate("2016-05-26"), None, Nil, "", "", 1, None, false, false)
  val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(employer.id), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)

  "onPageLoad" must {
    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {

        val testController = createTestIncomeUpdateCalculatorController

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(Matchers.eq(employer.id.toString))(any())).thenReturn(Future.successful(true))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.onPageLoad(employer.id)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.duplicateSubmissionWarningPage().url
      }
    }

    "redirect to the estimatedPayLanding url" when {
      "an income update has already been performed" in {

        val testController = createTestIncomeUpdateCalculatorController

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(Matchers.eq(employer.id.toString))(any())).thenReturn(Future.successful(false))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.onPageLoad(employer.id)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.estimatedPayLandingPage().url
      }
    }

    "generate an internal server error " when {

      "no employments are found" in {

        val testController = createTestIncomeUpdateCalculatorController

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(taxCodeIncome))))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(Matchers.eq(employer.id.toString))(any())).thenReturn(Future.successful(false))

        val result = testController.onPageLoad(employer.id)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }

  }

  "estimatedPayLandingPage" must {
    "display the estimatedPayLandingPage view" in {
      val testController = createTestIncomeUpdateCalculatorController

      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any())).thenReturn(
        Future.successful(Seq(employer.name, employer.id.toString, TaiConstants.IncomeTypeEmployment)))

      val result = testController.estimatedPayLandingPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
    }
  }

  "duplicateSubmissionWarning" must {
    "show employment duplicateSubmissionWarning view" in {
      val testController = createTestIncomeUpdateCalculatorController
      val newAmount = "123456"

      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any())).thenReturn(
        Future.successful(Seq(employer.name, employer.id.toString, newAmount, TaiConstants.IncomeTypeEmployment)))

      val result = testController.duplicateSubmissionWarningPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc must haveHeadingWithText(messages("tai.incomes.warning.employment.heading", employer.name))

    }
  }

  "submitDuplicateSubmissionWarning" must {
    "redirect to the estimatedPayLandingPage url when yes is selected" in {
      val testController = createTestIncomeUpdateCalculatorController
      val newAmount = "123456"

      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any())).thenReturn(
        Future.successful(Seq(employer.name, employer.id.toString, newAmount, TaiConstants.IncomeTypeEmployment)))

      val result = testController.submitDuplicateSubmissionWarning()(RequestBuilder
        .buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(YesNoChoice -> YesValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.estimatedPayLandingPage().url
    }

    "redirect to the IncomeSourceSummaryPage url when no is selected" in {
      val testController = createTestIncomeUpdateCalculatorController
      val newAmount = "123456"

      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any())).thenReturn(
        Future.successful(Seq(employer.name, employer.id.toString, newAmount, TaiConstants.IncomeTypeEmployment)))

      val result = testController.submitDuplicateSubmissionWarning()(RequestBuilder
        .buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(YesNoChoice -> NoValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
    }
  }

  "howToUpdatePage" must {
    "render the right response to the user" in {
      val testController = createTestIncomeUpdateCalculatorController
      val employment = Employment("company", Some("123"), new LocalDate("2016-05-26"), None, Nil, "", "", 1, None, false, false)
      val employmentAmount = EmploymentAmount(name = "name", description = "description", employmentId = employer.id,
        newAmount = 200, oldAmount = 200, isLive = false, isOccupationalPension = true)

      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
      when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
      when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome])))
      when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      val result = testController.howToUpdatePage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
    }

    "employments return empty income is none" in {
      val testController = createTestIncomeUpdateCalculatorController
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

      val result = testController.howToUpdatePage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "processHowToUpdatePage" must {
    val employmentAmount = (isLive: Boolean, isOccupationalPension: Boolean) => EmploymentAmount(name = "name", description = "description", employmentId = employer.id,
      newAmount = 200, oldAmount = 200, isLive = isLive, isOccupationalPension = isOccupationalPension)

    "redirect user for non live employment " when {
      "employment amount is occupation income" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(false, true),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe SEE_OTHER
        result.header.headers.get(LOCATION) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
      }

      "employment amount is not occupation income" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(false, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe SEE_OTHER
        result.header.headers.get(LOCATION) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }

    "redirect user for is live employment " when {
      "editable incomes are greater than one" in {
        val testController = createTestIncomeUpdateCalculatorController
        val taxCodeIncome1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
        val taxCodeIncome2 = TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
        when(incomeService.editableIncomes(any())).thenReturn(Seq(taxCodeIncome1, taxCodeIncome2))

        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(true, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe OK
        val doc = Jsoup.parse(contentAsString(Future.successful(result)))
        doc.title() must include(messages("tai.howToUpdate.title", "name"))
      }

      "editable income is singular" in {
        val testController = createTestIncomeUpdateCalculatorController
        val taxCodeIncome1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
        when(incomeService.editableIncomes(any())).thenReturn(Seq(taxCodeIncome1))
        when(incomeService.singularIncomeId(any())).thenReturn(Some(1))

        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(true, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe OK
        val doc = Jsoup.parse(contentAsString(Future.successful(result)))
        doc.title() must include(messages("tai.howToUpdate.title", "name"))
      }

      "editable income is none" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(incomeService.editableIncomes(any())).thenReturn(Nil)
        when(incomeService.singularIncomeId(any())).thenReturn(None)
        val ex = the[RuntimeException] thrownBy testController.processHowToUpdatePage(1, "name", employmentAmount(true, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        ex.getMessage mustBe "Employment id not present"
      }
    }

  }

  "handleChooseHowToUpdate" must {
    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "incomeCalculator"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.workingHoursPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "income"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", ""))
      }
    }
  }

  "workingHoursPage" must {
    "display workingHours page" when {
      "journey cache returns employment name and id" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.workingHoursPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }
  }

  "handleWorkingHours" must {
    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> REGULAR_HOURS))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payPeriodPage().url)
      }
    }

    "redirect user back to workingHours page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }

    "redirect user back to workingHours page" when {
      "bad data submitted in form" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "anything"))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.workingHours.heading"))
      }
    }
  }

  "payPeriodPage" must {
    "display payPeriod page" when {
      "journey cache returns employment name and id" in {
        val testController = createTestIncomeUpdateCalculatorController

        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(Some(MONTHLY)))
        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_OtherInDaysKey))(any())).thenReturn(Future.successful(None))

        val result = testController.payPeriodPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }
  }

  "handlePayPeriod" must {
    "redirect the user to payslipAmountPage page" when {
      "user selected monthly" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handlePayPeriod()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "monthly"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipAmountPage().url)
      }
    }

    "redirect user back to how to payPeriod page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handlePayPeriod()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "otherInDays"))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payPeriod.heading"))
      }
    }
  }

  "payslipAmountPage" must {
    "display payslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {
        val testController = createTestIncomeUpdateCalculatorController

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful(
            (
              Seq[String](employer.id.toString, employer.name),
              Seq[Option[String]](Some(MONTHLY), None, None)
            )
          )
        )

        val result = testController.payslipAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslip.title.month"))
      }

      "journey cache returns a prepopulated pay slip amount" in {
        val testController = createTestIncomeUpdateCalculatorController
        val cachedAmount = Some("998787")
        val payPeriod = Some(MONTHLY)

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful(
            (Seq[String](employer.id.toString, employer.name),
              Seq[Option[String]](payPeriod, None, cachedAmount))
          )
        )

        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
        val result = testController.payslipAmountPage()(fakeRequest)
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
        val testController = createTestIncomeUpdateCalculatorController

        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(Some(MONTHLY)))
        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_OtherInDaysKey))(any())).thenReturn(Future.successful(None))
        when(journeyCacheService.cache(Matchers.eq(Map(UpdateIncome_TotalSalaryKey -> "£3,000")))(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = testController.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("totalSalary" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url)
      }
    }

    "redirect user back to how to payslip page with an error form" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController

        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(Some(MONTHLY)))
        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_OtherInDaysKey))(any())).thenReturn(Future.successful(None))

        val result = testController.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
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
        val testController = createTestIncomeUpdateCalculatorController
        val cachedAmount = Some("9888787")
        val payPeriod = Some(MONTHLY)

        val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
        val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TaxablePayKey)

        when(journeyCacheService.collectedValues(Matchers.eq(mandatoryKeys), Matchers.eq(optionalKeys))(any())).thenReturn(
          Future.successful(
            (Seq[String](employer.id.toString, employer.name),
              Seq[Option[String]](payPeriod, None, cachedAmount))
          )
        )

        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = testController.taxablePayslipAmountPage()(fakeRequest)
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
        val testController = createTestIncomeUpdateCalculatorController
        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_TotalSalaryKey))(any())).thenReturn(Future.successful(Some("4000")))
        when(journeyCacheService.cache(Matchers.eq(Map(UpdateIncome_TaxablePayKey -> "3000")))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxablePay" -> "3000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to taxablePayslip page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController

        val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
        val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey)

        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_TotalSalaryKey))(any())).thenReturn(Future.successful(Some("4000")))
        when(journeyCacheService.collectedValues(Matchers.eq(mandatoryKeys), Matchers.eq(optionalKeys))(any())).thenReturn(
          Future.successful(
            (
              Seq[String](employer.id.toString, employer.name),
              Seq[Option[String]](Some(MONTHLY), None)
            )
          )
        )

        val result = testController.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.taxablePayslip.title.month"))
      }
    }
  }

  "payslipDeductionsPage" must {
    "display payslipDeductions" when {
      "journey cache returns employment name and id" in {
        val testController = createTestIncomeUpdateCalculatorController

        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayslipDeductionsKey))(any())).thenReturn(Future.successful(Some("Yes")))

        val result = testController.payslipDeductionsPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }

  "handlePayslipDeductions" must {
    "redirect the user to taxablePayslipAmountPage page" when {
      "user selected yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(journeyCacheService.cache(Matchers.eq(Map(UpdateIncome_PayslipDeductionsKey -> "Yes")))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "Yes"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url)
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map("" -> "")))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        when(journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "No"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to payslipDeductions page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }

  "bonusPaymentsPage" must {
    "display bonusPayments" in {
      val testController = createTestIncomeUpdateCalculatorController
      val cachedAmount = "1231231"
      implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

      when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_BonusPaymentsKey))(any())).thenReturn(Future.successful(Some(cachedAmount)))

      val result: Future[Result] = testController.bonusPaymentsPage()(fakeRequest)
      status(result) mustBe OK

      val expectedForm = BonusPaymentsForm.createForm.fill(YesNoForm(Some(cachedAmount)))
      val expectedView = bonusPayments(expectedForm, employer)

      result rendersTheSameViewAs expectedView
    }
  }

  "handleBonusPayments" must {
    "redirect the user to bonusOvertimeAmountPage page" when {
      "user selected yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        val cacheMap = Map(UpdateIncome_BonusPaymentsKey -> YesValue)
        when(journeyCacheService.cache(Matchers.eq(cacheMap))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(YesNoChoice -> YesValue))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url)
      }
    }

    "redirect the user to checkYourAnswers page" when {
      "user selected no" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map("" -> "")))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        when(journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))
        val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(YesNoChoice -> NoValue))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
      }
    }

    "redirect user back to how to bonusPayments page" when {
      "user input has error" in {
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> "")

        val testController = createTestIncomeUpdateCalculatorController
        when(journeyCacheService.mandatoryValues(any())(any())).thenReturn(
          Future.successful(Seq(employer.id.toString, employer.name)))

        val result = testController.handleBonusPayments()(fakeRequest)
        status(result) mustBe BAD_REQUEST
        result rendersTheSameViewAs bonusPayments(BonusPaymentsForm.createForm.bindFromRequest()(fakeRequest), employer)

      }
    }
  }

  "bonusOvertimeAmountPage" must {
    "display bonusPaymentAmount" in {
      val testController = createTestIncomeUpdateCalculatorController
      val cachedAmount = "313321"

      when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_BonusOvertimeAmountKey))(any()))
        .thenReturn(Future.successful(Some(cachedAmount)))

      implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

      val result: Future[Result] = testController.bonusOvertimeAmountPage()(fakeRequest)
      status(result) mustBe OK

      val expectedForm = BonusOvertimeAmountForm.createForm().fill(BonusOvertimeAmountForm(Some(cachedAmount)))
      result rendersTheSameViewAs bonusPaymentAmount(expectedForm, employer)
    }
  }

  "handleBonusOvertimeAmount" must {
    "redirect the user to checkYourAnswers page" in {
      val testController = createTestIncomeUpdateCalculatorController
      when(journeyCacheService.cache(Matchers.eq(Map(UpdateIncome_BonusOvertimeAmountKey -> "£3,000")))(any())).thenReturn(Future.successful(Map("" -> "")))
      val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("amount" -> "£3,000"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
    }

    "redirect the user to bonusPaymentAmount page" when {
      "user input has error" in {
        val employerName = "employer1"
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("amount" -> "")

        val testController = createTestIncomeUpdateCalculatorController
        when(journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq(employer.id.toString, employer.name)))
        val result = testController.handleBonusOvertimeAmount()(fakeRequest)
        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs bonusPaymentAmount(BonusOvertimeAmountForm.createForm().bindFromRequest()(fakeRequest), employer)
      }
    }
  }

  "checkYourAnswers page" must {
    "display check your answers containing populated values from the journey cache" in {
      val testController = createTestIncomeUpdateCalculatorController
      val employerName = "Employer1"
      val payFrequency = "monthly"
      val totalSalary = "10000"
      val payslipDeductions = "yes"
      val bonusPayments = "yes"
      val taxablePay = "8000"
      val bonusAmount = "1000"
      val payPeriodInDays = "3"
      val employerId = "1"

      when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
        Future.successful((
          Seq[String](employerName, payFrequency, totalSalary, payslipDeductions, bonusPayments, employerId),
          Seq[Option[String]](Some(taxablePay), Some(bonusAmount), Some(payPeriodInDays))
        ))
      )
      val result = testController.checkYourAnswersPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.checkYourAnswers.title"))
    }
  }

  "estimatedPayPage" must {
    def createTestController(currentCache: Map[String, String]): TestIncomeUpdateCalculatorController = {
      val employmentAmount = EmploymentAmount("", "", 1, 1, 1)
      val payment = Payment(new LocalDate(), 200, 50, 25, 100, 50, 25, Monthly)

      when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
      when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(currentCache))
      when(incomeService.calculateEstimatedPay(any(), any())(any())).thenReturn(
        Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))
      when(incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
      when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      createTestIncomeUpdateCalculatorController
    }

    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay" in {
        val testController = createTestController(Map.empty)

        val payment = Payment(new LocalDate(), 50, 1, 1, 1, 1, 1, Monthly)
        when(incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))

        val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "payYearToDate is None" in {
        val testController = createTestController(Map.empty)
        when(incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }

    "display incorrectTaxableIncome page" when {
      "payYearToDate is greater than gross annual pay" in {
        val testController = createTestController(Map.empty)

        val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))
      }
    }

    "redirect to sameEstimatedPay page" when {
      "the pay is the same" in {
        val currentCache = Map(UpdateIncome_ConfirmedNewAmountKey -> "100")
        val testController = createTestController(currentCache)

        val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }
    }
  }

  "handleCalculationResult" must {
    "display confirm_save_Income page" when {
      "journey cache returns employment name, net amount and id" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any())).thenReturn(Future.successful(Some("100")))

        val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "journey cache returns employment name, net amount with large decimal value and id" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any())).thenReturn(Future.successful(Some("4632.460273972602739726027397260273")))

        val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }

      "redirects to the same amount entered page" ignore {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any())).thenReturn(Future.successful(Some("1")))

        val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }
  }

  "editIncomeIrregularHours" must {
    "respond with OK and show the irregular hours edit page" in {
      val testController = createTestIncomeUpdateCalculatorController

      val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 123, "description", "taxCode", "name", OtherBasisOfOperation, Live)
      when(
        taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any())
      ).thenReturn(
        Future.successful(Some(taxCodeIncome))
      )

      val payment = Payment(LocalDate.now().minusDays(1), 0, 0, 0, 0, 0, 0, Monthly)
      when(
        incomeService.latestPayment(any(), any())(any())
      ).thenReturn(
        Future.successful(Some(payment))
      )

      when(
        journeyCacheService.cache(any())(any())
      ).thenReturn(
        Future.successful(Map.empty[String, String])
      )

      val result = testController.editIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithAuth("GET")
      )

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.irregular.heading"))
    }

    "respond with INTERNAL_SERVER_ERROR" when {
      "the employment income cannot be found" in {
        val testController = createTestIncomeUpdateCalculatorController
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 123, "description", "taxCode", "name", OtherBasisOfOperation, Live)

        when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = testController.editIncomeIrregularHours(2)(
          RequestBuilder.buildFakeRequestWithAuth("GET")
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "handleIncomeIrregularHours" must {
    "respond with Redirect to Confirm page" in {
      val testController = createTestIncomeUpdateCalculatorController
      val employmentId = 1
      val employerName = "name"
      val payToDate = 123

      val cacheMap = Map(
        UpdateIncome_NameKey -> "name",
        UpdateIncome_PayToDateKey -> "123",
        UpdateIncome_DateKey -> LocalDate.now().toString()
      )

      when(
        journeyCacheService.currentCache(any())
      ).thenReturn(
        Future.successful(cacheMap)
      )

      when(
        journeyCacheService.cache(any(), any())(any())
      ).thenReturn(
        Future.successful(Map.empty[String, String])
      )

      val result = testController.handleIncomeIrregularHours(1)(
        RequestBuilder
          .buildFakeRequestWithOnlySession("POST")
          .withFormUrlEncodedBody("income" -> "999")
      )

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.confirmIncomeIrregularHours(employmentId).url.toString)

    }

    "respond with BAD_REQUEST" when {
      "given an input which is less than the current amount" in {

        val testController = createTestIncomeUpdateCalculatorController
        val employerName = "name"
        val payToDate = 123
        val payDate = LocalDate.now().toString(TaiConstants.MONTH_AND_YEAR)

        when(
          journeyCacheService.mandatoryValues(any())(any())
        ).thenReturn(
          Future.successful(Seq(employerName, payToDate.toString))
        )

        val cacheMap = Map(
          UpdateIncome_NameKey -> employerName,
          UpdateIncome_PayToDateKey -> payToDate.toString,
          UpdateIncome_DateKey -> payDate
        )

        when(
          journeyCacheService.currentCache(any())
        ).thenReturn(
          Future.successful(cacheMap)
        )

        val result = testController.handleIncomeIrregularHours(1)(
          RequestBuilder
            .buildFakeRequestWithOnlySession("POST")
            .withFormUrlEncodedBody("income" -> (payToDate - 1).toString)
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(messages("tai.irregular.error.error.incorrectTaxableIncome", payToDate, payDate, employerName))
      }

      "given invalid form data of invalid currency" in {

        val testController = createTestIncomeUpdateCalculatorController
        val employerName = "name"
        val payToDate = 123
        val payDate = LocalDate.now().toString(TaiConstants.MONTH_AND_YEAR)
        val cacheMap = Map(
          UpdateIncome_NameKey -> employerName,
          UpdateIncome_PayToDateKey -> payToDate.toString,
          UpdateIncome_DateKey -> payDate
        )

        when(
          journeyCacheService.currentCache(any())
        ).thenReturn(
          Future.successful(cacheMap)
        )

        when(
          journeyCacheService.mandatoryValues(any())(any())
        ).thenReturn(
          Future.successful(Seq(employerName, payToDate.toString))
        )

        val result = testController.handleIncomeIrregularHours(1)(
          RequestBuilder
            .buildFakeRequestWithOnlySession("POST")
            .withFormUrlEncodedBody("income" -> "ABC")
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(messages("tai.irregular.instruction.wholePounds"))

      }

      "given invalid form data of no input" in {

        val testController = createTestIncomeUpdateCalculatorController
        val employerName = "name"
        val payToDate = 123
        val payDate = LocalDate.now().toString(TaiConstants.MONTH_AND_YEAR)
        val cacheMap = Map(
          UpdateIncome_NameKey -> employerName,
          UpdateIncome_PayToDateKey -> payToDate.toString,
          UpdateIncome_DateKey -> payDate
        )

        when(
          journeyCacheService.currentCache(any())
        ).thenReturn(
          Future.successful(cacheMap)
        )

        when(
          journeyCacheService.mandatoryValues(any())(any())
        ).thenReturn(
          Future.successful(Seq(employerName, payToDate.toString))
        )

        val result = testController.handleIncomeIrregularHours(1) {
          RequestBuilder.buildFakeRequestWithOnlySession("POST")
        }

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("error.tai.updateDataEmployment.blankValue"))

      }

      "given invalid form data of more than 9 numbers" in {

        val testController = createTestIncomeUpdateCalculatorController
        val employerName = "name"
        val payToDate = 123
        val payDate = LocalDate.now().toString(TaiConstants.MONTH_AND_YEAR)
        val cacheMap = Map(
          UpdateIncome_NameKey -> employerName,
          UpdateIncome_PayToDateKey -> payToDate.toString,
          UpdateIncome_DateKey -> payDate
        )

        when(
          journeyCacheService.currentCache(any())
        ).thenReturn(
          Future.successful(cacheMap)
        )

        when(
          journeyCacheService.mandatoryValues(any())(any())
        ).thenReturn(
          Future.successful(Seq(employerName, payToDate.toString))
        )

        val result = testController.handleIncomeIrregularHours(1) {
          RequestBuilder
            .buildFakeRequestWithOnlySession("POST")
            .withFormUrlEncodedBody("income" -> "1234567890")
        }

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("error.tai.updateDataEmployment.maxLength"))

      }
    }
  }

  "confirmIncomeIrregularHours" must {
    "respond with Ok" in {
      val testController = createTestIncomeUpdateCalculatorController

      val employerName = "name"
      val payToDate = 123
      val newAmount = 1235
      val confirmedNewAmount = 1234

      when(
        journeyCacheService.collectedValues(any(), any())(any()))
        .thenReturn(Future.successful(
          Seq(employer.name, newAmount.toString, payToDate.toString), Seq(Some(confirmedNewAmount.toString))))


      val result: Future[Result] = testController.confirmIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithOnlySession("GET")
      )

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.irregular.title"))
    }

    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {
      val testController = createTestIncomeUpdateCalculatorController

      when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.failed(new Exception))

      val result: Future[Result] = testController.confirmIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithOnlySession("GET")
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "redirect to SameEstimatedPayPage" when {
      "the same amount of pay has been entered" in {
        val testController = createTestIncomeUpdateCalculatorController
        val newAmount = 123
        val confirmednewAmount = 123
        val paymentToDate = 100

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful(
            Seq(employer.name, newAmount.toString, paymentToDate.toString),
            Seq(Some(confirmednewAmount.toString))
          )
        )

        val result: Future[Result] = testController.confirmIncomeIrregularHours(1)(
          RequestBuilder.buildFakeRequestWithOnlySession("GET")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }
    }

    "redirect to IrregularSameEstimatedPayPage" when {
      "the same amount of payment to date has been entered" in {
        val testController = createTestIncomeUpdateCalculatorController
        val newAmount = 123
        val paymentToDate = 123

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful(
            Seq(employer.name, newAmount.toString, paymentToDate.toString),
            Seq(None)
          )
        )

        val result: Future[Result] = testController.confirmIncomeIrregularHours(1)(
          RequestBuilder.buildFakeRequestWithOnlySession("GET")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }
  }

  "submitIncomeIrregularHours" must {
    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {
      val testController = createTestIncomeUpdateCalculatorController

      when(
        journeyCacheService.mandatoryValues(any())(any())
      ).thenReturn(
        Future.failed(new Exception)
      )

      val result: Future[Result] = testController.submitIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithOnlySession("GET")
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }


    "sends Ok on successful submit" in {
      val testController = createTestIncomeUpdateCalculatorController

      val newAmount = 123

      when(
        journeyCacheService.mandatoryValues(any())(any())
      ).thenReturn(
        Future.successful(Seq(employer.name, newAmount.toString, employer.id.toString))
      )

      when(
        taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())
      ).thenReturn(
        Future.successful(TaiSuccessResponse)
      )

      when(estimatedPayJourneyCompletionService.journeyCompleted(Matchers.eq(employer.id.toString))(any())).
        thenReturn(Future.successful(Map.empty[String, String]))

      val result: Future[Result] = testController.submitIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithOnlySession("GET")
      )

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      doc.title() must include(messages("tai.incomes.updated.check.title", employer.name))
    }
  }
}