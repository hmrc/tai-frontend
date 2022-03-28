/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.{ErrorPagesHandler, FakeAuthAction}
import mocks.MockTemplateRenderer

import java.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiUnauthorisedResponse}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.TaiConstants.MONTH_AND_YEAR
import uk.gov.hmrc.tai.util.constants._
import utils.BaseSpec
import views.html.incomes.{ConfirmAmountEnteredView, EditIncomeIrregularHoursView, EditSuccessView}

import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class IncomeUpdateIrregularHoursControllerSpec extends BaseSpec with JourneyCacheConstants {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")

  val incomeService: IncomeService = mock[IncomeService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService =
    mock[EstimatedPayJourneyCompletionService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  class TestIncomeUpdateIrregularHoursController
      extends IncomeUpdateIrregularHoursController(
        FakeAuthAction,
        FakeValidatePerson,
        incomeService,
        taxAccountService,
        estimatedPayJourneyCompletionService,
        mcc,
        inject[EditSuccessView],
        inject[EditIncomeIrregularHoursView],
        inject[ConfirmAmountEnteredView],
        journeyCacheService,
        MockTemplateRenderer,
        inject[ErrorPagesHandler]
      ) {
    when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
      .thenReturn(Future.successful(Right(employer.id)))
    when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncome_NameKey))(any()))
      .thenReturn(Future.successful(Right(employer.name)))
  }

  "editIncomeIrregularHours" must {
    object EditIncomeIrregularHoursHarness {

      sealed class EditIncomeIrregularHoursHarness(taxCodeIncome: Option[TaxCodeIncome]) {

        when(incomeService.latestPayment(any(), any())(any()))
          .thenReturn(Future.successful(Some(Payment(LocalDate.now().minusDays(1), 0, 0, 0, 0, 0, 0, Monthly))))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncome)))

        def editIncomeIrregularHours(
          incomeNumber: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateIrregularHoursController()
            .editIncomeIrregularHours(incomeNumber)(request)
      }

      def setup(taxCodeIncome: Option[TaxCodeIncome]): EditIncomeIrregularHoursHarness =
        new EditIncomeIrregularHoursHarness(taxCodeIncome)
    }
    "respond with OK and show the irregular hours edit page" in {

      val result = EditIncomeIrregularHoursHarness
        .setup(Some(
          TaxCodeIncome(EmploymentIncome, Some(1), 123, "description", "taxCode", "name", OtherBasisOfOperation, Live)))
        .editIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.irregular.heading"))
    }

    "respond with INTERNAL_SERVER_ERROR" when {
      "the employment income cannot be found" in {

        val result = EditIncomeIrregularHoursHarness
          .setup(Option.empty[TaxCodeIncome])
          .editIncomeIrregularHours(2, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to unauthorised page" when {
      "there is an unauthorised response" in {

        val service = EditIncomeIrregularHoursHarness.setup(Option.empty[TaxCodeIncome])

        when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(TaiUnauthorisedResponse("error"))))
        val result = service.editIncomeIrregularHours(2, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }
    }
  }

  "handleIncomeIrregularHours" must {
    object HandleIncomeIrregularHoursHarness {

      sealed class HandleIncomeIrregularHoursHarness() {

        val cacheMap = Map(
          UpdateIncome_NameKey      -> "name",
          UpdateIncome_PayToDateKey -> "123",
          UpdateIncome_DateKey      -> LocalDate.now().format(DateTimeFormatter.ofPattern(MONTH_AND_YEAR))
        )

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.mandatoryJourneyValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(Right(Seq("name", "123"))))

        when(journeyCacheService.cache(eqTo(UpdateIncome_IrregularAnnualPayKey), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(cacheMap))

        def handleIncomeIrregularHours(
          employmentId: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateIrregularHoursController()
            .handleIncomeIrregularHours(employmentId)(request)
      }

      def setup(): HandleIncomeIrregularHoursHarness =
        new HandleIncomeIrregularHoursHarness()
    }
    "respond with Redirect to Confirm page" in {
      val result = HandleIncomeIrregularHoursHarness
        .setup()
        .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth("income" -> "999"))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(
        routes.IncomeUpdateIrregularHoursController.confirmIncomeIrregularHours(1).url)
    }

    "respond with BAD_REQUEST" when {
      "given an input which is less than the current amount" in {
        val result = HandleIncomeIrregularHoursHarness
          .setup()
          .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth("income" -> "122"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(
          messages(
            "tai.irregular.error.error.incorrectTaxableIncome",
            123,
            LocalDate.now().format(DateTimeFormatter.ofPattern(MONTH_AND_YEAR)),
            "name"))
      }

      "given invalid form data of invalid currency" in {
        val result = HandleIncomeIrregularHoursHarness
          .setup()
          .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth("income" -> "ABC"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(messages("tai.irregular.instruction.wholePounds"))
      }

      "given invalid form data of no input" in {
        val result = HandleIncomeIrregularHoursHarness
          .setup()
          .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("tai.irregular.error.blankValue"))

      }

      "given invalid form data of more than 9 numbers" in {
        val result = HandleIncomeIrregularHoursHarness
          .setup()
          .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth("income" -> "1234567890"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("error.tai.updateDataEmployment.maxLength"))

      }
    }
  }

  "confirmIncomeIrregularHours" must {
    object ConfirmIncomeIrregularHoursHarness {

      sealed class ConfirmIncomeIrregularHoursHarness(
        failure: Boolean,
        newAmount: Int,
        confirmedNewAmount: Int,
        payToDate: Int) {

        val future: Future[Either[String, (Seq[String], Seq[Option[String]])]] =
          if (failure) {
            Future.successful(Left("Error"))
          } else {
            Future.successful(
              Right(Seq(employer.name, newAmount.toString, payToDate.toString), Seq(Some(confirmedNewAmount.toString))))
          }

        when(journeyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(future)

        def confirmIncomeIrregularHours(
          employmentId: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateIrregularHoursController()
            .confirmIncomeIrregularHours(employmentId)(request)
      }

      def setup(
        failure: Boolean = false,
        newAmount: Int = 1235,
        confirmedNewAmount: Int = 1234,
        payToDate: Int = 123): ConfirmIncomeIrregularHoursHarness =
        new ConfirmIncomeIrregularHoursHarness(failure, newAmount, confirmedNewAmount, payToDate)
    }

    "respond with Ok" in {

      val result = ConfirmIncomeIrregularHoursHarness
        .setup()
        .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRange))
    }

    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {

      val result = ConfirmIncomeIrregularHoursHarness
        .setup(failure = true)
        .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "redirect to SameEstimatedPayPage" when {
      "the same amount of pay has been entered" in {

        val result = ConfirmIncomeIrregularHoursHarness
          .setup(confirmedNewAmount = 123, newAmount = 123)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameEstimatedPayInCache().url)
      }
    }

    "redirect to IrregularSameEstimatedPayPage" when {
      "the same amount of payment to date has been entered" in {
        val result = ConfirmIncomeIrregularHoursHarness
          .setup(newAmount = 123)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }
  }

  "submitIncomeIrregularHours" must {
    object SubmitIncomeIrregularHoursHarness {

      sealed class SubmitIncomeIrregularHoursHarness(mandatoryValuesFuture: Future[Either[String, Seq[String]]]) {

        when(
          journeyCacheService.mandatoryJourneyValues(any())(any())
        ).thenReturn(mandatoryValuesFuture)
        when(
          taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())
        ).thenReturn(
          Future.successful(TaiSuccessResponse)
        )
        when(estimatedPayJourneyCompletionService.journeyCompleted(Matchers.eq(employer.id.toString))(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        def submitIncomeIrregularHours(
          employmentId: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateIrregularHoursController()
            .submitIncomeIrregularHours(employmentId)(request)
      }

      def setup(mandatoryValuesFuture: Future[Either[String, Seq[String]]]): SubmitIncomeIrregularHoursHarness =
        new SubmitIncomeIrregularHoursHarness(mandatoryValuesFuture)
    }
    "respond with INTERNAL_SERVER_ERROR for failed request to cache" in {

      val result: Future[Result] = SubmitIncomeIrregularHoursHarness
        .setup(Future.failed(new Exception))
        .submitIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "sends Ok on successful submit" in {

      val result = SubmitIncomeIrregularHoursHarness
        .setup(Future.successful(Right(Seq(employer.name, "123", employer.id.toString))))
        .submitIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      doc.title() must include(messages("tai.incomes.updated.check.title", employer.name))
    }
  }

}
