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

import org.apache.pekko.Done
import builders.RequestBuilder
import controllers.ControllerViewTestHelper
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.forms.income.incomeCalculator.{PayslipForm, TaxablePayslipForm}
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.PayPeriodConstants.Monthly
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{PaySlipAmountViewModel, TaxablePaySlipAmountViewModel}
import utils.BaseSpec
import views.html.incomes.{PayslipAmountView, PayslipDeductionsView, TaxablePayslipAmountView}

import scala.concurrent.Future

class IncomeUpdatePayslipAmountControllerSpec extends BaseSpec with ControllerViewTestHelper {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  private val payslipAmountView = inject[PayslipAmountView]
  private val taxablePayslipAmountView = inject[TaxablePayslipAmountView]

  class TestIncomeUpdatePayslipAmountController
      extends IncomeUpdatePayslipAmountController(
        mockAuthJourney,
        mcc,
        payslipAmountView,
        taxablePayslipAmountView,
        inject[PayslipDeductionsView],
        journeyCacheService
      ) {
    when(journeyCacheService.mandatoryJourneyValues(any())(any(), any()))
      .thenReturn(Future.successful(Right(Seq(employer.id.toString, employer.name))))
  }

  "payslipAmountPage" must {
    object PayslipAmountPageHarness {
      sealed class PayslipAmountPageHarness(payPeriod: Option[String], cachedAmount: Option[String]) {

        when(journeyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(
            Future.successful(
              Right(
                (
                  Seq[String](employer.id.toString, employer.name),
                  Seq[Option[String]](payPeriod, None, cachedAmount)
                )
              )
            )
          )

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
        val payPeriod = Some(Monthly)

        val result = PayslipAmountPageHarness
          .setup(payPeriod, cachedAmount)
          .payslipAmountPage(RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.payslip.title.month"))
      }

      "journey cache returns a prepopulated pay slip amount" in {
        val cachedAmount = Some("998787")
        val payPeriod = Some(Monthly)

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        val result = PayslipAmountPageHarness
          .setup(payPeriod, cachedAmount)
          .payslipAmountPage(request)

        status(result) mustBe OK

        val expectedForm = PayslipForm
          .createForm(messages("tai.payslip.error.form.totalPay.input.mandatory"))
          .fill(PayslipForm(cachedAmount))

        val expectedViewModel = PaySlipAmountViewModel(expectedForm, payPeriod, None, employer)
        val expectedView = payslipAmountView(expectedViewModel)

        result rendersTheSameViewAs expectedView
      }
    }

    "Redirect user to /income-summary" when {
      "there is no data in the cache" in {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        val cachedAmount = None
        val payPeriod = None

        val controller = PayslipAmountPageHarness
          .setup(payPeriod, cachedAmount)

        when(journeyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(
            Future.successful(Left("failed"))
          )

        val result = controller.payslipAmountPage(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handlePayslipAmount" must {
    object HandlePayslipAmountHarness {
      sealed class HandlePayslipAmountHarness(salary: String) {
        when(journeyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some(Monthly), None)))
        when(
          journeyCacheService.cache(
            meq(Map(UpdateIncomeConstants.TotalSalaryKey -> salary))
          )(any())
        )
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
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage().url
        )
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

        val mandatoryKeys: Seq[String] = Seq(UpdateIncomeConstants.IdKey, UpdateIncomeConstants.NameKey)
        val optionalKeys: Seq[String] = Seq(
          UpdateIncomeConstants.PayPeriodKey,
          UpdateIncomeConstants.OtherInDaysKey,
          UpdateIncomeConstants.TaxablePayKey
        )

        when(journeyCacheService.collectedJourneyValues(meq(mandatoryKeys), meq(optionalKeys))(any(), any()))
          .thenReturn(
            Future.successful(
              Right(
                (
                  Seq[String](employer.id.toString, employer.name),
                  Seq[Option[String]](payPeriod, None, cachedAmount)
                )
              )
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

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        val cachedAmount = Some("9888787")
        val payPeriod = Some(Monthly)

        val result = TaxablePayslipAmountPageHarness
          .setup(payPeriod, cachedAmount)
          .taxablePayslipAmountPage(request)

        status(result) mustBe OK

        val expectedForm = TaxablePayslipForm.createForm().fill(TaxablePayslipForm(cachedAmount))
        val expectedViewModel = TaxablePaySlipAmountViewModel(expectedForm, payPeriod, None, employer)
        result rendersTheSameViewAs taxablePayslipAmountView(expectedViewModel)
      }
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        val controller = new TestIncomeUpdatePayslipAmountController

        when(journeyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(Future.successful(Left("failed")))

        val result = controller.taxablePayslipAmountPage(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handleTaxablePayslipAmount" must {
    object HandleTaxablePayslipAmountPageHarness {
      sealed class HandleTaxablePayslipAmountPageHarness() {

        when(journeyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some(Monthly), None, Some("4000"))))

        when(
          journeyCacheService
            .cache(meq[Map[String, String]](Map(UpdateIncomeConstants.TaxablePayKey -> "3000")))(any())
        )
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(
          Future.successful(
            Right((Seq[String](employer.id.toString, employer.name), Seq[Option[String]](Some(Monthly), None)))
          )
        )

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
          controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url
        )
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

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {
        val controller = new TestIncomeUpdatePayslipAmountController

        when(journeyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Left("")))

        val result = controller.handleTaxablePayslipAmount(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "payslipDeductionsPage" must {
    object PayslipDeductionsPageHarness {
      sealed class PayslipDeductionsPageHarness() {

        when(journeyCacheService.currentValue(meq(UpdateIncomeConstants.PayslipDeductionsKey))(any()))
          .thenReturn(Future.successful(Some("Yes")))

        def payslipDeductionsPage(): Future[Result] =
          new TestIncomeUpdatePayslipAmountController()
            .payslipDeductionsPage()(RequestBuilder.buildFakeGetRequestWithAuth())
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
          .thenReturn(Future.successful(Done))

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
            .url
        )
      }
    }

    "redirect the user to bonusPaymentsPage page" when {
      "user selected no" in {
        val result = HandlePayslipDeductionsHarness
          .setup()
          .handlePayslipDeductions(RequestBuilder.buildFakePostRequestWithAuth("payslipDeductions" -> "No"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url
        )
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
