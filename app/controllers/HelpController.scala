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

import javax.inject.Inject
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.{ApplicationConfig, ProxyHttpClient}

import scala.concurrent.Future

class HelpController @Inject()(val config: ApplicationConfig,
                               val httpGet: ProxyHttpClient,
                               authenticate: AuthAction,
                               validatePerson: ValidatePerson,
                               override implicit val partialRetriever: FormPartialRetriever,
                               override implicit val templateRenderer: TemplateRenderer
                              ) extends TaiBaseController {

  val webChatURL = config.webchatAvailabilityUrl

  def helpPage() = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      try {
        getEligibilityStatus map { status =>
          Ok(views.html.help.getHelp(status))
        } recover {
          case _ => internalServerError("Could not get eligibility status")
        }
      } catch {
        case _: Exception => {
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
