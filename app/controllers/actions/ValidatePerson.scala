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

import com.google.inject.ImplementedBy
import controllers.auth.{AuthenticatedRequest, InternalAuthenticatedRequest}
import controllers.routes
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tai.service.PersonService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ValidatePersonImpl])
trait ValidatePerson extends ActionRefiner[InternalAuthenticatedRequest, AuthenticatedRequest]

@Singleton
class ValidatePersonImpl @Inject() (personService: PersonService)(implicit ec: ExecutionContext)
    extends ValidatePerson {

  override protected def refine[A](
    request: InternalAuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedRequest[A]]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val personNino = request.taiUser.nino
    val person = personService.personDetails(personNino)

    person map {
      case p if p.isDeceased =>
        Left(Redirect(routes.DeceasedController.deceased()))
      case p if p.manualCorrespondenceInd =>
        Left(Redirect(routes.ServiceController.mciErrorPage()))
      case p => Right(AuthenticatedRequest(request, request.taiUser, p.name))
    }
  }
  override protected def executionContext: ExecutionContext = ec
}
