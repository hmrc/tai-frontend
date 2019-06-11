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

import controllers.auth.TaiAuthenticationProvider
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

class AuthProviderController extends FrontendController {

  // this is magically called in a redirect by some other service, maybe from citizen-auth-frontend
  def verifyEntryPoint =  UnauthorisedAction { implicit request =>
    TaiAuthenticationProvider.logger.info("verifyEntryPoint")
    Redirect(routes.TaxAccountSummaryController.onPageLoad().url).withNewSession.addingToSession(
      SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
    )
  }

  def governmentGatewayEntryPoint =  UnauthorisedAction { implicit request =>
    TaiAuthenticationProvider.logger.info("governmentGatewayEntryPoint")
    Redirect(routes.TaxAccountSummaryController.onPageLoad().url).withNewSession.addingToSession(
      SessionKeys.authProvider -> AuthenticationProviderIds.GovernmentGatewayId
    )
  }
}
