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

package utils

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.Injecting
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.tai.model.TaxYear

import scala.concurrent.ExecutionContext

trait IntegrationSpec
    extends PlaySpec with GuiceOneAppPerSuite with WireMockHelper with ScalaFutures with IntegrationPatience
    with Injecting {
  val generatedNino = new Generator().nextNino

  val generatedSaUtr = new Generator().nextAtedUtr

  lazy val messagesApi = inject[MessagesApi]

  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  val taxYear = TaxYear().year

  lazy val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  override def beforeEach() = {

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
  }
}
