/*
 * Copyright 2019 HM Revenue & Customs
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

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class EstimatedPayJourneyCompletionServiceSpec extends PlaySpec with MockitoSugar with JourneyCacheConstants{

  private def createTestService = new EstimatedPayJourneyCompletionServiceTest

  val successfulJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
  private implicit val hc: HeaderCarrier = HeaderCarrier()



  private class EstimatedPayJourneyCompletionServiceTest extends EstimatedPayJourneyCompletionService(
    successfulJourneyCacheService

  )

  "Estimated Pay Journey Completed Service" must {
    "add a cache entry upon successful completion of a journey" in {
      val incomeId = "1"
      val trueValue = true.toString
      val idKey=s"$TrackSuccessfulJourney_EstimatedPayKey-$incomeId"
      when(successfulJourneyCacheService.cache(Matchers.eq(idKey), Matchers.eq(trueValue))(any())).thenReturn(Future.successful(Map(idKey -> trueValue)))

      Await.result(createTestService.journeyCompleted(incomeId)(hc), 5 seconds)
      verify(successfulJourneyCacheService, times(1)).cache(Matchers.eq(idKey),Matchers.eq(trueValue))(any())
    }

  }

}
