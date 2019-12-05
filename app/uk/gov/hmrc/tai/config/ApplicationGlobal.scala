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

package uk.gov.hmrc.tai.config

import com.typesafe.config.Config
import javax.inject.Inject
import net.ceedubs.ficus.Ficus._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import uk.gov.hmrc.play.config.ControllerConfig
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.urls.Link
import play.api.mvc.Results.NotFound

import scala.concurrent.{ExecutionContext, Future}

class TaiErrorHandler @Inject()(
  localTemplateRenderer: LocalTemplateRenderer,
  taiHtmlPartialRetriever: TaiHtmlPartialRetriever,
  val messagesApi: MessagesApi,
  val configuration: Configuration,
  override val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends FrontendErrorHandler with AuthorisedFunctions {

  implicit val templateRenderer = localTemplateRenderer
  implicit val partialRetriever = taiHtmlPartialRetriever

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
    implicit request: Request[_]) =
    views.html.error_template_noauth(pageTitle, heading, message)

  def badRequestErrorTemplate(pageTitle: String, heading: String, message1: String, message2: Option[String] = None)(
    implicit request: Request[_]): Html =
    views.html.error_template_noauth(pageTitle, heading, message1, message2)

  override def badRequestTemplate(implicit request: Request[_]): Html = badRequestErrorTemplate(
    Messages("global.error.badRequest400.title"),
    Messages("tai.errorMessage.heading"),
    Messages("tai.errorMessage.frontend400.message1"),
    Some(
      Messages(
        "tai.errorMessage.frontend400.message2",
        Link
          .toInternalPage(
            url = "#report-name",
            cssClasses = Some("report-error__toggle"),
            value = Some(Messages("tai.errorMessage.reportAProblem")))
          .toHtml
      ))
  )

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    implicit val req: Request[_] = Request(request, "")
    statusCode match {
      case play.mvc.Http.Status.NOT_FOUND =>
        authorised() {
          Future.successful(NotFound(views.html.page_not_found(isAuthenticated = true)))
        } recover {
          case _ =>
            NotFound(views.html.page_not_found(isAuthenticated = false))
        }
      case _ => super.onClientError(request, statusCode, message)
    }
  }
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}
