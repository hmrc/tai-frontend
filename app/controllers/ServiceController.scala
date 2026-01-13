/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.auth.AuthJourney
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.config.ApplicationConfig
import views.html.{ManualCorrespondenceView, TimeoutView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ServiceController @Inject() (
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  timeout: TimeoutView,
  manualCorrespondence: ManualCorrespondenceView,
  journeyCacheRepository: JourneyCacheRepository
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def timeoutPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(timeout()))
  }

  def serviceSignout(): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    Future.successful(basSignOutRedirect)
  }

  def sessionExpired(): Action[AnyContent] = Action(implicit request => basSignOutRedirect)

  private def basSignOutRedirect: Result = Redirect(applicationConfig.basGatewayFrontendSignOutUrl)

  def mciErrorPage(): Action[AnyContent] = authenticate.authWithoutValidatePerson.async { implicit request =>
    val contactUrl = request2Messages.lang.code match {
      case "cy" => applicationConfig.contactHelplineWelshUrl
      case _    => applicationConfig.contactHelplineUrl
    }

    Future.successful(Locked(manualCorrespondence(contactUrl)))
  }

  def keepAlive(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    journeyCacheRepository.keepAlive(request.userAnswers.sessionId, request.userAnswers.nino).map { _ =>
      Ok("")
    }
  }
}
