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

import play.Logger
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.constants.TaiConstants._
import uk.gov.hmrc.urls.Link
import views.html.{error_no_primary, error_template_noauth}

import scala.concurrent.Future

trait ErrorPagesHandler {

  val error_template_noauth: error_template_noauth
  val error_no_primary: error_no_primary

  implicit def templateRenderer: TemplateRenderer

  implicit def partialRetriever: FormPartialRetriever

  type RecoveryLocation = Class[_]

  def error4xxPageWithLink(pageTitle: String)(implicit request: Request[_], messages: Messages) =
    error_template_noauth(
      pageTitle,
      messages("tai.errorMessage.heading"),
      messages("tai.errorMessage.frontend400.message1"),
      List(
        messages(
          "tai.errorMessage.frontend400.message2",
          Link
            .toInternalPage(
              url = "#report-name",
              cssClasses = Some("report-error__toggle"),
              value = Some(messages("tai.errorMessage.reportAProblem")))
            .toHtml
        ))
    )

  def badRequestPageWrongVersion(implicit request: Request[_], messages: Messages) =
    error_template_noauth(
      messages("global.error.badRequest400.title"),
      messages("tai.errorMessage.heading"),
      messages("tai.errorMessage.frontend400.message1.version"),
      List.empty
    )

  def error4xxFromNps(pageTitle: String)(implicit request: Request[_], messages: Messages) =
    error_template_noauth(
      pageTitle,
      messages("tai.errorMessage.heading.nps"),
      messages("tai.errorMessage.frontend400.message1.nps"),
      List(messages("tai.errorMessage.frontend400.message2.nps"))
    )

  def error5xx(pageBody: String)(implicit request: Request[_], messages: Messages) =
    error_template_noauth(
      messages("global.error.InternalServerError500.title"),
      messages("tai.technical.error.heading"),
      pageBody,
      List.empty)

  @deprecated("Prefer chaining of named partial functions for clarity", "Introduction of new WDYWTD page")
  def handleErrorResponse(methodName: String, nino: Nino)(
    implicit request: Request[_],
    messages: Messages): PartialFunction[Throwable, Future[Result]] =
    PartialFunction[Throwable, Future[Result]] { throwable: Throwable =>
      throwable match {
        case e: Upstream4xxResponse => {
          Logger.warn(s"<Upstream4xxResponse> - $methodName nino $nino")
          Future.successful(BadRequest(error4xxPageWithLink(messages("global.error.badRequest400.title"))))
        }

        case e: BadRequestException => {
          Logger.warn(s"<BadRequestException> - $methodName nino $nino")

          val incorrectVersion = request.method == "POST" &&
            e.getMessage().contains("appStatusMessage") &&
            e.getMessage().contains("Version")

          val noPrimary = request.method == "GET" &&
            e.getMessage().contains("appStatusMessage") &&
            (e.getMessage().contains("Primary") ||
              e.getMessage.toLowerCase().contains(NpsNoEmploymentForCurrentTaxYear))

          incorrectVersion match {
            case true =>
              Logger.warn(s"<Incorrect Version Number> - $methodName nino $nino")
              Future.successful(BadRequest(badRequestPageWrongVersion))
            case _ =>
              e.getMessage().contains("appStatusMessage") match {
                case true =>
                  if (noPrimary) {
                    Logger.warn(
                      s"<Cannot complete a coding calculation without Primary Employment> - $methodName nino $nino")
                    Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
                  } else {
                    Future.successful(BadRequest(error4xxFromNps(messages("global.error.badRequest400.title"))))
                  }
                case _ =>
                  Future.successful(BadRequest(error4xxPageWithLink(messages("global.error.badRequest400.title"))))
              }
          }
        }

        case e: NotFoundException => {

          val noCyInfo = request.method == "GET" &&
            e.getMessage().contains("appStatusMessage") &&
            e.getMessage().contains("No Tax Account Information Found")

          Logger.warn(s"<NotFoundException> - $methodName nino $nino")
          e.getMessage().contains("appStatusMessage") match {
            case true =>
              if (noCyInfo) {
                Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
              } else {
                Future.successful(NotFound(error4xxFromNps(messages("global.error.pageNotFound404.title"))))
              }
            case _ =>
              Future.successful(NotFound(error4xxPageWithLink(messages("global.error.pageNotFound404.title"))))
          }
        }

        case e: InternalServerException => {
          Logger.warn(s"<InternalServerException> - $methodName nino $nino")
          Future.successful(InternalServerError(error5xx(messages("tai.technical.error.npsdown.message"))))
        }

        case e: Upstream5xxResponse => {
          Logger.warn(s"<Upstream5xxResponse> - $methodName nino $nino")
          Future.successful(InternalServerError(error5xx(messages("tai.technical.error.npsdown.message"))))
        }

        case e => {
          Logger.warn(s"<Unknown Exception> - $methodName nino $nino", e)
          Future.successful(InternalServerError(error5xx(messages("tai.technical.error.message"))))
        }
      }
    }

  def npsEmploymentAbsentResult(nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e: NotFoundException if e.getMessage.toLowerCase.contains(NpsAppStatusMsg) =>
      Logger.warn(s"<Not found response received from NPS> - for nino $nino @${rl.getName}")
      Future.successful(NotFound(error4xxFromNps(messages("global.error.pageNotFound404.title"))))
  }

  def rtiEmploymentAbsentResult(nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e: NotFoundException =>
      Logger.warn(s"<Not found response received from rti> - for nino $nino @${rl.getName}")
      Future.successful(NotFound(error4xxPageWithLink(messages("global.error.pageNotFound404.title"))))
  }

  def hodInternalErrorResult(nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e @ (_: InternalServerException | _: HttpException) =>
      Logger.warn(s"<Exception returned from HOD call for nino $nino @${rl.getName} with exception: ${e.getClass()}", e)
      Future.successful(InternalServerError(error5xx(messages("tai.technical.error.message"))))
  }

  def hodBadRequestResult(nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e: BadRequestException =>
      Logger.warn(
        s"<Bad request exception returned from HOD call for nino $nino @${rl.getName} with exception: ${e.getClass}",
        e)
      Future.successful(BadRequest(error4xxPageWithLink(messages("global.error.badRequest400.title"))))
  }

  def hodAnyErrorResult(nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e =>
      Logger.warn(s"<Exception returned from HOD call for nino $nino @${rl.getName} with exception: ${e.getClass()}", e)
      Future.successful(InternalServerError(error5xx(messages("tai.technical.error.message"))))
  }

  def npsTaxAccountDeceasedResult(nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[TaiResponse, Option[Result]] = {
    case TaiTaxAccountFailureResponse(msg) if msg.contains(TaiConstants.NpsTaxAccountDeceasedMsg) => {
      Logger.warn(s"<Deceased response received from nps tax account> - for nino $nino @${rl.getName}")
      Some(Redirect(routes.DeceasedController.deceased()))
    }
  }
  def npsTaxAccountCYAbsentResult_withEmployCheck(prevYearEmployments: Seq[Employment], nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[TaiResponse, Option[Result]] = {
    case TaiTaxAccountFailureResponse(msg) if msg.toLowerCase.contains(TaiConstants.NpsTaxAccountCYDataAbsentMsg) => {
      prevYearEmployments match {
        case Nil => {
          Logger.warn(
            s"<No current year data returned from nps tax account, and subsequent nps previous year employment check also empty> - for nino $nino @${rl.getName}")
          Some(BadRequest(error_no_primary()))
        }
        case _ => {
          Logger.info(
            s"<No current year data returned from nps tax account, but nps previous year employment data is present> - for nino $nino @${rl.getName}")
          None
        }
      }
    }
  }

  def npsTaxAccountAbsentResult_withEmployCheck(prevYearEmployments: Seq[Employment], nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[TaiResponse, Option[Result]] = {
    case TaiTaxAccountFailureResponse(msg) if msg.toLowerCase.contains(TaiConstants.NpsTaxAccountDataAbsentMsg) => {
      prevYearEmployments match {
        case Nil => {
          Logger.warn(
            s"<No data returned from nps tax account, and subsequent nps previous year employment check also empty> - for nino $nino @${rl.getName}")
          Some(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
        }
        case _ => {
          Logger.warn(
            s"<No data returned from nps tax account, but nps previous year employment data is present> - for nino $nino @${rl.getName}")
          None
        }
      }
    }
  }

  def npsNoEmploymentResult(nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[TaiResponse, Option[Result]] = {
    case TaiTaxAccountFailureResponse(msg) if msg.toLowerCase.contains(TaiConstants.NpsNoEmploymentsRecorded) => {
      Logger.warn(s"<No data returned from nps employments> - for nino $nino @${rl.getName}")
      Some(BadRequest(error_no_primary()))
    }
  }

  def npsNoEmploymentForCYResult_withEmployCheck(prevYearEmployments: Seq[Employment], nino: String)(
    implicit request: Request[AnyContent],
    messages: Messages,
    rl: RecoveryLocation): PartialFunction[TaiResponse, Option[Result]] = {
    case TaiTaxAccountFailureResponse(msg)
        if msg.toLowerCase.contains(TaiConstants.NpsNoEmploymentForCurrentTaxYear) => {
      prevYearEmployments match {
        case Nil => {
          Logger.warn(
            s"<No data returned from nps tax account, and subsequent nps previous year employment check also empty> - for nino $nino @${rl.getName}")
          Some(BadRequest(error_no_primary()))
        }
        case _ => {
          Logger.info(
            s"<No data returned from nps tax account, but nps previous year employment data is present> - for nino $nino @${rl.getName}")
          None
        }
      }
    }
  }

  def internalServerError(logMessage: String, ex: Option[Throwable] = None)(
    implicit request: Request[_],
    messages: Messages): Result = {
    Logger.warn(logMessage)
    ex.map(x => Logger.error(x.getMessage(), x))
    InternalServerError(error5xx(messages("tai.technical.error.message")))
  }
}
