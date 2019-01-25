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

package uk.gov.hmrc.tai.connectors

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.tai.model.domain.tracking.{TrackedForm, TrackedFormAcquired, TrackedFormReceived}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.HeaderCarrier

class TrackingConnectorSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(httpHandler)
  }

  "Tracking Url" should {
    "fetch the correct service url" when {
      "given an id and idType" in {
        val sut = createSUT()

        sut.trackingUrl(nino) mustBe s"mockUrl/tracking-data/user/nino/$nino"
      }
    }
  }

  "getUserTracking" should {
    "fetch the user tracking details" when {
      "provided with id and idType" in {
        val sut = createSUT()
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(trackedFormSeqJson)))

        val result = sut.getUserTracking(nino)
        Await.result(result, 50 seconds) mustBe trackedFormSeq
      }
    }

    "throw exception" when {
      "json is not valid" in {
        val sut = createSUT()
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse("""{}""")))

        val result = sut.getUserTracking(nino)
        the[JsResultException] thrownBy Await.result(result, 5 seconds)

      }
    }
  }

  val nino: String = new Generator().nextNino.nino
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val trackedFormSeqJson =  """{"submissions":[{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
                        "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
                        "milestones":[
                          {"milestone": "Received","status": "current"},
                          {"milestone": "Acquired","status": "incomplete"},
                          {"milestone": "InProgress","status": "incomplete"},
                          {"milestone": "Done","status": "incomplete"}
                        ]},
                        {"formId":"R38_EN","formName":"TES2","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
                         "receivedDate":"01 Apr 2016","completionDate":"06 May 2016",
                         "milestones":[
                           {"milestone": "Received","status": "complete"},
                           {"milestone": "Acquired","status": "current"},
                           {"milestone": "InProgress","status": "incomplete"},
                           {"milestone": "Done","status": "incomplete"}
                         ]}]}"""

  val trackedFormSeq = Seq(TrackedForm("R39_EN","TES1",TrackedFormReceived),
    TrackedForm("R38_EN", "TES2",TrackedFormAcquired))

  private def createSUT() = new SUT

  val httpHandler: HttpHandler = mock[HttpHandler]
  
  private class SUT extends TrackingConnector(httpHandler) {
    override lazy val serviceUrl: String = "mockUrl"
  }

}
