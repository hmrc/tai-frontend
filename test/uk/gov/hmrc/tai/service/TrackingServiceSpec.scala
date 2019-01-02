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

package uk.gov.hmrc.tai.service

import uk.gov.hmrc.tai.connectors.TrackingConnector
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.tai.model.domain.tracking._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

class TrackingServiceSpec extends PlaySpec with MockitoSugar with JourneyCacheConstants {

  "isAnyIFormInProgress" must {
    "return true" when {
      "there is an iForm with status In Progress" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("TES1", "name1", TrackedFormInProgress))))
        when(sut.successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        val result = sut.isAnyIFormInProgress(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe true
      }

      "there is an iForm with status in Acquired" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("TES2", "name1", TrackedFormAcquired))))
        when(sut.successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        val result = sut.isAnyIFormInProgress(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe true
      }

      "return true when there is an iForm with status in Received" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("TES3", "name1", TrackedFormReceived))))
        when(sut.successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        val result = sut.isAnyIFormInProgress(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe true
      }

      "there is one iForm done and one IForm is in progress" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("TES4", "name1", TrackedFormDone),
          TrackedForm("TES1", "name1", TrackedFormReceived))))
        when(sut.successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        val result = sut.isAnyIFormInProgress(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe true
      }

      "user has completed add employment iFormJourney but tracking service has return empty sequence" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq.empty[TrackedForm]))
        when(sut.successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(TrackSuccessfulJourney_AddEmploymentKey -> "true")))
        val result = sut.isAnyIFormInProgress(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe true
      }

      "tracking service throws back an exception but user has completed a journey" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.failed(new RuntimeException("an error occurred")))
        when(sut.successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map[String, String](TrackSuccessfulJourney_AddEmploymentKey -> "true")))
        val result = sut.isAnyIFormInProgress(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe true
      }
    }

    "return false" when {
      "there is no iForm in progress" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("TES1", "name1", TrackedFormDone))))
        when(sut.successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        val result = sut.isAnyIFormInProgress(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe false
      }

      "tracking service throws back an exception" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.failed(new Exception("an error occurred")))
        when(sut.successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
        val result = sut.isAnyIFormInProgress(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe false
      }
    }
  }

  "trackingForTesForms" must {
    "return nil" when {
      "the list from tracking connector has a form that is not TES" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("a1", "name1", TrackedFormDone))))
        val result = sut.trackingForTesForms(new Generator().nextNino.nino)
        Await.result(result, 5 seconds) mustBe Nil
      }
    }

    "return TES forms from TES1 to TES7" when {
      "the list from tracking connector has TES and non-TES uk.gov.hmrc.tai.forms" in {
        val sut = createSut
        when(sut.trackingConnector.getUserTracking(any())(any())).
          thenReturn(Future.successful(Seq(
            TrackedForm("TES0", "name1", TrackedFormDone),
            TrackedForm("TES1", "name1", TrackedFormDone),
            TrackedForm("TES2", "name1", TrackedFormDone),
            TrackedForm("TES3", "name1", TrackedFormDone),
            TrackedForm("TES4", "name1", TrackedFormDone),
            TrackedForm("TES5", "name1", TrackedFormDone),
            TrackedForm("TES6", "name1", TrackedFormDone),
            TrackedForm("TES7", "name1", TrackedFormDone),
            TrackedForm("TES8", "name1", TrackedFormDone),
            TrackedForm("AAA1", "name1", TrackedFormDone),
            TrackedForm("AAA", "name1", TrackedFormDone))))
        val result = sut.trackingForTesForms(new Generator().nextNino.nino)
        val expectedResult = Seq(
          TrackedForm("TES1", "name1", TrackedFormDone),
          TrackedForm("TES2", "name1", TrackedFormDone),
          TrackedForm("TES3", "name1", TrackedFormDone),
          TrackedForm("TES4", "name1", TrackedFormDone),
          TrackedForm("TES5", "name1", TrackedFormDone),
          TrackedForm("TES6", "name1", TrackedFormDone),
          TrackedForm("TES7", "name1", TrackedFormDone))

        Await.result(result, 5 seconds) mustBe expectedResult
      }
    }
  }


  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createSut = new TrackingServiceTest

  val trackingConnector: TrackingConnector = mock[TrackingConnector]
  val successfulJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  private class TrackingServiceTest extends TrackingService(
    trackingConnector,
    successfulJourneyCacheService
  )

}
