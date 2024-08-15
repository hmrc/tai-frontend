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

package controllers.benefits

import builders.RequestBuilder
import controllers.ErrorPagesHandler
import controllers.auth.{AuthedUser, DataRequest}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.stubbing.ScalaOngoingStubbing
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import play.api.i18n.Messages
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, Request, Result}
import play.api.test.Helpers._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.{Benefits, CompanyCarBenefit, GenericBenefit}
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.service.{EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.TaiConstants.UpdateIncomeConfirmedAmountKey
import utils.BaseSpec
import views.html.benefits.CompanyBenefitsView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class CompanyBenefitsSummaryControllerSpec extends BaseSpec {

  def randomNino(): Nino = new Generator(new Random()).nextNino

  val employmentId = 1
  val empId = "1"
  val pensionId = 2
  val sessionId = "testSessionId"
  val cacheKeyEmployment = s"$UpdateIncomeConfirmedAmountKey-$employmentId"
  val cacheKeyPension = s"$UpdateIncomeConfirmedAmountKey-$pensionId"

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

  val benefitsService: BenefitsService = mock[BenefitsService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService =
    mock[EstimatedPayJourneyCompletionService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]
  val mockPerson: Person = mock[Person]

  def sut: CompanyBenefitsSummaryController = new CompanyBenefitsSummaryController(
    taxAccountService,
    employmentService,
    benefitsService,
    estimatedPayJourneyCompletionService,
    mockAuthJourney,
    appConfig,
    mcc,
    inject[CompanyBenefitsView],
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

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheNewRepository)
    reset(benefitsService)
  }

  "onPageLoad" must {
    "display the benefits details page" when {
      "asked for benefits details" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsUpdateIncomePage(employmentId), employmentId.toString)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(meq(employmentId.toString))(any(), any()))
          .thenReturn(Future.successful(true))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages(
            "tai.income.details.companyBenefitsHeading",
            TaxYearRangeUtil.currentTaxYearRangeBreak.replaceAll("\u00A0", " ")
          )
        )
      }

      "asked for company details" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsUpdateIncomePage(employmentId), employmentId.toString)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(meq(pensionId.toString))(any(), any()))
          .thenReturn(Future.successful(true))

        val result = sut.onPageLoad(pensionId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages(
            "tai.income.details.companyBenefitsHeading",
            TaxYearRangeUtil.currentTaxYearRangeBreak.replaceAll("\u00A0", " ")
          )
        )
      }
    }

    "throw error" when {
      "failed to read tax code incomes" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsUpdateIncomePage(employmentId), employmentId.toString)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "failed to read employment details" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsUpdateIncomePage(employmentId), employmentId.toString)
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "flush the cache" when {
      "cache update amount is the same as the HOD amount" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsUpdateIncomePage(employmentId), employmentId.toString)
        setup(mockUserAnswers)

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(taxCodeIncomes)))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(benefitsService.benefits(any(), any())(any())).thenReturn(Future.successful(benefits))
        when(estimatedPayJourneyCompletionService.hasJourneyCompleted(meq(employmentId.toString))(any(), any()))
          .thenReturn(Future.successful(true))
        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

        val result = sut.onPageLoad(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages("tai.income.details.companyBenefitsHeading", TaxYearRangeUtil.currentTaxYearRangeBreak)
        )
      }
    }

  }

}
