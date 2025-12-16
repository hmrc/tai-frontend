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

package controllers.income.estimatedPay.update

import builders.RequestBuilder
import controllers.ErrorPagesHandler
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import pages.income.*
import play.api.mvc.Result
import play.api.test.Helpers.*
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.*
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live}
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import utils.BaseSpec
import views.html.incomes.{EstimatedPayLandingPageView, EstimatedPayView, IncorrectTaxableIncomeView}

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Random

class IncomeUpdateEstimatedPayControllerSpec extends BaseSpec {

  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val empId: Int             = 1
  val sessionId: String      = "testSessionId"

  def randomNino(): Nino = new Generator(new Random()).nextNino
  def createSUT          = new SUT

  val mockIncomeService: IncomeService                   = mock[IncomeService]
  val mockEmploymentService: EmploymentService           = mock[EmploymentService]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  val defaultEmployment: Employment =
    Employment(
      "company",
      Live,
      Some("123"),
      Some(LocalDate.parse("2016-05-26")),
      None,
      "",
      "",
      empId,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    )

  class SUT
      extends IncomeUpdateEstimatedPayController(
        mockAuthJourney,
        mockIncomeService,
        mockEmploymentService,
        appConfig,
        mcc,
        inject[EstimatedPayLandingPageView],
        inject[EstimatedPayView],
        inject[IncorrectTaxableIncomeView],
        mockJourneyCacheRepository,
        inject[ErrorPagesHandler]
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockJourneyCacheRepository, mockIncomeService, mockEmploymentService)
  }

  "estimatedPayLandingPage" must {

    val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)

    def estimatedPayLandingPage(): Future[Result] =
      new SUT().estimatedPayLandingPage(empId)(RequestBuilder.buildFakeGetRequestWithAuth())

    "display the estimatedPayLandingPage view" in {
      setup(mockUserAnswers)
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(
          Future.successful(Some(defaultEmployment.copy(name = employer.name, receivingOccupationalPension = false)))
        )

      val result = estimatedPayLandingPage()

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.incomes.landing.title"))
    }

    "return to /income-details when employment not found" in {
      setup(mockUserAnswers)
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = estimatedPayLandingPage()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url)
    }
  }

  "estimatedPayPage" must {
    "display estimatedPay page when payYearToDate is less than gross annual pay" in {
      val payment = Some(Payment(LocalDate.now, 50, 1, 1, 1, 1, 1, Monthly))
      val ua      = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeIdPage, employer.id)
        .setOrException(UpdateIncomeNamePage, employer.name)

      val controller = createSUT
      setup(ua)

      when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(defaultEmployment.copy(name = employer.name))))
      when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(payment))
      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(EmploymentAmount("", "", 1, Some(1))))
      when(mockIncomeService.calculateEstimatedPay(any(), any())(any()))
        .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))

      val result = controller.estimatedPayPage(empId)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(
        messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeBreak.replace("\u00A0", " "))
      )
    }

    "display estimatedPay page when payYearToDate is None" in {
      val ua = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeIdPage, employer.id)
        .setOrException(UpdateIncomeNamePage, employer.name)

      val controller = createSUT
      setup(ua)

      when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(defaultEmployment.copy(name = employer.name))))
      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(EmploymentAmount("", "", 1, Some(1))))
      when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(None))
      when(mockIncomeService.calculateEstimatedPay(any(), any())(any()))
        .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))

      val result = controller.estimatedPayPage(empId)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.estimatedPay.title", TaxYearRangeUtil.currentTaxYearRangeBreak))
    }

    "show incorrectTaxableIncome when grossAnnualPay is None" in {
      val ua = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeIdPage, employer.id)
        .setOrException(UpdateIncomeNamePage, employer.name)
        .setOrException(UpdateIncomeConfirmedNewAmountPage(empId), "150")

      val controller = createSUT
      setup(ua)

      val payment = Some(Payment(LocalDate.now, 200, 50, 25, 100, 50, 25, Monthly))

      when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(defaultEmployment.copy(name = employer.name))))
      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(EmploymentAmount("", "", 1, Some(1))))
      when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(payment))
      when(mockIncomeService.calculateEstimatedPay(any(), any())(any()))
        .thenReturn(Future.successful(CalculatedPay(None, Some(BigDecimal(100)))))

      val result = controller.estimatedPayPage(empId)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))
    }

    "display incorrectTaxableIncome page when payYearToDate is greater than gross annual pay" in {
      val ua = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeIdPage, employer.id)
        .setOrException(UpdateIncomeNamePage, employer.name)

      val controller = createSUT
      setup(ua)

      val payment = Some(Payment(LocalDate.now, 200, 50, 25, 100, 50, 25, Monthly))

      when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(defaultEmployment.copy(name = employer.name))))
      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(EmploymentAmount("", "", 1, Some(1))))
      when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(payment))
      when(mockIncomeService.calculateEstimatedPay(any(), any())(any()))
        .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(100)), Some(BigDecimal(100)))))

      val result = controller.estimatedPayPage(empId)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.estimatedPay.error.incorrectTaxableIncome.title"))
    }

    "redirect to sameEstimatedPay page when confirmed amount equals calculated gross" in {
      val payment = Some(Payment(LocalDate.now, 200, 50, 25, 100, 50, 25, Monthly))
      val ua      = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeIdPage, employer.id)
        .setOrException(UpdateIncomeNamePage, employer.name)
        .setOrException(UpdateIncomeGrossAnnualPayPage, "100")
        .setOrException(UpdateIncomeNewAmountPage, "123")
        .setOrException(UpdateIncomeBonusPaymentsPage, "")
        .setOrException(UpdateIncomeConfirmedNewAmountPage(empId), "150")

      val controller = createSUT
      setup(ua)

      when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(defaultEmployment.copy(name = employer.name))))
      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(EmploymentAmount("", "", 1, Some(1))))
      when(mockIncomeService.latestPayment(any(), any())(any(), any())).thenReturn(Future.successful(payment))
      when(mockIncomeService.calculateEstimatedPay(any(), any())(any()))
        .thenReturn(Future.successful(CalculatedPay(Some(BigDecimal(150)), Some(BigDecimal(100)))))

      val result = controller.estimatedPayPage(empId)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.IncomeController.sameEstimatedPayInCache(empId).url
      )
    }

    "redirect to /income-summary page when employment not found" in {
      val controller = createSUT
      setup(UserAnswers(sessionId, randomNino().nino))

      when(mockEmploymentService.employment(any(), any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = controller.estimatedPayPage(empId)(RequestBuilder.buildFakeGetRequestWithAuth())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
    }
  }
}
