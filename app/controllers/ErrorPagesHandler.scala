/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc.{AnyContent, Request, Result}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http._
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.constants.TaiConstants._
import views.html.includes.link
import views.html.{ErrorNoPrimary, ErrorTemplateNoauth}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ErrorPagesHandler @Inject() (errorTemplateNoauth: ErrorTemplateNoauth, errorNoPrimary: ErrorNoPrimary)(implicit
  val ec: ExecutionContext
) extends Logging {
  type RecoveryLocation = Class[_]

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

  def badRequestPageWrongVersion(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable =
    errorTemplateNoauth(
      messages("global.error.badRequest400.title"),
      messages("tai.errorMessage.heading"),
      messages("tai.errorMessage.frontend400.message1.version"),
      List.empty
    )

  def error4xxFromNps(pageTitle: String)(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable =
    errorTemplateNoauth(
      pageTitle,
      messages("tai.errorMessage.heading.nps"),
      messages("tai.errorMessage.frontend400.message1.nps"),
      List(messages("tai.errorMessage.frontend400.message2.nps"))
    )

  def error5xx(pageBody: String)(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable =
    errorTemplateNoauth(
      messages("global.error.InternalServerError500.title"),
      messages("tai.technical.error.heading"),
      pageBody,
      List.empty
    )

  def npsEmploymentAbsentResult(nino: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Future[Result]] = {
    case e: NotFoundException if e.getMessage.toLowerCase.contains(NpsAppStatusMsg) =>
      logger.warn(s"<Not found response received from NPS> - for nino $nino @${rl.getName}")
      Future.successful(NotFound(error4xxFromNps(messages("global.error.pageNotFound404.title"))))
  }

  def rtiEmploymentAbsentResult(nino: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Future[Result]] = { case e: NotFoundException =>
    logger.warn(s"<Not found response received from rti> - for nino $nino @${rl.getName}")
    Future.successful(NotFound(error4xxPageWithLink(messages("global.error.pageNotFound404.title"))))
  }

  def hodInternalErrorResult(nino: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Future[Result]] = { case e @ (_: InternalServerException | _: HttpException) =>
    logger.warn(s"<Exception returned from HOD call for nino $nino @${rl.getName} with exception: ${e.getClass()}", e)
    Future.successful(InternalServerError(error5xx(messages("tai.technical.error.message"))))
  }

  def hodBadRequestResult(nino: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Future[Result]] = { case e: BadRequestException =>
    logger.warn(
      s"<Bad request exception returned from HOD call for nino $nino @${rl.getName} with exception: ${e.getClass}",
      e
    )
    Future.successful(BadRequest(error4xxPageWithLink(messages("global.error.badRequest400.title"))))
  }

  def hodAnyErrorResult(nino: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Future[Result]] = { case e =>
    logger.warn(s"<Exception returned from HOD call for nino $nino @${rl.getName} with exception: ${e.getClass()}", e)
    Future.successful(InternalServerError(error5xx(messages("tai.technical.error.message"))))
  }

  def npsTaxAccountDeceasedResult(nino: String)(implicit
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Option[Result]] = {
    case NonFatal(e) if e.getMessage.contains(TaiConstants.NpsTaxAccountDeceasedMsg) =>
      logger.warn(s"<Deceased response received from nps tax account> - for nino $nino @${rl.getName}")
      Some(Redirect(routes.DeceasedController.deceased()))
  }
  def npsTaxAccountCYAbsentResult_withEmployCheck(prevYearEmployments: Seq[Employment], nino: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Option[Result]] = {
    case NonFatal(e) if e.getMessage.toLowerCase.contains(TaiConstants.NpsTaxAccountCYDataAbsentMsg) =>
      prevYearEmployments match {
        case Nil =>
          logger.warn(
            s"<No current year data returned from nps tax account, and subsequent nps previous year employment check also empty> - for nino $nino @${rl.getName}"
          )
          Some(BadRequest(errorNoPrimary()))
        case _ =>
          logger.info(
            s"<No current year data returned from nps tax account, but nps previous year employment data is present> - for nino $nino @${rl.getName}"
          )
          None
      }
  }

  def npsTaxAccountAbsentResult_withEmployCheck(prevYearEmployments: Seq[Employment], nino: String)(implicit
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Option[Result]] = {
    case NonFatal(e) if e.getMessage.toLowerCase.contains(TaiConstants.NpsTaxAccountDataAbsentMsg) =>
      prevYearEmployments match {
        case Nil =>
          logger.warn(
            s"<No data returned from nps tax account, and subsequent nps previous year employment check also empty> - for nino $nino @${rl.getName}"
          )
          Some(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
        case _ =>
          logger.warn(
            s"<No data returned from nps tax account, but nps previous year employment data is present> - for nino $nino @${rl.getName}"
          )
          None
      }
  }

  def npsNoEmploymentResult(nino: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Option[Result]] = {
    case NonFatal(e) if e.getMessage.toLowerCase.contains(TaiConstants.NpsNoEmploymentsRecorded) =>
      logger.warn(s"<No data returned from nps employments> - for nino $nino @${rl.getName}")
      Some(BadRequest(errorNoPrimary()))
  }

  def npsNoEmploymentForCYResult_withEmployCheck(prevYearEmployments: Seq[Employment], nino: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation
  ): PartialFunction[Throwable, Option[Result]] = {
    case NonFatal(e) if e.getMessage.toLowerCase.contains(TaiConstants.NpsNoEmploymentForCurrentTaxYear) =>
      prevYearEmployments match {
        case Nil =>
          logger.warn(
            s"<No data returned from nps tax account, and subsequent nps previous year employment check also empty> - for nino $nino @${rl.getName}"
          )
          Some(BadRequest(errorNoPrimary()))
        case _ =>
          logger.info(
            s"<No data returned from nps tax account, but nps previous year employment data is present> - for nino $nino @${rl.getName}"
          )
          None
      }
  }

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
