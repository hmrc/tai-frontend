/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import controllers.auth.{AuthenticatedRequest, InternalAuthenticatedRequest}
import controllers.routes
import play.api.mvc.{ActionFilter, ActionRefiner, Result}
import uk.gov.hmrc.tai.service.PersonService
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidatePersonImpl @Inject()(personService: PersonService)(implicit ec: ExecutionContext) extends ValidatePerson {

  override protected def refine[A](
    request: InternalAuthenticatedRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    val personNino = request.taiUser.nino
    val person = personService.personDetails(personNino)

    person map {
      case p if p.isDeceased              => Left(Redirect(routes.DeceasedController.deceased()))
      case p if p.manualCorrespondenceInd => Left(Redirect(routes.ServiceController.mciErrorPage()))
      case p                              => Right(AuthenticatedRequest(request, request.taiUser, p.name))
    }
  }
  override protected def executionContext: ExecutionContext = ec
}

@ImplementedBy(classOf[ValidatePersonImpl])
trait ValidatePerson extends ActionRefiner[InternalAuthenticatedRequest, AuthenticatedRequest]
