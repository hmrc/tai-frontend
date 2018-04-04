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
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import play.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Results.{BadRequest, Redirect}
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.config.{FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.forms.WhatDoYouWantToDoForm
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.model.{SessionData, TaiRoot}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaiConstants
import uk.gov.hmrc.tai.viewModels.WhatDoYouWantToDoViewModel
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future

trait WhatDoYouWantToDoController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with FeatureTogglesConfig {

  def taiService: TaiService
  def employmentService: EmploymentService
  def auditService: AuditService
  def trackingService: TrackingService
  def taxAccountService: TaxAccountService

  def whatDoYouWantToDoPage(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

        //    sessionData(user).flatMap( _ => requestedPage.apply(taiRoot) ) //recoverWith hodStatusRedirect

            val taxAccountSummaryFuture = taxAccountService.taxAccountSummary(Nino(user.getNino), TaxYear())
            val employmentsFuture = employmentService.employments(Nino(user.getNino), TaxYear())
            val prevYearEmploymentsFuture = previousYearEmployments.apply(Nino(user.getNino))

            val possibleRedirectFuture =
              for {
                taxAccountSummary <- taxAccountSummaryFuture
                _ <- employmentsFuture
                prevYearEmployments <- prevYearEmploymentsFuture
              } yield {

                taxAccountSummary match {
                  case TaiTaxAccountFailureResponse(msg) if msg.contains(TaiConstants.NpsTaxAccountDeceasedMsg) => {
                    Logger.warn(s"<Deceased response received from nps tax account> - for nino ${user.getNino} @${"blah"}")
                    Some(Redirect(routes.DeceasedController.deceased()))
                  }
                  case TaiTaxAccountFailureResponse(msg) if msg.toLowerCase.contains(TaiConstants.NpsTaxAccountCYDataAbsentMsg) => {
                    prevYearEmployments match {
                      case Nil => {
                        Logger.warn(s"<No current year data returned from nps tax account, and subsequent nps employment check also empty> - for nino ${user.getNino} @${""}")
                        Some(BadRequest(views.html.error_no_primary()))
                      }
                      case _ => {
                        Logger.info(s"<No current year data returned from nps tax account, but nps employment data is present> - for nino ${user.getNino} @${""}")
                        None
                      }
                    }
                  }
                  case TaiTaxAccountFailureResponse(msg) if msg.toLowerCase.contains(TaiConstants.NpsTaxAccountDataAbsentMsg) => {
                    prevYearEmployments match {
                      case Nil => {
                        Logger.warn(s"<No data returned from nps tax account, and subsequent nps employment check also empty> - for nino ${user.getNino} @${""}")
                        Some(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
                      }
                      case _ => {
                        Logger.warn(s"<No data returned from nps tax account, but nps employment data is present> - for nino ${user.getNino} @${""}")
                        None
                      }
                    }
                  }
                  case TaiTaxAccountFailureResponse(msg) if msg.toLowerCase.contains(TaiConstants.NpsNoEmploymentsRecorded) => {
                    Logger.warn(s"<No data returned from nps employments> - for nino ${user.getNino} @${""}")
                    Some(BadRequest(views.html.error_no_primary()))
                  }
                  case TaiTaxAccountFailureResponse(msg) if msg.toLowerCase.contains(TaiConstants.NpsNoEmploymentForCurrentTaxYear) => {
                    prevYearEmployments match {
                      case Nil => {
                        Logger.warn(s"<No data returned from nps tax account, and subsequent nps employment check also empty> - for nino ${user.getNino} @${""}")
                        Some(BadRequest(views.html.error_no_primary()))
                      }
                      case _ => {
                        Logger.info(s"<No data returned from nps tax account, but nps employment data is present> - for nino ${user.getNino} @${""}")
                        None
                      }
                    }
                  }
                  case _ => None
                }
              }

              possibleRedirectFuture.flatMap(
                _.map(Future.successful(_)).getOrElse( requestedPage.apply(taiRoot) )
              )
          } recoverWith hodStatusRedirect
  }

  def handleWhatDoYouWantToDoPage(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          WhatDoYouWantToDoForm.createForm.bindFromRequest.fold(
            formWithErrors => {
              trackingService.isAnyIFormInProgress(user.getNino) flatMap { trackingResponse =>
                if(cyPlusOneEnabled){
                    taxAccountService.taxAccountSummary(Nino(user.getNino), TaxYear().next) map {
                      case TaiSuccessResponseWithPayload(_) =>
                        BadRequest(views.html.whatDoYouWantToDo(formWithErrors, WhatDoYouWantToDoViewModel(trackingResponse, cyPlusOneEnabled)))
                      case _ =>
                        BadRequest(views.html.whatDoYouWantToDo(formWithErrors, WhatDoYouWantToDoViewModel(trackingResponse, isCyPlusOneEnabled = false)))
                    }
                } else {
                  Future.successful(BadRequest(views.html.whatDoYouWantToDo(formWithErrors, WhatDoYouWantToDoViewModel(trackingResponse, cyPlusOneEnabled))))
                }
              }
            },
            formData => {
              formData.whatDoYouWantToDo match {
                case Some("currentTaxYear") => Future.successful(Redirect(routes.TaxAccountSummaryController.onPageLoad()))
                case Some("lastTaxYear") => Future.successful(Redirect(routes.PayeControllerHistoric.payePage(TaxYear(TaxYearResolver.currentTaxYear-1))))
                case Some("nextTaxYear") => Future.successful(Redirect(routes.IncomeTaxComparisonController.onPageLoad()))
              }
            }
          )
  }

  private def requestedPage(implicit request: Request[AnyContent], user: TaiUser): TaiRoot => Future[Result] = {
    _ => {
      auditService.sendUserEntryAuditEvent(Nino(user.getNino), request.headers.get("Referer").getOrElse("NA"))
      trackingService.isAnyIFormInProgress(user.getNino) flatMap { trackingResponse =>
        if(cyPlusOneEnabled){
          taxAccountService.taxAccountSummary(Nino(user.getNino), TaxYear().next) map {
            case TaiSuccessResponseWithPayload(_) =>
              Ok(views.html.whatDoYouWantToDo(WhatDoYouWantToDoForm.createForm, WhatDoYouWantToDoViewModel(trackingResponse, cyPlusOneEnabled)))
            case _ =>
              Ok(views.html.whatDoYouWantToDo(WhatDoYouWantToDoForm.createForm, WhatDoYouWantToDoViewModel(trackingResponse, isCyPlusOneEnabled = false)))
          }
        } else {
          Future.successful(Ok(views.html.whatDoYouWantToDo(WhatDoYouWantToDoForm.createForm, WhatDoYouWantToDoViewModel(trackingResponse, cyPlusOneEnabled))))
        }
      }
    }
  }

  def hodStatusRedirect(implicit request: Request[AnyContent], user: TaiUser, taiRoot: TaiRoot): PartialFunction[Throwable, Future[Result]] = {

    implicit val rl:RecoveryLocation = classOf[WhatDoYouWantToDoController]

    npsTaxAccountAbsentResult_withEmployCheck(previousYearEmployments, requestedPage) orElse
    npsTaxAccountCYAbsentResult_withEmployCheck(previousYearEmployments, requestedPage) orElse
    npsNoEmploymentForCYResult(previousYearEmployments, requestedPage) orElse
    npsNoEmploymentResult orElse
    npsTaxAccountDeceasedResult orElse
    hodBadRequestResult orElse
    hodInternalErrorResult
  }

  private def sessionData(user: TaiUser)(implicit hc: HeaderCarrier) : Future[SessionData] = {
    val taiAccount = user.authContext.principal.accounts.paye.getOrElse(throw new IllegalArgumentException("Cannot find tai user authority"))
    taiService.taiSession(Nino(user.getNino),TaxYearResolver.currentTaxYear,taiAccount.link)
  }

  private[controllers] def previousYearEmployments(implicit hc: HeaderCarrier): Nino => Future[Seq[Employment]] = {
    nino => employmentService.employments(nino, TaxYear(TaxYearResolver.currentTaxYear-1)) recover {
      case _ => Nil
    }
  }
}

object WhatDoYouWantToDoController extends WhatDoYouWantToDoController with AuthenticationConnectors {
  override val taiService = TaiService
  override val employmentService = EmploymentService
  override val auditService = AuditService
  override val taxAccountService = TaxAccountService

  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: PartialRetriever = TaiHtmlPartialRetriever
  override val trackingService = TrackingService
}
