/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{ApplicationConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.{LocalTemplateRenderer, UserDetailsConnector}
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.PersonService
import uk.gov.hmrc.tai.util.TaiConstants

import scala.concurrent.Future

trait ServiceController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable {

  def personService: PersonService

  def userDetailsConnector: UserDetailsConnector

  def timeoutPage() = UnauthorisedAction.async {
    implicit request => Future.successful(Ok(views.html.timeout()))
  }

  def serviceSignout = authorisedForTai(personService).async {
    implicit user => implicit taiRoot => implicit request =>
      userDetailsConnector.userDetails(user.authContext).map { x =>
            if (x.hasVerifyAuthProvider) {
              Redirect(ApplicationConfig.citizenAuthFrontendSignOutUrl).
                withSession(TaiConstants.SessionPostLogoutPage -> ApplicationConfig.feedbackSurveyUrl)
            } else {
              Redirect(ApplicationConfig.companyAuthFrontendSignOutUrl)
            }
          }
  }

  def gateKeeper() = authorisedForTai(personService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          getGateKeeper(Nino(user.getNino.toString))
  }

  def getGateKeeper(nino: Nino)(implicit request: Request[AnyContent], user: TaiUser, person: Person): Future[Result] = {
    Future.successful(Ok(views.html.manualCorrespondence()))
  } recoverWith handleErrorResponse("getServiceUnavailable", nino)

}

object ServiceController extends ServiceController with AuthenticationConnectors {
  override val personService = PersonService

  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

  override def userDetailsConnector = UserDetailsConnector
}
