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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatest.prop.TableDrivenPropertyChecks._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TrackingConnector
import uk.gov.hmrc.tai.model.domain.tracking._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TrackingServiceSpec extends PlaySpec
  with MockitoSugar
  with JourneyCacheConstants {

  val nino = new Generator().nextNino.nino

  private val name = "name1"

  "isAnyIFormInProgress" must {
    "return a time to process" when {

      "the iForm status is not done" in {
        val notDoneStatus = Table("TrackedFormStatus", TrackedFormReceived, TrackedFormAcquired, TrackedFormInProgress)

        forAll(notDoneStatus) { (status: TrackedFormStatus) =>
          when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("TES1", name, status))))

          val result = sut.isAnyIFormInProgress(nino)
          Await.result(result, 5 seconds) mustBe ThreeWeeks
        }
      }

      Seq("TES1", "TES7"
      ) foreach { case (tes) =>
        s"$tes should take three weeks to process" in {
          when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm(tes, name, TrackedFormReceived))))
          val result = sut.isAnyIFormInProgress(nino)
          Await.result(result, 5 seconds) mustBe ThreeWeeks
        }
      }

      Seq("TES2", "TES3", "TES4", "TES5", "TES6"
      ) foreach { case (tes) =>
        s"$tes should take seven days to process" in {
          when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm(tes, name, TrackedFormReceived))))
          val result = sut.isAnyIFormInProgress(nino)
          Await.result(result, 5 seconds) mustBe SevenDays
        }
      }

      "there is one iForm done and one IForm is in progress" in {
        val doneIForm = TrackedForm("TES4", name, TrackedFormDone)
        val notDoneIForm = TrackedForm("TES1", name, TrackedFormReceived)
        when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(doneIForm, notDoneIForm)))

        val result = sut.isAnyIFormInProgress(nino)
        Await.result(result, 5 seconds) mustBe ThreeWeeks
      }

      Seq(
        Map(TrackSuccessfulJourney_AddEmploymentKey -> "true"),
        Map(TrackSuccessfulJourney_EndEmploymentKey -> "true"),
        Map(TrackSuccessfulJourney_UpdateEmploymentKey -> "true"),
        Map(TrackSuccessfulJourney_UpdatePensionKey -> "true"),
        Map(TrackSuccessfulJourney_UpdatePreviousYearsIncomeKey -> "true"),
        Map(TrackSuccessfulJourney_AddPensionProviderKey -> "true")
      ) foreach { case entry =>
        s"user has completed add employment iFormJourney but tracking service returns empty sequence, for $entry" in {
          val controller = sut
          when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq.empty[TrackedForm]))
          when(successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(entry))

          val result = controller.isAnyIFormInProgress(nino)
          Await.result(result, 5 seconds) mustBe SevenDays
        }
      }

      Seq(
        Map(TrackSuccessfulJourney_EndEmploymentBenefitKey -> "true")
      ) foreach { case entry =>
        s"user has completed add employment iFormJourney but tracking service returns empty sequence, for $entry" in {
          val controller = sut
          when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq.empty[TrackedForm]))
          when(successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(entry))

          val result = controller.isAnyIFormInProgress(nino)
          Await.result(result, 5 seconds) mustBe ThreeWeeks
        }
      }


      "tracking service throws back an exception but user has completed a journey" in {
        val controller = sut
        when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.failed(new RuntimeException("an error occurred")))
        when(successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(TrackSuccessfulJourney_AddEmploymentKey -> "true")))

        val result = controller.isAnyIFormInProgress(nino)
        Await.result(result, 5 seconds) mustBe SevenDays
      }
    }

    "return no time to process" when {
      "there is no iForm in progress" in {
        when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("TES1", name, TrackedFormDone))))
        val result = sut.isAnyIFormInProgress(nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }

      "the TES value is not valid" in {
        when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq(TrackedForm("AAA", name, TrackedFormDone))))
        val result = sut.isAnyIFormInProgress(nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }

      "tracking service throws back an exception" in {
        when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.failed(new Exception("an error occurred")))
        val result = sut.isAnyIFormInProgress(nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def sut = new TrackingServiceTest

  val trackingConnector: TrackingConnector = mock[TrackingConnector]
  val successfulJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  private class TrackingServiceTest extends TrackingService(
    trackingConnector,
    successfulJourneyCacheService
  ) {
    when(successfulJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map.empty[String, String]))
  }

}
