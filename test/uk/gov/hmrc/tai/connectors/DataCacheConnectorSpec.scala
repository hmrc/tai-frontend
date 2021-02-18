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

package uk.gov.hmrc.tai.connectors

import controllers.auth.{AuthenticatedRequest, OptionalDataRequest}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.test.FakeRequest
import repositories.SessionRepository
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.tai.identifiers.JrsClaimsId
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import uk.gov.hmrc.tai.util.CachedData
import utils.BaseSpec

import scala.concurrent.Future

class DataCacheConnectorSpec extends BaseSpec with ScalaFutures {

  val mockSessionRepository = mock[SessionRepository]

  val dataCacheConnectorImpl = new DataCacheConnectorImpl(
    mockSessionRepository
  )

  val jrsClaimsAPIResponse = JrsClaims(
    List(
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2020-11"))),
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-02"), YearAndMonth("2021-01")))
    ))

  val cachedDataObj = new CachedData(CacheMap("id", Map(JrsClaimsId.toString -> Json.toJson(jrsClaimsAPIResponse))))
  implicit val request = OptionalDataRequest(
    AuthenticatedRequest(FakeRequest(), "id", authedUser, "Some One"),
    "id",
    None
  )

  "DataCacheConnector" when {
    "save is called" must {
      "return cachedData" in {

        when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(true))

        dataCacheConnectorImpl.save(cachedDataObj).futureValue shouldBe cachedDataObj

      }
    }

    "fetch is called" must {
      "return cachedDate when a record is found" in {

        when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(cachedDataObj.cacheMap)))

        dataCacheConnectorImpl.fetch("id").futureValue shouldBe Some(cachedDataObj)

      }
      "return None when no record is found" in {

        when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))

        dataCacheConnectorImpl.fetch("id").futureValue shouldBe None

      }
    }
  }
}
