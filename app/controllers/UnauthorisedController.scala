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

import javax.inject.Inject
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants._
import views.html.ErrorTemplateNoauth

import scala.concurrent.Future

class UnauthorisedController @Inject()(
  mcc: MessagesControllerComponents,
  applicationConfig: ApplicationConfig,
  val error_template_noauth: ErrorTemplateNoauth,
  implicit val partialRetriever: FormPartialRetriever,
  implicit val templateRenderer: TemplateRenderer)
    extends TaiBaseController(mcc) {

  def upliftUrl: String = applicationConfig.sa16UpliftUrl
  def failureUrl: String = applicationConfig.pertaxServiceUpliftFailedUrl
  def completionUrl: String = applicationConfig.taiHomePageUrl

  def onPageLoad: Action[AnyContent] = Action { implicit request =>
    Ok(unauthorisedView()).withNewSession
  }

  def loginGG: Action[AnyContent] = Action.async { implicit request =>
    ggRedirect
  }

  def loginVerify: Action[AnyContent] = Action.async { implicit request =>
    verifyRedirect
  }

  def upliftFailedUrl: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Redirect(
        upliftUrl,
        Map(
          Origin          -> Seq("TAI"),
          ConfidenceLevel -> Seq("200"),
          CompletionUrl   -> Seq(completionUrl),
          FailureUrl      -> Seq(failureUrl))
      )
    )
  }

  private def verifyRedirect(implicit request: Request[_]): Future[Result] = {
    lazy val idaSignIn = s"${applicationConfig.citizenAuthHost}/ida/login"
    Future.successful(
      Redirect(idaSignIn).withSession(
        SessionKeys.loginOrigin -> "TAI",
        SessionKeys.redirect -> applicationConfig.postSignInRedirectUrl.getOrElse(
          controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
      ))
  }

  private def ggRedirect(implicit request: Request[_]): Future[Result] = {
    val postSignInUpliftUrl =
      s"${ViewModelHelper.urlEncode(applicationConfig.pertaxServiceUrl)}/do-uplift?redirectUrl=${ViewModelHelper.urlEncode(applicationConfig.postSignInRedirectUrl
        .getOrElse(controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url))}"

    lazy val ggSignIn =
      s"${applicationConfig.basGatewayFrontendSignInUrl}?continue_url=$postSignInUpliftUrl&accountType=individual"

    Future.successful(Redirect(ggSignIn))
  }

  private def unauthorisedView()(implicit request: Request[_]) =
    error_template_noauth(
      Messages("tai.unauthorised.heading"),
      Messages("tai.unauthorised.heading"),
      Messages("tai.unauthorised.message"),
      List(
        views.html.includes
          .link(
            copy = Messages("tai.unauthorised.button-text"),
            url = applicationConfig.unauthorisedSignOutUrl,
            isButton = true,
            id = Some("sign-in")
          )
          .toString()
      )
    )
}
