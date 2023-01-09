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

package uk.gov.hmrc.tai.connectors

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HttpResponse
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SessionConnectorSpec extends BaseSpec with BeforeAndAfterEach {

  override def beforeEach: Unit =
    Mockito.reset(httpHandler)

  "Session Connector" must {
    "return Http response" when {
      "cache is invalidated" in {
        val result = Await.result(sut.invalidateCache(), 5.seconds)

        result.status mustBe 200
      }
    }

    "call the proper url to invalidate the cache" in {
      Await.result(sut.invalidateCache(), 5.seconds)
      verify(httpHandler, times(1)).deleteFromApi(Matchers.eq("localhost/tai/session-cache"))(any(), any())
    }
  }

  val httpHandler: HttpHandler = mock[HttpHandler]

  def sut: SessionConnector = new SessionConnector(httpHandler, servicesConfig) {
    override val serviceUrl: String = "localhost"

    when(httpHandler.deleteFromApi(any())(any(), any()))
      .thenReturn(Future.successful(HttpResponse(200)))
  }

}
