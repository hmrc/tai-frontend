/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service.journeyCompletion

import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class EstimatedPayJourneyCompletionServiceSpec extends BaseSpec with JourneyCacheConstants with BeforeAndAfterEach {

  private def createTestService = new EstimatedPayJourneyCompletionServiceTest

  val successfulJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  val incomeId = "1"
  val trueValue = "true"
  val idKey = s"$TrackSuccessfulJourney_EstimatedPayKey-$incomeId"
  val failedCacheCall = Future.failed(new Exception)

  private class EstimatedPayJourneyCompletionServiceTest
      extends EstimatedPayJourneyCompletionService(
        successfulJourneyCacheService
      )

  override def beforeEach: Unit =
    Mockito.reset(successfulJourneyCacheService)

  "Estimated Pay Journey Completed Service" must {

    "add a successful journey completion" in {

      when(successfulJourneyCacheService.cache(meq(idKey), meq(trueValue))(any()))
        .thenReturn(Future.successful(Map(idKey -> trueValue)))
      Await.result(createTestService.journeyCompleted(incomeId)(hc), 5 seconds)
      verify(successfulJourneyCacheService, times(1)).cache(meq(idKey), meq(trueValue))(any())
    }

    "return an empty collection upon failing to add a journey completion" in {
      when(successfulJourneyCacheService.cache(meq(idKey), meq(trueValue))(any())).thenReturn(failedCacheCall)
      Await.result(createTestService.journeyCompleted(incomeId)(hc), 5 seconds) mustBe Map.empty[String, String]
    }

    "retrieve a successful journey completion" in {
      when(successfulJourneyCacheService.currentValue(meq(idKey))(any())).thenReturn(Future.successful(Some(trueValue)))
      Await.result(createTestService.hasJourneyCompleted(incomeId)(hc), 5 seconds)
      verify(successfulJourneyCacheService, times(1)).currentValue(meq(idKey))(any())
    }

    "return false upon failing to retrieve a journey completion" in {
      when(successfulJourneyCacheService.currentValue(meq(idKey))(any())).thenReturn(failedCacheCall)
      Await.result(createTestService.hasJourneyCompleted(incomeId)(hc), 5 seconds) mustBe false
    }

  }
}
