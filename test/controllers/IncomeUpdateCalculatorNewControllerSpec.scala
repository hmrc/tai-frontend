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
import data.TaiData
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Employment, Monthly, Payment}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.JourneyCacheConstants

import scala.concurrent.Future
import scala.util.Random

class IncomeUpdateCalculatorNewControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JourneyCacheConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "processChooseHowToUpdate" must {
    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val sut = createSut
        val result = sut.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "incomeCalculator"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.workingHoursPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val sut = createSut
        val result = sut.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "income"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val sut = createSut
        val result = sut.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.howToUpdate.title")
      }
    }
  }

  "workingHoursPage" must {
    "display workingHours page" when {
      "journey cache returns employment name and id" in {
        val sut = createSut
        val result = sut.workingHoursPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.workingHours.title")
      }
    }
  }

  "handleWorkingHours" must {
    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val sut = createSut
        val result = sut.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "same"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.payPeriodPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val sut = createSut
        val result = sut.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "income"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.calcUnavailablePage().url)
      }
    }

    "redirect user back to workingHours page" when {
      "user input has error" in {
        val sut = createSut
        val result = sut.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.workingHours.title")
      }
    }
  }

  "payPeriodPage" must {
    "display payPeriod page" when {
      "journey cache returns employment name and id" in {
        val sut = createSut
        val result = sut.payPeriodPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payPeriod.title")
      }
    }
  }

  "handlePayPeriod" must {
    "redirect the user to payslipAmountPage page" when {
      "user selected monthly" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map(""->"")))
        when(sut.incomeService.cachePayPeriod(any())(any())).thenReturn(Map(""->""))
        val result = sut.handlePayPeriod()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "monthly"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.payslipAmountPage().url)
      }
    }

    "redirect user back to how to payPeriod page" when {
      "user input has error" in {
        val sut = createSut
        val result = sut.handlePayPeriod()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "otherInDays"))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payPeriod.title")
      }
    }
  }

  "payslipAmountPage" must {
    "display payslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = sut.payslipAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payslip.title")
      }
    }
  }

  "handlePayslipAmount" must {
    "redirect the user to payslipDeductionsPage page" when {
      "user entered valid pay" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(Matchers.eq(UpdateIncome_TotalSalaryKey), Matchers.eq("£3,000"))(any())).thenReturn(Future.successful(Map(""->"")))
        val result = sut.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("totalSalary" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.payslipDeductionsPage().url)
      }
    }

    "redirect user back to how to payslip page" when {
      "user input has error" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = sut.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody( "" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payslip.title")
      }
    }
  }

  "taxablePayslipAmountPage" must {
    "display taxablePayslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = sut.taxablePayslipAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.taxablePayslip.title")
      }
    }
  }

  "handleTaxablePayslipAmount" must {
    "redirect the user to bonusPaymentsPage page" when {
      "user entered valid taxable pay" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_TotalSalaryKey))(any())).thenReturn(Future.successful(None))
        when(sut.journeyCacheService.cache(Matchers.eq(UpdateIncome_TaxablePayKey), Matchers.eq("£3,000"))(any())).thenReturn(Future.successful(Map(""->"")))
        val result = sut.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxablePay" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to taxablePayslip page" when {
      "user input has error" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_TotalSalaryKey))(any())).thenReturn(Future.successful(None))
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayPeriodKey))(any())).thenReturn(Future.successful(None))
        val result = sut.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.taxablePayslip.title")
      }
    }
  }

  "payslipDeductionsPage" must {
    "display payslipDeductions" when {
      "journey cache returns employment name and id" in {
        val sut = createSut
        val result = sut.payslipDeductionsPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payslipDeductions.title")
      }
    }
  }

  "handlePayslipDeductions" must {
    "redirect the user to taxablePayslipAmountPage page" when {
      "user selected yes" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(Matchers.eq(UpdateIncome_PayslipDeductionsKey), Matchers.eq("Yes"))(any())).thenReturn(Future.successful(Map(""->"")))
        val result = sut.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "Yes"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage().url)
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(Matchers.eq(UpdateIncome_PayslipDeductionsKey), Matchers.eq("No"))(any())).thenReturn(Future.successful(Map(""->"")))
        val result = sut.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "No"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to payslipDeductions page" when {
      "user input has error" in {
        val sut = createSut
        val result = sut.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payslipDeductions.title")
      }
    }
  }

  "bonusPaymentsPage" must {
    "display bonusPayments" when {
      "journey cache returns employment name and id" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayslipDeductionsKey))(any())).thenReturn(Future.successful(None))
        val result = sut.bonusPaymentsPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.bonusPayments.title")
      }
    }
  }

  "handleBonusPayments" must {
    "redirect the user to bonusOvertimeAmountPage page" when {
      "user selected yes" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map(""->"")))
        val result = sut.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("bonusPayments" -> "Yes", "bonusPaymentsMoreThisYear" -> "No"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage().url)
      }
    }

    "redirect the user to estimatedPayPage page" when {
      "user selected no" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map(""->"")))
        val result = sut.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("bonusPayments" -> "No"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.estimatedPayPage().url)
      }
    }

    "redirect user back to how to bonusPayments page" when {
      "user input has error" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_PayslipDeductionsKey))(any())).thenReturn(Future.successful(None))
        val result = sut.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.bonusPayments.title")
      }
    }
  }

  "bonusOvertimeAmountPage" must {
    "display bonusPaymentAmount" when {
      "more this year from journey cache returns yes" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_BonusPaymentsThisYearKey))(any())).thenReturn(Future.successful(Some("Yes")))
        val result = sut.bonusOvertimeAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.bonusPaymentsAmount.year.title")
      }

      "more this year from journey cache does not return yes" in {
        val sut = createSut
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_BonusPaymentsThisYearKey))(any())).thenReturn(Future.successful(None))
        val result = sut.bonusOvertimeAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.bonusPaymentsAmount.period.title")
      }
    }
  }

  "handleBonusOvertimeAmount" must {
    "redirect the user to estimatedPayPage page" when {
      "user selected yes" in {
        val sut = createSut
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(UpdateIncome_IdKey->"1", UpdateIncome_BonusPaymentsThisYearKey -> "Yes")))
        when(sut.journeyCacheService.cache(Matchers.eq(UpdateIncome_BonusOvertimeAmountKey), Matchers.eq("£3,000"))(any())).thenReturn(Future.successful(Map(""->"")))
        val result = sut.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("amount" -> "£3,000"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.estimatedPayPage().url)
      }
    }

    "redirect the user to bonusPaymentAmount page" when {
      "bonus payment is yes" in {
        val sut = createSut
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(UpdateIncome_IdKey->"1", UpdateIncome_BonusPaymentsThisYearKey -> "Yes")))
        val result = sut.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.bonusPaymentsAmount.year.title")
      }

      "bonus payment is none" in {
        val sut = createSut
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(UpdateIncome_IdKey->"1")))
        val result = sut.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.bonusPaymentsAmount.period.title")
      }
    }
  }

  "estimatedPayPage" must {
    "display estimatedPay page" when {
      "payYearToDate is less than gross annual pay"  in {
        val sut = createSut
        val employmentAmount = EmploymentAmount("","",1,1,1)

        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        when(sut.incomeService.calculateEstimatedPay(any(), any())(any())).thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))
        when(sut.incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(None))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.estimatedPay.title")
      }
    }

    "display incorrectTaxableIncome page" when {
      "payYearToDate is greater than gross annual pay"  in {
        val sut = createSut
        val employmentAmount = EmploymentAmount("","",1,1,1)
        val payment = Payment(new LocalDate(), 200, 50, 25, 100, 50, 25, Monthly)

        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        when(sut.incomeService.calculateEstimatedPay(any(), any())(any())).thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))
        when(sut.incomeService.latestPayment(any(), any())(any())).thenReturn(Future.successful(Some(payment)))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.currentYearSummary.heading")

      }
    }
  }

  "handleCalculationResult" must {
    "display confirm_save_Income page" when {
      "journey cache returns employment name, net amount and id" in {
        val sut = createSut
        val employmentAmount = EmploymentAmount("","",1,1,1)

        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NetAnnualPayKey))(any())).thenReturn(Future.successful(Some("100")))

        val result = sut.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.incomes.confirm.save.title")
      }

      "journey cache does not returns net amount" in {
        val sut = createSut
        val employmentAmount = EmploymentAmount("","",1,1,1)

        when(sut.incomeService.employmentAmount(any(), any())(any())).thenReturn(Future.successful(employmentAmount))
        when(sut.journeyCacheService.currentValue(Matchers.eq(UpdateIncome_NetAnnualPayKey))(any())).thenReturn(Future.successful(None))

        val result = sut.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.incomes.confirm.save.title")
      }
    }
  }

  "calcUnavailablePage" must {
    "display calcUnavailable page" when {
      "journey cache returns employment name and id" in {
        val sut = createSut
        val result = sut.calcUnavailablePage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.unableToCalculate.title")
      }
    }
  }

  private val SampleId = 1
  private val EmployerName = "sample employer"
  private val fakeTaiRoot = TaiRoot(fakeNino.nino, 0, "Mr", "Kkk", None, "Sss", "Kkk Sss", false, Some(false))
  private def fakeNino = new Generator(new Random).nextNino

  private def createSut = new SUT()

  private class SUT extends IncomeUpdateCalculatorNewController {
    override val taiService: TaiService = mock[TaiService]
    override val activityLoggerService: ActivityLoggerService = mock[ActivityLoggerService]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: PartialRetriever = MockPartialRetriever
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val incomeService: IncomeService = mock[IncomeService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]

    val ad: Future[Some[Authority]] = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(AuthBuilder.createFakeSessionDataWithPY))
    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(fakeTaiRoot))

    when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(SampleId))
    when(journeyCacheService.mandatoryValue(Matchers.eq(UpdateIncome_NameKey))(any())).thenReturn(Future.successful(EmployerName))
  }

}
