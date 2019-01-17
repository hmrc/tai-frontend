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

import com.google.inject.Inject
import controllers.actions.DeceasedActionFilter
import controllers.auth.{AuthAction, AuthActionedTaiUser}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future

class TestAuthController @Inject()(authenticate: AuthAction,
                                   filterDeceased: DeceasedActionFilter,
                                   override implicit val partialRetriever: FormPartialRetriever,
                                   override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController {

  def authCheckAction: Action[AnyContent] = (authenticate andThen filterDeceased).async {
    implicit request =>
      implicit val user: AuthActionedTaiUser = request.taiUser
      Future.successful(Ok(views.html.authCheck()))
  }
}
