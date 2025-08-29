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

package uk.gov.hmrc.tai.service

import controllers.FakeTaiPlayApplication
import controllers.auth.{AuthedUser, DataRequest}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, times, verify, when}
import pages.income.{UpdateIncomeNewAmountPage, UpdateNextYearsIncomeNewAmountPage, UpdateNextYearsIncomeSuccessPage, UpdateNextYearsIncomeSuccessPageForEmployment}
import play.api.mvc.AnyContent
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.model.domain.income.*
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.util.FormHelper.convertCurrencyToInt
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
      cessationPay = None,
      employmentType = EmploymentIncome
    )

  def randomNino(): Nino = new Generator(new Random()).nextNino

  private def taxCodeIncome(name: String, id: Int, amount: Int): TaxCodeIncome =
    TaxCodeIncome(EmploymentIncome, Some(id), amount, "description", "1185L", name, OtherBasisOfOperation, Live)

  private val employmentName   = "employmentName"
  private val employmentId     = 1
  private val isPension        = false
  private val employmentAmount = 1000
  private val sessionId        = "testSessionId"

  val employmentService: EmploymentService               = mock[EmploymentService]
  val taxAccountService: TaxAccountService               = mock[TaxAccountService]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  class UpdateNextYearsIncomeServiceTest
      extends UpdateNextYearsIncomeService(
        mockJourneyCacheRepository,
        employmentService,
        taxAccountService
      )

  val updateNextYearsIncomeService = new UpdateNextYearsIncomeServiceTest

  class SubmitSetup {
    when(employmentService.employment(meq(nino), meq(employmentId))(any()))
      .thenReturn(Future.successful(Some(employment(employmentName))))

    when(
      taxAccountService
        .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any())
    ).thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

    when(mockJourneyCacheRepository.keepAlive(any(), any())).thenReturn(Future.successful(true))

    when(
      taxAccountService.updateEstimatedIncome(
        meq(nino),
        meq(employmentAmount),
        meq(TaxYear().next),
        meq(employmentId)
      )(any())
    ).thenReturn(Future.successful(Done))

    when(mockJourneyCacheRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(employmentService, taxAccountService, mockJourneyCacheRepository)
  }

  "get" must {
    "return the cache model when TCI and Employment are returned" in {
      val userAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)

      when(mockJourneyCacheRepository.keepAlive(any(), any())).thenReturn(Future.successful(true))
      when(employmentService.employment(meq(nino), meq(employmentId))(any()))
        .thenReturn(Future.successful(Some(employment(employmentName))))
      when(
        taxAccountService
          .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any())
      ).thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

      val result = updateNextYearsIncomeService.get(employmentId, nino, userAnswers)

      result.futureValue mustBe UpdateNextYearsIncomeCacheModel(
        employmentName,
        employmentId,
        isPension,
        Some(employmentAmount)
      )
    }

    "return cache model with None currentValue when taxCodeIncomeForEmployment returns Right(None)" in {
      val userAnswers = UserAnswers(sessionId, randomNino().nino)

      when(mockJourneyCacheRepository.keepAlive(any(), any())).thenReturn(Future.successful(true))
      when(employmentService.employment(meq(nino), meq(employmentId))(any()))
        .thenReturn(Future.successful(Some(employment(employmentName))))
      when(
        taxAccountService
          .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any())
      ).thenReturn(Future.successful(Right(None)))

      val result = updateNextYearsIncomeService.get(employmentId, nino, userAnswers)

      result.futureValue mustBe UpdateNextYearsIncomeCacheModel(
        employmentName,
        employmentId,
        isPension = false,
        currentValue = None
      )
    }

    "throw a runtime exception when employment cannot be retrieved" in {
      val userAnswers = UserAnswers(sessionId, randomNino().nino)

      when(mockJourneyCacheRepository.keepAlive(any(), any())).thenReturn(Future.successful(true))
      when(employmentService.employment(meq(nino), meq(employmentId))(any()))
        .thenReturn(Future.successful(None))
      when(
        taxAccountService
          .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any())
      ).thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

      val result = updateNextYearsIncomeService.get(employmentId, nino, userAnswers)

      whenReady(result.failed) { e =>
        e mustBe a[RuntimeException]
        e.getMessage must include(
          "[UpdateNextYearsIncomeService] Could not set up next years estimated income journey"
        )
      }
    }

    "set up cache via keepAlive when journey values are not present" in {
      when(mockJourneyCacheRepository.keepAlive(any(), any())).thenReturn(Future.successful(true))
      when(employmentService.employment(meq(nino), meq(employmentId))(any()))
        .thenReturn(Future.successful(Some(employment(employmentName))))
      when(
        taxAccountService
          .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any())
      ).thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

      val result = updateNextYearsIncomeService.get(employmentId, nino, userAnswers)

      result.futureValue mustBe UpdateNextYearsIncomeCacheModel(
        employmentName,
        employmentId,
        isPension,
        Some(employmentAmount)
      )
    }

    "support switching employer ids" in {
      val newEmploymentId = 2

      when(mockJourneyCacheRepository.keepAlive(any(), any())).thenReturn(Future.successful(true))
      when(employmentService.employment(meq(nino), meq(newEmploymentId))(any()))
        .thenReturn(Future.successful(Some(employment(employmentName))))
      when(
        taxAccountService
          .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(newEmploymentId))(any())
      ).thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, newEmploymentId, employmentAmount)))))

      val ua     = UserAnswers(sessionId, randomNino().nino)
      val result = updateNextYearsIncomeService.get(newEmploymentId, nino, ua)

      result.futureValue mustBe UpdateNextYearsIncomeCacheModel(
        employmentName,
        newEmploymentId,
        isPension,
        Some(employmentAmount)
      )
    }
  }

  "setNewAmount" must {
    "cache the amount with the employment-specific page key" in {
      val amount   = convertCurrencyToInt(Some(employmentAmount.toString)).toString
      val expected = Map(UpdateNextYearsIncomeNewAmountPage(employmentId).toString -> amount)

      when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

      val result =
        updateNextYearsIncomeService.setNewAmount(employmentAmount.toString, employmentId, userAnswers)

      result.futureValue mustBe expected
      verify(mockJourneyCacheRepository).set(any())
    }
  }

  "submit" must {
    "post values to tax account, then cache success flags" in new SubmitSetup {
      val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateNextYearsIncomeNewAmountPage(employmentId), employmentAmount.toString)
        .setOrException(UpdateNextYearsIncomeSuccessPageForEmployment(employmentId), true)
        .setOrException(UpdateNextYearsIncomeSuccessPage, true)

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

      verify(mockJourneyCacheRepository, times(1)).set(any[UserAnswers])
    }

    "cache success when it completes" in new SubmitSetup {
      val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateNextYearsIncomeNewAmountPage(employmentId), employmentAmount.toString)
        .setOrException(UpdateNextYearsIncomeSuccessPageForEmployment(employmentId), true)
        .setOrException(UpdateNextYearsIncomeSuccessPage, true)
        .setOrException(UpdateIncomeNewAmountPage, employmentAmount.toString)

      val result: Future[Done] = updateNextYearsIncomeService.submit(employmentId, nino, mockUserAnswers)

      result.futureValue mustBe Done
      verify(mockJourneyCacheRepository, times(1)).set(any[UserAnswers])
    }

    "fail when getNewAmount returns a Left (no amount present)" in new SubmitSetup {
      val key: String  = UpdateNextYearsIncomeNewAmountPage(employmentId).toString
      val errorMessage = s"Value for $key not found"

      val mockUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)

      whenReady(updateNextYearsIncomeService.submit(employmentId, nino, mockUserAnswers).failed) { err =>
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
    userAnswers = UserAnswers("", "")
  )

  "isEstimatedPayJourneyComplete" must {
    "be true when a journey is successful" in {
      val service = new UpdateNextYearsIncomeServiceTest

      val ua =
        UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateNextYearsIncomeSuccessPageForEmployment(employmentId), true)
          .setOrException(UpdateNextYearsIncomeSuccessPage, true)

      service.isEstimatedPayJourneyComplete(ua).futureValue mustBe true
    }

    "be false when a journey is incomplete" in {
      val service = new UpdateNextYearsIncomeServiceTest
      val ua      = UserAnswers(sessionId, randomNino().nino)

      service.isEstimatedPayJourneyComplete(ua).futureValue mustBe false
    }
  }

  "isEstimatedPayJourneyCompleteForEmployer" must {
    "return true when success flag for employment is set" in {
      val ua = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateNextYearsIncomeSuccessPageForEmployment(employmentId), true)

      updateNextYearsIncomeService
        .isEstimatedPayJourneyCompleteForEmployer(employmentId, ua)
        .futureValue mustBe true
    }

    "return false when no flag is set for employment" in {
      val ua = UserAnswers(sessionId, randomNino().nino)

      updateNextYearsIncomeService
        .isEstimatedPayJourneyCompleteForEmployer(employmentId, ua)
        .futureValue mustBe false
    }
  }
}
