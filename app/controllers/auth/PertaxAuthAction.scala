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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Logging
import play.api.http.Status.UNAUTHORIZED
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.PertaxConnector
import uk.gov.hmrc.tai.model.PertaxResponse
import uk.gov.hmrc.tai.service.URLService
import views.html.{InternalServerErrorView, MainTemplate}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PertaxAuthAction extends ActionFilter[Request]

@Singleton
class PertaxAuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  pertaxConnector: PertaxConnector,
  featureFlagService: FeatureFlagService,
  internalServerErrorView: InternalServerErrorView,
  mainTemplate: MainTemplate,
  cc: ControllerComponents,
  appConfig: ApplicationConfig,
  urlService: URLService
) extends PertaxAuthAction with AuthorisedFunctions with Results with I18nSupport with Logging {

  override def messagesApi: MessagesApi = cc.messagesApi

  // scalastyle:off method.length
  override def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val implicitRequest: Request[A] = request
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def continueUrl: String = urlService.localFriendlyUrl(request.uri, request.host)

    pertaxConnector
      .pertaxPostAuthorise()
      .value
      .flatMap {
        case Left(UpstreamErrorResponse(_, status, _, _)) if status == UNAUTHORIZED =>
          Future.successful(Some(signInJourney))
        case Left(_) =>
          Future.successful(Some(InternalServerError(internalServerErrorView(appConfig))))
        case Right(PertaxResponse("ACCESS_GRANTED", _, _, _)) =>
          Future.successful(None)
        case Right(PertaxResponse("NO_HMRC_PT_ENROLMENT", _, _, Some(redirect))) =>
          Future.successful(Some(Redirect(s"$redirect/?redirectUrl=${SafeRedirectUrl(continueUrl).encodedUrl}")))
        case Right(PertaxResponse("CONFIDENCE_LEVEL_UPLIFT_REQUIRED", _, _, Some(redirect))) =>
          Future.successful(Some(upliftJourney(redirect)))
        case Right(PertaxResponse("CREDENTIAL_STRENGTH_UPLIFT_REQUIRED", _, _, Some(_))) =>
          val ex =
            new RuntimeException(
              s"Weak credentials should be dealt before the service"
            )
          logger.error(ex.getMessage, ex)
          Future.successful(Some(InternalServerError(internalServerErrorView(appConfig))))
        case Right(PertaxResponse(_, _, Some(errorView), _)) =>
          pertaxConnector.loadPartial(errorView.url).map {
            case partial: HtmlPartial.Success =>
              Some(
                Status(errorView.statusCode)(
                  mainTemplate(
                    title = partial.title.getOrElse(""),
                    pageTitle = partial.title,
                    backLinkContent = None,
                    showPtaAccountNav = false
                  )(partial.content)
                )
              )
            case _: HtmlPartial.Failure =>
              logger.error(s"The partial ${errorView.url} failed to be retrieved")
              Some(InternalServerError(internalServerErrorView(appConfig)))
          }
        case Right(response) =>
          val ex = new RuntimeException(
            s"Pertax response `${response.code}` with message ${response.message} is not handled"
          )
          logger.error(ex.getMessage, ex)
          Future.successful(
            Some(InternalServerError(internalServerErrorView(appConfig)))
          )
      }
  }

  private def signInJourney[A]: Result =
    Redirect(
      appConfig.basGatewayFrontendSignInUrl,
      Map(
        "continue_url" -> Seq(s"${appConfig.taiHomePageUrl}"),
        "origin"       -> Seq("tai-frontend"),
        "accountType"  -> Seq("individual")
      )
    )

  private def upliftJourney(redirect: String): Result =
    Redirect(
      redirect,
      Map(
        "origin"          -> Seq("TAI"),
        "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
        "completionURL"   -> Seq(s"${appConfig.taiHomePageUrl}"),
        "failureURL"      -> Seq(s"${appConfig.pertaxServiceUpliftFailedUrl}?continueUrl=${appConfig.taiHomePageUrl}")
      )
    )

  override protected implicit val executionContext: ExecutionContext = cc.executionContext

}
