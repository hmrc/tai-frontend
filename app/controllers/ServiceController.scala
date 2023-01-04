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

package controllers

import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import views.html.{ManualCorrespondenceView, SessionExpiredView, TimeoutView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ServiceController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  timeout: TimeoutView,
  sessionExpired: SessionExpiredView,
  manualCorrespondence: ManualCorrespondenceView,
  implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def timeoutPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(timeout()))
  }

  def serviceSignout: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    Future.successful(Redirect(applicationConfig.basGatewayFrontendSignOutUrl))
  }

  def mciErrorPage(): Action[AnyContent] = authenticate.async { implicit request =>
    val contactUrl = request2Messages.lang.code match {
      case "cy" => applicationConfig.contactHelplineWelshUrl
      case _    => applicationConfig.contactHelplineUrl
    }

    Future.successful(Locked(manualCorrespondence(contactUrl)))
  }

  def keepAlive: Action[AnyContent] = Action {
    Ok("")
  }

  def sessionExpiredPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(sessionExpired(applicationConfig)).withNewSession)
  }
}
