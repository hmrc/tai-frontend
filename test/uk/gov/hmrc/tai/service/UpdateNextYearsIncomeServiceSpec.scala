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

import org.apache.pekko.Done
import controllers.FakeTaiPlayApplication
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper.convertCurrencyToInt
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants
import utils.BaseSpec

import scala.concurrent.Future

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

  private def taxCodeIncome(name: String, id: Int, amount: Int): TaxCodeIncome =
    TaxCodeIncome(EmploymentIncome, Some(id), amount, "description", "1185L", name, OtherBasisOfOperation, Live)

  private def expectedMap(name: String, id: Int, isPension: Boolean, amount: Int): Map[String, String] =
    Map(
      UpdateNextYearsIncomeConstants.EmploymentName -> name,
      UpdateNextYearsIncomeConstants.EmploymentId   -> id.toString,
      UpdateNextYearsIncomeConstants.IsPension      -> isPension.toString,
      UpdateNextYearsIncomeConstants.CurrentAmount  -> amount.toString
    )

  private def fullMap(name: String, id: Int, isPension: Boolean, amount: Int): Map[String, String] =
    expectedMap(name, id, isPension, amount) ++ Map(UpdateNextYearsIncomeConstants.NewAmount -> amount.toString)

  private val employmentName = "employmentName"
  private val employmentId = 1
  private val isPension = false
  private val employmentAmount = 1000

  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val successfulJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  class UpdateNextYearsIncomeServiceTest
      extends UpdateNextYearsIncomeService(
        journeyCacheService,
        successfulJourneyCacheService,
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

    when(successfulJourneyCacheService.cache(any())(any()))
      .thenReturn(Future.successful(Map(UpdateNextYearsIncomeConstants.Successful -> "true")))

    when(successfulJourneyCacheService.cache(any())(any()))
      .thenReturn(Future.successful(Map(s"${UpdateNextYearsIncomeConstants.Successful}-$employmentId" -> "true")))

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
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(successfulJourneyCacheService)
  }

  "get" must {
    "initialize the journey cache and return the cache model" when {
      "an taxCodeIncome and Employment is returned" in {
        when(employmentService.employment(meq(nino), meq(employmentId))(any()))
          .thenReturn(Future.successful(Some(employment(employmentName))))

        when(
          taxAccountService
            .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any(), any())
        )
          .thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

        when(journeyCacheService.currentCache(any())).thenReturn(
          Future.successful(Map.empty[String, String])
        )

        updateNextYearsIncomeService.get(employmentId, nino).futureValue mustBe UpdateNextYearsIncomeCacheModel(
          employmentName,
          employmentId,
          isPension,
          employmentAmount
        )
      }
    }

    "throw a runtime exception" when {
      "could not retrieve a TaxCodeIncome" in {
        when(employmentService.employment(meq(nino), meq(employmentId))(any()))
          .thenReturn(Future.successful(Some(employment(employmentName))))

        when(
          taxAccountService
            .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any(), any())
        )
          .thenReturn(Future.successful(Right(None)))

        val result = updateNextYearsIncomeService.get(employmentId, nino)

        whenReady(result.failed) { e =>
          e mustBe a[RuntimeException]
          e.getMessage must include(
            "[UpdateNextYearsIncomeService] Could not set up next years estimated income journey"
          )
        }
      }

      "could not retrieve a Employment" in {
        when(employmentService.employment(meq(nino), meq(employmentId))(any()))
          .thenReturn(Future.successful(None))

        when(
          taxAccountService
            .taxCodeIncomeForEmployment(meq(nino), meq(TaxYear().next), meq(employmentId))(any(), any())
        )
          .thenReturn(Future.successful(Right(Some(taxCodeIncome(employmentName, employmentId, employmentAmount)))))

        val result = updateNextYearsIncomeService.get(employmentId, nino)

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

        when(journeyCacheService.currentCache(any())).thenReturn(
          Future.successful(Map[String, String]())
        )

        val result = updateNextYearsIncomeService.get(employmentId, nino)

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

        when(journeyCacheService.currentCache(any())).thenReturn(
          Future.successful(fullMap(employmentName, employmentId, isPension, employmentAmount))
        )

        val result = updateNextYearsIncomeService.get(newEmploymentId, nino)

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
    "caches the amount with the employment id key" in {

      val key = s"${UpdateNextYearsIncomeConstants.NewAmount}-$employmentId"
      val amount = convertCurrencyToInt(Some(employmentAmount.toString)).toString
      val expected = Map(key -> amount)

      when(
        journeyCacheService.cache(any(), any())(any())
      ).thenReturn(
        Future.successful(expected)
      )

      val result = updateNextYearsIncomeService.setNewAmount(employmentAmount.toString, employmentId)

      result.futureValue mustBe expected

      verify(
        journeyCacheService,
        times(1)
      ).cache(any(), any())(any())
    }
  }

  "submit" must {
    "post the values from cache to the tax account" in new SubmitSetup {
      val service = new UpdateNextYearsIncomeServiceTest

      when(journeyCacheService.mandatoryJourneyValueAsInt(service.amountKey(employmentId)))
        .thenReturn(Future.successful(Right(employmentAmount)))

      service.submit(employmentId, nino).futureValue mustBe Done

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
      val service = new UpdateNextYearsIncomeServiceTest

      when(journeyCacheService.mandatoryJourneyValueAsInt(service.amountKey(employmentId)))
        .thenReturn(Future.successful(Right(employmentAmount)))

      service.submit(employmentId, nino).futureValue mustBe Done

      verify(successfulJourneyCacheService, times(1)).cache(Map(UpdateNextYearsIncomeConstants.Successful -> "true"))
      verify(successfulJourneyCacheService, times(1))
        .cache(Map(s"${UpdateNextYearsIncomeConstants.Successful}-$employmentId" -> "true"))
    }

    "return a TaiCacheError if there is a cache error" in new SubmitSetup {
      val service = new UpdateNextYearsIncomeServiceTest

      val errorMessage = "cache error"

      when(journeyCacheService.mandatoryJourneyValueAsInt(service.amountKey(employmentId)))
        .thenReturn(Future.successful(Left(errorMessage)))

      whenReady(service.submit(employmentId, nino).failed) { err =>
        err.getMessage mustBe errorMessage
      }
    }
  }

  "isEstimatedPayJourneyComplete" must {
    "be true when a journey is successful" in {
      val service = new UpdateNextYearsIncomeServiceTest

      when(successfulJourneyCacheService.currentCache(any()))
        .thenReturn(Future.successful(Map(UpdateNextYearsIncomeConstants.Successful -> "true")))

      service.isEstimatedPayJourneyComplete.futureValue mustBe true
    }

    "be false when a journey is incomplete" in {
      val service = new UpdateNextYearsIncomeServiceTest

      when(successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))

      service.isEstimatedPayJourneyComplete.futureValue mustBe false
    }
  }
}
