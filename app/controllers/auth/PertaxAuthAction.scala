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
import uk.gov.hmrc.tai.model.admin.PertaxBackendToggle
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
  appConfig: ApplicationConfig
) extends PertaxAuthAction with AuthorisedFunctions with Results with I18nSupport with Logging {

  private def failureUrl: String = appConfig.pertaxServiceUpliftFailedUrl
  private def completionUrl: String = appConfig.taiHomePageUrl
  private def confidenceLevel: Int = ConfidenceLevel.L250.level

  override def messagesApi: MessagesApi = cc.messagesApi

  // scalastyle:off method.length
  override def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val implicitRequest: Request[A] = request
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    featureFlagService.get(PertaxBackendToggle).flatMap { toggle =>
      if (toggle.isEnabled) {
        pertaxConnector
          .pertaxPostAuthorise()
          .fold(
            { _: UpstreamErrorResponse =>
              Future.successful(
                Some(InternalServerError(internalServerErrorView(appConfig)))
              )
            },
            {
              case PertaxResponse("ACCESS_GRANTED", _, _, _) =>
                Future.successful(None)
              case PertaxResponse("NO_HMRC_PT_ENROLMENT", _, _, Some(redirect)) =>
                Future.successful(Some(Redirect(s"$redirect/?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}")))
              case PertaxResponse("CREDENTIAL_STRENGTH_UPLIFT_REQUIRED", _, _, Some(redirect)) =>
                Future.successful(
                  Some(
                    Redirect(
                      s"$redirect?origin=tai-frontend&continueUrl=${SafeRedirectUrl(request.uri).encodedUrl}"
                    )
                  )
                )
              case PertaxResponse("CONFIDENCE_LEVEL_UPLIFT_REQUIRED", _, _, Some(redirect)) =>
                Future.successful(
                  Some(
                    Redirect(
                      s"$redirect?origin=tai-frontend&confidenceLevel=$confidenceLevel&completionURL=$completionUrl&failureURL=$failureUrl=${SafeRedirectUrl(request.uri).encodedUrl}"
                    )
                  )
                )
              case PertaxResponse(_, _, Some(errorView), _) =>
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
              case response =>
                val ex = new RuntimeException(
                  s"Pertax response `${response.code}` with message ${response.message} is not handled"
                )
                logger.error(ex.getMessage, ex)
                Future.successful(
                  Some(InternalServerError(internalServerErrorView(appConfig)))
                )
            }
          )
          .flatten
      } else {
        Future.successful(None)
      }
    }
  }

  override protected implicit val executionContext: ExecutionContext = cc.executionContext

}
