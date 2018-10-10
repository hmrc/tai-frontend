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

package controllers

import builders.{AuthBuilder, RequestBuilder, UserBuilder}
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
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, _}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.JourneyCacheConstants
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class IncomeUpdateCalculatorControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JourneyCacheConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "estimatedPayLandingPage" must {
    "display the estimatedPayLandingPage view" in {
      val employerName = "Test Employment Name"
      val testController = createTestIncomeUpdateCalculatorController
      val employment = Employment(employerName, Some("123"), new LocalDate("2016-05-26"), None, Nil, "", "", 1, None, false, false)
      val taxCodeIncome1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOperation, Live)

      when(testController.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
      when(testController.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(taxCodeIncome1))))
      val result = testController.estimatedPayLandingPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.incomes.edit.preHeading", employerName))
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
      redirectLocation(result) mustBe Some(routes.IncomeController.pensionIncome().url)
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
        result.header.headers.get(LOCATION) mustBe Some(routes.IncomeController.pensionIncome().url)
      }

      "employment amount is not occupation income" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(false, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe SEE_OTHER
        result.header.headers.get(LOCATION) mustBe Some(routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }

    "redirect user for is live employment " when {
      "editable incomes are greater than one" in {
        val testController = createTestIncomeUpdateCalculatorController
        val taxCodeIncome1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOperation, Live)
        val taxCodeIncome2 = TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employer", "S1150L", "employer", OtherBasisOperation, Live)
        when(testController.incomeService.editableIncomes(any())).thenReturn(Seq(taxCodeIncome1, taxCodeIncome2))

        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(true, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe OK
        val doc = Jsoup.parse(contentAsString(Future.successful(result)))
        doc.title() must include(Messages("tai.howToUpdate.title", "name"))
      }

      "editable income is singular" in {
        val testController = createTestIncomeUpdateCalculatorController
        val taxCodeIncome1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOperation, Live)
        when(testController.incomeService.editableIncomes(any())).thenReturn(Seq(taxCodeIncome1))
        when(testController.incomeService.singularIncomeId(any())).thenReturn(Some(1))

        val result: Result = testController.processHowToUpdatePage(1, "name", employmentAmount(true, false),
          TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply())

        result.header.status mustBe OK
        val doc = Jsoup.parse(contentAsString(Future.successful(result)))
        doc.title() must include(Messages("tai.howToUpdate.title", "name"))
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
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.workingHoursPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "income"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.howToUpdate.title", ""))
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
        doc.title() must include(Messages("tai.workingHours.title"))
      }
    }
  }

  "handleWorkingHours" must {
    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "same"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.payPeriodPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "income"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.calcUnavailablePage().url)
      }
    }

    "redirect user back to workingHours page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.workingHours.title"))
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
        doc.title() must include(Messages("tai.payPeriod.title"))
      }
    }
  }

  "handlePayPeriod" must {
    "redirect the user to payslipAmountPage page" when {
      "user selected monthly" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        when(testController.incomeService.cachePayPeriod(any())(any())).thenReturn(Map("" -> ""))
        val result = testController.handlePayPeriod()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "monthly"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.payslipAmountPage().url)
      }
    }

    "redirect user back to how to payPeriod page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handlePayPeriod()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "otherInDays"))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.payPeriod.title"))
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
        doc.title() must include(Messages("tai.payslip.title"))
      }
    }
  }

  "handlePayslipAmount" must {
    "redirect the user to payslipDeductionsPage page" when {
      "user entered valid pay" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.cache(Matchers.eq(UpdateIncome_TotalSalaryKey), Matchers.eq("£3,000"))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("totalSalary" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url)
      }
    }

    "redirect user back to how to payslip page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = testController.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.payslip.title"))
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
        doc.title() must include(Messages("tai.taxablePayslip.title"))
      }
    }
  }

  "handleTaxablePayslipAmount" must {
    "redirect the user to bonusPaymentsPage page" when {
      "user entered valid taxable pay" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_TotalSalaryKey))(any())).thenReturn(Future.successful(None))
        when(testController.journeyCacheService.cache(Matchers.eq(UpdateIncome_TaxablePayKey), Matchers.eq("£3,000"))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxablePay" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
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
        doc.title() must include(Messages("tai.taxablePayslip.title"))
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
        doc.title() must include(Messages("tai.payslipDeductions.title"))
      }
    }
  }

  "handlePayslipDeductions" must {
    "redirect the user to taxablePayslipAmountPage page" when {
      "user selected yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.cache(Matchers.eq(UpdateIncome_PayslipDeductionsKey), Matchers.eq("Yes"))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "Yes"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url)
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.cache(Matchers.eq(UpdateIncome_PayslipDeductionsKey), Matchers.eq("No"))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "No"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to payslipDeductions page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.payslipDeductions.title"))
      }
    }
  }

  "bonusPaymentsPage" must {
    "display bonusPayments" when {
      "journey cache returns employment name and id" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayslipDeductionsKey))(any())).thenReturn(Future.successful(None))
        val result = testController.bonusPaymentsPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bonusPayments.title"))
      }
    }
  }

  "handleBonusPayments" must {
    "redirect the user to bonusOvertimeAmountPage page" when {
      "user selected yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("bonusPayments" -> "Yes", "bonusPaymentsMoreThisYear" -> "No"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url)
      }
    }

    "redirect the user to estimatedPayPage page" when {
      "user selected no" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("bonusPayments" -> "No"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.estimatedPayPage().url)
      }
    }

    "redirect user back to how to bonusPayments page" when {
      "user input has error" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayslipDeductionsKey))(any())).thenReturn(Future.successful(None))
        val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bonusPayments.title"))
      }
    }
  }

  "bonusOvertimeAmountPage" must {
    "display bonusPaymentAmount" when {
      "more this year from journey cache returns yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_BonusPaymentsThisYearKey))(any())).thenReturn(Future.successful(Some("Yes")))
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(Some("Weekly")))
        val result = testController.bonusOvertimeAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bonusPaymentsAmount.year.title"))
      }

      "more this year from journey cache does not return yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_BonusPaymentsThisYearKey))(any())).thenReturn(Future.successful(None))
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(Some("Weekly")))
        val result = testController.bonusOvertimeAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bonusPaymentsAmount.period.title"))
      }
    }
  }

  "handleBonusOvertimeAmount" must {
    "redirect the user to estimatedPayPage page" when {
      "user selected yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(UpdateIncome_IdKey -> "1", UpdateIncome_BonusPaymentsThisYearKey -> "Yes")))
        when(testController.journeyCacheService.cache(Matchers.eq(UpdateIncome_BonusOvertimeAmountKey), Matchers.eq("£3,000"))(any())).thenReturn(Future.successful(Map("" -> "")))
        val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("amount" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.estimatedPayPage().url)
      }
    }

    "redirect the user to bonusPaymentAmount page" when {
      "bonus payment is yes" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(UpdateIncome_IdKey -> "1", UpdateIncome_BonusPaymentsThisYearKey -> "Yes")))
        val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bonusPaymentsAmount.year.title"))
      }

      "bonus payment is none" in {
        val testController = createTestIncomeUpdateCalculatorController
        when(testController.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(UpdateIncome_IdKey -> "1")))
        val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.bonusPaymentsAmount.period.title"))
      }
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
        doc.title() must include(Messages("tai.estimatedPay.title"))
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
        doc.title() must include(Messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))

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
        doc.title() must include(Messages("tai.incomes.confirm.save.title"))
      }

      "journey cache returns employment name, net amount with large decimal value and id" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any())).thenReturn(Future.successful(Some("4632.460273972602739726027397260273")))

        val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.confirm.save.title"))
      }

      "journey cache does not returns net amount" in {
        val testController = createTestIncomeUpdateCalculatorController
        val employmentAmount = EmploymentAmount("", "", 1, 1, 1)

        when(testController.incomeService.employmentAmount(any(), any())(any(), any())).thenReturn(Future.successful(employmentAmount))
        when(testController.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NewAmountKey))(any())).thenReturn(Future.successful(None))

        val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.confirm.save.title"))
      }
    }
  }

  "calcUnavailablePage" must {
    "display calcUnavailable page" when {
      "journey cache returns employment name and id" in {
        val testController = createTestIncomeUpdateCalculatorController
        val result = testController.calcUnavailablePage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.unableToCalculate.title"))
      }
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
