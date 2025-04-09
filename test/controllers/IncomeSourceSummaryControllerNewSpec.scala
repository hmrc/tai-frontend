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
import cats.data.EitherT
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.mockito.{ArgumentCaptor, Mockito}
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import play.api.i18n.Messages
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
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

class IncomeSourceSummaryControllerNewSpec extends BaseSpec {

  private val firstPayment: Payment = Payment(LocalDate.now.minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
  private val secondPayment: Payment = Payment(LocalDate.now.minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
  private val thirdPayment: Payment = Payment(LocalDate.now.minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
  private val latestPayment: Payment = Payment(LocalDate.now.minusWeeks(1), 400, 50, 25, 100, 50, 25, Irregular)

  private val annualAccount: AnnualAccount = AnnualAccount(
    taxYear = uk.gov.hmrc.tai.model.TaxYear(),
    realTimeStatus = Available,
    payments = Seq(latestPayment, secondPayment, thirdPayment, firstPayment),
    endOfTaxYearUpdates = Nil
  )
  private val employment: Employment = Employment(
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

  private val personService: PersonService = mock[PersonService]
  private val benefitsService: BenefitsService = mock[BenefitsService]
  private val mockEploymentService: EmploymentService = mock[EmploymentService]
  private val taxAccountService: TaxAccountService = mock[TaxAccountService]
  private val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]
  private val mockRtiService = mock[RtiService]
  private val mockApiBackendChoice: ApiBackendChoice = mock[ApiBackendChoice] // TODO: DDCNL-10086 New API
  private val baseUserAnswers: UserAnswers = UserAnswers("testSessionId", nino.nino)

  private def sut = new IncomeSourceSummaryController(
    mock[AuditConnector],
    taxAccountService,
    mockEploymentService,
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

  private def rtiResponse(
    aa: AnnualAccount = annualAccount
  ): EitherT[Future, UpstreamErrorResponse, Seq[AnnualAccount]] = EitherT(
    Future.successful[Either[UpstreamErrorResponse, Seq[AnnualAccount]]](Right(Seq(aa)))
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(baseUserAnswers)
    Mockito.reset(mockJourneyCacheRepository)
    Mockito.reset(mockApiBackendChoice)
    Mockito.reset(mockRtiService)
    Mockito.reset(mockEploymentService)
    when(mockApiBackendChoice.isNewApiBackendEnabled(any())).thenReturn(true)
  }

  private val employmentId = 1
  private val pensionId = 2

  "onPageLoad" must {
    "display the income details page" when {
      "asked for employment details" in {
        val userAnswers = baseUserAnswers
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(employmentId), true)
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockEploymentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(mockRtiService.getPaymentsForYear(any(), any())(any())).thenReturn(rtiResponse())
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
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }

      def setUpPension(): OngoingStubbing[Future[Boolean]] = {
        val userAnswers = baseUserAnswers
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(pensionId), true)
        setup(userAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockEploymentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment copy (receivingOccupationalPension = true))))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(mockJourneyCacheRepository.set(any())).thenReturn(Future.successful(true))
      }

      "asked for pension details and include RTI section where RTI data present" in {
        setUpPension()
        when(mockRtiService.getPaymentsForYear(any(), any())(any())).thenReturn(rtiResponse())

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages(
            "tai.pension.income.details.mainHeading.gaTitle",
            TaxYearRangeUtil.currentTaxYearRangeBreak.replaceAll("\u00A0", " ")
          )
        )
        Option(doc.getElementById("updatePension")).isDefined mustBe true
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }

      "asked for pension details and NOT include RTI section where RTI data NOT present" in {
        setUpPension()
        when(mockRtiService.getPaymentsForYear(any(), any())(any()))
          .thenReturn(EitherT(Future.successful[Either[UpstreamErrorResponse, Seq[AnnualAccount]]](Right(Nil))))
        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("updatePension")).isDefined mustBe false
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }

      "asked for pension details and NOT include RTI section where RTI data present but RTI unavailable" in {
        setUpPension()
        when(mockRtiService.getPaymentsForYear(any(), any())(any()))
          .thenReturn(rtiResponse(annualAccount copy (realTimeStatus = TemporarilyUnavailable)))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("updatePension")).isDefined mustBe false
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }

      "asked for pension details and NOT include RTI section where RTI response is 500" in {
        setUpPension()
        when(mockRtiService.getPaymentsForYear(any(), any())(any()))
          .thenReturn(
            EitherT(
              Future.successful[Either[UpstreamErrorResponse, Seq[AnnualAccount]]](
                Left(UpstreamErrorResponse("error", 500))
              )
            )
          )
        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("updatePension")).isDefined mustBe false
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }

      "asked for pension details and NOT include RTI section where left not found response" in {
        setUpPension()
        when(mockRtiService.getPaymentsForYear(any(), any())(any()))
          .thenReturn(
            EitherT(
              Future.successful[Either[UpstreamErrorResponse, Seq[AnnualAccount]]](
                Left(UpstreamErrorResponse("No payments found", NOT_FOUND))
              )
            )
          )
        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("updatePension")).isDefined mustBe false
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }

      "failed to read tax code incomes" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(mockEploymentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(mockRtiService.getPaymentsForYear(any(), any())(any())).thenReturn(rtiResponse())

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }
    }

    "throw error" when {
      "failed to read employment details" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockEploymentService.employmentOnly(any(), any(), any())(any())).thenReturn(Future.successful(None))
        when(mockRtiService.getPaymentsForYear(any(), any())(any()))
          .thenReturn(EitherT(Future.successful[Either[UpstreamErrorResponse, Seq[AnnualAccount]]](Right(Nil))))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }
    }

    "flush the cache" when {
      "cache update amount is the same as the HOD amount" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockEploymentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(mockRtiService.getPaymentsForYear(any(), any())(any())).thenReturn(rtiResponse())
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
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }
    }
    "display the income details page with an update message" when {
      "update is in progress for employment as cache update amount is different to the HOD amount" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockEploymentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(mockRtiService.getPaymentsForYear(any(), any())(any())).thenReturn(rtiResponse())
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
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }
    }

    "display the income details page with an update message" when {
      "update is in progress for pension as cache update amount is different to the HOD amount" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(mockEploymentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(mockRtiService.getPaymentsForYear(any(), any())(any())).thenReturn(rtiResponse())
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
        verify(mockEploymentService, times(1)).employmentOnly(any(), any(), any())(any())
      }
    }
  }
}
