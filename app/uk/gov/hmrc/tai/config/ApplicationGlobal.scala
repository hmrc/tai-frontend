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

package uk.gov.hmrc.tai.config

import javax.inject.Inject
import play.api.Configuration
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.urls.Link

class TaiErrorHandler @Inject()(
  localTemplateRenderer: LocalTemplateRenderer,
  taiHtmlPartialRetriever: TaiHtmlPartialRetriever,
  applicationConfig: ApplicationConfig,
  val messagesApi: MessagesApi,
  val configuration: Configuration)
    extends FrontendErrorHandler {

  implicit val templateRenderer = localTemplateRenderer
  implicit val partialRetriever = taiHtmlPartialRetriever

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
    implicit request: Request[_]) =
    views.html.error_template_noauth(pageTitle, heading, message, List.empty)

  def badRequestErrorTemplate(
    pageTitle: String,
    heading: String,
    message1: String,
    additionalMessages: List[String] = List.empty)(implicit request: Request[_]): Html =
    views.html.error_template_noauth(pageTitle, heading, message1, additionalMessages)

  override def badRequestTemplate(implicit request: Request[_]): Html = badRequestErrorTemplate(
    Messages("global.error.badRequest400.title"),
    Messages("tai.errorMessage.heading"),
    Messages("tai.errorMessage.frontend400.message1"),
    List(
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

  override def notFoundTemplate(implicit request: Request[_]): Html = {

    val contactUrl = request2Messages.lang.code match {
      case "cy" => applicationConfig.contactHelplineWelshUrl
      case _    => applicationConfig.contactHelplineUrl
    }

    badRequestErrorTemplate(
      Messages("tai.errorMessage.pageNotFound.title"),
      Messages("tai.errorMessage.pageNotFound.heading"),
      Messages("tai.errorMessage.pageNotFound.ifYouTyped"),
      List(
        Messages("tai.errorMessage.pageNotFound.ifYouPasted"),
        Messages(
          "tai.errorMessage.pageNotFound.contactHelpline.text",
          Link
            .toInternalPage(
              url = contactUrl,
              value = Some(Messages("tai.errorMessage.pageNotFound.contactHelpline.link"))
            )
            .toHtml
        )
      )
    )
  }
}
