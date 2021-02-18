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

package uk.gov.hmrc.tai.service

import cats.data.OptionT
import cats.implicits.catsStdInstancesForFuture
import controllers.auth.{AuthenticatedRequest, OptionalDataRequest}
import org.mockito.Matchers.any
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.{DataCacheConnector, JrsConnector}
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.tai.identifiers.JrsClaimsId
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import uk.gov.hmrc.tai.util.CachedData
import utils.BaseSpec

import scala.concurrent.Future

class JrsServiceSpec extends BaseSpec with ScalaFutures with IntegrationPatience {

  val jrsConnector = mock[JrsConnector]
  val mockAppConfig = mock[ApplicationConfig]
  val dataCacheConnector = mock[DataCacheConnector]

  val jrsService = new JrsService(jrsConnector, mockAppConfig, dataCacheConnector)

  when(mockAppConfig.jrsClaimsEnabled).thenReturn(true)

  val jrsClaimsAPIResponse = JrsClaims(
    List(
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2020-11"))),
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-02"), YearAndMonth("2021-01")))
    ))

  val jrsClaimsServiceResponse = JrsClaims(
    List(
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2021-02"))),
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12")))
    ))

  val cachedDataObj = new CachedData(CacheMap("id", Map(JrsClaimsId.toString -> Json.toJson(jrsClaimsAPIResponse))))
  implicit val request = OptionalDataRequest(
    AuthenticatedRequest(FakeRequest(), "id", authedUser, "Some One"),
    "id",
    None
  )

  "getJrsClaims" should {

    "sort the employers alphabetically and yearAndMonth in ascending order" when {

      "connector returns some jrs data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(OptionT.pure[Future](jrsClaimsAPIResponse))

        when(mockAppConfig.jrsClaimsFromDate).thenReturn("2020-12")

        when(dataCacheConnector.save(any())).thenReturn(Future.successful(cachedDataObj))

        val result = jrsService.getJrsClaims(nino)

        result.value.futureValue mustBe Some(jrsClaimsServiceResponse)

        jrsClaimsServiceResponse.toString mustNot contain("November 2020")

      }

      "cache returns some jrs data" in {

        implicit val requestWithCache: OptionalDataRequest[_] =
          OptionalDataRequest(
            AuthenticatedRequest(FakeRequest(), "id", authedUser, "Some One"),
            "id",
            Some(cachedDataObj)
          )

        when(mockAppConfig.jrsClaimsFromDate).thenReturn("2020-12")

        val result = jrsService.getJrsClaims(nino)

        result.value.futureValue mustBe Some(jrsClaimsServiceResponse)

      }
    }

    "return none" when {

      "connector returns empty jrs data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(OptionT.pure[Future](JrsClaims(List.empty)))

        val result = jrsService.getJrsClaims(nino).value.futureValue

        result mustBe None

      }

      "connector returns None" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(OptionT.none[Future, JrsClaims])

        val result = jrsService.getJrsClaims(nino).value.futureValue

        result mustBe None

      }
    }
  }

  "checkIfJrsClaimsDataExist" should {

    "return true" when {

      "connector returns jrs claim data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(OptionT.pure[Future](jrsClaimsAPIResponse))

        when(dataCacheConnector.save(any())).thenReturn(Future.successful(cachedDataObj))

        val result = jrsService.checkIfJrsClaimsDataExist(nino).futureValue

        result mustBe (true)
      }

    }

    "return false" when {

      "connector returns empty jrs claim data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(OptionT.pure[Future](JrsClaims(List.empty)))

        val result = jrsService.checkIfJrsClaimsDataExist(nino).futureValue

        result mustBe (false)
      }

      "connector returns no jrs claim data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(OptionT.none[Future, JrsClaims])

        val result = jrsService.checkIfJrsClaimsDataExist(nino).futureValue

        result mustBe (false)
      }

      "jrs claim feature toggle is disabled" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(false)

        val result = jrsService.checkIfJrsClaimsDataExist(nino).futureValue

        result mustBe (false)
      }
    }

  }

}
