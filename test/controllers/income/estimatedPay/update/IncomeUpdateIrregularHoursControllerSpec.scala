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
import controllers.ErrorPagesHandler
import org.apache.pekko.Done
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import pages.income._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.TaiConstants.MonthAndYear
import utils.BaseSpec
import views.html.incomes.{ConfirmAmountEnteredView, EditIncomeIrregularHoursView, EditSuccessView}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import scala.util.Random

class IncomeUpdateIrregularHoursControllerSpec extends BaseSpec {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val sessionId = "testSessionId"

  def randomNino(): Nino = new Generator(new Random()).nextNino
  def createSUT = new SUT

  val incomeService: IncomeService = mock[IncomeService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  class SUT
      extends IncomeUpdateIrregularHoursController(
        mockAuthJourney,
        incomeService,
        taxAccountService,
        mcc,
        inject[EditSuccessView],
        inject[EditIncomeIrregularHoursView],
        inject[ConfirmAmountEnteredView],
        mockJourneyCacheRepository,
        inject[ErrorPagesHandler]
      ) {
    when(mockJourneyCacheRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheRepository)

    when(incomeService.latestPayment(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(Payment(LocalDate.now().minusDays(1), 0, 0, 0, 0, 0, 0, Monthly))))
    when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
      .thenReturn(Future.successful(Right(None: Option[TaxCodeIncome])))
  }

  def BuildTaxCodeIncomes(incomeCount: Int): Seq[TaxCodeIncome] = {

    val taxCodeIncome1 =
      TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
    val taxCodeIncome2 =
      TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "pension", Week1Month1BasisOfOperation, Live)

    incomeCount match {
      case 2 => Seq(taxCodeIncome1, taxCodeIncome2)
      case 1 => Seq(taxCodeIncome1)
      case 0 => Nil
    }
  }

  def editIncomeIrregularHours(
    incomeNumber: Int,
    request: FakeRequest[AnyContentAsFormUrlEncoded]
  ): Future[Result] =
    new SUT()
      .editIncomeIrregularHours(incomeNumber)(request)

  "editIncomeIrregularHours" must {
    object EditIncomeIrregularHoursHarness {

      sealed class EditIncomeIrregularHoursHarness(taxCodeIncome: Option[TaxCodeIncome]) {

        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Payment(LocalDate.now().minusDays(1), 0, 0, 0, 0, 0, 0, Monthly))))

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncome)))

        def editIncomeIrregularHours(
          incomeNumber: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]
        ): Future[Result] =
          new SUT()
            .editIncomeIrregularHours(incomeNumber)(request)
      }

      def setup(taxCodeIncome: Option[TaxCodeIncome]): EditIncomeIrregularHoursHarness =
        new EditIncomeIrregularHoursHarness(taxCodeIncome)
    }
    "respond with OK and show the irregular hours edit page" in {
      reset(mockJourneyCacheRepository)

      val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeIrregularAnnualPayPage, "123")

      setup(mockUserAnswers)

      BuildTaxCodeIncomes(1)

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

        when(taxAccountService.taxCodeIncomeForEmployment(any(), any(), any())(any()))
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
        reset(mockJourneyCacheRepository)

        val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeNamePage, "name")
          .setOrException(UpdateIncomePayToDatePage, "123")
          .setOrException(UpdatedIncomeDatePage, LocalDate.now().format(DateTimeFormatter.ofPattern(MonthAndYear)))

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        setup(mockUserAnswers)

        def handleIncomeIrregularHours(
          employmentId: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]
        ): Future[Result] =
          new SUT()
            .handleIncomeIrregularHours(employmentId)(request)
      }

      def harnessSetup(): HandleIncomeIrregularHoursHarness =
        new HandleIncomeIrregularHoursHarness()
    }
    "respond with Redirect to Confirm page" in {

      val result = HandleIncomeIrregularHoursHarness
        .harnessSetup()
        .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth("income" -> "999"))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(
        routes.IncomeUpdateIrregularHoursController.confirmIncomeIrregularHours(1).url
      )
    }

    "respond with BAD_REQUEST" when {
      "given an input which is less than the current amount" in {
        val result = HandleIncomeIrregularHoursHarness
          .harnessSetup()
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
          .harnessSetup()
          .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth("income" -> "ABC"))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))

        doc.body().text must include(messages("tai.irregular.instruction.wholePounds"))
      }

      "given invalid form data of no input" in {
        val result = HandleIncomeIrregularHoursHarness
          .harnessSetup()
          .handleIncomeIrregularHours(1, RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.irregular.heading"))
        doc.body().text must include(messages("tai.irregular.error.blankValue"))

      }

      "given invalid form data of more than 9 numbers" in {
        val result = HandleIncomeIrregularHoursHarness
          .harnessSetup()
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
        newIrregularPay: Int,
        confirmedNewAmount: Int,
        payToDate: Int,
        isEmptyCache: Boolean
      ) {

        val defaultUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
        val mockUserAnswers: UserAnswers = if (isEmptyCache) {
          defaultUserAnswers
        } else {
          defaultUserAnswers
            .setOrException(UpdateIncomeNamePage, employer.name)
            .setOrException(UpdateIncomeIrregularAnnualPayPage, newIrregularPay.toString)
            .setOrException(UpdateIncomePayToDatePage, payToDate.toString)
            .setOrException(UpdateIncomeConfirmedNewAmountPage(1), confirmedNewAmount.toString)
        }

        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        def confirmIncomeIrregularHours(
          employmentId: Int,
          request: FakeRequest[AnyContentAsFormUrlEncoded]
        ): Future[Result] = new SUT().confirmIncomeIrregularHours(employmentId)(request)

      }

      def harnessSetup(
        newIrregularPay: Int = 1235,
        confirmedNewAmount: Int = 1234,
        payToDate: Int = 123,
        isEmptyCache: Boolean = false
      ): ConfirmIncomeIrregularHoursHarness =
        new ConfirmIncomeIrregularHoursHarness(newIrregularPay, confirmedNewAmount, payToDate, isEmptyCache)
    }

    "respond with Ok" in {

      val result = ConfirmIncomeIrregularHoursHarness
        .harnessSetup()
        .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.confirm.save.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
    }

    "redirect to SameEstimatedPayPage" when {
      "the same amount of pay has been entered" in {
        val amount = 123

        val result = ConfirmIncomeIrregularHoursHarness
          .harnessSetup(confirmedNewAmount = amount, newIrregularPay = amount)
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
          .harnessSetup(newIrregularPay = 123)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }

    "redirect to /income-details" when {
      "no value is present in the cache" in {
        val result = ConfirmIncomeIrregularHoursHarness
          .harnessSetup(isEmptyCache = true)
          .confirmIncomeIrregularHours(1, RequestBuilder.buildFakeGetRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employer.id).url
        )
      }
    }
  }

  "submitIncomeIrregularHours" must {

    "respond with INTERNAL_SERVER_ERROR when mandatory values are missing" in {

      val userAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)

      setup(userAnswers)

      val result = new SUT().submitIncomeIrregularHours(1)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "sends Ok on successful submit" in {

      val userAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeIrregularAnnualPayPage, "123")

      setup(userAnswers)

      when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

      val result = new SUT().submitIncomeIrregularHours(1)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      doc.title() must include(messages("tai.incomes.updated.check.title", employer.name))
    }
  }

}
