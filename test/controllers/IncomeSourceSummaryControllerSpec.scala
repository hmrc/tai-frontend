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
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import play.api.i18n.Messages
import play.api.test.Helpers._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, GenericBenefit}
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.TaiConstants.UpdateIncomeConfirmedAmountKey
import utils.BaseSpec
import views.html.IncomeSourceSummaryView

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceSummaryControllerSpec extends BaseSpec {

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
    "test employment",
    Live,
    Some("EMPLOYER-1122"),
    Some(LocalDate.now()),
    None,
    Seq(annualAccount),
    "",
    "",
    2,
    None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false
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
  val estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService =
    mock[EstimatedPayJourneyCompletionService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  def sut = new IncomeSourceSummaryController(
    mock[AuditConnector],
    journeyCacheService,
    taxAccountService,
    employmentService,
    benefitsService,
    estimatedPayJourneyCompletionService,
    mockAuthJourney,
    appConfig,
    mcc,
    inject[IncomeSourceSummaryView],
    mockJourneyCacheNewRepository,
    inject[ErrorPagesHandler]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers("testSessionId"))
    Mockito.reset(journeyCacheService, mockJourneyCacheNewRepository)
  }

  val employmentId = 1
  val pensionId = 2
  val cacheKeyEmployment = s"$UpdateIncomeConfirmedAmountKey-$employmentId"
  val cacheKeyPension = s"$UpdateIncomeConfirmedAmountKey-$pensionId"

  "onPageLoad" must {
    "display the income details page" when {
      "asked for employment details" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(meq(employmentId.toString))(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(journeyCacheService.currentValueAsInt(meq(cacheKeyEmployment))(any(), any(), any(), any()))
          .thenReturn(Future.successful(None))
        when(journeyCacheService.flushWithEmpId(meq(employmentId))(any())).thenReturn(Future.successful(Done))
        when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

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
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(meq(pensionId.toString))(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(journeyCacheService.currentValueAsInt(meq(cacheKeyPension))(any(), any(), any(), any()))
          .thenReturn(Future.successful(None))
        when(journeyCacheService.flushWithEmpId(meq(pensionId))(any())).thenReturn(Future.successful(Done))
        when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

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
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(meq(employmentId.toString))(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(
          journeyCacheService.currentValueAsInt(meq(cacheKeyEmployment))(any(), any(), any(), any())
        ) thenReturn Future
          .successful(Some(1111))
        when(journeyCacheService.flushWithEmpId(meq(employmentId))(any())) thenReturn Future.successful(Done)
        when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages("tai.employment.income.details.mainHeading.gaTitle", TaxYearRangeUtil.currentTaxYearRangeBreak)
        )
        verify(journeyCacheService, times(1)).flushWithEmpId(meq(employmentId))(any())
        verify(mockJourneyCacheNewRepository, times(1)).set(any())
      }
    }
    "display the income details page with an update message" when {
      "update is in progress for employment as cache update amount is different to the HOD amount" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(meq(employmentId.toString))(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(
          journeyCacheService.currentValueAsInt(meq(cacheKeyEmployment))(any(), any(), any(), any())
        ) thenReturn Future
          .successful(Some(3333))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.toString must include(Messages("tai.income.details.updateInProgress"))
        verify(journeyCacheService, times(0)).flush()(any())
      }
    }
    "display the income details page with an update message" when {
      "update is in progress for pension as cache update amount is different to the HOD amount" in {
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(meq(pensionId.toString))(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(journeyCacheService.currentValueAsInt(meq(cacheKeyPension))(any(), any(), any(), any())) thenReturn Future
          .successful(Some(3333))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.toString must include(Messages("tai.income.details.updateInProgress"))
      }
    }
  }
}
