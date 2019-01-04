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

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.connectors.ActivityLoggerConnector

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ActivityLoggerServiceSpec extends PlaySpec with MockitoSugar {

  private implicit val hc = HeaderCarrier()

  "View Income " must {
    "be logged to the activity logger" in {
      val sut = createSut
      when(sut.activityLoggerConnector.logActivity(any(), any())(any())).thenReturn(Future.successful(HttpResponse(OK)))

      val activityResponse = Await.result(sut.viewIncome(nino), 5.seconds)

      activityResponse.status mustBe 200
      verify(sut.activityLoggerConnector, times(1)).logActivity(any(), Matchers.eq(nino.nino))(any())
    }
  }

  "Update Income " must {
    "be logged to the activity logger" in {
      val sut = createSut
      when(sut.activityLoggerConnector.logActivity(any(), any())(any())).thenReturn(Future.successful(HttpResponse(OK)))

      val activityResponse = Await.result(sut.updateIncome(nino), 5.seconds)

      activityResponse.status mustBe 200
      verify(sut.activityLoggerConnector, times(1)).logActivity(any(), Matchers.eq(nino.nino))(any())
    }
  }

  private val nino = new Generator().nextNino

  def createSut = new SUT

  class SUT extends ActivityLoggerService {
    override val activityLoggerConnector: ActivityLoggerConnector = mock[ActivityLoggerConnector]
  }

}
