/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.NonEmptyList
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import play.twirl.api.HtmlFormat
import views.html.includes.link
import views.html.ErrorTemplateNoauth

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ErrorPagesHandler @Inject() (errorTemplateNoauth: ErrorTemplateNoauth)(implicit
  val ec: ExecutionContext
) extends Logging {

  def error4xxPageWithLink(pageTitle: String)(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable =
    errorTemplateNoauth(
      pageTitle,
      messages("tai.errorMessage.heading"),
      messages("tai.errorMessage.frontend400.message1"),
      List(
        messages(
          "tai.errorMessage.frontend400.message2",
          link(
            url = "#report-name",
            copy = messages("tai.errorMessage.reportAProblem"),
            linkClasses = Seq("report-error__toggle")
          )
        )
      )
    )

  def error5xx(pageBody: String)(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable =
    errorTemplateNoauth(
      messages("global.error.InternalServerError500.title"),
      messages("tai.technical.error.heading"),
      pageBody,
      List.empty
    )

  def internalServerError(logMessage: String, ex: Option[Throwable] = None)(implicit
    request: Request[_],
    messages: Messages
  ): Result = {
    logger.warn(logMessage)
    ex.foreach(x => logger.error(x.getMessage, x))
    InternalServerError(error5xx(messages("tai.technical.error.message")))
  }

  def internalServerError(logMessage: String, ex: NonEmptyList[Throwable])(implicit
    request: Request[_],
    messages: Messages
  ): Result = {
    logger.warn(logMessage)
    ex.map(x => logger.error(x.getMessage, x))
    InternalServerError(error5xx(messages("tai.technical.error.message")))
  }
}
