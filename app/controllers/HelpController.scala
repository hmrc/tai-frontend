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

import com.google.inject.Inject
import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.{ApplicationConfig, WSHttpProxy}
import uk.gov.hmrc.tai.service.PersonService

import scala.concurrent.Future

class HelpController @Inject()(val config: ApplicationConfig,
                               val httpGet: WSHttpProxy,
                               personService: PersonService,
                               val auditConnector: AuditConnector,
                               val delegationConnector: DelegationConnector,
                               val authConnector: AuthConnector,
                               override implicit val partialRetriever: FormPartialRetriever,
                               override implicit val templateRenderer: TemplateRenderer
                              ) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable {

  val webChatURL = config.webchatAvailabilityUrl

  def helpPage() = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          try {
            getEligibilityStatus map { status =>
              Ok(views.html.help.getHelp(status))
            } recoverWith handleErrorResponse("getHelpPage", Nino(user.getNino))
          } catch {
            case e: Exception => {
              Future.successful(Ok(views.html.help.getHelp(None)))
            }
          }
  }

  private def getEligibilityStatus()(implicit headerCarrier: HeaderCarrier): Future[Option[String]] = {
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
