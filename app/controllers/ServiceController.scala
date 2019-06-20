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

package controllers

import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import javax.inject.Inject
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.UnauthorisedAction
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.UserDetailsConnector
import uk.gov.hmrc.tai.util.constants.TaiConstants

import scala.concurrent.Future
import scala.util.control.NonFatal

class ServiceController @Inject()(authenticate: AuthAction,
                                  validatePerson: ValidatePerson,
                                  override implicit val partialRetriever: FormPartialRetriever,
                                  override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController {

  def timeoutPage() = UnauthorisedAction.async {
    implicit request => Future.successful(Ok(views.html.timeout()))
  }

  def serviceSignout = (authenticate andThen validatePerson).async {
    implicit request =>

      if (request.taiUser.providerType == TaiConstants.AuthProviderVerify) {
        Future.successful(Redirect(ApplicationConfig.citizenAuthFrontendSignOutUrl).
          withSession(TaiConstants.SessionPostLogoutPage -> ApplicationConfig.feedbackSurveyUrl))
      } else {
        Future.successful(Redirect(ApplicationConfig.companyAuthFrontendSignOutUrl))
      }
  }

  def gateKeeper() = (authenticate andThen validatePerson).async {
    implicit request =>
      getGateKeeper(request.taiUser.nino)
  }

  def getGateKeeper(nino: Nino)(implicit request: Request[AnyContent]): Future[Result] = {
    Future.successful(Ok(views.html.manualCorrespondence()))
  } recoverWith handleErrorResponse("getServiceUnavailable", nino)

}
