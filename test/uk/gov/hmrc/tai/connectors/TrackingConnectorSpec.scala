/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import uk.gov.hmrc.http.LockedException
import uk.gov.hmrc.tai.model.domain.tracking.{TrackedForm, TrackedFormAcquired, TrackedFormReceived}
import utils.BaseSpec

import scala.concurrent.Future

class TrackingConnectorSpec extends BaseSpec with BeforeAndAfterEach with ScalaFutures {

  override def beforeEach: Unit =
    Mockito.reset(httpHandler)

  "Tracking Url" should {
    "fetch the correct service url" when {
      "given an id and idType" in {
        sut.trackingUrl(nino.nino) mustBe s"mockUrl/tracking-data/user/nino/${nino.nino}"
      }
    }
  }

  "getUserTracking" should {
    "fetch the user tracking details" when {
      "provided with id and idType" in {
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(trackedFormSeqJson)))

        val result = sut.getUserTracking(nino.nino)
        result.futureValue mustBe trackedFormSeq
      }
    }

    "return an empty response" when {
      "json is not valid" in {
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse("""{}""")))

        val result = sut.getUserTracking(nino.nino)
        result.futureValue mustBe Seq.empty[TrackedForm]
      }

      "getFromApi throws" in {
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new LockedException("locked")))

        val result = sut.getUserTracking(nino.nino)
        result.futureValue mustBe Seq.empty[TrackedForm]
      }
    }

  }

  val trackedFormSeqJson =
    """{"submissions":[{"formId":"R39_EN","formName":"TES1","dfsSubmissionReference":"123-ABCD-456","businessArea":"PSA",
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

  val trackedFormSeq =
    Seq(TrackedForm("R39_EN", "TES1", TrackedFormReceived), TrackedForm("R38_EN", "TES2", TrackedFormAcquired))

  val httpHandler: HttpHandler = mock[HttpHandler]

  def sut: TrackingConnector = new TrackingConnector(httpHandler) {
    override lazy val serviceUrl: String = "mockUrl"
  }
}
