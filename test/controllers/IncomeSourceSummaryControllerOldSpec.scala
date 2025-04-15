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
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import play.api.i18n.Messages
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, GenericBenefit}
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, RtiService, TaxAccountService}
import uk.gov.hmrc.tai.util.{ApiBackendChoice, TaxYearRangeUtil}
import utils.BaseSpec
import views.html.IncomeSourceSummaryView

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceSummaryControllerOldSpec extends BaseSpec {

  val firstPayment: Payment = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  val secondPayment: Payment = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  val thirdPayment: Payment = Payment(LocalDate.now.minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  val latestPayment: Payment = Payment(LocalDate.now.minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)

  val annualAccount: AnnualAccount = AnnualAccount(
    uk.gov.hmrc.tai.model.TaxYear(),
    Available,
    Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
    Nil
  )
  val employment: Employment = Employment(
    name = "test employment",
    employmentStatus = Live,
    payrollNumber = Some("EMPLOYER-1122"),
    startDate = Some(LocalDate.now()),
    endDate = None,
    annualAccounts = Seq(annualAccount),
    taxDistrictNumber = "",
    payeNumber = "",
    sequenceNumber = 2,
    cessationPay = None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false,
    EmploymentIncome
  )

  private val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "pension", Week1Month1BasisOfOperation, Live)
  )

  private val benefits = Benefits(Seq.empty[CompanyCarBenefit], Seq.empty[GenericBenefit])

  val personService: PersonService = mock[PersonService]
  val benefitsService: BenefitsService = mock[BenefitsService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]
  private val mockRtiService = mock[RtiService]
  private val mockApiBackendChoice: ApiBackendChoice = mock[ApiBackendChoice] // TODO: DDCNL-10086 New API
  val baseUserAnswers: UserAnswers = UserAnswers("testSessionId", nino.nino)

  def sut = new IncomeSourceSummaryController(
    mock[AuditConnector],
    taxAccountService,
    employmentService,
    benefitsService,
    mockAuthJourney,
    appConfig,
    mcc,
    inject[IncomeSourceSummaryView],
    mockJourneyCacheRepository,
    mockRtiService,
    mockApiBackendChoice,
    inject[ErrorPagesHandler]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(baseUserAnswers)
    Mockito.reset(mockJourneyCacheRepository)
    Mockito.reset(mockApiBackendChoice)
    when(mockApiBackendChoice.isNewApiBackendEnabled(any())).thenReturn(false)
  }

  val employmentId = 1
  val pensionId = 2

  "onPageLoad" must {
    "display the income details page" when {
      "asked for employment details" in {

        val userAnswers = baseUserAnswers
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(employmentId), true)
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(mockJourneyCacheRepository.set(any())).thenReturn(Future.successful(true))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages(
            "tai.employment.income.details.mainHeading.gaTitle",
            TaxYearRangeUtil.currentTaxYearRangeBreak.replaceAll("\u00A0", " ")
          )
        )
      }

      "asked for pension details" in {

        val userAnswers = baseUserAnswers
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(pensionId), true)
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(mockJourneyCacheRepository.set(any())).thenReturn(Future.successful(true))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages(
            "tai.pension.income.details.mainHeading.gaTitle",
            TaxYearRangeUtil.currentTaxYearRangeBreak.replaceAll("\u00A0", " ")
          )
        )
      }
    }

    "throw error" when {
      "failed to read tax code incomes" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "failed to read employment details" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "flush the cache" when {
      "cache update amount is the same as the HOD amount" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))

        val userAnswers = baseUserAnswers
          .setOrException(EndCompanyBenefitsUpdateIncomePage(employmentId), "1111")
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(employmentId), true)
        setup(userAnswers)

        val updatedUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockJourneyCacheRepository.set(any())).thenReturn(Future.successful(true))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages("tai.employment.income.details.mainHeading.gaTitle", TaxYearRangeUtil.currentTaxYearRangeBreak)
        )

        verify(mockJourneyCacheRepository, times(1)).set(updatedUserAnswersCaptor.capture())

        val updatedAnswers = updatedUserAnswersCaptor.getValue
        updatedAnswers.get(EndCompanyBenefitsUpdateIncomePage(employmentId)) mustBe None
      }
    }
    "display the income details page with an update message" when {
      "update is in progress for employment as cache update amount is different to the HOD amount" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))

        val userAnswers = baseUserAnswers
          .setOrException(EndCompanyBenefitsUpdateIncomePage(employmentId), "3333")
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(employmentId), true)
        setup(userAnswers)

        val updatedUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockJourneyCacheRepository.set(any())).thenReturn(Future.successful(true))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.toString must include(Messages("tai.income.details.updateInProgress"))
        verify(mockJourneyCacheRepository, times(0)).set(updatedUserAnswersCaptor.capture())
      }
    }

    "display the income details page with an update message" when {
      "update is in progress for pension as cache update amount is different to the HOD amount" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        val userAnswers = baseUserAnswers
          .setOrException(EndCompanyBenefitsUpdateIncomePage(pensionId), "3333")
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(pensionId), true)
        setup(userAnswers)

        val updatedUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockJourneyCacheRepository.set(any())).thenReturn(Future.successful(true))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.toString must include(Messages("tai.income.details.updateInProgress"))

        verify(mockJourneyCacheRepository, times(0)).set(updatedUserAnswersCaptor.capture())
      }
    }
  }
}
