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

package controllers.testOnly

import com.google.inject.name.Named
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.mvc._
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TaiUpdateIncomeController @Inject()(
  @Named("Update Income") journeyCacheService: JourneyCacheService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  implicit val templateRenderer: TemplateRenderer
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def delete(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.delete() map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }
}
