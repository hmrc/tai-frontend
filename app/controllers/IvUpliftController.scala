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

import com.google.inject.{Inject, Singleton}
import controllers.auth.AuthedUser
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.{Incomplete, InsufficientEvidence, PrecondFailed, TechnicalIssue}
import uk.gov.hmrc.tai.service.IvUpliftFrontendService
import views.html.InternalServerErrorView
import views.html.ivFailureJourneyOutcome.{IvIncompleteView, IvInsufficientEvidenceView, IvPreconditionFailedView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IvUpliftController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  ivUpliftFrontendService: IvUpliftFrontendService,
  ivInsufficientEvidenceView: IvInsufficientEvidenceView,
  ivFailedIvIncompleteView: IvIncompleteView,
  ivPreconditionFailedView: IvPreconditionFailedView,
  internalServerErrorView: InternalServerErrorView
)(implicit appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private lazy val logger = Logger(this.getClass)

  def showUpliftFailedJourneyOutcome: Action[AnyContent] =
    Action.async { implicit request =>
//      implicit val user: AuthedUser = request
      val journeyId = request.getQueryString("journeyId")

      journeyId match {
        case Some(jid) =>
          ivUpliftFrontendService
            .getIVJourneyStatus(jid)
            .map {
              case InsufficientEvidence =>
                logErrorMessage(InsufficientEvidence.toString)
                Unauthorized(ivInsufficientEvidenceView())

              case Incomplete =>
                logErrorMessage(Incomplete.toString)
                Unauthorized(ivFailedIvIncompleteView())

              case PrecondFailed =>
                logErrorMessage(PrecondFailed.toString)
                Unauthorized(ivPreconditionFailedView())

              case TechnicalIssue =>
                logErrorMessage(TechnicalIssue.toString)
                InternalServerError(internalServerErrorView(appConfig))

              case _ =>
                logErrorMessage("TechnicalIssue response from identityVerificationFrontendService")
                Unauthorized(internalServerErrorView(appConfig))
            }
            .getOrElse {
              logErrorMessage("Unable to get IV journey status")
              InternalServerError(internalServerErrorView(appConfig))
            }
        case None =>
          logger.error("journeyId missing or incorrect")
          Future.successful(InternalServerError(internalServerErrorView(appConfig)))
      }
    }

  private def logErrorMessage(reason: String): Unit =
    logger.warn(s"Unable to confirm user identity: $reason")

}
