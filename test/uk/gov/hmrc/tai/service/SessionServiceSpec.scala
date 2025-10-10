/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.connectors.SessionConnector
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SessionServiceSpec extends BaseSpec {

  "Session Service" must {
    "invalidate the cache" in {
      val sut = new SUT
      when(sessionConnector.invalidateCache(any())(any())).thenReturn(Future.successful(HttpResponse(200, "")))

      val result = Await.result(sut.invalidateCache(nino)(HeaderCarrier()), 5.seconds)

      result.status mustBe 200
    }
  }

  val sessionConnector: SessionConnector = mock[SessionConnector]

  class SUT
      extends SessionService(
        sessionConnector
      )

}
