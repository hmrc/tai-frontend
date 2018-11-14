/*
 * Copyright 2018 HM Revenue & Customs
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
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms.{BonusOvertimeAmountForm, BonusPaymentsForm, YesNoForm}
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, _}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.ViewModelHelper.currentTaxYearRangeHtmlNonBreak
import uk.gov.hmrc.tai.util.constants.{EditIncomeIrregularPayConstants, FormValuesConstants, JourneyCacheConstants, TaiConstants}
import views.html.incomes.{bonusPaymentAmount, bonusPayments}

import scala.concurrent.Future
import scala.util.Random

class IncomeUpdateCalculatorControllerSpec
  extends PlaySpec
    with FakeTaiPlayApplication
    with MockitoSugar
    with JourneyCacheConstants
    with EditIncomeIrregularPayConstants
    with FormValuesConstants
    with ControllerViewTestHelper {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "estimatedPayLandingPage" must {
    "display the estimatedPayLandingPage view" in {
      val employerName = "Test Employment Name"
      val testController = createTestIncomeUpdateCalculatorController
      val taxCodeIncome1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
      val employment = Employment(employerName, Some("123"), new LocalDate("2016-05-26"), None, Nil, "", "", 1, None, false, false)

      when(testController.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
      when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(taxCodeIncome1))))
      val result = testController.estimatedPayLandingPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
    }
  }

  "howToUpdatePage" must {
    "render the right response to the user" in {
      val testController = createTestIncomeUpdateCalculatorController
      val employment = Employment("company", Some("123"), new LocalDate("2016-05-26"), None, Nil, "", "", 1, None, false, false)
      val employmentAmount = EmploymentAmount(name = "name", description = "description", employmentId = SampleId,
        newAmount = 200, oldAmount = 200, isLive = false, isOccupationalPension = true)

      when(testController.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
      when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
      when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome])))
      when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      val result = testController.howToUpdatePage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
    }
    "employments return empty income is none" in {
      val testController = createTestIncomeUpdateCalculatorController
      when(testController.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

      val result = testController.howToUpdatePage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "processHowToUpdatePage" must {
    val employmentAmount = (isLive: Boolean, isOccupationalPension: Boolean) => EmploymentAmount(name = "name", description = "description", employmentId = SampleId,
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
        when(testController.incomeService.editableIncomes(any())).thenReturn(Seq(taxCodeIncome1, taxCodeIncome2))

        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(true, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe OK
        val doc = Jsoup.parse(contentAsString(Future.successful(result)))
        doc.title() must include(messages("tai.howToUpdate.title", "name"))
      }

      "editable income is singular" in {
        val testController = createTestIncomeUpdateCalculatorController
        val taxCodeIncome1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
        when(testController.incomeService.editableIncomes(any())).thenReturn(Seq(taxCodeIncome1))
        when(testController.incomeService.singularIncomeId(any())).thenReturn(Some(1))

        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(true, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe OK
        val doc = Jsoup.parse(contentAsString(Future.successful(result)))
        doc.title() must include(messages("tai.howToUpdate.title", "name"))
      }

      "editable income is none" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.incomeService.editableIncomes(any())).thenReturn(Nil)
        when(testController.incomeService.singularIncomeId(any())).thenReturn(None)
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
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
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
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = testController.payslipAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslip.title"))
      }
    }
  }

  "handlePayslipAmount" must {
    "redirect the user to payslipDeductionsPage page" when {
      "user entered valid pay" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.cache(Matchers.eq(Map(UpdateIncome_TotalSalaryKey -> "£3,000")))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("totalSalary" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url)
      }
    }

    "redirect user back to how to payslip page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = testController.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslip.title"))
      }
    }
  }

  "taxablePayslipAmountPage" must {
    "display taxablePayslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = testController.taxablePayslipAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.taxablePayslip.title"))
      }
    }
  }

  "handleTaxablePayslipAmount" must {
    "redirect the user to bonusPaymentsPage page" when {
      "user entered valid taxable pay" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_TotalSalaryKey))(any())).thenReturn(Future.successful(None))
        when(testController.journeyCacheService.cache(Matchers.eq(Map(UpdateIncome_TaxablePayKey -> "£3,000")))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxablePay" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to taxablePayslip page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_TotalSalaryKey))(any())).thenReturn(Future.successful(None))
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = testController.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.taxablePayslip.title"))
      }
    }
  }

  "payslipDeductionsPage" must {
    "display payslipDeductions" when {
      "journey cache returns employment name and id" in {
        val testController = createTestIncomeUpdateCalculatorController
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
        when(testController.journeyCacheService.cache(Matchers.eq(Map(UpdateIncome_PayslipDeductionsKey -> "Yes")))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "Yes"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url)
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map("" -> "")))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        when(testController.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

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
      val employerId = 1
      val employerName = "employer1"
      val errorMessage = "error"
      implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

      when(testController.journeyCacheService.mandatoryValues(any())(any())).thenReturn(
        Future.successful(Seq(employerId.toString, employerName)))

      val result: Future[Result] = testController.bonusPaymentsPage()(fakeRequest)
      status(result) mustBe OK
      result rendersTheSameViewAs bonusPayments(BonusPaymentsForm.createForm,employerId,employerName)

    }
  }

  "handleBonusPayments" must {
    "redirect the user to bonusOvertimeAmountPage page" when {
      "user selected yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        val cacheMap = Map(UpdateIncome_BonusPaymentsKey -> YesValue)
        when(testController.journeyCacheService.cache(Matchers.eq(cacheMap))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(YesNoChoice -> YesValue))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url)
      }
    }

    "redirect the user to checkYourAnswers page" when {
      "user selected no" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map("" -> "")))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        when(testController.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))
        val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(YesNoChoice -> NoValue))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
      }
    }

    "redirect user back to how to bonusPayments page" when {
      "user input has error" in {

        val employerId = 1
        val employerName = "employer1"
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> "")

        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.mandatoryValues(any())(any())).thenReturn(
          Future.successful(Seq(employerId.toString,employerName)))

        val result = testController.handleBonusPayments()(fakeRequest)
        status(result) mustBe BAD_REQUEST
        result rendersTheSameViewAs bonusPayments(BonusPaymentsForm.createForm.bindFromRequest()(fakeRequest),employerId, employerName)

      }
    }
  }

  "bonusOvertimeAmountPage" must {
    "display bonusPaymentAmount" in {

        val testController = createTestIncomeUpdateCalculatorController
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
        val employerId = 1
        val employerName = "employer1"

        when(testController.journeyCacheService.mandatoryValues(any())(any())).thenReturn(
          Future.successful(Seq(employerId.toString,employerName)))

        val result: Future[Result] = testController.bonusOvertimeAmountPage()(fakeRequest)
        status(result) mustBe OK
        result rendersTheSameViewAs bonusPaymentAmount(BonusOvertimeAmountForm.createForm(),employerId, employerName)
    }
  }

  "handleBonusOvertimeAmount" must {
    "redirect the user to checkYourAnswers page" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.cache(Matchers.eq(Map(UpdateIncome_BonusOvertimeAmountKey -> "£3,000")))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("amount" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController.checkYourAnswersPage().url)
    }

    "redirect the user to bonusPaymentAmount page" when {
      "user input has error" in {
        val employerId = 1
        val employerName = "employer1"
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("amount" -> "")

        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq(employerId.toString,employerName)))
        val result = testController.handleBonusOvertimeAmount()(fakeRequest)
        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs bonusPaymentAmount(BonusOvertimeAmountForm.createForm().bindFromRequest()(fakeRequest),employerId, employerName)
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

      when(testController.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
        Future.successful((
          Seq[String](employerName,payFrequency,totalSalary,payslipDeductions,bonusPayments),
          Seq[Option[String]](Some(taxablePay),Some(bonusAmount))
        ))
      )
      val result = testController.checkYourAnswersPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.checkYourAnswers.title"))
    }
  }

  "estimatedPayPage" must {
    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        when(testController.incomeService.calculateEstimatedPay(any(), any())(any())).thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))
        when(testController.incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(None))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeSingleLine))
      }
    }

    "display incorrectTaxableIncome page" when {
      "payYearToDate is greater than gross annual pay" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)
        val payment = Payment(new LocalDate(), 200, 50, 25, 100, 50, 25, Monthly)

        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        when(testController.incomeService.calculateEstimatedPay(any(), any())(any())).thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))
        when(testController.incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))

      }
    }
  }

  "handleCalculationResult" must {
    "display confirm_save_Income page" when {
      "journey cache returns employment name, net amount and id" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any())).thenReturn(Future.successful(Some("100")))

        val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", currentTaxYearRangeHtmlNonBreak))
      }

      "journey cache returns employment name, net amount with large decimal value and id" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any())).thenReturn(Future.successful(Some("4632.460273972602739726027397260273")))

        val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", currentTaxYearRangeHtmlNonBreak))
      }

      "journey cache does not returns net amount" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any())).thenReturn(Future.successful(None))

        val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.incomes.confirm.save.title", currentTaxYearRangeHtmlNonBreak))
      }
    }
  }

  "editIncomeIrregularHours" must {
    "respond with OK and show the irregular hours edit page" in {
      val testController = createTestIncomeUpdateCalculatorController

      val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 123,"description","taxCode","name",OtherBasisOfOperation,Live)
      when(
        testController.taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any())
      ).thenReturn(
        Future.successful(Some(taxCodeIncome))
      )

      val payment = Payment(LocalDate.now().minusDays(1), 0,0,0,0,0,0, Monthly)
      when(
        testController.incomeService.latestPayment(any(), any())(any())
      ).thenReturn(
        Future.successful(Some(payment))
      )

      when(
        testController.journeyCacheService.cache(any())(any())
      ).thenReturn(
        Future.successful(Map.empty[String, String])
      )

      val result = testController.editIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithAuth("GET")
      )

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.irregular.title"))
    }

    "respond with INTERNAL_SERVER_ERROR" when {
      "the employment income cannot be found" in {
        val testController = createTestIncomeUpdateCalculatorController
        val taxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(1), 123, "description", "taxCode", "name", OtherBasisOfOperation, Live)

        when(testController.taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
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
        testController.journeyCacheService.currentCache(any())
      ).thenReturn(
        Future.successful(cacheMap)
      )

      when(
        testController.journeyCacheService.cache(any(), any())(any())
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
          testController.journeyCacheService.mandatoryValues(any())(any())
        ).thenReturn(
          Future.successful(Seq(employerName, payToDate.toString))
        )

        val cacheMap = Map(
          UpdateIncome_NameKey -> employerName,
          UpdateIncome_PayToDateKey -> payToDate.toString,
          UpdateIncome_DateKey -> payDate
        )

        when(
          testController.journeyCacheService.currentCache(any())
        ).thenReturn(
          Future.successful(cacheMap)
        )

        val result = testController.handleIncomeIrregularHours(1)(
          RequestBuilder
            .buildFakeRequestWithOnlySession("POST")
            .withFormUrlEncodedBody("income" -> (payToDate-1).toString)
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.title"))

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
          testController.journeyCacheService.currentCache(any())
        ).thenReturn(
          Future.successful(cacheMap)
        )

        when(
          testController.journeyCacheService.mandatoryValues(any())(any())
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
        doc.title() must include(messages("tai.irregular.title"))

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
          testController.journeyCacheService.currentCache(any())
        ).thenReturn(
          Future.successful(cacheMap)
        )

        when(
          testController.journeyCacheService.mandatoryValues(any())(any())
        ).thenReturn(
          Future.successful(Seq(employerName, payToDate.toString))
        )

        val result = testController.handleIncomeIrregularHours(1) {
          RequestBuilder.buildFakeRequestWithOnlySession("POST")
        }

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.title"))
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
          testController.journeyCacheService.currentCache(any())
        ).thenReturn(
          Future.successful(cacheMap)
        )

        when(
          testController.journeyCacheService.mandatoryValues(any())(any())
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
        doc.title() must include(messages("tai.irregular.title"))
        doc.body().text must include(messages("error.tai.updateDataEmployment.maxLength"))

      }
    }
  }

  "confirmIncomeIrregularHours" must {
    "respond with Ok" in {
      val testController = createTestIncomeUpdateCalculatorController

      val employerName = "name"
      val payToDate = 123
      val newAmount = 123

      when(
        testController.journeyCacheService.mandatoryValues(any())(any())
      ).thenReturn(
        Future.successful(Seq(employerName, newAmount.toString))
      )



      val result: Future[Result] = testController.confirmIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithOnlySession("GET")
      )


      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      doc.title() must include(messages("tai.irregular.title"))
    }

    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {
      val testController = createTestIncomeUpdateCalculatorController

      when(
        testController.journeyCacheService.mandatoryValues(any())(any())
      ).thenReturn(
        Future.failed(new Exception)
      )

      val result: Future[Result] = testController.confirmIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithOnlySession("GET")
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "submitIncomeIrregularHours" must {

    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {
      val testController = createTestIncomeUpdateCalculatorController

      when(
        testController.journeyCacheService.mandatoryValues(any())(any())
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

      val employerName = "name"
      val payToDate = 123
      val newAmount = 123
      val employerId = "1"

      when(
        testController.journeyCacheService.mandatoryValues(any())(any())
      ).thenReturn(
        Future.successful(Seq(employerName, newAmount.toString, employerId))
      )

      when(
        testController.taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())
      ).thenReturn(
        Future.successful(TaiSuccessResponse)
      )

      val result: Future[Result] = testController.submitIncomeIrregularHours(1)(
        RequestBuilder.buildFakeRequestWithOnlySession("GET")
      )

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      doc.title() must include(messages("tai.incomes.updated.check.title", employerName))
    }
  }

  private val SampleId = 1
  private val EmployerName = "sample employer"

  private def fakeNino = new Generator(new Random).nextNino

  private def createTestIncomeUpdateCalculatorController = new TestIncomeUpdateCalculatorController()

  private class TestIncomeUpdateCalculatorController extends IncomeUpdateCalculatorController {
    override val personService: PersonService = mock[PersonService]
    override val activityLoggerService: ActivityLoggerService = mock[ActivityLoggerService]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val incomeService: IncomeService = mock[IncomeService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]

    val ad: Future[Some[Authority]] = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(fakeNino)))

    when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(SampleId))
    when(journeyCacheService.mandatoryValue(Matchers.eq(UpdateIncome_NameKey))(any())).thenReturn(Future.successful(EmployerName))
  }

}
