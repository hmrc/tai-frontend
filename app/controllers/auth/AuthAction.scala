/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.routes
import play.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.tai.service.PersonService

import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedRequest[A](request: Request[A], taiUser: AuthActionedTaiUser) extends WrappedRequest[A](request)

case class AuthActionedTaiUser(name: String, validNino: String, utr: String) {
  def getDisplayName = name

  def getNino = validNino

  def nino: Nino = Nino(validNino)

  def getUTR = utr
}

object AuthActionedTaiUser {
  def apply(name: Option[Name], nino: Option[String], saUtr: Option[String]): AuthActionedTaiUser = {
    val validNino = nino.getOrElse("")
    val validName = name.flatMap(_.name).getOrElse("")
    val validUtr = saUtr.getOrElse("")

    AuthActionedTaiUser(validName, validNino, validUtr)
  }
}

@Singleton
class AuthActionImpl @Inject()(personService: PersonService,
                               override val authConnector: AuthConnector)
                              (implicit ec: ExecutionContext) extends AuthAction
  with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L200 and AffinityGroup.Individual)
      .retrieve(Retrievals.nino and Retrievals.name and Retrievals.saUtr) {
        case nino ~ name ~ saUtr => {
          val taiUser = AuthActionedTaiUser(name, nino, saUtr)

          for {
            result <- block(AuthenticatedRequest(request, taiUser))
          } yield {
            result
          }
        }

        case _ => {
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
        }
      } recover handleFailure
  }

  private def handleFailure: PartialFunction[Throwable, Result] = {
    case ex => {
      Logger.warn(s"<Exception returned during authorisation with exception: ${ex.getClass()}", ex)
      Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]
