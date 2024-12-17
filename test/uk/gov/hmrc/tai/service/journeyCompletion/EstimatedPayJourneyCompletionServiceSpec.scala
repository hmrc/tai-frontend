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

package uk.gov.hmrc.tai.service.journeyCompletion

import controllers.auth.{AuthedUser, DataRequest}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class EstimatedPayJourneyCompletionServiceSpec extends BaseSpec {

  private def createTestService = new EstimatedPayJourneyCompletionServiceTest

  val successfulJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  val incomeId = "1"
  val trueValue = "true"
  val idKey = s"${TrackSuccessfulJourneyConstants.EstimatedPayKey}-$incomeId"
  val failedCacheCall = Future.failed(new Exception)

  private class EstimatedPayJourneyCompletionServiceTest
      extends EstimatedPayJourneyCompletionService(
        successfulJourneyCacheService
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(successfulJourneyCacheService)
  }

  protected val dataRequest: DataRequest[AnyContent] = DataRequest(
    fakeRequest,
    taiUser = AuthedUser(
      Nino(nino.toString()),
      Some("saUtr"),
      None
    ),
    fullName = "",
    userAnswers = UserAnswers("")
  )

  "Estimated Pay Journey Completed Service" must {

    "retrieve a successful journey completion" in {
      when(successfulJourneyCacheService.currentValue(meq(idKey))(any(), any(), any(), any()))
        .thenReturn(Future.successful(Some(trueValue)))
      Await.result(createTestService.hasJourneyCompleted(incomeId)(hc, ec, dataRequest), 5 seconds)
      verify(successfulJourneyCacheService, times(1)).currentValue(meq(idKey))(any(), any(), any(), any())
    }

    "return false upon failing to retrieve a journey completion" in {
      when(successfulJourneyCacheService.currentValue(meq(idKey))(any(), any(), any(), any()))
        .thenReturn(failedCacheCall)
      Await.result(createTestService.hasJourneyCompleted(incomeId)(hc, ec, dataRequest), 5 seconds) mustBe false
    }

  }
}
