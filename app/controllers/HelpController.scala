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

package controllers

import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTai}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.config.{ApplicationConfig, TaiHtmlPartialRetriever, WSHttpProxy}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.SessionData
import uk.gov.hmrc.tai.service.TaiService

import scala.concurrent.Future

trait HelpController  extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTai
  with Auditable {

  def taiService: TaiService
  def httpGet: WSHttpProxy
  def webChatURL: String

  def helpPage() = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user => implicit sessionData => implicit request =>
      getHelpPage(Nino(user.getNino))
  }

  private def getHelpPage(nino: Nino)(implicit request: Request[AnyContent], user: TaiUser, sessionData: SessionData): Future[Result] = {
    getEligibilityStatus map { status =>
      Ok(views.html.help.getHelp(status))
    }
  } recoverWith handleErrorResponse("getHelpPage", nino)

 private def getEligibilityStatus()(implicit
                             headerCarrier: HeaderCarrier
  ): Future[Option[String]] = {
    httpGet.GET[HttpResponse](webChatURL) map {
      response =>
        Logger.debug(s"Response Body: $response")
        if (response.body.nonEmpty) {
          scala.xml.XML.loadString(response.body).
            attribute("responseType").fold[Option[String]](None)(x => Some(x.head.toString()))
        } else {
          Logger.warn(s"No content returned from call to webchat: $response")
          None
        }
    }
  }.recoverWith {
    case e: Exception => {
      Logger.warn(s"Call to webchat threw exception: ${e.getMessage}")
      Future.successful(None)
    }
  }
}

object HelpController extends HelpController with AuthenticationConnectors {
  override val taiService = TaiService
  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: PartialRetriever = TaiHtmlPartialRetriever
  override val httpGet = WSHttpProxy
  override val webChatURL = ApplicationConfig.webchatAvailabilityUrl
}
