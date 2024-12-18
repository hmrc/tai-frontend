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

package uk.gov.hmrc.tai.service

import controllers.FakeTaiPlayApplication
import controllers.auth.{AuthedUser, DataRequest}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, times, verify, when}
import pages.income.{UpdateIncomeNewAmountPage, UpdateNextYearsIncomeNewAmountPage, UpdateNextYearsIncomeSuccessPage, UpdateNextYearsIncomeSuccessPageForEmployment}
import play.api.mvc.AnyContent
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.util.FormHelper.convertCurrencyToInt
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants
import utils.BaseSpec

import scala.concurrent.Future
import scala.util.Random

class UpdateNextYearsIncomeServiceSpec extends BaseSpec with FakeTaiPlayApplication {

  private def employment(name: String): Employment =
    Employment(
      name = name,
      employmentStatus = Live,
      payrollNumber = None,
      startDate = Some(TaxYear().start),
      endDate = None,
      annualAccounts = Seq.empty,
      taxDistrictNumber = "123",
      payeNumber = "321",
      sequenceNumber = 1,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      cessationPay = None
    )

  def randomNino(): Nino = new Generator(new Random()).nextNino

  private def taxCodeIncome(name: String, id: Int, amount: Int): TaxCodeIncome =
    TaxCodeIncome(EmploymentIncome, Some(id), amount, "description", "1185L", name, OtherBasisOfOperation, Live)

  private val employmentName = "employmentName"
  private val employmentId = 1
  private val isPension = false
  private val employmentAmount = 1000
  private val sessionId = "testSessionId"

  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  class UpdateNextYearsIncomeServiceTest
      extends UpdateNextYearsIncomeService(
        mockJourneyCacheNewRepository,
        employmentService,
        taxAccountService
      )

  val updateNextYearsIncomeService = new UpdateNextYearsIncomeServiceTest

  class SubmitSetup {
    when(employmentService.employment(meq(nino), meq(employmentId))(any()))
      .thenReturn(Future.successful(Some(employment(employmentName))))

    when(
      taxAccountService
        .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any(), any())
    )
      .thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

    when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(None))

    when(
      taxAccountService.updateEstimatedIncome(
        meq(nino),
        meq(employmentAmount),
        meq(TaxYear().next),
        meq(employmentId)
      )(any())
    ).thenReturn(
      Future.successful(Done)
    )

    when(mockJourneyCacheNewRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(employmentService, taxAccountService, mockJourneyCacheNewRepository)
  }

  "get" must {
    "return the cache model" when {
      "an taxCodeIncome and Employment is returned" in {
        val userAnswers: UserAnswers = UserAnswers(sessionId)
        when(mockJourneyCacheNewRepository.get(any())).thenReturn(
          Future.successful(Some(userAnswers))
        )

        when(employmentService.employment(meq(nino), meq(employmentId))(any()))
          .thenReturn(Future.successful(Some(employment(employmentName))))

        when(
          taxAccountService
            .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any(), any())
        )
          .thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

        val result = updateNextYearsIncomeService.get(employmentId, nino, userAnswers)

        result.futureValue mustBe UpdateNextYearsIncomeCacheModel(
          employmentName,
          employmentId,
          isPension,
          employmentAmount
        )
      }
    }

    "throw a runtime exception" when {
      "could not retrieve a TaxCodeIncome" in {
        val userAnswers = UserAnswers(sessionId)
        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

        when(employmentService.employment(meq(nino), meq(employmentId))(any()))
          .thenReturn(Future.successful(Some(employment(employmentName))))

        when(
          taxAccountService
            .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any(), any())
        )
          .thenReturn(Future.successful(Right(None)))

        val result = updateNextYearsIncomeService.get(employmentId, nino, userAnswers)

        whenReady(result.failed) { e =>
          e mustBe a[RuntimeException]
          e.getMessage must include(
            "[UpdateNextYearsIncomeService] Could not set up next years estimated income journey"
          )
        }
      }

      "could not retrieve a Employment" in {
        val userAnswers = UserAnswers(sessionId)
        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

        when(employmentService.employment(meq(nino), meq(employmentId))(any()))
          .thenReturn(Future.successful(None))

        when(
          taxAccountService
            .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any(), any())
        )
          .thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

        val result = updateNextYearsIncomeService.get(employmentId, nino, userAnswers)

        whenReady(result.failed) { e =>
          e mustBe a[RuntimeException]
          e.getMessage must include(
            "[UpdateNextYearsIncomeService] Could not set up next years estimated income journey"
          )
        }
      }
    }

    "setup the cache" when {
      "journey values do not exist in the cache" in {
        when(employmentService.employment(meq(nino), meq(employmentId))(any()))
          .thenReturn(Future.successful(Some(employment(employmentName))))

        when(
          taxAccountService
            .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any(), any())
        )
          .thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(None))

        val result = updateNextYearsIncomeService.get(employmentId, nino, userAnswers)

        result.futureValue mustBe UpdateNextYearsIncomeCacheModel(
          employmentName,
          employmentId,
          isPension,
          employmentAmount
        )
      }

      "user selects a different employer" in {
        val newEmploymentId = 2

        when(employmentService.employment(meq(nino), meq(newEmploymentId))(any()))
          .thenReturn(Future.successful(Some(employment(employmentName))))

        when(
          taxAccountService
            .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(newEmploymentId))(any(), any())
        )
          .thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, newEmploymentId, employmentAmount)))))

        val userAnswers = UserAnswers(sessionId)
        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

        val result = updateNextYearsIncomeService.get(newEmploymentId, nino, userAnswers)

        result.futureValue mustBe UpdateNextYearsIncomeCacheModel(
          employmentName,
          newEmploymentId,
          isPension,
          employmentAmount
        )
      }
    }
  }

  "setNewAmount" must {
    "cache the amount with the employment ID key" in {

      val key = s"${UpdateNextYearsIncomeConstants.NewAmount}"
      val amount = convertCurrencyToInt(Some(employmentAmount.toString)).toString
      val expected = Map(key -> amount)

      val mockUserAnswers: UserAnswers = UserAnswers(sessionId)
        .setOrException(UpdateNextYearsIncomeNewAmountPage(employmentId), amount)

      when(mockJourneyCacheNewRepository.get(any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

      val result: Future[Map[String, String]] =
        updateNextYearsIncomeService.setNewAmount(employmentAmount.toString, employmentId, userAnswers)

      result.futureValue mustBe expected

      verify(mockJourneyCacheNewRepository).set(any())
    }
  }

  "submit" must {
    "post the values from cache to the tax account" in new SubmitSetup {

      val mockUserAnswers: UserAnswers = UserAnswers(sessionId)
        .setOrException(UpdateNextYearsIncomeNewAmountPage(employmentId), employmentAmount.toString)
        .setOrException(UpdateNextYearsIncomeSuccessPageForEmployment(employmentId), "true")
        .setOrException(UpdateNextYearsIncomeSuccessPage, "true")

      when(mockJourneyCacheNewRepository.get(any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

      val result: Future[Done] = updateNextYearsIncomeService.submit(employmentId, nino, mockUserAnswers)

      result.futureValue mustBe Done

      verify(
        taxAccountService,
        times(1)
      ).updateEstimatedIncome(
        meq(nino),
        meq(employmentAmount),
        meq(TaxYear().next),
        meq(employmentId)
      )(any())
    }

    "cache as a successful journey" in new SubmitSetup {

      val mockUserAnswers: UserAnswers = UserAnswers(sessionId)
        .setOrException(UpdateNextYearsIncomeNewAmountPage(employmentId), employmentAmount.toString)
        .setOrException(UpdateNextYearsIncomeSuccessPageForEmployment(employmentId), "true")
        .setOrException(UpdateNextYearsIncomeSuccessPage, "true")
        .setOrException(UpdateIncomeNewAmountPage, employmentAmount.toString)

      when(mockJourneyCacheNewRepository.get(any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val service = new UpdateNextYearsIncomeServiceTest

      val result: Future[Done] = service.submit(employmentId, nino, mockUserAnswers)

      result.futureValue mustBe Done

      verify(mockJourneyCacheNewRepository, times(1)).set(any[UserAnswers])
    }

    "return an error if getNewAmount returns a Left" in new SubmitSetup {
      val key: String = UpdateNextYearsIncomeNewAmountPage(employmentId).toString
      val errorMessage = s"Value for $key not found"

      val mockUserAnswers: UserAnswers = UserAnswers(sessionId)
      when(mockJourneyCacheNewRepository.get(any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val service = new UpdateNextYearsIncomeServiceTest

      whenReady(service.submit(employmentId, nino, mockUserAnswers).failed) { err =>
        err.getMessage mustBe errorMessage
      }
    }
  }

  protected implicit val dataRequest: DataRequest[AnyContent] = DataRequest(
    fakeRequest,
    taiUser = AuthedUser(
      Nino(nino.toString()),
      Some("saUtr"),
      None
    ),
    fullName = "",
    userAnswers = UserAnswers("")
  )

  "isEstimatedPayJourneyComplete" must {
    "be true when a journey is successful" in {
      val service = new UpdateNextYearsIncomeServiceTest

      val userAnswers =
        UserAnswers(sessionId)
          .setOrException(UpdateNextYearsIncomeSuccessPageForEmployment(employmentId), "true")
          .setOrException(UpdateNextYearsIncomeSuccessPage, "true")
      when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

      service.isEstimatedPayJourneyComplete(userAnswers).futureValue mustBe true
    }

    "be false when a journey is incomplete" in {
      val service = new UpdateNextYearsIncomeServiceTest
      val emptyUserAnswers = UserAnswers(sessionId)
      when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(emptyUserAnswers)))

      service.isEstimatedPayJourneyComplete(emptyUserAnswers).futureValue mustBe false
    }
  }
}
