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
import controllers.actions.FakeValidatePerson
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.metrics.Metrics
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import uk.gov.hmrc.tai.service.JrsService
import utils.BaseSpec

import scala.concurrent.Future

class JrsClaimsControllerSpec extends BaseSpec {

  val jrsService = mock[JrsService]
  val metrics = mock[Metrics]
  val mockAppConfig = mock[ApplicationConfig]

  val jrsClaimsController = new JrsClaimsController(
    inject[AuditConnector],
    FakeAuthAction,
    FakeValidatePerson,
    jrsService,
    metrics,
    mcc,
    mockAppConfig,
    partialRetriever,
    templateRenderer)

  val jrsClaimsServiceResponse = JrsClaims(
    List(
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("January 2021"), YearAndMonth("February 2021"))),
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("December 2020")))
    ))

  implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

  "jrsClaimsController" should {

    "success view with JRS claim data" when {

      "some jrs data is received from service" in {

        when(jrsService.getJrsClaims(any())(any())).thenReturn(Future(Some(jrsClaimsServiceResponse)))
        when(mockAppConfig.jrsClaimsFromDate).thenReturn("2020-12")

        val result = jrsClaimsController.getJrsClaims()(request)

        status(result) mustBe (OK)
        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("check.jrs.claims.title"))
      }
    }

    "not found view" when {

      "no jrs data is received from service" in {

        when(jrsService.getJrsClaims(any())(any())).thenReturn(Future(None))

        val result = jrsClaimsController.getJrsClaims()(request)

        status(result) mustBe (NOT_FOUND)
        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("check.jrs.claims.no.claim.title"))
      }
    }
  }

}
