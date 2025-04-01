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

package controllers

import builders.RequestBuilder
import org.apache.pekko.Done
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import pages.income._
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.TaiConstants
import utils.BaseSpec
import views.html.incomes._

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class IncomeControllerSpec extends BaseSpec with I18nSupport {

  val incomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val personService: PersonService = mock[PersonService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  val baseUserAnswers: UserAnswers = UserAnswers("testSessionId", nino.nino)

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(baseUserAnswers)
    reset(incomeService, mockJourneyCacheRepository)
  }

  val payToDate = "100"
  val employerId = 1
  val cachedUpdateIncomeNewAmount: String = "700"

  def employmentWithAccounts(accounts: List[AnnualAccount]): Employment =
    Employment(
      "ABCD",
      Live,
      Some("ABC123"),
      Some(LocalDate.of(2000, 5, 20)),
      None,
      accounts,
      "",
      "",
      8,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    )

  def paymentOnDate(date: LocalDate): Payment =
    Payment(
      date = date,
      amountYearToDate = 2000,
      taxAmountYearToDate = 200,
      nationalInsuranceAmountYearToDate = 100,
      amount = 1000,
      taxAmount = 100,
      nationalInsuranceAmount = 50,
      payFrequency = Monthly
    )

  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val employmentAmount: EmploymentAmount = EmploymentAmount(
    "employment",
    "(Current employer)",
    1,
    1111,
    1111,
    None,
    None,
    Some(LocalDate.of(2000, 5, 20)),
    None
  )

  private def createTestIncomeController() = new TestIncomeController()

  private val editSuccessView = inject[EditSuccessView]
  private val editPensionSuccessView = inject[EditPensionSuccessView]

  private class TestIncomeController()
      extends IncomeController(
        taxAccountService,
        employmentService,
        incomeService,
        mockAuthJourney,
        mcc,
        inject[ConfirmAmountEnteredView],
        editSuccessView,
        inject[EditPensionView],
        editPensionSuccessView,
        inject[EditIncomeView],
        inject[SameEstimatedPayView],
        mockJourneyCacheRepository,
        inject[ErrorPagesHandler]
      ) {

    def renderSuccess(employerName: String, employerId: Int): FakeRequest[_] => HtmlFormat.Appendable = {
      implicit request: FakeRequest[_] =>
        editSuccessView(employerName, employerId)
    }

    def renderPensionSuccess(employerName: String, employerId: Int): FakeRequest[_] => HtmlFormat.Appendable = {
      implicit request: FakeRequest[_] =>
        editPensionSuccessView(employerName, employerId)
    }

    val editIncomeForm: EditIncomeForm = EditIncomeForm("Test", "Test", 1, None, 10, None, None, None, None)
  }

  "cancel" must {
    "clear the data from JourneyCacheRepository and redirect to the employer id's income details page" in {
      val testController = createTestIncomeController()

      when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))

      val result = testController.cancel(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url

      verify(mockJourneyCacheRepository, times(1)).clear(any(), any())
    }
  }

  "regularIncome" must {
    "return OK with regular income view" when {
      "valid inputs are passed" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))
        when(incomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(Some(payment)))

        val updatedUserAnswers = baseUserAnswers.setOrException(UpdateIncomePayToDatePage, "0")
        when(incomeService.cachePaymentForRegularIncome(any(), any())).thenReturn(updatedUserAnswers)
        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = testController.regularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result = testController.regularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result = testController.regularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController()
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result = testController.regularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "editRegularIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, payToDate)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdatedIncomeDatePage, LocalDate.of(2017, 2, 1).toString)

        setup(userAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val result =
          testController.editRegularIncome(empId = employment.sequenceNumber)(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController
          .confirmRegularIncome(empId = employment.sequenceNumber)
          .url

        verify(mockJourneyCacheRepository, times(1)).set(any())
      }
    }

    "handle exception" when {
      "an invalid UpdateIncomeConstants.DateKey is present " in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, payToDate)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdatedIncomeDatePage, "May 2020")

        setup(userAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("200"))
        val formData = Json.toJson(editIncomeForm)
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val result =
          testController.editRegularIncome(empId = employment.sequenceNumber)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController
          .confirmRegularIncome(empId = employment.sequenceNumber)
          .url

        verify(mockJourneyCacheRepository, times(1)).set(any())
      }
    }

    "redirect to the same estimated pay page" when {
      "new input is the same as the cached input" in {
        val testController = createTestIncomeController()

        val sameAmount = "200"

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(sameAmount))
        val formData = Json.toJson(editIncomeForm)
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, payToDate)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdatedIncomeDatePage, LocalDate.of(2017, 2, 1).toString)
          .setOrException(UpdateIncomeConfirmedNewAmountPage(employment.sequenceNumber), sameAmount)

        setup(userAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val result =
          testController.editRegularIncome(empId = employment.sequenceNumber)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeController.sameEstimatedPayInCache(employment.sequenceNumber).url
        )

        verify(mockJourneyCacheRepository, never).set(any())
      }

      "new amount is the same as the current amount" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, "1")
          .setOrException(UpdateIncomeNamePage, "employer name")

        setup(userAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = 212)
        val formData = Json.toJson(editIncomeForm)
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val result =
          testController.editRegularIncome(empId = employment.sequenceNumber)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)

        verify(mockJourneyCacheRepository, never).set(any())
      }
    }

    "redirect to /income-details" when {
      "cache is empty" in {

        val testController = createTestIncomeController()

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = 212)
        val formData = Json.toJson(editIncomeForm)

        val result =
          testController.editRegularIncome(empId = employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
        )

      }
    }

    "return Bad request" when {
      "an input error occurs" in {
        val invalidNewAmount = ""
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, payToDate)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdatedIncomeDatePage, LocalDate.of(2017, 2, 1).toString)

        setup(userAnswers)

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(invalidNewAmount))
        val formData = Json.toJson(editIncomeForm)
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val result =
          testController.editRegularIncome(empId = employment.sequenceNumber)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().toString must include(Messages("error.tai.updateDataEmployment.blankValue"))

      }

    }
  }

  "confirmRegularIncome" must {
    "return OK" when {
      "valid values are present in cache" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, "100")
        setup(userAnswers)

        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result =
          testController.confirmRegularIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }

      "pay to date cannot be determined, due to no annual account records on the income source" in {
        val testController = createTestIncomeController()
        val employment = employmentWithAccounts(Nil)
        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, "100")
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result =
          testController.confirmRegularIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, "100")
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result =
          testController.confirmRegularIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, "100")
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result =
          testController.confirmRegularIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController()
        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, "100")
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result =
          testController.confirmRegularIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "user navigates backwards" in {
      val testController = createTestIncomeController()

      val userAnswers = baseUserAnswers
        .setOrException(UpdateIncomeNewAmountPage, "100")
        .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), "100")
      setup(userAnswers)

      val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
        TaxCodeIncome(
          EmploymentIncome,
          Some(123),
          1111,
          "employment1",
          "1150L",
          "employment",
          OtherBasisOfOperation,
          Live
        )
      )
      when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(Right(taxCodeIncomes)))

      val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
      val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
      val employment = employmentWithAccounts(List(annualAccount))
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

      when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))

      val result =
        testController.confirmRegularIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url

      verify(mockJourneyCacheRepository, times(1)).clear(any(), any())
    }

    "Redirect to /Income-details" when {
      "cache is empty" in {
        val testController = createTestIncomeController()
        val result =
          testController.confirmRegularIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
      }
    }
  }

  "updateEstimatedIncome" must {
    "return OK" when {
      "income from employment is successfully updated" in {
        val testController = createTestIncomeController()

        val employerName = "Employer"
        val employerType = TaiConstants.IncomeTypeEmployment

        val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

        val expected = testController.renderSuccess(employerName, employerId)(fakeRequest)

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdateIncomeNewAmountPage, "100,000")
          .setOrException(UpdateIncomeIdPage, employerId)
          .setOrException(UpdateIncomeTypePage, employerType)
        setup(userAnswers)

        when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
        when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))

        val result = testController.updateEstimatedIncome(employerId)(fakeRequest)

        status(result) mustBe OK

        contentAsString(result) must equal(expected.toString)
      }

      "income from pension is successfully updated" in {
        val testController = createTestIncomeController()
        val employerType = TaiConstants.IncomeTypePension

        val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

        val expected = testController.renderPensionSuccess(employerName, employerId)(fakeRequest)

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdateIncomeNewAmountPage, "100,000")
          .setOrException(UpdateIncomeIdPage, employerId)
          .setOrException(UpdateIncomeTypePage, employerType)
        setup(userAnswers)

        when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
        when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))

        val result = testController.updateEstimatedIncome(employerId)(fakeRequest)

        status(result) mustBe OK

        contentAsString(result) must equal(expected.toString)
      }
    }

    "return Internal Server Error" when {
      "update is failed" in {
        val testController = createTestIncomeController()
        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdateIncomeNewAmountPage, "100,000")
          .setOrException(UpdateIncomeIdPage, employerId)
          .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)
        setup(userAnswers)

        when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
        when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new Exception("Failed")))

        val result = testController.updateEstimatedIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "flush the cache" in {
      val testController = createTestIncomeController()

      val employerType = TaiConstants.IncomeTypeEmployment
      val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

      val userAnswers = baseUserAnswers
        .setOrException(UpdateIncomeNamePage, employerName)
        .setOrException(UpdateIncomeNewAmountPage, "100,000")
        .setOrException(UpdateIncomeIdPage, employerId)
        .setOrException(UpdateIncomeTypePage, employerType)
      setup(userAnswers)

      when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      Await.result(testController.updateEstimatedIncome(employerId)(fakeRequest), 5.seconds)

      verify(mockJourneyCacheRepository, times(1)).clear(any(), any())
    }

    "Redirect to /Income-details" when {
      "cache is empty" in {
        val testController = createTestIncomeController()

        val result =
          testController.updateEstimatedIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
      }
    }
  }

  "pension" must {
    "return OK with regular income view" when {
      "valid inputs are passed" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))
        when(incomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(Some(payment)))
        val updatedUserAnswers = baseUserAnswers.setOrException(UpdateIncomePayToDatePage, "0")
        when(incomeService.cachePaymentForRegularIncome(any(), any())).thenReturn(updatedUserAnswers)
        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val result = testController.pensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("failed")))

        val result = testController.pensionIncome(56)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("failed")))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val result = testController.pensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController()
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("failed")))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val result = testController.pensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "editPensionIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, payToDate)
          .setOrException(UpdateIncomeIdPage, employerId)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdatedIncomeDatePage, LocalDate.of(2017, 2, 1).toString)
        setup(userAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("201"))
        val formData = Json.toJson(editIncomeForm)

        val result =
          testController.editPensionIncome(employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.confirmPensionIncome(employerId).url
      }
    }

    "handle exception" when {
      "an invalid UpdateIncomeConstants.DateKey is present in pension income" in {
        val testController = createTestIncomeController()
        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, payToDate)
          .setOrException(UpdateIncomeIdPage, employerId)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdatedIncomeDatePage, "May 2020")
        setup(userAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("201"))
        val formData = Json.toJson(editIncomeForm)

        val result =
          testController.editPensionIncome(employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.confirmPensionIncome(employerId).url
      }
    }

    "return Bad request" when {
      "new amount is blank" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, payToDate)
          .setOrException(UpdateIncomeIdPage, employerId)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdatedIncomeDatePage, LocalDate.of(2017, 2, 1).toString)
        setup(userAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(""))
        val formData = Json.toJson(editIncomeForm)

        val result =
          testController.editPensionIncome(employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to the same estimated pay page" when {
      "new input is the same as the cached input" in {
        val testController = createTestIncomeController()

        val sameAmount = "987"

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, payToDate)
          .setOrException(UpdateIncomeIdPage, employerId)
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdatedIncomeDatePage, LocalDate.of(2017, 2, 1).toString)
          .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), sameAmount)
        setup(userAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(sameAmount))
        val formData = Json.toJson(editIncomeForm)

        val result =
          testController.editPensionIncome(employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.sameEstimatedPayInCache(employerId).url
      }

      "new amount is the same as the current amount" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomePayToDatePage, "1")
          .setOrException(UpdateIncomeIdPage, 2)
          .setOrException(UpdateIncomeNamePage, "employer name")
        setup(userAnswers)

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = 212)
        val formData = Json.toJson(editIncomeForm)

        val result =
          testController.editPensionIncome(employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay().url)
      }
    }
    "redirect to /income-details" when {
      "nothing is present in the cache" in {
        val testController = createTestIncomeController()

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = 212)
        val formData = Json.toJson(editIncomeForm)

        val result =
          testController.editPensionIncome(employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
        )
      }
    }
  }

  "confirmPensionIncome" must {
    "return OK" when {
      "valid values are present in cache" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, cachedUpdateIncomeNewAmount)
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmPensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return Internal Server Error" when {
      "employment doesn't present" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, cachedUpdateIncomeNewAmount)
        setup(userAnswers)

        val result = testController.confirmPensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "tax code incomes return failure" in {
        val testController = createTestIncomeController()
        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, cachedUpdateIncomeNewAmount)
        setup(userAnswers)
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = testController.confirmPensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "employment return None" in {
        val testController = createTestIncomeController()
        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, cachedUpdateIncomeNewAmount)
        setup(userAnswers)
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = testController.confirmPensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return SEE_OTHER" when {
      "nohing is present in the cache" in {

        val testController = createTestIncomeController()
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = testController.confirmPensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
        )
      }
    }
  }

  "viewIncomeForEdit" must {
    "redirect user" when {
      "employment is live and is not occupational pension" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeIdPage, 1)
        setup(userAnswers)

        val employmentAmount =
          EmploymentAmount("employment", "(Current employer)", 1, 11, 11, None, None, None, None)

        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeController.regularIncome(employerId).url
      }

      "employment is not live and is not occupational pension" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeIdPage, 1)
        setup(userAnswers)

        val employmentAmount =
          EmploymentAmount("employment", "(Current employer)", 1, 11, 11, None, None, None, None, isLive = false)

        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.TaxAccountSummaryController.onPageLoad().url
      }

      "employment is not live and is occupational pension" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers.setOrException(UpdateIncomeIdPage, 1)
        setup(userAnswers)

        val employmentAmount =
          EmploymentAmount(
            "employment",
            "(Current employer)",
            1,
            11,
            11,
            None,
            None,
            None,
            None,
            isLive = false,
            isOccupationalPension = true
          )

        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeController.pensionIncome(employerId).url
      }
    }

    "sameEstimatedPay page" should {
      "contain the employer name and current pay " in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomeNamePage, "Employer Name")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), "987")
        setup(userAnswers)

        val result = testController.sameEstimatedPayInCache(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        val body = doc.body().toString
        body must include("Employer Name")
        body must include("987")
      }

      "fail if there are no mandatory values " in {
        val testController = createTestIncomeController()

        val result = testController.sameEstimatedPayInCache(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "sameAnnualEstimatedPay" must {
      "show the same annual estimated pay page" in {
        val testController = createTestIncomeController()

        val userAnswers = baseUserAnswers
          .setOrException(UpdateIncomeNamePage, employerName)
          .setOrException(UpdateIncomeIdPage, 1)
        setup(userAnswers)

        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))

        val result = testController.sameAnnualEstimatedPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messagesApi("tai.updateEmployment.incomeSame.title", ""))
      }

      "return Internal Server Error when mandatory values missing from cache" in {
        val testController = createTestIncomeController()

        val result = testController.sameAnnualEstimatedPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }
}
