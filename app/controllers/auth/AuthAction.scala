/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import play.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.{Retrievals, TrustedHelper}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.tai.util.constants.TaiConstants

import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedRequest[A](request: Request[A], taiUser: AuthedUser) extends WrappedRequest[A](request)

case class AuthedUser(
  validNino: String,
  utr: Option[String],
  providerType: Option[String],
  confidenceLevel: ConfidenceLevel,
  trustedHelper: Option[TrustedHelper]) {
  val nino: Nino = Nino(validNino)
}

object AuthedUser {
  def apply(
    nino: Option[String],
    utr: Option[String],
    providerType: Option[String],
    confidenceLevel: ConfidenceLevel,
    trustedHelper: Option[TrustedHelper]): AuthedUser = {
    val validNino = nino.getOrElse("")

    AuthedUser(validNino, utr, providerType, confidenceLevel, trustedHelper)
  }
}

@Singleton
class AuthActionImpl @Inject()(override val authConnector: AuthConnector)(implicit ec: ExecutionContext)
    extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised().retrieve(
      Retrievals.credentials and Retrievals.nino and Retrievals.saUtr and Retrievals.confidenceLevel and Retrievals.trustedHelper) {
      case credentials ~ nino ~ saUtr ~ confidenceLevel ~ trustedHelper => {
        val providerType = credentials.map(_.providerType)

        val userNino = trustedHelper.fold(nino)(helper => Some(helper.principalNino))

        val user = AuthedUser(userNino, saUtr, providerType, confidenceLevel, trustedHelper)

        authWithCredentials(request, block, credentials, user)
      }
      case _ => throw new RuntimeException("Can't find credentials for user")
    } recover handleEntryPointFailure(request)
  }

  private def authWithCredentials[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result],
    credentials: Option[Credentials],
    user: AuthedUser)(implicit hc: HeaderCarrier): Future[Result] =
    credentials match {
      case Some(Credentials(_, TaiConstants.AuthProviderGG)) => {
        processRequest(user, request, block, handleGGFailure)
      }
      case Some(Credentials(_, TaiConstants.AuthProviderVerify)) => {
        processRequest(user, request, block, handleVerifyFailure)
      }
      case _ => throw new RuntimeException("Can't find valid credentials for user")
    }

  private def processRequest[A](
    user: AuthedUser,
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result],
    failureHandler: PartialFunction[Throwable, Result])(implicit hc: HeaderCarrier): Future[Result] =
    (user.confidenceLevel.level match {
      case level if level >= ConfidenceLevel.L200.level => {
        for {
          result <- block(AuthenticatedRequest(request, user))
        } yield {
          result
        }
      }
      case _ => Future.successful(Redirect(routes.UnauthorisedController.upliftFailedUrl()))
    }) recover failureHandler

  private def handleEntryPointFailure[A](request: Request[A]): PartialFunction[Throwable, Result] =
    request.session.get(SessionKeys.authProvider) match {
      case Some(TaiConstants.AuthProviderVerify) =>
        handleVerifyFailure
      case _ =>
        handleGGFailure
    }

  private def handleGGFailure: PartialFunction[Throwable, Result] =
    handleFailure(routes.UnauthorisedController.loginGG())

  private def handleVerifyFailure: PartialFunction[Throwable, Result] =
    handleFailure(routes.UnauthorisedController.loginVerify())

  private def handleFailure(redirect: Call): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession => Redirect(redirect)
    case ex: AuthorisationException => {
      Logger.warn(s"Exception returned during authorisation with exception: ${ex.getClass()}", ex)
      Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]
