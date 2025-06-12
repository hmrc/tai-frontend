/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.ControllerViewTestHelper
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import pages.income._
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.forms.income.incomeCalculator.{PayslipForm, TaxablePayslipForm}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.PayPeriodConstants.Monthly
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{PaySlipAmountViewModel, TaxablePaySlipAmountViewModel}
import utils.BaseSpec
import views.html.incomes.{PayslipAmountView, PayslipDeductionsView, TaxablePayslipAmountView}

import scala.concurrent.Future
import scala.util.Random

class IncomeUpdatePayslipAmountControllerSpec extends BaseSpec with ControllerViewTestHelper {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val sessionId              = "testSessionId"

  def randomNino(): Nino = new Generator(new Random()).nextNino
  def createSUT          = new SUT

  private val payslipAmountView                          = inject[PayslipAmountView]
  private val taxablePayslipAmountView                   = inject[TaxablePayslipAmountView]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  class SUT
      extends IncomeUpdatePayslipAmountController(
        mockAuthJourney,
        mcc,
        payslipAmountView,
        taxablePayslipAmountView,
        inject[PayslipDeductionsView],
        mockJourneyCacheRepository
      ) {
    when(mockJourneyCacheRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockJourneyCacheRepository)
  }

  "payslipAmountPage" must {

    "display payslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, Monthly)
          .setOrException(UpdateIncomeOtherInDaysPage, "12")
          .setOrException(UpdateIncomeTotalSalaryPage, "1234")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result =
          SUT.payslipAmountPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslip.title.month"))
      }

      "journey cache returns a prepopulated pay slip amount" in {
        val cachedAmount = Some("998787")
        val payPeriod    = Some(Monthly)

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, payPeriod.get)
          .setOrException(UpdateIncomeOtherInDaysPage, "12")
          .setOrException(UpdateIncomeTotalSalaryPage, cachedAmount.get)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        val result =
          SUT.payslipAmountPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        status(result) mustBe OK

        val expectedForm = PayslipForm
          .createForm(messages("tai.payslip.error.form.totalPay.input.mandatory"))
          .fill(PayslipForm(cachedAmount))

        val expectedViewModel = PaySlipAmountViewModel(expectedForm, payPeriod, None, employer)
        val expectedView      = payslipAmountView(expectedViewModel)

        result rendersTheSameViewAs expectedView
      }
    }

    "Redirect user to /income-summary" when {
      "there is no data in the cache" in {

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)

        val SUT                                                       = createSUT
        setup(mockUserAnswers)
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        val result = SUT.payslipAmountPage(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handlePayslipAmount" must {

    "redirect the user to payslipDeductionsPage page" when {
      "user entered valid pay" in {
        val salary = "£3,000"

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, Monthly)
          .setOrException(UpdateIncomeOtherInDaysPage, "12")
          .setOrException(UpdateIncomeTotalSalaryPage, "£3,000")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)
        when(mockJourneyCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handlePayslipAmount(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("totalSalary" -> salary)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage().url
        )
      }
    }

    "redirect user back to how to payslip page with an error form" when {
      "user input has error" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, Monthly)
          .setOrException(UpdateIncomeOtherInDaysPage, "12")
          .setOrException(UpdateIncomeTotalSalaryPage, "£3,000")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(false)
        when(mockJourneyCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handlePayslipAmount(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().text must include(messages("tai.income.error.form.summary"))
        doc.body().text must include(messages("tai.payslip.title.month"))
        doc.title()     must include(messages("tai.payslip.title.month"))
      }
    }
  }

  "taxablePayslipAmountPage" must {
    "display taxablePayslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {
        val cachedAmount = Some("5000")
        val payPeriod    = Some(Monthly)

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, payPeriod.get)
          .setOrException(UpdateIncomeTaxablePayPage, cachedAmount.get)
          .setOrException(UpdateIncomeOtherInDaysPage, "12")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        val result =
          SUT.taxablePayslipAmountPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val expectedForm      = TaxablePayslipForm.createForm().fill(TaxablePayslipForm(cachedAmount))
        val expectedViewModel = TaxablePaySlipAmountViewModel(expectedForm, payPeriod, None, employer)
        result rendersTheSameViewAs taxablePayslipAmountView(expectedViewModel)
      }
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
        val SUT             = createSUT
        setup(mockUserAnswers)

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(None))

        val result =
          SUT.taxablePayslipAmountPage(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handleTaxablePayslipAmount" must {

    "redirect the user to bonusPaymentsPage page" when {
      "user entered valid taxable pay" in {
        val cachedAmount = Some("4000")
        val payPeriod    = Some(Monthly)

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, payPeriod.get)
          .setOrException(UpdateIncomeTotalSalaryPage, cachedAmount.get)
          .setOrException(UpdateIncomeTaxablePayPage, "3000")
          .setOrException(UpdateIncomeOtherInDaysPage, "12")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.handleTaxablePayslipAmount(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("taxablePay" -> "3000")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url
        )
      }
    }

    "redirect user back to how to taxablePayslip page" when {
      "user input has error" in {
        val cachedAmount = Some("4000")
        val payPeriod    = Some(Monthly)

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayPeriodPage, payPeriod.get)
          .setOrException(UpdateIncomeTotalSalaryPage, cachedAmount.get)
          .setOrException(UpdateIncomeTaxablePayPage, "3000")
          .setOrException(UpdateIncomeOtherInDaysPage, "12")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
        when(mockJourneyCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handleTaxablePayslipAmount(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.taxablePayslip.title.month"))
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {

        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handleTaxablePayslipAmount(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "payslipDeductionsPage" must {
    "display payslipDeductions" when {
      "journey cache returns employment name and id" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayslipDeductionsPage, "Yes")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result =
          SUT.payslipDeductionsPage(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }

  "handlePayslipDeductions" must {

    "redirect the user to taxablePayslipAmountPage page" when {
      "user selected yes" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayslipDeductionsPage, "Yes")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        when(mockJourneyCacheRepository.clear(any(), any())) thenReturn Future.successful(true)

        val result = SUT.handlePayslipDeductions(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payslipDeductions" -> "Yes")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController
            .taxablePayslipAmountPage()
            .url
        )
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayslipDeductionsPage, "No")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        when(mockJourneyCacheRepository.clear(any(), any())) thenReturn Future.successful(true)

        val result = SUT.handlePayslipDeductions(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("payslipDeductions" -> "No")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url
        )
      }
    }

    "redirect user back to how to payslipDeductions page" when {
      "user input has error" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeIdPage, employer.id)
          .setOrException(UpdateIncomeNamePage, employer.name)
          .setOrException(UpdateIncomePayslipDeductionsPage, "No")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handlePayslipDeductions(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }
}
