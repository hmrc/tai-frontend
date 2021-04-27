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

package controllers

import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.constants.TaiConstants
import views.html.{error_no_primary, error_template_noauth, manualCorrespondence, timeout}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ServiceController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  timeout: timeout,
  manualCorrespondence: manualCorrespondence,
  override val error_template_noauth: error_template_noauth,
  override val error_no_primary: error_no_primary,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def timeoutPage() = Action.async { implicit request =>
    Future.successful(Ok(timeout()))
  }

  def serviceSignout = (authenticate andThen validatePerson).async { implicit request =>
    request.taiUser.providerType match {
      case Some(TaiConstants.AuthProviderVerify) =>
        Future.successful(
          Redirect(applicationConfig.citizenAuthFrontendSignOutUrl)
            .withSession(TaiConstants.SessionPostLogoutPage -> applicationConfig.feedbackSurveyUrl))
      case _ => Future.successful(Redirect(applicationConfig.basGatewayFrontendSignOutUrl))
    }
  }

  def gateKeeper() = (authenticate andThen validatePerson).async { implicit request =>
    getGateKeeper(request.taiUser.nino)
  }

  def getGateKeeper(nino: Nino)(implicit request: Request[AnyContent]): Future[Result] = {
    Future.successful(Ok(manualCorrespondence()))
  } recoverWith errorPagesHandler.handleErrorResponse("getServiceUnavailable", nino)

}
