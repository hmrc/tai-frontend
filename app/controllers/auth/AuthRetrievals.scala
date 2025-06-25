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
import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tai.connectors.FandFConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthRetrievalsImpl])
trait AuthRetrievals
    extends ActionBuilder[InternalAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, InternalAuthenticatedRequest]

@Singleton
class AuthRetrievalsImpl @Inject() (
  override val authConnector: AuthConnector,
  mcc: MessagesControllerComponents,
  fandFConnector: FandFConnector
)(implicit ec: ExecutionContext)
    extends AuthRetrievals
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](
    request: Request[A],
    block: InternalAuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    fandFConnector
      .getTrustedHelper()
      .recoverWith { e =>
        logger.error(s"Trusted helper retrieval failed", e)
        Future.successful(None)
      }
      .flatMap { helper =>
        authorised().retrieve(Retrievals.nino and Retrievals.saUtr) {
          case optNino ~ saUtr =>
            helper match {
              case Some(helper) =>
                val user = AuthedUser.apply(
                  nino = uk.gov.hmrc.domain.Nino(optNino.getOrElse("")),
                  trustedHelper = helper,
                  saUtr = saUtr
                )
                block(InternalAuthenticatedRequest(request, user))

              case _ =>
                val user = AuthedUser.apply(
                  nino = uk.gov.hmrc.domain.Nino(optNino.getOrElse("")),
                  utr = saUtr,
                  trustedHelper = None
                )
                block(InternalAuthenticatedRequest(request, user))
            }

          case _ =>
            throw new RuntimeException("Can't find credentials for user")
        }
      }
  }

  override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

  override protected def executionContext: ExecutionContext = ec
}
