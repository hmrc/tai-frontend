/*
 * Copyright 2022 HM Revenue & Customs
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
import views.html.employmentHistory.IncomeTaxHistoryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IncomeTaxHistoryController @Inject()(
  val config: ApplicationConfig,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  incomeTaxHistoryView: IncomeTaxHistoryView,
  mcc: MessagesControllerComponents,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen validatePerson) { implicit request =>
    Ok(incomeTaxHistoryView(config))
  }
}
