/*
 * Copyright 2022 HM Revenue & Customs
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

import builders.{RequestBuilder, UserBuilder}
import cats.data.{EitherT, OptionT}
import cats.implicits.catsStdInstancesForFuture
import controllers.actions.FakeValidatePerson
import controllers.auth.{AuthedUser, AuthenticatedRequest}
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.http.{HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import uk.gov.hmrc.tai.service.JrsService
import uk.gov.hmrc.tai.util.constants.TaiConstants
import utils.BaseSpec
import views.html.{InternalServerErrorView, JrsClaimSummaryView, NoJrsClaimView}

import scala.concurrent.Future

class JrsClaimsControllerSpec extends BaseSpec {

  private val jrsService = mock[JrsService]
  private val mockAppConfig = mock[ApplicationConfig]

  val jrsClaimsController = new JrsClaimsController(
    inject[AuditConnector],
    FakeAuthAction,
    FakeValidatePerson,
    jrsService,
    mcc,
    mockAppConfig,
    inject[JrsClaimSummaryView],
    inject[InternalServerErrorView],
    inject[NoJrsClaimView],
    templateRenderer
  )

  val jrsClaimsServiceResponse = JrsClaims(
    List(
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2021-02"))),
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12")))
    ))

  implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")

  "jrsClaimsController" should {

    "success view with JRS claim data" when {

      "some jrs data is received from service" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(true)

        when(jrsService.getJrsClaims(any())(any())).thenReturn(OptionT.pure[Future](jrsClaimsServiceResponse))
        when(mockAppConfig.jrsClaimsFromDate).thenReturn("2020-12")

        val result = jrsClaimsController.onPageLoad()(request)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("check.jrs.claims.title"))
      }
    }

    "RuntimeException" when {

      "Failed response is received from service" in {

        implicit val authedTrustedUser: AuthedUser =
          UserBuilder("utr", TaiConstants.AuthProviderVerify, "principalName")

        implicit val request = AuthenticatedRequest[AnyContent](fakeRequest, authedTrustedUser, "name")

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(true)

        when(jrsService.getJrsClaims(any())(any()))
          .thenReturn(OptionT[Future, JrsClaims](Future.failed(new RuntimeException("Error"))))
        when(mockAppConfig.jrsClaimsFromDate).thenReturn("2020-12")

        val result = jrsClaimsController.onPageLoad()(request)

        whenReady(result.failed) { e =>
          e mustBe a[RuntimeException]
        }
      }
    }

    "not found view" when {

      "no jrs data is received from service" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(true)

        when(jrsService.getJrsClaims(any())(any())).thenReturn(OptionT.none[Future, JrsClaims])

        val result = jrsClaimsController.onPageLoad()(request)

        status(result) mustBe NOT_FOUND
        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("check.jrs.claims.no.claim.title"))
      }
    }

    "internal server error page" when {

      "JrsClaimsEnabled is false" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(false)

        val result = jrsClaimsController.onPageLoad()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

      }

    }

  }
//  implicit val request = AuthenticatedRequest[AnyContent](fakeRequest, authedUser, "name")
}
