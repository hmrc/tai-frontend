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

package controllers

import builders.RequestBuilder
import cats.data.OptionT
import cats.implicits.catsStdInstancesForFuture
import controllers.actions.{DataRequiredActionImpl, FakeDataRetrievalAction, FakeDataRetrievalActionProvider, FakeValidatePerson}
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.{DataCacheConnector, DataCacheConnectorImpl}
import uk.gov.hmrc.tai.identifiers.JrsClaimsId
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import uk.gov.hmrc.tai.service.JrsService
import uk.gov.hmrc.tai.util.CachedData
import utils.BaseSpec

import scala.concurrent.Future

class JrsClaimsControllerSpec extends BaseSpec with BeforeAndAfterEach {

  val jrsService = mock[JrsService]
  val mockAppConfig = mock[ApplicationConfig]
  val mockDataCacheConnector = mock[DataCacheConnectorImpl]

  def createController(cachedData: Option[CachedData]) = {
    when(mockDataCacheConnector.fetch(any())).thenReturn(Future.successful(cachedData))

    new JrsClaimsController(
      inject[AuditConnector],
      FakeAuthAction,
      FakeValidatePerson,
      new FakeDataRetrievalActionProvider(
        mockDataCacheConnector,
        new FakeDataRetrievalAction(cachedData.map(_.cacheMap))),
      new DataRequiredActionImpl(),
      mockDataCacheConnector,
      jrsService,
      mcc,
      mockAppConfig,
      partialRetriever,
      templateRenderer
    )
  }

  val jrsClaimsServiceResponse = JrsClaims(
    List(
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2021-02"))),
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12")))
    ))

  val cachedData = CachedData(CacheMap("id", Map(JrsClaimsId.toString -> Json.toJson(jrsClaimsServiceResponse))))

  implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

  "jrsClaimsController" should {

    "success view with JRS claim data" when {

      "some jrs data is received from the request" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(true)

        when(mockAppConfig.jrsClaimsFromDate).thenReturn("2020-12")

        val result = createController(Some(cachedData)).onPageLoad()(request)

        status(result) mustBe (OK)
        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("check.jrs.claims.title"))

        verify(jrsService, never()).getJrsClaims(any())(any())
      }

      "some jrs data is received from service" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(true)

        when(jrsService.getJrsClaims(any())(any())).thenReturn(OptionT.pure[Future](jrsClaimsServiceResponse))

        when(mockDataCacheConnector.save(any())) thenReturn Future.successful(cachedData)

        when(mockAppConfig.jrsClaimsFromDate).thenReturn("2020-12")

        val result = createController(Some(CachedData(new CacheMap("id", Map.empty)))).onPageLoad()(request)

        println(redirectLocation(result))

        status(result) mustBe (OK)
        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("check.jrs.claims.title"))

        verify(jrsService).getJrsClaims(any())(any())
        verify(mockDataCacheConnector).save(any())
      }
    }

    "not found view" when {

      "no jrs data is received from service" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(true)

        when(jrsService.getJrsClaims(any())(any())).thenReturn(OptionT.none[Future, JrsClaims])

        val result = createController(Some(CachedData(new CacheMap("id", Map.empty)))).onPageLoad()(request)

        status(result) mustBe (NOT_FOUND)
        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("check.jrs.claims.no.claim.title"))
      }
    }

    "internal server error page" when {

      "JrsClaimsEnabled is false" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(false)

        val result = createController(Some(CachedData(new CacheMap("id", Map.empty)))).onPageLoad()(request)

        status(result) mustBe (INTERNAL_SERVER_ERROR)

      }

    }

  }

  override protected def beforeEach(): Unit = reset(
    mockDataCacheConnector,
    jrsService
  )
}
