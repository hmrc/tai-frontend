/*
 * Copyright 2025 HM Revenue & Customs
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

package utils

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, ok, post, urlEqualTo, urlMatching}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.*
import play.api.test.Injecting
import uk.gov.hmrc.domain.{AtedUtr, Generator, Nino}
import uk.gov.hmrc.tai.model.TaxYear

import scala.concurrent.ExecutionContext

trait IntegrationSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with Injecting {
  val generatedNino: Nino = new Generator().nextNino

  val generatedSaUtr: AtedUtr = new Generator().nextAtedUtr

  lazy val messagesApi: MessagesApi = inject[MessagesApi]

  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  val taxYear: Int = TaxYear().year

  lazy val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  override def beforeEach(): Unit = {

    super.beforeEach()

    val authResponse =
      s"""
         |{
         |    "confidenceLevel": 200,
         |    "nino": "$generatedNino",
         |    "saUtr": "$generatedSaUtr",
         |    "name": {
         |        "name": "John",
         |        "lastName": "Smith"
         |    },
         |    "loginTimes": {
         |        "currentLogin": "2021-06-07T10:52:02.594Z",
         |        "previousLogin": null
         |    },
         |    "optionalCredentials": {
         |        "providerId": "4911434741952698",
         |        "providerType": "GovernmentGateway"
         |    },
         |    "authProviderId": {
         |        "ggCredId": "xyz"
         |    },
         |    "externalId": "testExternalId"
         |}
         |""".stripMargin

    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(aResponse().withBody(authResponse))
    )

    server.stubFor(
      post(urlEqualTo("/pertax/authorise"))
        .willReturn(aResponse().withBody("""{"code":"ACCESS_GRANTED", "message":"test"}"""))
    )

    val wrapperDataResponse: String =
      """
        |{
        |    "menuItemConfig": [
        |        {
        |            "id": "home",
        |            "text": "Check your Income Tax",
        |            "href": "http://localhost:9230/check-income-tax/what-do-you-want-to-do",
        |            "leftAligned": true,
        |            "position": 0,
        |            "icon": "hmrc-account-icon hmrc-account-icon--home"
        |        }
        |    ],
        |    "ptaMinMenuConfig": {
        |        "menuName": "Account menu",
        |        "backName": "Back"
        |    },
        |    "urBanners": [
        |        {
        |           "page": "test-page",
        |           "link": "test-link",
        |           "isEnabled": true
        |        }
        |    ],
        |    "webchatPages": [
        |        {
        |            "pattern": "^/check-income-tax/.*",
        |            "skinElement": "skinElement",
        |            "isEnabled": true,
        |            "chatType": "loadWebChatContainer"
        |        }
        |    ]
        |}
        |""".stripMargin

    server.stubFor(
      WireMock
        .get(urlMatching("/single-customer-account-wrapper-data/wrapper-data.*"))
        .willReturn(ok(wrapperDataResponse))
    )
  }
}
