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
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.PersonService

import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedRequest[A](request: Request[A], taiUser: TaiUserA) extends WrappedRequest[A](request)

case class TaiUserA(name: String, rnino: String, utr: String) {
  def getDisplayName = name
  def getNino = rnino
  def getUTR = utr
  def nino: Nino = Nino(getNino)
}

@Singleton
class AuthActionImpl @Inject()(personService: PersonService,
                               override val authConnector: core.AuthConnector)
                              (implicit ec: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L200 and AffinityGroup.Individual)
      .retrieve(Retrievals.nino) {
        case Some(nino) => {
          for {
            person <- personService.personDetails(Nino(nino))
            // TODO see if person is alive here
            taiUserA <- getTaiUser(nino, "", person)
            result <- block(AuthenticatedRequest(request, taiUserA))
          } yield {
            result
          }
        }

        //TODO specific error pages
        case _ => {
          Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
        }
      }
    //TODO redirect to failed auth page

  }

  def getTaiUser(nino: String, saUTR: String, person: Person): Future[TaiUserA] = {
    val name = person.firstName + " " + person.surname
    Future.successful(TaiUserA(name, nino, saUTR))
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]