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
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{PaySlipAmountViewModel, TaxablePaySlipAmountViewModel}
import views.html.incomes.{payslipAmount, taxablePayslipAmount}

import scala.concurrent.Future

class IncomeUpdatePayslipAmountControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with EditIncomePayPeriodConstants
    with ControllerViewTestHelper with JourneyCacheConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val employer = IncomeSource(id = 1, name = "sample employer")

  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  class TestIncomeUpdatePayslipAmountController
      extends IncomeUpdatePayslipAmountController(
        FakeAuthAction,
        FakeValidatePerson,
        journeyCacheService,
        mock[FormPartialRetriever],
        MockTemplateRenderer) {
    when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
      .thenReturn(Future.successful(employer.id))
    when(journeyCacheService.mandatoryValue(Matchers.eq(UpdateIncome_NameKey))(any()))
      .thenReturn(Future.successful(employer.name))
  }

  "payslipAmountPage" must {
    object PayslipAmountPageHarness {
      sealed class PayslipAmountPageHarness(payPeriod: Option[String], cachedAmount: Option[String]) {
        when(journeyCacheService.collectedValues(any(), any())(any()))
          .thenReturn(
            Future.successful(
              Seq[String](employer.id.toString, employer.name),
              Seq[Option[String]](payPeriod, None, cachedAmount)))

        def payslipAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdatePayslipAmountController()
            .payslipAmountPage()(request)
      }

      def setup(payPeriod: Option[String], cachedAmount: Option[String]): PayslipAmountPageHarness =
        new PayslipAmountPageHarness(payPeriod, cachedAmount)
    }

    "display payslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {

        val cachedAmount = None
        val payPeriod = Some(MONTHLY)

        val result = PayslipAmountPageHarness
          .setup(payPeriod, cachedAmount)
          .payslipAmountPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslip.title.month"))
      }

      "journey cache returns a prepopulated pay slip amount" in {
        val cachedAmount = Some("998787")
        val payPeriod = Some(MONTHLY)

        implicit val request = RequestBuilder.buildFakeGetRequestWithAuth()

        val result = PayslipAmountPageHarness
          .setup(payPeriod, cachedAmount)
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

        when(
          journeyCacheService.cache(Matchers.eq[Map[String, String]](Map(UpdateIncome_TotalSalaryKey -> salary)))(
            any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        def handlePayslipAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdatePayslipAmountController()
            .handlePayslipAmount()(request)
      }

      def setup(salary: String = ""): HandlePayslipAmountHarness =
        new HandlePayslipAmountHarness(salary)
    }

    "redirect the user to payslipDeductionsPage page" when {
      "user entered valid pay" in {

        val salary = "£3,000"
        val result = HandlePayslipAmountHarness
          .setup(salary)
          .handlePayslipAmount(RequestBuilder.buildFakePostRequestWithAuth("totalSalary" -> salary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage().url)
      }
    }

    "redirect user back to how to payslip page with an error form" when {
      "user input has error" in {

        val result = HandlePayslipAmountHarness
          .setup()
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

        def taxablePayslipAmountPage(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdatePayslipAmountController()
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

        val result = TaxablePayslipAmountPageHarness
          .setup(payPeriod, cachedAmount)
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

        when(journeyCacheService.cache(eqTo[Map[String, String]](Map(UpdateIncome_TaxablePayKey -> "3000")))(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(
          (Seq[String](employer.id.toString, employer.name), Seq[Option[String]](Some(MONTHLY), None))))

        def handleTaxablePayslipAmount(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdatePayslipAmountController()
            .handleTaxablePayslipAmount()(request)
      }

      def setup(): HandleTaxablePayslipAmountPageHarness =
        new HandleTaxablePayslipAmountPageHarness()
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user entered valid taxable pay" in {

        val result = HandleTaxablePayslipAmountPageHarness
          .setup()
          .handleTaxablePayslipAmount(RequestBuilder.buildFakePostRequestWithAuth("taxablePay" -> "3000"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to taxablePayslip page" when {
      "user input has error" in {

        val result = HandleTaxablePayslipAmountPageHarness
          .setup()
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

        def payslipDeductionsPage(): Future[Result] =
          new TestIncomeUpdatePayslipAmountController()
            .payslipDeductionsPage()(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(): PayslipDeductionsPageHarness =
        new PayslipDeductionsPageHarness()
    }
    "display payslipDeductions" when {
      "journey cache returns employment name and id" in {
        val result = PayslipDeductionsPageHarness
          .setup()
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
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.flush()(any()))
          .thenReturn(Future.successful(TaiSuccessResponse))

        def handlePayslipDeductions(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdatePayslipAmountController()
            .handlePayslipDeductions()(request)
      }

      def setup(): HandlePayslipDeductionsHarness =
        new HandlePayslipDeductionsHarness()
    }

    "redirect the user to taxablePayslipAmountPage page" when {
      "user selected yes" in {

        val result = HandlePayslipDeductionsHarness
          .setup()
          .handlePayslipDeductions(RequestBuilder.buildFakePostRequestWithAuth("payslipDeductions" -> "Yes"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController
            .taxablePayslipAmountPage()
            .url)
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val result = HandlePayslipDeductionsHarness
          .setup()
          .handlePayslipDeductions(RequestBuilder.buildFakePostRequestWithAuth("payslipDeductions" -> "No"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url)
      }
    }

    "redirect user back to how to payslipDeductions page" when {
      "user input has error" in {
        val result = HandlePayslipDeductionsHarness
          .setup()
          .handlePayslipDeductions(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslipDeductions.title"))
      }
    }
  }
}
