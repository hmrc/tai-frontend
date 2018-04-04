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

import controllers.auth.TaiUser
import play.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.urls.Link
import uk.gov.hmrc.tai.util.TaiConstants._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ BadRequestException, HttpException, InternalServerException, NotFoundException, Upstream4xxResponse, Upstream5xxResponse }

trait ErrorPagesHandler {

  implicit def templateRenderer: TemplateRenderer
  implicit def partialRetriever: PartialRetriever

  type RecoveryLocation = Class[_]

  def error4xxPageWithLink(pageTitle: String)
                          (implicit request: Request[_], user: TaiUser) = {
    views.html.error_template_noauth(
      pageTitle,
      Messages("tai.errorMessage.heading"),
      Messages("tai.errorMessage.frontend400.message1"),
      Some(Messages("tai.errorMessage.frontend400.message2", Link.toInternalPage(
        url = "#report-name",
        cssClasses = Some("report-error__toggle"),
        value = Some(Messages("tai.errorMessage.reportAProblem"))).toHtml
      )))
  }

  def badRequestPageWrongVersion(implicit request: Request[_], user: TaiUser) = {
    views.html.error_template_noauth(
      Messages("global.error.badRequest400.title"),
      Messages("tai.errorMessage.heading"),
      Messages("tai.errorMessage.frontend400.message1.version"))
  }

  def error4xxFromNps(pageTitle: String)
                     (implicit request: Request[_], user: TaiUser)= {
    views.html.error_template_noauth(
      pageTitle,
      Messages("tai.errorMessage.heading.nps"),
      Messages("tai.errorMessage.frontend400.message1.nps"),
      Some(Messages("tai.errorMessage.frontend400.message2.nps")))
  }

  def error5xxFromNps(pageBody: String)
                     (implicit request: Request[_], user: TaiUser)= {
    views.html.error_template_noauth(
      Messages("global.error.InternalServerError500.title"),
      Messages("tai.technical.error.heading"),
      pageBody)
  }

  @deprecated("Prefer chaining of named partial functions for clarity", "Introduction of new WDYWTD page")
  def handleErrorResponse(methodName: String, nino: Nino)
                         (implicit request: Request[_],
                          user: TaiUser):
  PartialFunction[Throwable, Future[Result]] = PartialFunction[Throwable, Future[Result]] {
    throwable: Throwable =>
      throwable match {
        case e: Upstream4xxResponse => {
          Logger.warn(s"<Upstream4xxResponse> - $methodName nino $nino")
          Future.successful(BadRequest(error4xxPageWithLink(
            Messages("global.error.badRequest400.title"))))
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
            case _    =>  e.getMessage().contains("appStatusMessage") match {
              case true =>
                if(noPrimary) {
                  Logger.warn(s"<Cannot complete a coding calculation without Primary Employment> - $methodName nino $nino")
                  Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
                } else {
                  Future.successful(BadRequest(error4xxFromNps(
                    Messages("global.error.badRequest400.title"))))
                }
              case _ =>  Future.successful(BadRequest(error4xxPageWithLink(
                Messages("global.error.badRequest400.title"))))
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
                Future.successful(NotFound(error4xxFromNps(
                  Messages("global.error.pageNotFound404.title"))))
              }
            case _ =>
              Future.successful(NotFound(error4xxPageWithLink(
                Messages("global.error.pageNotFound404.title"))))
          }
        }

        case e: InternalServerException => {
          Logger.warn(s"<InternalServerException> - $methodName nino $nino")
          Future.successful(InternalServerError(error5xxFromNps(
            Messages("tai.technical.error.npsdown.message"))))
        }

        case e: Upstream5xxResponse => {
          Logger.warn(s"<Upstream5xxResponse> - $methodName nino $nino")
          Future.successful(InternalServerError(error5xxFromNps(
            Messages("tai.technical.error.npsdown.message"))))
        }

        case e => {
          Logger.warn(s"<Unknown Exception> - $methodName nino $nino", e)
          Future.successful(InternalServerError(error5xxFromNps(
            Messages("tai.technical.error.message"))))
        }
      }
  }

  def npsTaxAccountDeceasedResult(implicit request: Request[AnyContent], user: TaiUser, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:InternalServerException if e.getMessage.toLowerCase.contains(NpsTaxAccountDeceasedMsg) =>
      Logger.warn(s"<Deceased response received from nps tax account> - for nino ${user.getNino} @${rl.getName}")
      Future.successful(Redirect(routes.DeceasedController.deceased()))
  }

  def npsEmploymentAbsentResult(implicit request: Request[AnyContent], user: TaiUser, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:NotFoundException if e.getMessage.toLowerCase.contains(NpsAppStatusMsg) =>
      Logger.warn(s"<Not found response received from NPS> - for nino ${user.getNino} @${rl.getName}")
      Future.successful(NotFound(error4xxFromNps(Messages("global.error.pageNotFound404.title"))))
  }

  def rtiEmploymentAbsentResult(implicit request: Request[AnyContent], user: TaiUser, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:NotFoundException =>
      Logger.warn(s"<Not found response received from rti> - for nino ${user.getNino} @${rl.getName}")
      Future.successful(NotFound(error4xxPageWithLink(Messages("global.error.pageNotFound404.title"))))
  }

  def rtiDataAbsentResult(implicit request: Request[AnyContent], user: TaiUser, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:NotFoundException if e.getMessage.toLowerCase.contains(RtiPaymentDataAbsentMsg) =>
      Logger.warn(s"<Data not found response received from rti payments> - for nino ${user.getNino} @${rl.getName}")
      Future.successful(NotFound(error4xxPageWithLink(Messages("global.error.pageNotFound404.title"))))
  }

  def npsTaxAccountCYAbsentResult_withEmployCheck(employments: Nino => Future[Seq[Employment]],
                                                  proceed: TaiRoot => Future[Result])
                                                 (implicit request: Request[AnyContent], user: TaiUser, ec: ExecutionContext, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:NotFoundException if e.getMessage.toLowerCase.contains(NpsTaxAccountCYDataAbsentMsg) =>
      employments(Nino(user.getNino)) flatMap {
        case Nil => {
          Logger.warn(s"<No current year data returned from nps tax account, and subsequent nps employment check also empty> - for nino ${user.getNino} @${rl.getName}")
          Future.successful(BadRequest(views.html.error_no_primary()))
        }
        case _ => {
          Logger.info(s"<No current year data returned from nps tax account, but nps employment data is present> - for nino ${user.getNino} @${rl.getName}")
          proceed(user.taiRoot)
        }
      }
  }

  def npsTaxAccountAbsentResult_withEmployCheck(employments: Nino => Future[Seq[Employment]],
                                                proceed: TaiRoot => Future[Result])
                                               (implicit request: Request[AnyContent], user: TaiUser, ec: ExecutionContext, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:BadRequestException if e.getMessage.toLowerCase.contains(NpsTaxAccountDataAbsentMsg) && request.method == "GET" =>
      employments(Nino(user.getNino)) flatMap {
        case Nil => {
          Logger.warn(s"<No data returned from nps tax account, and subsequent nps employment check also empty> - for nino ${user.getNino} @${rl.getName}")
          Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
        }
        case _ => {
          Logger.info(s"<No data returned from nps tax account, but nps employment data is present> - for nino ${user.getNino} @${rl.getName}")
          proceed(user.taiRoot)
        }
      }
  }

  def npsNoEmploymentForCYResult(employments: Nino => Future[Seq[Employment]],
                                                proceed: TaiRoot => Future[Result])
                                               (implicit request: Request[AnyContent], user: TaiUser, ec: ExecutionContext, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:BadRequestException if e.getMessage.toLowerCase.contains(NpsNoEmploymentForCurrentTaxYear) && request.method == "GET" =>
      employments(Nino(user.getNino)) flatMap {
        case Nil => {
          Logger.warn(s"<No data returned from nps tax account, and subsequent nps employment check also empty> - for nino ${user.getNino} @${rl.getName}")
          Future.successful(BadRequest(views.html.error_no_primary()))
        }
        case _ => {
          Logger.info(s"<No data returned from nps tax account, but nps employment data is present> - for nino ${user.getNino} @${rl.getName}")
          proceed(user.taiRoot)
        }
      }
  }

  def npsNoEmploymentResult(implicit request: Request[AnyContent], user: TaiUser, ec: ExecutionContext, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:BadRequestException if e.getMessage.toLowerCase.contains(NpsNoEmploymentsRecorded) && request.method == "GET" =>
      Logger.warn(s"<No data returned from nps employments> - for nino ${user.getNino} @${rl.getName}")
      Future.successful(BadRequest(views.html.error_no_primary()))
  }

  def hodInternalErrorResult(implicit request: Request[AnyContent], user: TaiUser, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e @ (_:InternalServerException | _:HttpException) =>
      Logger.warn(s"<Exception returned from HOD call for nino ${user.getNino} @${rl.getName} with exception: ${e.getClass()}", e)
      Future.successful(InternalServerError(error5xxFromNps(Messages("tai.technical.error.message"))))
  }

  def hodBadRequestResult(implicit request: Request[AnyContent], user: TaiUser, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e:BadRequestException =>
      Logger.warn(s"<Bad request exception returned from HOD call for nino ${user.getNino} @${rl.getName} with exception: ${e.getClass}", e)
      Future.successful(BadRequest(error4xxPageWithLink(Messages("global.error.badRequest400.title"))))
  }

  def hodAnyErrorResult(implicit request: Request[AnyContent], user: TaiUser, rl: RecoveryLocation): PartialFunction[Throwable, Future[Result]] = {
    case e =>
      Logger.warn(s"<Exception returned from HOD call for nino ${user.getNino} @${rl.getName} with exception: ${e.getClass()}", e)
      Future.successful(InternalServerError(error5xxFromNps(Messages("tai.technical.error.message"))))
  }
}

