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

import play.api.mvc.*
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UnauthorisedController @Inject() (
  mcc: MessagesControllerComponents,
  applicationConfig: ApplicationConfig,
  implicit val ec: ExecutionContext
) extends TaiBaseController(mcc) {

  def upliftUrl: String     = applicationConfig.sa16UpliftUrl
  def failureUrl: String    = applicationConfig.pertaxServiceUpliftFailedUrl
  def completionUrl: String = applicationConfig.taiHomePageUrl

  // TODO: Verify if this still used ?
  def onPageLoad: Action[AnyContent] = Action { implicit request =>
    Redirect(applicationConfig.basGatewayFrontendSignOutUrl)
  }

  def loginGG: Action[AnyContent] = Action.async {
    ggRedirect
  }

  def upliftFailedUrl: Action[AnyContent] = Action.async {
    Future.successful(
      Redirect(
        upliftUrl,
        Map(
          Origin          -> Seq("TAI"),
          ConfidenceLevel -> Seq("200"),
          CompletionUrl   -> Seq(completionUrl),
          FailureUrl      -> Seq(failureUrl)
        )
      )
    )
  }

  private def ggRedirect: Future[Result] = {
    val postSignInUpliftUrl =
      s"${ViewModelHelper.urlEncode(applicationConfig.pertaxServiceUrl)}/do-uplift?redirectUrl=${ViewModelHelper.urlEncode(
          applicationConfig.postSignInRedirectUrl
            .getOrElse(controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
        )}"

    lazy val ggSignIn =
      s"${applicationConfig.basGatewayFrontendSignInUrl}?continue_url=$postSignInUpliftUrl&accountType=individual"

    Future.successful(Redirect(ggSignIn))
  }
}
