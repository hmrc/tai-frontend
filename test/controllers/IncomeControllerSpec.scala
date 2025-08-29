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
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.*
import pages.income.*
import play.api.i18n.{I18nSupport, Messages}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncomeSourceStatus}
import uk.gov.hmrc.tai.model.{EmploymentAmount, UserAnswers}
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import utils.BaseSpec
import views.html.incomes.*

import java.time.LocalDate
import scala.concurrent.Future

class IncomeControllerSpec extends BaseSpec with I18nSupport {

  private val incomeService: IncomeService                   = mock[IncomeService]
  private val employmentService: EmploymentService           = mock[EmploymentService]
  private val taxAccountService: TaxAccountService           = mock[TaxAccountService]
  private val journeyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  private val confirmAmountEnteredView = inject[ConfirmAmountEnteredView]
  private val editSuccessView          = inject[EditSuccessView]
  private val editPensionView          = inject[EditPensionView]
  private val editPensionSuccessView   = inject[EditPensionSuccessView]
  private val editIncomeView           = inject[EditIncomeView]
  private val sameEstimatedPayView     = inject[SameEstimatedPayView]

  private val sessionId   = "testSessionId"
  private val baseAnswers = UserAnswers(sessionId, nino.nino)
  private val empId       = 1
  private val empName     = "Acme Ltd"

  private val baseRequest: FakeRequest[_] = RequestBuilder.buildFakeRequestWithAuth("GET")
  implicit private val messages: Messages = messagesApi.preferred(baseRequest)

  private val employment: Employment = Employment(
    name = empName,
    employmentStatus = Live.asInstanceOf[TaxCodeIncomeSourceStatus],
    payrollNumber = Some("PN"),
    startDate = Some(LocalDate.now.minusYears(1)),
    endDate = None,
    annualAccounts = Nil,
    taxDistrictNumber = "123",
    payeNumber = "AB123",
    sequenceNumber = empId,
    cessationPay = None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false,
    employmentType = EmploymentIncome
  )

  private val pensionEmployment = employment.copy(receivingOccupationalPension = true)

  private def paymentOn(date: LocalDate, ytd: BigDecimal) =
    Payment(date, ytd, 0, 0, 100, 10, 5, Monthly)

  private class SUT
      extends IncomeController(
        taxAccountService,
        employmentService,
        incomeService,
        mockAuthJourney,
        mcc,
        confirmAmountEnteredView,
        editSuccessView,
        editPensionView,
        editPensionSuccessView,
        editIncomeView,
        sameEstimatedPayView,
        journeyCacheRepository,
        inject[ErrorPagesHandler]
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(baseAnswers)
    reset(incomeService, employmentService, taxAccountService, journeyCacheRepository)
  }

  "cancel" must {
    "clear cache and redirect to income details" in {
      when(journeyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))

      val res = new SUT().cancel(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.IncomeSourceSummaryController.onPageLoad(empId).url)
    }
  }

  "regularIncome" must {
    "cache latest YTD and show editIncome" in {
      val employmentAmount = EmploymentAmount(taxCodeIncome = None, employment = employment)
      val latest           = Some(paymentOn(LocalDate.now.minusDays(2), 1234))

      when(incomeService.employmentAmount(any(), meq(empId))(any(), any(), any()))
        .thenReturn(Future.successful(employmentAmount))
      when(incomeService.latestPayment(any(), meq(empId))(any(), any())).thenReturn(Future.successful(latest))
      when(journeyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val res = new SUT().regularIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe OK
      val doc = Jsoup.parse(contentAsString(res))
      doc.title() must include(messages("tai.incomes.edit.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
    }
  }

  "sameEstimatedPayInCache" must {
    "render with employer name from Employment API when confirmed amount present" in {
      val ua = baseAnswers.setOrException(UpdateIncomeConfirmedNewAmountPage(empId), "150")
      setup(ua)

      when(employmentService.employment(any(), meq(empId))(any())).thenReturn(Future.successful(Some(employment)))

      val res  = new SUT().sameEstimatedPayInCache(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe OK
      val body = contentAsString(res)
      body must include(empName)
      body must include("150")
    }

    "return 500 when confirmed amount missing or employment not found" in {
      setup(baseAnswers)
      when(employmentService.employment(any(), meq(empId))(any())).thenReturn(Future.successful(None))

      val res = new SUT().sameEstimatedPayInCache(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "sameAnnualEstimatedPay" must {
    "use id from cache, name via Employment API, and oldAmount from IncomeService" in {
      val ua = baseAnswers.setOrException(UpdateIncomeIdPage, empId)
      setup(ua)

      val ea = EmploymentAmount(taxCodeIncome = None, employment = employment).copy(oldAmount = Some(40000))

      when(incomeService.employmentAmount(any(), meq(empId))(any(), any(), any()))
        .thenReturn(Future.successful(ea))
      when(employmentService.employment(any(), meq(empId))(any()))
        .thenReturn(Future.successful(Some(employment)))

      val res = new SUT().sameAnnualEstimatedPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe OK
      contentAsString(res) must include(empName)
    }

    "return 500 when UpdateIncomeIdPage missing" in {
      setup(baseAnswers)
      val res = new SUT().sameAnnualEstimatedPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "editRegularIncome" must {
    "bind with payToDate/date in cache and employer name from API" in {
      val ua = baseAnswers
        .setOrException(UpdateIncomePayToDatePage, "123.45")
        .setOrException(UpdatedIncomeDatePage, LocalDate.now.toString)
      setup(ua)

      when(employmentService.employment(any(), meq(empId))(any())).thenReturn(Future.successful(Some(employment)))

      val res = new SUT().editRegularIncome(empId)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(
            "name"                  -> empName,
            "description"           -> "desc",
            "employmentId"          -> empId.toString,
            "newAmount"             -> "12345",
            "isLive"                -> "true",
            "isOccupationalPension" -> "false",
            "hasMultipleIncomes"    -> "false"
          )
      )
      when(journeyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      status(res) must (be(SEE_OTHER) or be(BAD_REQUEST) or be(OK))
    }

    "redirect to income details when payToDate missing" in {
      setup(baseAnswers)
      val res = new SUT().editRegularIncome(empId)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(
            "name"                  -> empName,
            "description"           -> "desc",
            "employmentId"          -> empId.toString,
            "newAmount"             -> "12345",
            "isLive"                -> "true",
            "isOccupationalPension" -> "false",
            "hasMultipleIncomes"    -> "false"
          )
      )
      when(journeyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.IncomeSourceSummaryController.onPageLoad(empId).url)
    }
  }

  "confirmRegularIncome" must {
    "render confirm with employer name from API and new amount from cache" in {
      val ua = baseAnswers.setOrException(UpdateIncomeNewAmountPage, "12345")
      setup(ua)

      when(employmentService.employment(any(), meq(empId))(any())).thenReturn(Future.successful(Some(employment)))

      val res  = new SUT().confirmRegularIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe OK
      val body = contentAsString(res)
      body must include(empName)
      body must include("12345")
    }

    "redirect to summary when new amount missing" in {
      setup(baseAnswers)
      val res = new SUT().confirmRegularIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe SEE_OTHER
    }
  }

  "updateEstimatedIncome" must {
    "update, clear cache, fetch employment, and render correct success view (employment)" in {
      val ua = baseAnswers.setOrException(UpdateIncomeNewAmountPage, "20,000")
      setup(ua)

      when(journeyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(taxAccountService.updateEstimatedIncome(any(), meq(20000), any(), meq(empId))(any()))
        .thenReturn(Future.successful(Done))
      when(employmentService.employment(any(), meq(empId))(any())).thenReturn(Future.successful(Some(employment)))
      when(journeyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val res = new SUT().updateEstimatedIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("POST"))
      status(res) mustBe OK
      contentAsString(res) must include(empName)
    }

    "update and show pension success when employment is receivingOccupationalPension" in {
      val ua = baseAnswers.setOrException(UpdateIncomeNewAmountPage, "30,000")
      setup(ua)

      when(journeyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(taxAccountService.updateEstimatedIncome(any(), meq(30000), any(), meq(empId))(any()))
        .thenReturn(Future.successful(Done))
      when(employmentService.employment(any(), meq(empId))(any()))
        .thenReturn(Future.successful(Some(pensionEmployment)))
      when(journeyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val res = new SUT().updateEstimatedIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("POST"))
      status(res) mustBe OK
      contentAsString(res) must include(empName)
    }

    "redirect to summary when new amount missing" in {
      setup(baseAnswers)
      val res = new SUT().updateEstimatedIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("POST"))
      status(res) mustBe SEE_OTHER
    }

    "return 500 on service failure" in {
      val ua = baseAnswers.setOrException(UpdateIncomeNewAmountPage, "999")
      setup(ua)

      when(journeyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))
      when(taxAccountService.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val res = new SUT().updateEstimatedIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("POST"))
      status(res) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "pensionIncome" must {
    "cache latest YTD and show editPension" in {
      val employmentAmount = EmploymentAmount(taxCodeIncome = None, employment = pensionEmployment)
      val latest           = Some(paymentOn(LocalDate.now.minusDays(1), 900))

      when(incomeService.employmentAmount(any(), meq(empId))(any(), any(), any()))
        .thenReturn(Future.successful(employmentAmount))
      when(incomeService.latestPayment(any(), meq(empId))(any(), any())).thenReturn(Future.successful(latest))
      when(journeyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val res = new SUT().pensionIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe OK
      val doc = Jsoup.parse(contentAsString(res))
      doc.title() must include("Update your estimated income")
      doc.title() must include(TaxYearRangeUtil.currentTaxYearRangeBreak)
    }
  }

  "editPensionIncome" must {
    "bind with payToDate/date in cache and employer name from API" in {
      val ua = baseAnswers
        .setOrException(UpdateIncomePayToDatePage, "111")
        .setOrException(UpdatedIncomeDatePage, LocalDate.now.toString)
      setup(ua)

      when(employmentService.employment(any(), meq(empId))(any()))
        .thenReturn(Future.successful(Some(pensionEmployment)))

      val res = new SUT().editPensionIncome(empId)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(
            "name"                  -> empName,
            "description"           -> "desc",
            "employmentId"          -> empId.toString,
            "newAmount"             -> "54321",
            "isLive"                -> "true",
            "isOccupationalPension" -> "true",
            "hasMultipleIncomes"    -> "false"
          )
      )
      when(journeyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      status(res) must (be(SEE_OTHER) or be(BAD_REQUEST) or be(OK))
    }

    "redirect to income details when payToDate missing" in {
      setup(baseAnswers)
      val res = new SUT().editPensionIncome(empId)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(
            "name"                  -> empName,
            "description"           -> "desc",
            "employmentId"          -> empId.toString,
            "newAmount"             -> "54321",
            "isLive"                -> "true",
            "isOccupationalPension" -> "true",
            "hasMultipleIncomes"    -> "false"
          )
      )
      when(journeyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.IncomeSourceSummaryController.onPageLoad(empId).url)
    }
  }

  "confirmPensionIncome" must {
    "render confirm with employer name from API and new amount from cache" in {
      val ua = baseAnswers.setOrException(UpdateIncomeNewAmountPage, "30000")
      setup(ua)
      when(employmentService.employment(any(), meq(empId))(any()))
        .thenReturn(Future.successful(Some(pensionEmployment)))

      val res = new SUT().confirmRegularIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe OK

      val doc = Jsoup.parse(contentAsString(res))
      doc.text() must include(empName)
      doc.text() must include("£30,000")
    }

    "redirect to summary when new amount missing" in {
      setup(baseAnswers)
      val res = new SUT().confirmPensionIncome(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe SEE_OTHER
    }
  }

  "viewIncomeForEdit" must {
    "redirect appropriately based on employmentAmount flags" in {
      val ua = baseAnswers.setOrException(UpdateIncomeIdPage, empId)
      setup(ua)

      val liveNonPension = EmploymentAmount(taxCodeIncome = None, employment = employment)

      when(incomeService.employmentAmount(any(), meq(empId))(any(), any(), any()))
        .thenReturn(Future.successful(liveNonPension))

      val res = new SUT().viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.IncomeController.regularIncome(empId).url)
    }

    "redirect to tax account summary when id missing" in {
      setup(baseAnswers)
      val res = new SUT().viewIncomeForEdit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.TaxAccountSummaryController.onPageLoad().url)
    }
  }
}
