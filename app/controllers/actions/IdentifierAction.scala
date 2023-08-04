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

package controllers.actions

import com.google.inject.Inject
import controllers.auth.{AuthenticatedRequest, IdentifierRequest}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class IdentifierActionImpl @Inject() ()(implicit val executionContext: ExecutionContext) extends IdentifierAction {

  override protected def transform[A](request: AuthenticatedRequest[A]): Future[IdentifierRequest[A]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request.request, request.request.session)
    hc.sessionId match {
      case Some(session) =>
        Future.successful(IdentifierRequest(request, request.fullName, request.taiUser, session.value))
      case None =>
        //        Future.successful(Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))) // TODO - Consider journey recovery controller or alternative
        throw new Exception("Missing identifier")
    }
  }
}

trait IdentifierAction extends ActionTransformer[AuthenticatedRequest, IdentifierRequest]
