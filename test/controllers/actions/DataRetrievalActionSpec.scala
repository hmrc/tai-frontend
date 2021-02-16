/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.actions

import controllers.auth.{AuthenticatedRequest, OptionalDataRequest}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.tai.connectors.DataCacheConnector
import uk.gov.hmrc.tai.util.CachedData
import utils.BaseSpec

import scala.concurrent.Future

class DataRetrievalActionSpec extends BaseSpec with ScalaFutures {

  class Harness(dataCacheConnector: DataCacheConnector) extends DataRetrievalActionImpl(dataCacheConnector) {

    def callTransform[A](request: AuthenticatedRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  val cachedData = CachedData(new CacheMap("id", Map()))

  val request = AuthenticatedRequest(fakeRequest, "id", authedUser, "Some one")

  "Data Retrieval Action" when {
    "there is no data in the cache" must {
      "set cachedData to 'None' in the request" in {
        val dataCacheConnector = mock[DataCacheConnector]

        when(dataCacheConnector.fetch(any())) thenReturn Future.successful(None)
        val action = new Harness(dataCacheConnector)
        val futureResult =
          action.callTransform(request)
        whenReady(futureResult) { result =>
          result.cachedData.isEmpty mustBe true
        }
      }
    }

    "there is data in the cache" must {
      "build a userAnswers object and add it to the request" in {
        val dataCacheConnector = mock[DataCacheConnector]

        when(dataCacheConnector.fetch(any())) thenReturn Future(Some(cachedData))
        val action = new Harness(dataCacheConnector)

        val futureResult =
          action.callTransform(request)
        whenReady(futureResult) { result =>
          result.cachedData.isDefined mustBe true
        }
      }
    }
  }

  "DataRetrievalActionImpl" should {
    "set userAnswers in the request" in {

      val dataCacheConnector = mock[DataCacheConnector]

      when(dataCacheConnector.fetch(any())) thenReturn Future.successful(Some(cachedData))

      val action = new Harness(dataCacheConnector)
      val result = action.callTransform(request)

      val expectedResult = OptionalDataRequest(request, "id", Some(cachedData))

      whenReady(result) { x =>
        x.request mustEqual expectedResult.request
        x.cacheId mustEqual s"${request.taiUser.validNino}-id"
        x.cachedData.map(_.cacheMap) mustEqual expectedResult.cachedData.map(_.cacheMap)
      }
    }
  }
}
