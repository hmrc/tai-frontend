/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.FakeTaiPlayApplication
import org.joda.time.DateTime
import uk.gov.hmrc.domain.{Generator, Nino, TaxIds}
import uk.gov.hmrc.play.http._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.model.Activity

class ActivityLoggerConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with ServicesConfig {

  "ActivityLoggerConnectorSpec" should {

    implicit val hc = HeaderCarrier()

    "have activity-logger base url as activityLoggerBaseUrl" in {
      ActivityLoggerConnector.activityLoggerBaseUrl mustBe baseUrl("activity-logger")
    }

    "have WSHttp as http" in {
      ActivityLoggerConnector.http mustBe WSHttp
    }

    "return a 200 response" when {
      "logActivity is called" in {
        val sut = new SUT

        when(sut.http.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful[HttpResponse](HttpResponse(OK)))

        val result = Await.result(sut.logActivity(activity, nino.nino), 5 seconds)

        result.status mustBe OK
      }
    }
  }

  private class SUT extends ActivityLoggerConnector with ServicesConfig {
    override lazy val activityLoggerBaseUrl: String = baseUrl("activity-logger")
    override lazy val http: WSHttp = mock[WSHttp]
  }

  private val nino: Nino = new Generator().nextNino

  val activity = Activity(
    applicationName = "tai-frontend",
    eventTime = DateTime.now(),
    eventType = "tai.viewincome",
    eventDescriptionId = "tai.viewincome",
    principalTaxIds = TaxIds(nino)
  )

}
