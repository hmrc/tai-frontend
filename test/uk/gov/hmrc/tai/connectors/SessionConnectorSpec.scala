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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
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
      verify(httpHandler, times(1)).deleteFromApi(meq("localhost/tai/session-cache"))(any())
    }
  }

  val httpHandler: HttpClientResponse = mock[HttpClientResponse]

  def sut: SessionConnector = new SessionConnector(httpHandler, servicesConfig) {
    override val serviceUrl: String = "localhost"

    when(httpHandler.deleteFromApi(any())(any()))
      .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, "")))))
  }

}
