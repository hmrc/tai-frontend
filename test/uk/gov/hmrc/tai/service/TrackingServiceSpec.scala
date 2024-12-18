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

import controllers.auth.{AuthedUser, DataRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.tai.connectors.TrackingConnector
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.tracking._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class TrackingServiceSpec extends BaseSpec {

  private val name = "name1"

  private def sut = new TrackingServiceTest

  val trackingConnector: TrackingConnector = mock[TrackingConnector]
  private class TrackingServiceTest extends TrackingService(trackingConnector)

  val employmentId = 1

  def mockDataRequest(userAnswersData: Map[String, String]): DataRequest[AnyContent] = {
    val userAnswers = UserAnswers("testSessionId", Json.toJson(userAnswersData).as[JsObject])
    DataRequest(
      fakeRequest,
      taiUser = AuthedUser(
        Nino(nino.toString()),
        Some("saUtr"),
        None
      ),
      fullName = "",
      userAnswers
    )
  }

  "isAnyIFormInProgress" must {
    "return a time to process" when {

      val dataRequest = mockDataRequest(Map.empty)
      implicit val request: DataRequest[AnyContent] = dataRequest

      "the iForm status is not done" in {
        val notDoneStatus = Table("TrackedFormStatus", TrackedFormReceived, TrackedFormAcquired, TrackedFormInProgress)

        forAll(notDoneStatus) { (status: TrackedFormStatus) =>
          when(trackingConnector.getUserTracking(any())(any()))
            .thenReturn(Future.successful(Seq(TrackedForm("TES1", name, status))))

          val result = sut.isAnyIFormInProgress(nino.nino)
          Await.result(result, 5 seconds) mustBe ThreeWeeks
        }
      }

      Seq("TES1", "TES7") foreach { tes =>
        s"$tes should take three weeks to process" in {
          when(trackingConnector.getUserTracking(any())(any()))
            .thenReturn(Future.successful(Seq(TrackedForm(tes, name, TrackedFormReceived))))
          val result = sut.isAnyIFormInProgress(nino.nino)
          Await.result(result, 5 seconds) mustBe ThreeWeeks
        }
      }

      Seq("TES2", "TES3", "TES4", "TES5", "TES6") foreach { tes =>
        s"$tes should take seven days to process" in {
          when(trackingConnector.getUserTracking(any())(any()))
            .thenReturn(Future.successful(Seq(TrackedForm(tes, name, TrackedFormReceived))))
          val result = sut.isAnyIFormInProgress(nino.nino)
          Await.result(result, 5 seconds) mustBe FifteenDays
        }
      }

      "there is one iForm done and one IForm is in progress" in {
        val doneIForm = TrackedForm("TES4", name, TrackedFormDone)
        val notDoneIForm = TrackedForm("TES1", name, TrackedFormReceived)
        when(trackingConnector.getUserTracking(any())(any()))
          .thenReturn(Future.successful(Seq(doneIForm, notDoneIForm)))

        val result = sut.isAnyIFormInProgress(nino.nino)
        Await.result(result, 5 seconds) mustBe ThreeWeeks
      }

      Seq(
        Map(TrackSuccessfulJourneyConstants.AddEmploymentKey      -> "true"),
        Map(TrackSuccessfulJourneyConstants.UpdatePensionKey      -> "true"),
        Map(TrackSuccessfulJourneyConstants.AddPensionProviderKey -> "true")
      ) foreach { entry =>
        s"user has completed add employment iFormJourney but tracking service returns empty sequence, for $entry" in {
          val controller = sut
          when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq.empty[TrackedForm]))
          val dataRequest = mockDataRequest(entry)
          implicit val request: DataRequest[AnyContent] = dataRequest

          val result = controller.isAnyIFormInProgress(nino.nino)
          Await.result(result, 5 seconds) mustBe FifteenDays
        }
      }

      Seq(
        Map(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey -> "true")
      ) foreach { entry =>
        s"user has completed add employment iFormJourney but tracking service returns empty sequence, for $entry" in {
          val controller = sut
          val dataRequest = mockDataRequest(Map(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey -> "true"))
          implicit val request: DataRequest[AnyContent] = dataRequest
          when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq.empty[TrackedForm]))

          val result = controller.isAnyIFormInProgress(nino.nino)
          Await.result(result, 5 seconds) mustBe ThreeWeeks
        }
      }

      "tracking service returns an empty tracked form but user has completed a journey" in {
        val controller = sut
        when(trackingConnector.getUserTracking(any())(any()))
          .thenReturn(Future.successful(Seq.empty[TrackedForm]))

        val dataRequest = mockDataRequest(Map(TrackSuccessfulJourneyConstants.AddEmploymentKey -> "true"))
        implicit val request: DataRequest[AnyContent] = dataRequest

        val result = controller.isAnyIFormInProgress(nino.nino)
        Await.result(result, 5 seconds) mustBe FifteenDays
      }
    }

    "return no time to process" when {
      val dataRequest = mockDataRequest(Map.empty)
      implicit val request: DataRequest[AnyContent] = dataRequest

      "there is no iForm in progress" in {
        when(trackingConnector.getUserTracking(any())(any()))
          .thenReturn(Future.successful(Seq(TrackedForm("TES1", name, TrackedFormDone))))
        val result = sut.isAnyIFormInProgress(nino.nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }

      "the TES value is not valid" in {
        when(trackingConnector.getUserTracking(any())(any()))
          .thenReturn(Future.successful(Seq(TrackedForm("AAA", name, TrackedFormDone))))
        val result = sut.isAnyIFormInProgress(nino.nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }

      "tracking service return an empty form" in {
        when(trackingConnector.getUserTracking(any())(any()))
          .thenReturn(Future.successful(Seq.empty[TrackedForm]))
        val result = sut.isAnyIFormInProgress(nino.nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }

      "An Update Estimated key exists in the success journey cache" in {
        val controller = sut
        val incomeId = 1
        when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq.empty[TrackedForm]))

        val dataRequest =
          mockDataRequest(Map(s"${TrackSuccessfulJourneyConstants.EstimatedPayKey}-$incomeId" -> "true"))
        implicit val request: DataRequest[AnyContent] = dataRequest

        val result = controller.isAnyIFormInProgress(nino.nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }

      "An Update Estimated key for CY+1 exists in the success journey cache" in {
        val controller = sut
        when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq.empty[TrackedForm]))

        val dataRequest = mockDataRequest(Map(UpdateNextYearsIncomeConstants.Successful -> "true"))
        implicit val request: DataRequest[AnyContent] = dataRequest

        val result = controller.isAnyIFormInProgress(nino.nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }

      "An Update Estimated key for CY-1 exists in the success journey cache" in {
        val controller = sut
        when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.successful(Seq.empty[TrackedForm]))

        val dataRequest = mockDataRequest(Map(TrackSuccessfulJourneyConstants.UpdatePreviousYearsIncomeKey -> "true"))
        implicit val request: DataRequest[AnyContent] = dataRequest

        val result = controller.isAnyIFormInProgress(nino.nino)
        Await.result(result, 5 seconds) mustBe NoTimeToProcess
      }
    }
  }
}
