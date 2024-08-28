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
import controllers.ErrorPagesHandler
import controllers.auth.{AuthedUser, DataRequest}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.stubbing.ScalaOngoingStubbing
import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsFormUrlEncoded, BodyParser, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.TaiConstants.MonthAndYear
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec
import views.html.incomes.{ConfirmAmountEnteredView, EditIncomeIrregularHoursView, EditSuccessView}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class IncomeUpdateIrregularHoursControllerSpec extends BaseSpec {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val sessionId = "testSessionId"

  def randomNino(): Nino = new Generator(new Random()).nextNino
  def createSUT = new SUT

  val incomeService: IncomeService = mock[IncomeService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  class SUT
    extends IncomeUpdateIrregularHoursController(
      mockAuthJourney,
      incomeService,
      taxAccountService,
      mcc,
      inject[EditSuccessView],
      inject[EditIncomeIrregularHoursView],
      inject[ConfirmAmountEnteredView],
      mockJourneyCacheNewRepository,
      inject[ErrorPagesHandler]
    ) {
    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  private def setup(ua: UserAnswers): ScalaOngoingStubbing[ActionBuilder[DataRequest, AnyContent]] =
    when(mockAuthJourney.authWithDataRetrieval) thenReturn new ActionBuilder[DataRequest, AnyContent] {
      override def invokeBlock[A](
                                   request: Request[A],
                                   block: DataRequest[A] => Future[Result]
                                 ): Future[Result] =
        block(
          DataRequest(
            request,
            taiUser = AuthedUser(
              Nino(nino.toString()),
              Some("saUtr"),
              None
            ),
            fullName = "",
            userAnswers = ua
          )
        )

      override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = ec
    }

  private val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "pension", Week1Month1BasisOfOperation, Live)
  )
  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheNewRepository)

    when(incomeService.latestPayment(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(Payment(LocalDate.now().minusDays(1), 0, 0, 0, 0, 0, 0, Monthly))))
    when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(Right(taxCodeIncomes)))
  }

  private val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "pension", Week1Month1BasisOfOperation, Live)
  )

  "editIncomeIrregularHours" must {
    "respond with OK and show the irregular hours edit page" in {

      val result = EditIncomeIrregularHoursHarness
        .setup(
          Some(
            TaxCodeIncome(EmploymentIncome, Some(1), 123, "description", "taxCode", "name", OtherBasisOfOperation, Live)
          )
        )
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

        when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left("error")))
        val result = service.editIncomeIrregularHours(2, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
      }
    }
  }

  "handleIncomeIrregularHours" must {
    object HandleIncomeIrregularHoursHarness {

      sealed class HandleIncomeIrregularHoursHarness() {

        val cacheMap: Map[String, String] = Map(
          UpdateIncomeConstants.NameKey      -> "name",
          UpdateIncomeConstants.PayToDateKey -> "123",
          UpdateIncomeConstants.DateKey      -> LocalDate.now().format(DateTimeFormatter.ofPattern(MonthAndYear))
        )

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq("name", "123"))))

        when(journeyCacheService.cache(meq(UpdateIncomeConstants.IrregularAnnualPayKey), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(cacheMap))

        def handleIncomeIrregularHours(
          employmentId: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]
        ): Future[Result] =
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
        routes.IncomeUpdateIrregularHoursController.confirmIncomeIrregularHours(1).url
      )
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
            LocalDate.now().format(DateTimeFormatter.ofPattern(MonthAndYear)),
            "name"
          )
        )
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
        payToDate: Int,
        futureFailed: Boolean = false
      ) {

        val future: Future[Either[String, (Seq[String], Seq[Option[String]])]] =
          if (futureFailed) {
            Future.failed(new RuntimeException("failed"))
          } else if (failure) {
            Future.successful(Left("Error"))
          } else {
            Future.successful(
              Right(
                (Seq(employer.name, newAmount.toString, payToDate.toString), Seq(Some(confirmedNewAmount.toString)))
              )
            )
          }

        when(journeyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(future)

        def confirmIncomeIrregularHours(
          employmentId: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]
        ): Future[Result] =
          new TestIncomeUpdateIrregularHoursController()
            .confirmIncomeIrregularHours(employmentId)(request)
      }

      def setup(
        failure: Boolean = false,
        newAmount: Int = 1235,
        confirmedNewAmount: Int = 1234,
        payToDate: Int = 123,
        futureFailed: Boolean = false
      ): ConfirmIncomeIrregularHoursHarness =
        new ConfirmIncomeIrregularHoursHarness(failure, newAmount, confirmedNewAmount, payToDate, futureFailed)
    }

    "respond with Ok" in {

      val result = ConfirmIncomeIrregularHoursHarness
        .setup()
        .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
    }

    "respond with INTERNAL_SERVER_ERROR for a future failed when we call the cache" in {

      val result = ConfirmIncomeIrregularHoursHarness
        .setup(futureFailed = true)
        .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "redirect to SameEstimatedPayPage" when {
      "the same amount of pay has been entered" in {

        val result = ConfirmIncomeIrregularHoursHarness
          .setup(confirmedNewAmount = 123, newAmount = 123)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeController.sameEstimatedPayInCache(employer.id).url
        )
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

    "redirect to /income-details" when {
      "no value is present in the cache" in {
        val result = ConfirmIncomeIrregularHoursHarness
          .setup(failure = true)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
        )
      }
    }
  }

  "submitIncomeIrregularHours" must {
    object SubmitIncomeIrregularHoursHarness {

      sealed class SubmitIncomeIrregularHoursHarness(mandatoryValuesFuture: Future[Either[String, Seq[String]]]) {

        when(
          journeyCacheService.mandatoryJourneyValues(any())(any(), any())
        ).thenReturn(mandatoryValuesFuture)
        when(
          taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any())
        ).thenReturn(
          Future.successful(Done)
        )
        when(estimatedPayJourneyCompletionService.journeyCompleted(meq(employer.id.toString))(any(), any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        when(journeyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        def submitIncomeIrregularHours(
          employmentId: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]
        ): Future[Result] =
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
