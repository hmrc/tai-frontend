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

import TestConnectors.FakeAuthConnector
import builders.{RequestBuilder, UserBuilder}
import data.TaiData
import uk.gov.hmrc.tai.forms._
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import uk.gov.hmrc.tai.model._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.service.{ActivityLoggerService, EmploymentService, JourneyCacheService, TaiService}
import testServices.FakeActivityLoggerService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.config.{AuditConnector, FrontEndDelegationConnector}
import uk.gov.hmrc.tai.model._

import scala.concurrent.Future

class IncomeUpdateCalculatorControllerSpec extends UnitSpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val hc = HeaderCarrier()

  "Calling the chooseHowToUpdatePage method" should {
    "redirect to the correct page for a ceased employment" in {
      val testTaxSummary = TaiData.getEditableCeasedAndIncomeTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      val employmentAmount = EmploymentAmount( name = "test1", description = "description",
        employmentId=14,
        newAmount=1675,
        oldAmount=11,
        worksNumber=None,
        startDate=None,
        endDate=None,
        isLive=false,
        isOccupationalPension=false)

      when(testController.taiService.incomeForEdit(any(), any())(any())).thenReturn(Future.successful(Some(employmentAmount)))

      val result = testController.getChooseHowToUpdatePage(Nino(s"${testTaxSummary.nino}A"), 3, "Employer Name")(FakeRequest("GET", ""), UserBuilder.apply(), testController.sessionData)
      status(result) shouldBe 303

      redirectLocation(result).get shouldBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }


    "redirect to the correct page for a pension employment" in {
      val testTaxSummary = TaiData.getSinglePensionIncome
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      val employmentAmount = EmploymentAmount( name = "test1", description = "description",
        employmentId=14,
        newAmount=1675,
        oldAmount=11,
        worksNumber=None,
        startDate=None,
        endDate=None,
        isLive=true,
        isOccupationalPension=true)

      when(testController.taiService.incomeForEdit(any(), any())(any())).thenReturn(Future.successful(Some(employmentAmount)))

      val result = testController.getChooseHowToUpdatePage(Nino(s"${testTaxSummary.nino}A"), 1, "Employer Name")(FakeRequest("GET", ""), UserBuilder.apply(), testController.sessionData)

      status(result) shouldBe 303
      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/update-income/edit-pension"
    }

    "redirect to the view your-income-calculation screen if no income exists " in {
      val testTaxSummary = TaiData.getSinglePensionIncome
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      when(testController.taiService.incomeForEdit(any(), any())(any())).thenReturn(Future.successful(None))

      val result = testController.getChooseHowToUpdatePage(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("GET", ""), UserBuilder.apply(), testController.sessionData)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/check-income-tax/your-income-calculation"
    }


    "display the page for someone where income updates are enabled" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)


      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.getChooseHowToUpdatePage(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("GET", ""), UserBuilder.apply(), sessionData)

        status(result) shouldBe 200
        val content = contentAsString(result)
        val doc = Jsoup.parse(content)
        doc.title() shouldBe "Choose how to update your income"
      }
    }


    "throw an error in the form if posted to without selecting an option" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody()

      val howToUpdateForm = HowToUpdateForm.createForm().bindFromRequest()
      howToUpdateForm.hasErrors shouldBe true
    }
  }


  "Calling the processChooseHowToUpdate method" should {
    "redirect the user to workingHours page when they've chosen to use the income calculator" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.processChooseHowToUpdate(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("POST", "").withFormUrlEncodedBody("howToUpdate" -> "incomeCalculator"), UserBuilder.apply(), sessionData)

        status(result) shouldBe 303

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl shouldBe "/check-income-tax/update-income/working-hours"
      }
    }


    "display the correct page for someone who selected Enter an annual figure in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.processChooseHowToUpdate(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("POST", "").withFormUrlEncodedBody("howToUpdate" -> "enterAnnual"), UserBuilder.apply(), sessionData)

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl shouldBe "/check-income-tax/update-income/select-taxable-pay"
      }
    }
  }

  "Calling the getWorkingHoursPage method" should {
    "display the page for someone where income updates are enabled" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.getWorkingHoursPage(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("GET", ""), UserBuilder.apply(),sessionData)

        status(result) shouldBe 200
        val content = contentAsString(result)
        val doc = Jsoup.parse(content)
        doc.title() shouldBe "Your working hours"
      }
    }

    "throw an error in the form if posted to without selecting an option" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody()

      val hoursWorkedForm = HoursWorkedForm.createForm().bindFromRequest()
      hoursWorkedForm.hasErrors shouldBe true
    }
  }


  "Calling the processWorkingHoursPage method" should {

    "display the correct page for someone who selected Generally the same in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.processWorkingHours(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("POST", "").withFormUrlEncodedBody("workingHours" -> "same"), UserBuilder.apply(), sessionData)

        status(result) shouldBe 303

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl shouldBe "/check-income-tax/update-income/pay-period"
      }
    }

    "display the unable to calculate your pay page for someone who selected Very different in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.processWorkingHours(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("POST", "").withFormUrlEncodedBody("workingHours" -> "veryDifferent"), UserBuilder.apply(), sessionData)

        status(result) shouldBe 303

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl shouldBe "/check-income-tax/update-income/calculation-unavailable"
      }
    }

  }

  "Calling the getPayPeriodPage method" should {
    "display the page for someone where income updates are enabled" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary)

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.getPayPeriodPage(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("GET", ""), UserBuilder.apply(), sessionData)

        status(result) shouldBe 200
        val content = contentAsString(result)
        val doc = Jsoup.parse(content)
        doc.title() shouldBe "How often do you get paid?"
      }
    }
  }

  "Calling the processWorkingHours method" should {
    "throw an error in the form if posted to without selecting a pay frequency" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody()

      val payPeriodForm = PayPeriodForm.createForm(None).bindFromRequest()
      payPeriodForm.hasErrors shouldBe true
    }

    "throw an error in the form if posted to and pay frequency is other and howOften is blank" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody("payPeriod" -> "other")

      val payPeriodForm = PayPeriodForm.createForm(None,payPeriod = Some("other")).bindFromRequest()
      payPeriodForm.hasErrors shouldBe true
    }

    "not throw an error in the form if posted to and pay frequency is not other and howOften is blank" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody("payPeriod" -> "monthly", "otherInDays" -> "")

      val payPeriodForm = PayPeriodForm.createForm(None).bindFromRequest()
      payPeriodForm.hasErrors shouldBe false
    }


    "display the correct page for someone who selected Monthly in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(
        testTaxSummary,
        mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")))

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.processPayPeriod(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "monthly"), UserBuilder.apply(), sessionData)

        status(result) shouldBe 303

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl shouldBe "/check-income-tax/update-income/payslip-amount"
      }
    }
  }

  //PAYSLIP AMOUNT
  "Calling the getPayslipAmountPage method" should {
    "display the page for someone where income updates are enabled" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary,
        mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")))


      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.getPayslipAmountPage(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("GET", ""), UserBuilder.apply(), sessionData)

        val content = contentAsString(result)
        val doc = Jsoup.parse(content)
        doc.title() shouldBe "Your total pay from your payslip"
      }
    }

  }

  "Calling the processPayslipAmount method" should {
    "throw an error in the form if posted to without putting in any value" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody()

      val paySlipForm = PayslipForm.createForm().bindFromRequest()
      paySlipForm.hasErrors shouldBe true
    }

    "throw an error in the form if posted to without putting in a numeric value" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody("totalSalary" -> "test")

      val paySlipForm = PayslipForm.createForm().bindFromRequest()
      paySlipForm.hasErrors shouldBe true
    }

    "throw an error in the form if posted putting in a random value" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody("totalSalary" -> "££12,000")

      val paySlipForm = PayslipForm.createForm().bindFromRequest()
      paySlipForm.hasErrors shouldBe true
    }

    "not throw an error in the form if posted to, putting in a numeric value" in {
      implicit val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody("totalSalary" -> "£12,000")

      val payslipForm = PayslipForm.createForm().bindFromRequest()
      payslipForm.hasErrors shouldBe false
    }

    "display the correct page for someone who put a correct value in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary,
        mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")))

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.processPayslipAmount(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("totalSalary" -> "£3,000"), UserBuilder.apply(), sessionData)

        status(result) shouldBe 303

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl shouldBe "/check-income-tax/update-income/payslip-deductions"
      }
    }


    "go to the start of the update income journey if the income id is not already in session" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = None)))
      val result = testController.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("totalSalary" -> "£3,000"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"
    }


    "go to the start of the update Income journey if the income id is not the same as that in session" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(13))))
      val result = testController.handlePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody( "totalSalary" -> "£3,000"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"

    }

  }

  //TAXABLE PAYSLIP AMOUNT
  "Calling the getTaxablePayslipAmountPage method" should {
    "display the page for someone where income updates are enabled" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary,mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")))

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.getTaxablePayslipAmountPage(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(FakeRequest("GET", ""), UserBuilder.apply(),sessionData)

        val content = contentAsString(result)
        val doc = Jsoup.parse(content)
        doc.title() shouldBe "Taxable pay from your payslip"
      }
    }

  }

  "Calling the processTaxablePayslipAmount method" should {

    "display the correct page for someone who did not put a correct value in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14),
        payslipForm = Some(PayslipForm.create(PayslipForm(totalSalary = Some("3000"))).get))))

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.processTaxablePayslipAmount(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxablePay" -> ""), UserBuilder.apply(),sessionData)

        status(result) shouldBe 400

        val content = contentAsString(result)
        val doc = Jsoup.parse(content)
        doc.title() shouldBe "Taxable pay from your payslip"
      }
    }

    "display the correct page for someone who put a correct value in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14),
        payslipForm = Some(PayslipForm.create(PayslipForm(totalSalary = Some("3000"))).get))))

      val session = testController.taiService.taiSession(Nino(testTaxSummary.nino+"A"), 0, "")
      ScalaFutures.whenReady(session) {sessionData =>

        val result = testController.processTaxablePayslipAmount(Nino(s"${testTaxSummary.nino}A"), 14, "Employer Name")(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxablePay" -> "£3,000"), UserBuilder.apply(),sessionData)

        status(result) shouldBe 303

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl shouldBe "/check-income-tax/update-income/bonus-payments"
      }
    }


    "go to the start of the update income journey if the income id is not set in session" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = None,
        payslipForm = Some(PayslipForm.create(PayslipForm(totalSalary = Some("3000"))).get))))
      val result = testController.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxablePay" -> "£3,000"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"
    }


    "go to the start of the update Income journey if the income id is not the same as that in sessiong " in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(13),
        payslipForm = Some(PayslipForm.create(PayslipForm(totalSalary = Some("3000"))).get))))
      val result = testController.handleTaxablePayslipAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody( "taxablePay" -> "£3,000"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"

    }

  }


  "Calling the bonusPaymentsPage method" should {

    "display the correct page for someone who put a correct value in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary,mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")), mockAuthConnector = Some(auth))

      val result = testController.bonusPaymentsPage()(RequestBuilder.buildFakeRequestWithAuth("GET").withFormUrlEncodedBody())

      status(result) shouldBe 200

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.title() shouldBe "Bonus and overtime payments"

    }

  }

  "Calling the handleBonusPayments method" should {

    "go to the same page if incorrect data entered into form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary,
        mockIncomeCalculation = Some(IncomeCalculation(incomeId = None)),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),mockAuthConnector = Some(auth))

      val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())

      status(result) shouldBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.title() shouldBe "Bonus and overtime payments"

    }



    "go to the start of the update Income journey if the income id is not set in Session" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = None)),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),mockAuthConnector = Some(auth))

      val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody( "bonusPayments" -> "No" ))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"

    }


    "go to the start of the update Income journey if the income id is not the same as that in session " in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(13))))
      val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody( "bonusPayments" -> "No" ))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"

    }

    "display the correct page when No is selected for bonusPayments" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),
        mockAuthConnector = Some(auth))
      val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("bonusPayments" -> "No"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/update-income/estimated-pay"
    }

    "go to the correct page when Yes is selected for bonusPayments and nothing is entered for bonusPaymentsMoreThisYear" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),
        mockAuthConnector = Some(auth))

      val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("bonusPayments" -> "Yes"))

      status(result) shouldBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.title() shouldBe "Bonus and overtime payments"


    }

    "go to the correct page when Yes is selected for bonusPayments and Yes is entered for bonusPaymentsMoreThisYear" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),
        mockAuthConnector = Some(auth))
      val result = testController.handleBonusPayments()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("bonusPayments" -> "Yes", "bonusPaymentsMoreThisYear" -> "No"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/update-income/bonus-overtime-amount"

    }

  }

  "Calling the handleBonusOvertimeAmount method" should {

    "go to the start of the update income journey if the income id is not set in Session" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = None)))
      val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("amount" -> "£3,000"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"
    }


    "go to the start of the update Income journey if the income id is not the same as that in session " in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(13))))
      val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody( "amount" -> "£3,000"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"

    }


    "go to the same page if nothing has been entered for year " in {
      testInvalidBonusOvertimeAmountTitle("year","tai.bonusPaymentsAmount.year.title" )
    }

    "go to the same page if nothing has been entered for monthly " in {
      testInvalidBonusOvertimeAmountTitle("monthly", "tai.bonusPaymentsAmount.month.title")
    }

    "go to the same page if nothing has been entered for Weekly" in {
      testInvalidBonusOvertimeAmountTitle("weekly","tai.bonusPaymentsAmount.week.title" )
    }

    "go to the same page if nothing has been entered for fortnightly" in {
      testInvalidBonusOvertimeAmountTitle("fortnightly","tai.bonusPaymentsAmount.fortnightly.title" )
    }

    "go to the same page if nothing has been entered for other" in {
      testInvalidBonusOvertimeAmountTitle("other","tai.bonusPaymentsAmount.period.title" )
    }


    "go to the estimated pay page if data has been entered without error " in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val mockBonusPaymentsForm =  BonusPaymentsForm(bonusPayments = Some("Yes"), bonusPaymentsMoreThisYear = Some("No"))
      val payPeriodForm =  PayPeriodForm(payPeriod = Some("year"))
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14), bonusPaymentsForm = Some(mockBonusPaymentsForm), payPeriodForm = Some(payPeriodForm))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),mockAuthConnector = Some(auth))
      val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("amount" -> "£3,000"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/update-income/estimated-pay"

    }
  }

  def testInvalidBonusOvertimeAmountTitle(period : String, message : String) = {
    val testTaxSummary = TaiData.getBasicRateTaxSummary

    val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

    val mockBonusPaymentsForm =  BonusPaymentsForm(bonusPayments = Some("Yes"), bonusPaymentsMoreThisYear = Some("No"))
    val payPeriodForm =  PayPeriodForm(payPeriod = Some(period))
    val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14),
      bonusPaymentsForm = Some(mockBonusPaymentsForm), payPeriodForm = Some(payPeriodForm))) ,mockAuthConnector = Some(auth), mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST"))  )
    val result = testController.handleBonusOvertimeAmount()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())
    status(result) shouldBe 400

    val content = contentAsString(result)
    val doc = Jsoup.parse(content)
    doc.title() shouldBe Messages(message)
  }



  "Calling the payslipDeductionsPage method" should {

    "display the correct page for someone who put a correct value in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")) ,
        mockAuthConnector = Some(auth))
      val result = testController.payslipDeductionsPage()(RequestBuilder.buildFakeRequestWithAuth("GET").withFormUrlEncodedBody())

      status(result) shouldBe 200

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.title() shouldBe "Payslip deductions"
    }
  }

  "Calling the handlePayslipDeductions method" should {

    "go to the same page if incorrect data entered into form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = None)),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")) ,
        mockAuthConnector = Some(auth))
      val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())

      status(result) shouldBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.title() shouldBe "Payslip deductions"
    }

    "go to the start of the update Income journey if the income id is not set in Session" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = None)),mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")) ,
        mockAuthConnector = Some(auth))
      val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody( "payslipDeductions" -> "no" ))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"

    }

    "go to the start of the update Income journey if the income id is not the same as that in session " in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(13))),mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")) ,
        mockAuthConnector = Some(auth))
      val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody( "payslipDeductions" -> "no" ))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"
    }

    "display the correct page when nothing has been selected in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")) ,
        mockAuthConnector = Some(auth))
      val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())

      status(result) shouldBe 400

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.title() shouldBe "Payslip deductions"
    }

    "display the correct page when NO has been selected in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),mockAuthConnector = Some(auth))
      val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "No"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/update-income/bonus-payments"
    }

    "display the correct page when YES has been selected in the form" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),mockAuthConnector = Some(auth))
      val result = testController.handlePayslipDeductions()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payslipDeductions" -> "Yes"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/update-income/taxable-payslip-amount"
    }
  }

  "Calling the estimatedPayPage method" should {

    "go to the start of the update income journey if the income id is not set in Session" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = None)))
      val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"
    }

    "go to the start of the update Income journey if the income id is not the same as that in session " in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(13))))
      val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) shouldBe 303

      val redirectUrl = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => ""
      }
      redirectUrl shouldBe "/check-income-tax/your-income-calculation"
    }

    "go to the estimated pay page if all data is present" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14), netAmount = Some(80), grossAmount = Some(100))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),mockAuthConnector = Some(auth))
      val result = testController.estimatedPayPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) shouldBe 200

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.title() shouldBe "Your estimated pay for the year"

      val payBeforeTax = doc.getElementById("payBeforeTax").text()
      val amountPayTaxOn = doc.getElementById("amountPayTaxOn").text()

      payBeforeTax shouldBe "£100"
      amountPayTaxOn shouldBe "£80"
    }
  }

  "Calling the handleCalculationResult method" should {

    "go to the start of the update income journey if the income id is not set in Session" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = None)))
      val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/check-income-tax/your-income-calculation"
    }


    "go to the start of the update Income journey if the income id is not the same as that in session " in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(13))))
      val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/check-income-tax/your-income-calculation"

    }

    "go to the confirm income page if all data is present" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val auth = new FakeAuthConnector{override val nino = Some(testTaxSummary.nino + "A")}

      val testController = buildIncomeUpdateCalculatorController(testTaxSummary, mockIncomeCalculation = Some(IncomeCalculation(incomeId = Some(14), netAmount = Some(80), grossAmount = Some(100))),
        mockEditIncomeForm = Some(EditIncomeForm(employmentId = 14,newAmount = Some("1000"), name="TEST",description = "TEST")),mockAuthConnector = Some(auth)  )

      val result = testController.handleCalculationResult()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) shouldBe 200

      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      doc.title() shouldBe "Check your changes"

    }
  }


  def buildIncomeUpdateCalculatorController(
    mockDetails: TaxSummaryDetails,
    mockCachedDetails: Option[TaxSummaryDetails] = None,
    mockCalculatedAmount: BigDecimal = BigDecimal(0),
    mockIncomeCalculation : Option[IncomeCalculation] = None,
    mockEditIncomeForm : Option[EditIncomeForm] = None,
    mockAuthConnector : Option[FakeAuthConnector] = None) = new IncomeUpdateCalculatorController {
    
    override implicit def auditConnector: AuditConnector = AuditConnector
    override implicit def authConnector: AuthConnector = if(mockAuthConnector.isDefined){mockAuthConnector.get}else{FakeAuthConnector}
    override implicit def delegationConnector: DelegationConnector  = FrontEndDelegationConnector
    override def activityLoggerService : ActivityLoggerService = FakeActivityLoggerService
    override implicit def templateRenderer = MockTemplateRenderer
    override implicit def partialRetriever = MockPartialRetriever
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val employmentService: EmploymentService = mock[EmploymentService]

    override val taiService = mock[TaiService]

    val user = UserBuilder()

    val sessionData = SessionData(
          user.getNino,
          Some(user.taiRoot),
          mockDetails,
          mockEditIncomeForm,
          mockIncomeCalculation)

    when(taiService.taiSession(any(), any(), any())(any()))
      .thenReturn(Future.successful(sessionData))

    val employmentAmount = EmploymentAmount( name = "test1", description = "description",
      employmentId=14,
      newAmount=1675,
      oldAmount=11,
      worksNumber=None,
      startDate=None,
      endDate=None,
      isLive=true,
      isOccupationalPension=false)

    when(taiService.incomeForEdit(any(), any())(any())).thenReturn(Future.successful(Some(employmentAmount)))

    when(taiService.calculateEstimatedPay(any(), any())(any())).thenReturn(Future.successful(CalculatedPay(grossAnnualPay = mockIncomeCalculation.flatMap(_.grossAmount), netAnnualPay = mockIncomeCalculation.flatMap(_.netAmount))))

    when(taiService.updateTaiSession(any())(any())).thenReturn(Future.successful(sessionData))

    when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
  }
}
