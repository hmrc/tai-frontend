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

package controllers

import cats.data.EitherT
import controllers.{IvUpliftController, routes}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.model.{IdentityVerificationResponse, Incomplete, InsufficientEvidence, InvalidResponse, PrecondFailed, TechnicalIssue}
import uk.gov.hmrc.tai.service.IvUpliftFrontendService
import utils.BaseSpec
import views.html.InternalServerErrorView
import views.html.ivFailureJourneyOutcome.{IvIncompleteView, IvInsufficientEvidenceView, IvPreconditionFailedView}

import scala.concurrent.Future

class IvUpliftControllerSpec extends BaseSpec {

  lazy val mockIdentityVerificationFrontendService: IvUpliftFrontendService = mock[IvUpliftFrontendService]


  private val ivInsufficientEvidenceView = inject[IvInsufficientEvidenceView]
  private val ivIncompleteView = inject[IvIncompleteView]
  private val ivPreconditionFailedView = inject[IvPreconditionFailedView]
  private val internalServerErrorView = inject[InternalServerErrorView]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(
      bind[IvUpliftFrontendService].toInstance(mockIdentityVerificationFrontendService),
      bind[MessagesControllerComponents].toInstance(stubMessagesControllerComponents())
    )
    .build()

  private val controller = new IvUpliftController(
    messagesApi,
    mcc,
    mockIdentityVerificationFrontendService,
    ivInsufficientEvidenceView,
    ivIncompleteView,
    ivPreconditionFailedView,
    internalServerErrorView
  )(appConfig, implicitly)

  override def beforeEach(): Unit = {
    reset(mockIdentityVerificationFrontendService)
    super.beforeEach()
  }

  def getIVJourneyStatusResponse(
                                  expectedResponse: IdentityVerificationResponse
                                ): EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse] =
    EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(expectedResponse)))

  "Calling IvUpliftController showUpliftJourneyOutcome" must {
    "return Internal Server Error with IvTechnicalIssuesView when the result was missing journeyId" in {
      when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
        .thenReturn(
          getIVJourneyStatusResponse(TechnicalIssue)
        )

      val result: Future[Result] = controller.showUpliftFailedJourneyOutcome()(
        FakeRequest(GET, routes.IvUpliftController.showUpliftFailedJourneyOutcome().url)
      )
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return Internal Server Error with IvTechnicalIssuesView when the result was incorrect journeyId" in {
      when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
        .thenReturn(
          getIVJourneyStatusResponse(InsufficientEvidence)
        )

      val result: Future[Result] = controller.showUpliftFailedJourneyOutcome()(
        FakeRequest(
          GET,
          routes.IvUpliftController.showUpliftFailedJourneyOutcome().url + "/?journeyXX=XXXX"
        )
      )
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return Internal Server Error with IvTechnicalIssuesView when journey status is TechnicalIssue" in {
      when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
        .thenReturn(
          getIVJourneyStatusResponse(TechnicalIssue)
        )

      val result: Future[Result] = controller.showUpliftFailedJourneyOutcome()(
        FakeRequest(GET, routes.IvUpliftController.showUpliftFailedJourneyOutcome().url + "/?journeyId=TechnicalIssue")
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return Unauthorized with IvFailedIvIncompleteView when journey status is Incomplete" in {
      when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
        .thenReturn(
          getIVJourneyStatusResponse(Incomplete)
        )

      val result: Future[Result] = controller.showUpliftFailedJourneyOutcome()(
        FakeRequest(GET, routes.IvUpliftController.showUpliftFailedJourneyOutcome().url + "/?journeyId=Incomplete")
      )

      status(result) mustBe UNAUTHORIZED
    }

    "return Unauthorized with IvPreconditionFailedView when journey status is PrecondFailed" in {
      when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
        .thenReturn(
          getIVJourneyStatusResponse(PrecondFailed)
        )

      val result: Future[Result] = controller.showUpliftFailedJourneyOutcome()(
        FakeRequest(
          GET,
          routes.IvUpliftController.showUpliftFailedJourneyOutcome().url + "/?journeyId=PreconditionFailed"
        )
      )

      status(result) mustBe UNAUTHORIZED
    }

    "return Unauthorized with IvInsufficientEvidenceView when journey status is InsufficientEvidence" in {
      when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
        .thenReturn(
          getIVJourneyStatusResponse(InsufficientEvidence)
        )

      val result: Future[Result] = controller.showUpliftFailedJourneyOutcome()(
        FakeRequest(
          GET,
          routes.IvUpliftController.showUpliftFailedJourneyOutcome().url + "/?journeyId=InsufficientEvidence"
        )
      )

      status(result) mustBe UNAUTHORIZED
    }

    "return Internal Server Error when TechnicalIssue response from identityVerificationFrontendService" in {
      when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
        .thenReturn(getIVJourneyStatusResponse(InvalidResponse))

      val result: Future[Result] = controller.showUpliftFailedJourneyOutcome()(
        FakeRequest(GET, routes.IvUpliftController.showUpliftFailedJourneyOutcome().url + "/?journeyId=XXXX")
      )

      status(result) mustBe UNAUTHORIZED
    }

    "return InternalServerError to the IvTechnicalIssuesView if unable to get IV journey status" in {
      when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
        .thenReturn(EitherT.fromEither[Future](Left(UpstreamErrorResponse.apply("Internal Server Error", 500))))

      val result: Future[Result] = controller.showUpliftFailedJourneyOutcome()(
        FakeRequest(
          GET,
          routes.IvUpliftController.showUpliftFailedJourneyOutcome().url + "/?journeyId=XXXX"
        )
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

  }
}
