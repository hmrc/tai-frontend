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

import controllers.auth.{AuthJourney, AuthedUser}
import play.api.i18n.Messages
import play.api.mvc.*
import views.html.BeforeYouStart

import javax.inject.Inject

class BeforeYouStartController @Inject() (
  authenticate: AuthJourney,
  cc: MessagesControllerComponents,
  view: BeforeYouStart,
  implicit val errorPagesHandler: ErrorPagesHandler
) extends TaiBaseController(cc) {

  def onPageLoad(journeyType: String): Action[AnyContent] =
    authenticate.authWithValidatePerson { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyType match {
        case "employment" | "pension" =>
          Ok(view(journeyType))
        case _                        =>
          NotFound(errorPagesHandler.error4xxPageWithLink(Messages("global.error.pageNotFound404.title")))
      }
    }
}
