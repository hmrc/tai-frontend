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

import com.google.inject.ImplementedBy
import controllers.routes
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tai.service.MessageFrontendService
import uk.gov.hmrc.tai.util.constants.TaiConstants

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction
    extends ActionBuilder[InternalAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, InternalAuthenticatedRequest]

@Singleton
class AuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  messageFrontendService: MessageFrontendService,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends AuthAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](
    request: Request[A],
    block: InternalAuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(
      Retrievals.credentials and Retrievals.nino and Retrievals.saUtr and Retrievals.confidenceLevel and Retrievals.trustedHelper
    ) {
      case credentials ~ _ ~ saUtr ~ confidenceLevel ~ Some(helper) =>
        // TODO: _ above is logged in user nino - do we need to use this instead of helpee nino below?
        val providerType = credentials.map(_.providerType)
        messageFrontendService.getUnreadMessageCount(request).flatMap { messageCount =>
          val user = AuthedUser(helper, saUtr, providerType, confidenceLevel, messageCount)
          authWithCredentials(request, block, credentials, user)
        }

      case credentials ~ Some(nino) ~ saUtr ~ confidenceLevel ~ _ =>
        val providerType = credentials.map(_.providerType)
        messageFrontendService.getUnreadMessageCount(request).flatMap { messageCount =>
          val user = AuthedUser(uk.gov.hmrc.domain.Nino(nino), saUtr, providerType, confidenceLevel, messageCount, None)
          authWithCredentials(request, block, credentials, user)
        }
      case _ => throw new RuntimeException("Can't find credentials for user")
    } recover handleEntryPointFailure(request)

  }

  private def authWithCredentials[A](
    request: Request[A],
    block: InternalAuthenticatedRequest[A] => Future[Result],
    credentials: Option[Credentials],
    user: AuthedUser
  ): Future[Result] =
    credentials match {
      case Some(Credentials(_, TaiConstants.AuthProviderGG)) =>
        processRequest(user, request, block, handleGGFailure)
      case _ => throw new RuntimeException("Can't find valid credentials for user")
    }

  private def processRequest[A](
    user: AuthedUser,
    request: Request[A],
    block: InternalAuthenticatedRequest[A] => Future[Result],
    failureHandler: PartialFunction[Throwable, Result]
  ): Future[Result] =
    (user.confidenceLevel.level match {
      case level if level >= 200 =>
        for {
          result <- block(InternalAuthenticatedRequest(request, user))
        } yield result
      case _ =>
        Future.successful(Redirect(routes.UnauthorisedController.upliftFailedUrl()))
    }) recover failureHandler

  private def handleEntryPointFailure[A](request: Request[A]): PartialFunction[Throwable, Result] = handleGGFailure

  private def handleGGFailure: PartialFunction[Throwable, Result] =
    handleFailure(routes.UnauthorisedController.loginGG())

  private def handleFailure(redirect: Call): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Redirect(redirect)
    case ex: AuthorisationException =>
      logger.warn(s"Exception returned during authorisation with exception: ${ex.getClass()}", ex)
      Redirect(routes.UnauthorisedController.onPageLoad())
  }
  override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser
  override protected def executionContext: ExecutionContext = ec
}
