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

package controllers.auth

import cats.data.EitherT
import com.google.inject.Inject
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.PertaxConnector
import uk.gov.hmrc.tai.model.{ErrorView, PertaxResponse}
import uk.gov.hmrc.tai.service.URLService
import utils.BaseSpec
import views.html.{InternalServerErrorView, MainTemplate}

import scala.concurrent.Future

class PertaxAuthActionSpec extends BaseSpec {

  lazy val mockPertaxConnector: PertaxConnector = mock[PertaxConnector]
  lazy val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private lazy val mockURLService = mock[URLService]
  override lazy val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  private val internalServerErrorView: InternalServerErrorView = inject[InternalServerErrorView]
  private val mainTemplateView: MainTemplate = inject[MainTemplate]
  private val testAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val testAction: PertaxAuthActionImpl = new PertaxAuthActionImpl(
    mockAuthConnector,
    mockPertaxConnector,
    mockFeatureFlagService,
    internalServerErrorView,
    mainTemplateView,
    mcc,
    testAppConfig,
    mockURLService
  )

  class FakeController @Inject() (defaultActionBuilder: DefaultActionBuilder) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = (defaultActionBuilder andThen testAction).async {
      Future.successful(Ok("ok"))
    }
  }

  val fakeController = new FakeController(inject[DefaultActionBuilder])

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(
      bind[PertaxConnector].toInstance(mockPertaxConnector),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[FeatureFlagService].toInstance(mockFeatureFlagService)
    )
    .build()

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService)
    reset(mockAuthConnector)
    reset(mockPertaxConnector)
    reset(testAppConfig)
    when(testAppConfig.pertaxUrl).thenReturn("PERTAX_URL")
    when(testAppConfig.pertaxServiceUpliftFailedUrl).thenReturn("/failed")
    when(testAppConfig.taiHomePageUrl).thenReturn("/home")
    when(testAppConfig.taiRootUri).thenReturn("/taiRoot")
    when(mockURLService.localFriendlyUrl(any(), any())).thenReturn("/localfriendlyurl")
    super.beforeEach()
  }

  private val testRequest = FakeRequest("GET", "/paye/benefits/medical-benefit")

  val expectedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(testRequest, authedUser, fakePerson(nino))

  "Pertax auth action" when {
    "the pertax API returns an ACCESS_GRANTED response" must {
      "load the request" in {

        when(mockPertaxConnector.pertaxPostAuthorise()(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, PertaxResponse](
              Future.successful(Right(PertaxResponse("ACCESS_GRANTED", "", None, None)))
            )
          )

        val result = fakeController.onPageLoad()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "ok"
      }
    }

    "the pertax API response returns a NO_HMRC_PT_ENROLMENT response" must {
      "redirect to the returned location" in {

        when(mockPertaxConnector.pertaxPostAuthorise()(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, PertaxResponse](
              Future.successful(Right(PertaxResponse("NO_HMRC_PT_ENROLMENT", "", None, Some("redirectLocation"))))
            )
          )

        val result = fakeController.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("redirectLocation/?redirectUrl=%2Flocalfriendlyurl")
      }
    }

    "the pertax API response returns a CREDENTIAL_STRENGTH_UPLIFT_REQUIRED response" must {
      "return an error page" in {

        when(mockPertaxConnector.pertaxPostAuthorise()(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, PertaxResponse](
              Future.successful(
                Right(
                  PertaxResponse("CREDENTIAL_STRENGTH_UPLIFT_REQUIRED", "", None, Some("redirectLocation"))
                )
              )
            )
          )

        val result = fakeController.onPageLoad()(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(
          result
        ) must include("Sorry, the service is unavailable")
      }
    }

    "the pertax API response returns a CONFIDENCE_LEVEL_UPLIFT_REQUIRED response" must {
      "redirect to the returned location" in {

        when(mockPertaxConnector.pertaxPostAuthorise()(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, PertaxResponse](
              Future.successful(
                Right(PertaxResponse("CONFIDENCE_LEVEL_UPLIFT_REQUIRED", "", None, Some("redirectLocation")))
              )
            )
          )

        val result = fakeController.onPageLoad()(FakeRequest(method = "GET", path = "/redirectUri"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          "redirectLocation?origin=TAI&confidenceLevel=200&completionURL=%2FtaiRoot%2FredirectUri&failureURL=%2Ffailed%3FcontinueUrl%3D%2Fhome"
        )
      }
    }

    "the pertax API response returns an error view" must {
      "show the corresponding view" in {

        when(mockPertaxConnector.pertaxPostAuthorise()(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, PertaxResponse](
              Future.successful(
                Right(
                  PertaxResponse(
                    "INVALID_AFFINITY",
                    "The user is neither an individual or an organisation",
                    Some(ErrorView("/path/for/partial", UNAUTHORIZED)),
                    None
                  )
                )
              )
            )
          )

        when(mockPertaxConnector.loadPartial(any())(any(), any()))
          .thenReturn(Future.successful(HtmlPartial.Success(None, Html("Should be in the resulting view"))))

        val result = fakeController.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) must include(messages("Should be in the resulting view"))
      }

      "failed to show the corresponding view" in {

        when(mockPertaxConnector.pertaxPostAuthorise()(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, PertaxResponse](
              Future.successful(
                Right(
                  PertaxResponse(
                    "INVALID_AFFINITY",
                    "The user is neither an individual or an organisation",
                    Some(ErrorView("", UNAUTHORIZED)),
                    None
                  )
                )
              )
            )
          )

        when(mockPertaxConnector.loadPartial(any())(any(), any()))
          .thenReturn(Future.successful(HtmlPartial.Failure(Some(500), "Should be in the resulting view")))

        val result = fakeController.onPageLoad()(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "the pertax API returns an Unauthorised error response" must {
      "redirect to gg login" in {
        when(mockPertaxConnector.pertaxPostAuthorise()(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, PertaxResponse](
              Future.successful(
                Left(
                  UpstreamErrorResponse(
                    "Unauthorised response",
                    UNAUTHORIZED
                  )
                )
              )
            )
          )

        val result = fakeController.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("?continue_url=%2Fhome&origin=tai-frontend&accountType=individual")
      }
    }

    "the pertax API response returns an unexpected response" must {
      "throw an internal server error" in {

        when(mockPertaxConnector.pertaxPostAuthorise()(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, PertaxResponse](
              Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
            )
          )

        val result = fakeController.onPageLoad()(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR

        contentAsString(result) must include("Sorry, the service is unavailable")
      }
    }

  }
}
