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
import controllers.auth.{AuthJourney, AuthedUser, AuthenticatedRequest, InternalAuthenticatedRequest}
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.Injecting
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.constants.TaiConstants

import scala.concurrent.{ExecutionContext, Future}

class IntegrationSpec
    extends PlaySpec with GuiceOneAppPerSuite with WireMockHelper with ScalaFutures with IntegrationPatience
    with Injecting {

  protected lazy val mockAuthJourney: AuthJourney = mock[AuthJourney]
  val generatedNino = new Generator().nextNino

  val generatedSaUtr = new Generator().nextAtedUtr

  lazy val messagesApi = inject[MessagesApi]

  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  val taxYear = TaxYear().year

  lazy val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  when(mockAuthJourney.authWithValidatePerson).thenReturn(new ActionBuilder[AuthenticatedRequest, AnyContent] {
    private val user =
      AuthedUser(
        Nino(generatedNino.toString()),
        Some("saUtr"),
        Some(TaiConstants.AuthProviderGG),
        ConfidenceLevel.L200,
        None,
        None
      )

    override def invokeBlock[A](
      request: Request[A],
      block: AuthenticatedRequest[A] => Future[Result]
    ): Future[Result] =
      block(AuthenticatedRequest(request, user, "testUser"))

    override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = ec
  })

  when(mockAuthJourney.authWithoutValidatePerson).thenReturn(
    new ActionBuilder[InternalAuthenticatedRequest, AnyContent] {
      private val user =
        AuthedUser(
          Nino(generatedNino.toString()),
          Some("saUtr"),
          Some(TaiConstants.AuthProviderGG),
          ConfidenceLevel.L200,
          None,
          None
        )

      override def invokeBlock[A](
        request: Request[A],
        block: InternalAuthenticatedRequest[A] => Future[Result]
      ): Future[Result] =
        block(InternalAuthenticatedRequest(request, user))

      override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = ec
    }
  )

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
