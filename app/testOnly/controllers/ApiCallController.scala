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

package testOnly.controllers

import com.google.inject.Inject
import controllers.auth.AuthJourney
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.connectors.TaiConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

class ApiCallController @Inject() (
  mcc: MessagesControllerComponents,
  taiConnector: TaiConnector,
  authenticate: AuthJourney
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  def employmentDetails(taxYear: Int): Action[AnyContent] = authenticate.authWithoutValidatePerson.async {
    implicit request =>
      taiConnector.employmentDetails(request.taiUser.nino.nino, taxYear).map { httpResponse =>
        Status(httpResponse.status)(httpResponse.json)
      }
  }

  def taxAccount(taxYear: Int): Action[AnyContent] = authenticate.authWithoutValidatePerson.async { implicit request =>
    taiConnector.taxAccount(request.taiUser.nino.nino, taxYear).map { httpResponse =>
      Status(httpResponse.status)(httpResponse.json)
    }
  }
  def iabds(taxYear: Int): Action[AnyContent] = authenticate.authWithoutValidatePerson.async { implicit request =>
    taiConnector.iabds(request.taiUser.nino.nino, taxYear).map { httpResponse =>
      Status(httpResponse.status)(httpResponse.json)
    }
  }

}
