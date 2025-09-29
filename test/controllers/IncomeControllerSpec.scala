/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mockito.Mockito.*
import pages.income.*
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.mvc.Results.NotFound
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import utils.BaseSpec
import views.html.incomes.*

import java.time.LocalDate
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class IncomeControllerSpec extends BaseSpec with I18nSupport {

  val incomeService: IncomeService                       = mock[IncomeService]
  val employmentService: EmploymentService               = mock[EmploymentService]
  val personService: PersonService                       = mock[PersonService]
  val taxAccountService: TaxAccountService               = mock[TaxAccountService]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  val baseUserAnswers: UserAnswers = UserAnswers("testSessionId", nino.nino)

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(baseUserAnswers)
    reset(incomeService, employmentService, taxAccountService, mockJourneyCacheRepository)
    when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
  }

  val payToDate                           = "100"
  val employerId                          = 1
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

  private def empNamed(
                        name: String = employerName,
                        seq: Int = employerId,
                        pension: Boolean = false
                      ): Employment =
    employmentWithAccounts(Nil).copy(sequenceNumber = seq, name = name, receivingOccupationalPension = pension)

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
    Some(1111),
    None,
    None,
    Some(LocalDate.of(2000, 5, 20)),
    None
  )

  private def createTestIncomeController() = new TestIncomeController()

  private val editSuccessView        = inject[EditSuccessView]
  private val editPensionSuccessView = inject[EditPensionSuccessView]

  private class TestIncomeController
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
      mockEmpIdCheck,
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

    val editIncomeForm: EditIncomeForm = EditIncomeForm("Test", "Test", 1, None, Some(10), None, None, None, None)
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
        val payment        = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))
        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(payment)))

        val result = testController.regularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
        verify(mockJourneyCacheRepository, never()).set(any[UserAnswers])
      }
    }

    "return Internal Server Error" when {
      "employmentAmount fails" in {
        val testController = createTestIncomeController()
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("failed")))
        when(incomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(None))

        val result = testController.regularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "go to a NotFound page and not call typical downstream services" when {
      "the empId does not match the one provided" in {
        val testController = createTestIncomeController()

        when(mockEmpIdCheck.checkValidId(any(), any())(any()))
          .thenReturn(Future.successful(Some(NotFound("No match"))))

        val result = testController.regularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe NOT_FOUND

        verify(employmentService, times(0)).employment(any(), any())(any())
        verify(taxAccountService, times(0)).taxCodeIncomes(any(), any())(any())
        verify(incomeService, times(0)).employmentAmount(any(), any())(any(), any(), any())
      }
    }
  }

  "editRegularIncome" must {
    "redirect to confirm regular income page" when {
      "valid input is passed" in {
        val testController = createTestIncomeController()

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(empNamed(seq = employerId, name = employerName))))

        val latest = paymentOnDate(LocalDate.of(2017, 2, 1)).copy(amountYearToDate = BigDecimal(payToDate))
        when(incomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(Some(latest)))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("200"))
        val formData       = Json.toJson(editIncomeForm)

        val result =
          testController.editRegularIncome(empId = employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeController.confirmRegularIncome(employerId).url
      }
    }

    "redirect to the same estimated pay page" when {
      "new input is the same as the cached input" in {
        val testController = createTestIncomeController()

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(empNamed(seq = employerId, name = employerName))))
        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(paymentOnDate(LocalDate.now()).copy(amountYearToDate = BigDecimal(100)))))

        val sameAmount = "200"
        val ua         = baseUserAnswers.setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), sameAmount)
        setup(ua)

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(sameAmount))
        val formData       = Json.toJson(editIncomeForm)

        val result =
          testController.editRegularIncome(empId = employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeController.sameEstimatedPayInCache(employerId).url
        )
      }

      "new amount is the same as the current amount" in {
        val testController = createTestIncomeController()

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(empNamed(seq = employerId, name = "employer name"))))
        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(paymentOnDate(LocalDate.now()).copy(amountYearToDate = BigDecimal(100)))))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = Some(212))
        val formData       = Json.toJson(editIncomeForm)

        val result =
          testController.editRegularIncome(empId = employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay(employerId).url)
      }
    }

    "redirect to /income-details" when {
      "employment not found" in {
        val testController = createTestIncomeController()

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = Some(212))
        val formData       = Json.toJson(editIncomeForm)

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
        val testController = createTestIncomeController()

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(empNamed(seq = employerId, name = employerName))))
        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(paymentOnDate(LocalDate.now()))))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(""))
        val formData       = Json.toJson(editIncomeForm)

        val result =
          testController.editRegularIncome(empId = employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().toString must include(Messages("error.tai.updateDataEmployment.blankValue"))
      }
    }
  }

  "confirmRegularIncome" must {
    "return OK when employment is found" in {
      val testController = createTestIncomeController()

      val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, "100")
      setup(userAnswers)

      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed(seq = employerId))))

      val result = testController.confirmRegularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
    }

    "return Internal Server Error when employment is not found" in {
      val testController = createTestIncomeController()
      val userAnswers    = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, "100")
      setup(userAnswers)

      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

      val result = testController.confirmRegularIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "updateEstimatedIncome" must {
    "return OK showing employment success view when non-pension" in {
      val testController = createTestIncomeController()

      val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST")
      val expected    = testController.renderSuccess(employerName, employerId)(fakeRequest)

      val userAnswers = baseUserAnswers
        .setOrException(UpdateIncomeNewAmountPage, "100,000")
      setup(userAnswers)

      when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))
      when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed(name = employerName, seq = employerId))))

      val result = testController.updateEstimatedIncome(employerId)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) must equal(expected.toString)
    }

    "return OK showing pension success view when pension" in {
      val testController = createTestIncomeController()

      val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST")
      val expected    = testController.renderPensionSuccess(employerName, employerId)(fakeRequest)

      val userAnswers = baseUserAnswers
        .setOrException(UpdateIncomeNewAmountPage, "100,000")
      setup(userAnswers)

      when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))
      when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed(name = employerName, seq = employerId, pension = true))))

      val result = testController.updateEstimatedIncome(employerId)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) must equal(expected.toString)
    }

    "return Internal Server Error when update fails" in {
      val testController = createTestIncomeController()
      val userAnswers    = baseUserAnswers
        .setOrException(UpdateIncomeNewAmountPage, "100,000")
      setup(userAnswers)

      when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(new Exception("Failed")))

      assertThrows[Exception] {
        await(testController.updateEstimatedIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("POST")))
      }
    }

    "flush the cache" in {
      val testController = createTestIncomeController()

      val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

      val userAnswers = baseUserAnswers
        .setOrException(UpdateIncomeNewAmountPage, "100,000")
      setup(userAnswers)

      when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed())))

      Await.result(testController.updateEstimatedIncome(employerId)(fakeRequest), 5.seconds)

      verify(mockJourneyCacheRepository, times(1)).clear(any(), any())
    }

    "Redirect to /Income-details when new amount missing" in {
      val testController = createTestIncomeController()

      val result =
        testController.updateEstimatedIncome(empId = employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
    }

    "correctly parse formatted currency values" in {
      val testController = createTestIncomeController()

      val userAnswers = baseUserAnswers
        .setOrException(UpdateIncomeNewAmountPage, "1,000")
      setup(userAnswers)

      when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))
      when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed())))

      val result = testController.updateEstimatedIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("POST"))
      status(result) mustBe OK
    }
  }

  "sameEstimatedPayInCache" must {
    "gracefully handle missing confirmed amount" in {
      val testController = createTestIncomeController()
      setup(baseUserAnswers)

      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed())))

      val result = testController.sameEstimatedPayInCache(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "contain the employer name and confirmed pay when present" in {
      val testController = createTestIncomeController()

      val ua = baseUserAnswers
        .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), "987")
      setup(ua)

      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed(name = "Employer Name", seq = employerId))))

      val result = testController.sameEstimatedPayInCache(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc  = Jsoup.parse(contentAsString(result))
      val body = doc.body().toString
      body must include("Employer Name")
      body must include("987")
    }
  }

  "pensionIncome" must {
    "return OK with pension income view" when {
      "valid inputs are passed" in {
        val testController = createTestIncomeController()
        val payment        = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(employmentAmount))
        when(incomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(Some(payment)))

        val result = testController.pensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomes.edit.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
        verify(mockJourneyCacheRepository, never()).set(any[UserAnswers])
      }
    }

    "return Internal Server Error" when {
      "employmentAmount fails" in {
        val testController = createTestIncomeController()
        when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("failed")))
        when(incomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(None))

        val result = testController.pensionIncome(56)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "editPensionIncome" must {
    "redirect to confirm pension income page" when {
      "valid input is passed" in {
        val testController = createTestIncomeController()

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(empNamed(name = employerName, seq = employerId, pension = true))))
        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(paymentOnDate(LocalDate.of(2017, 2, 1)).copy(amountYearToDate = 201))))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("201"))
        val formData       = Json.toJson(editIncomeForm)

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

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(empNamed(name = employerName, seq = employerId, pension = true))))
        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(paymentOnDate(LocalDate.now()))))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(""))
        val formData       = Json.toJson(editIncomeForm)

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

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(empNamed(seq = employerId, name = employerName))))
        val sameAmount = "200"
        when(incomeService.latestPayment(any(), any())(any(), any())).thenReturn(
          Future.successful(Some(paymentOnDate(LocalDate.now()).copy(amountYearToDate = BigDecimal(sameAmount))))
        )

        val ua = baseUserAnswers.setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), sameAmount)
        setup(ua)

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some(sameAmount))
        val formData       = Json.toJson(editIncomeForm)
        val result         =
          testController.editRegularIncome(employerId)(
            RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IncomeController.sameEstimatedPayInCache(employerId).url
        )
      }

      "new amount is the same as the current amount" in {
        val testController = createTestIncomeController()

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(empNamed(seq = employerId, name = "employer name"))))
        when(incomeService.latestPayment(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(paymentOnDate(LocalDate.now()).copy(amountYearToDate = BigDecimal(100)))))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = Some(212))
        val formData       = Json.toJson(editIncomeForm)
        val result         = testController.editRegularIncome(employerId)(
          RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.sameAnnualEstimatedPay(employerId).url)
      }
    }

    "redirect to /income-details" when {
      "employment not found" in {
        val testController = createTestIncomeController()

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val editIncomeForm = testController.editIncomeForm.copy(newAmount = Some("212"), oldAmount = Some(212))
        val formData       = Json.toJson(editIncomeForm)

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
    "return OK when valid values are present in cache and employment exists" in {
      val testController = createTestIncomeController()

      val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, cachedUpdateIncomeNewAmount)
      setup(userAnswers)

      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed(seq = employerId, pension = true))))

      val result = testController.confirmPensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
    }

    "return Internal Server Error when employment doesn't exist" in {
      val testController = createTestIncomeController()

      val userAnswers = baseUserAnswers.setOrException(UpdateIncomeNewAmountPage, cachedUpdateIncomeNewAmount)
      setup(userAnswers)

      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

      val result = testController.confirmPensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return SEE_OTHER when nothing present in cache" in {
      val testController = createTestIncomeController()

      val result = testController.confirmPensionIncome(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url
      )
    }
  }

  "viewIncomeForEdit" must {
    "redirect user when employment is live and not occupational pension" in {
      val testController = createTestIncomeController()

      val userAnswers = baseUserAnswers.setOrException(UpdateIncomeIdPage, 1)
      setup(userAnswers)

      val employmentAmount =
        EmploymentAmount("employment", "(Current employer)", 1, Some(11), None, None, None, None)

      when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(employmentAmount))

      val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.IncomeController.regularIncome(employerId).url
    }

    "redirect user when employment is not live and not occupational pension" in {
      val testController = createTestIncomeController()

      val userAnswers = baseUserAnswers.setOrException(UpdateIncomeIdPage, 1)
      setup(userAnswers)

      val employmentAmount =
        EmploymentAmount("employment", "(Current employer)", 1, Some(11), None, None, None, None, isLive = false)

      when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(employmentAmount))

      val result = testController.viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.TaxAccountSummaryController.onPageLoad().url
    }

    "redirect user when employment is occupational pension" in {
      val testController = createTestIncomeController()

      val userAnswers = baseUserAnswers.setOrException(UpdateIncomeIdPage, 1)
      setup(userAnswers)

      val employmentAmount =
        EmploymentAmount(
          "employment",
          "(Current employer)",
          1,
          Some(11),
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

  "sameAnnualEstimatedPay" must {
    "show the same annual estimated pay page" in {
      val testController = createTestIncomeController()

      when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(employmentAmount))
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(empNamed(name = employerName, seq = employerId))))

      val result = testController.sameAnnualEstimatedPay(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messagesApi("tai.updateEmployment.incomeSame.title", ""))
    }

    "return Internal Server Error when employment not found" in {
      val testController = createTestIncomeController()

      when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(employmentAmount))
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = testController.sameAnnualEstimatedPay(employerId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
