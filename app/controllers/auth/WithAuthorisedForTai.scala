/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.auth

import controllers.ErrorPagesHandler
import play.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.auth.ConfigProperties
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.PersonService
import uk.gov.hmrc.tai.util.ViewModelHelper

import scala.concurrent.Future

object TaiAuthenticationProvider extends AnyAuthenticationProvider {

  override def ggwAuthenticationProvider: GovernmentGateway = new GovernmentGateway {
    def login: String = throw new RuntimeException("Unused")
    override def redirectToLogin(implicit request: Request[_]) = ggRedirect

    override def continueURL: String = throw new RuntimeException("Unused")

    override def loginURL: String = throw new RuntimeException("Unused")
  }

  override def verifyAuthenticationProvider: Verify = new Verify {
    override def login: String = throw new RuntimeException("Unused")

    override def redirectToLogin(implicit request: Request[_]) = idaRedirect
  }

  private def idaRedirect(implicit request: Request[_]): Future[Result] = {
    lazy val idaSignIn = s"${ApplicationConfig.citizenAuthHost}/${ApplicationConfig.ida_web_context}/login"
    Future.successful(Redirect(idaSignIn).withSession(
      SessionKeys.loginOrigin -> "TAI",
      SessionKeys.redirect -> ConfigProperties.postSignInRedirectUrl.getOrElse(controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
    ))
  }

  private def ggRedirect(implicit request: Request[_]): Future[Result] = {
    val postSignInUpliftUrl = s"${ViewModelHelper.urlEncode(ApplicationConfig.pertaxServiceUrl)}/do-uplift?redirectUrl=${ViewModelHelper.
      urlEncode(ConfigProperties.postSignInRedirectUrl.
      getOrElse(controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url))}"

    lazy val ggSignIn = s"${ApplicationConfig.
      companyAuthUrl}/${ApplicationConfig.gg_web_context}/sign-in?continue=${postSignInUpliftUrl}&accountType=individual"
    Future.successful(Redirect(ggSignIn))
  }

  override def redirectToLogin(implicit request: Request[_]) = ggRedirect

  override def login: String = throw new RuntimeException("Unused")
}

trait WithAuthorisedForTaiLite extends DelegationAwareActions { this: ErrorPagesHandler =>

  private type PlayRequest = (Request[AnyContent] => Result)
  private type AsyncPlayRequest = (Request[AnyContent] => Future[Result])
  private type TaiUserRequest = TaiUser => PlayRequest
  private type AsyncTaiUserRequest = TaiUser => Person => AsyncPlayRequest

  implicit private def createHeaderCarrier(implicit request: Request[_]): HeaderCarrier =
    fromHeadersAndSession(request.headers,Some(request.session))

  def authorisedForTai(implicit personService: PersonService) = {
    new AuthorisedByTai(AuthorisedFor(TaiRegime, TaiConfidenceLevelPredicate), personService)
  }

  class AuthorisedByTai(authedBy: AuthenticatedBy, personService: PersonService) {

    def async(action: AsyncTaiUserRequest) = authedBy.async {
      authContext => implicit request =>
        resolveLoggedInTaiUser(authContext => implicit request => action(authContext)(request), authContext)
    }

    private def resolveLoggedInTaiUser(body: AsyncTaiUserRequest, authContext: AuthContext) (implicit request: Request[AnyContent]): Future[Result] = {
      val taiAccount = authContext.principal.accounts.paye.getOrElse(throw new IllegalArgumentException("Cannot find tai user authority"))
      for {
        person <- personService.personDetails(taiAccount.nino)
        taiUser <- getTaiUser(authContext, person)
        result <- body(taiUser)(person)(request)
      } yield result
    }.recoverWith{
      case e => {
        val ninoString = authContext.principal.accounts.paye.map(paye => paye.nino.nino).getOrElse("")
        Logger.warn(s"<Exception returned during user resolution for nino ${ninoString} @${classOf[AuthorisedByTai].getName} with exception: ${e.getClass()}", e)
        Future.successful(InternalServerError(error5xx(Messages("tai.technical.error.message"))))
      }
    }

    def getTaiUser(authContext: AuthContext, person: Person)(implicit request: Request[_]): Future[TaiUser] = {
      Future.successful(TaiUser(authContext, person).getAuthContext(person.firstName + " " + person.surname))
    }
  }
}

case class TaiUser(authContext: AuthContext, person: Person){
    def getAuthContext(name: String) = if (!authContext.isDelegating){
       this.copy(authContext = authContext.copy(principal = authContext.principal.copy(Some(name))))
    } else { this }

    def getDisplayName = if (!authContext.isDelegating){
      authContext.principal.name.fold("Name Not Defined") { name => name }
    } else {
      authContext.attorney.fold("Name Not Defined") { name => name.name }
    }

    def getNino = authContext.principal.accounts.paye.map(paye => paye.nino.nino).getOrElse("")

    def getUTR = authContext.principal.accounts.sa.map(sa => sa.utr.utr).getOrElse("")

}
