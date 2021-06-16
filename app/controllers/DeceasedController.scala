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

import controllers.auth.AuthAction

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.renderer.TemplateRenderer
import views.html.DeceasedHelplineView

import scala.concurrent.{ExecutionContext, Future}

class DeceasedController @Inject()(
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  deceased_helpline: DeceasedHelplineView)(implicit val templateRenderer: TemplateRenderer, ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def deceased(): Action[AnyContent] =
    authenticate.async(implicit request => Future.successful(Ok(deceased_helpline())))

}
