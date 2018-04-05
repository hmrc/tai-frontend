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

import controllers.ServiceChecks._
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTai}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.SessionData
import uk.gov.hmrc.tai.service.TaiService.IncomeIDPage
import uk.gov.hmrc.tai.service.{ActivityLoggerService, TaiService}
import uk.gov.hmrc.tai.util.TaxSummaryHelper
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future


trait IncomeController extends TaiBaseController
with DelegationAwareActions
with WithAuthorisedForTai
with Auditable {

  def taiService: TaiService
  def activityLoggerService: ActivityLoggerService


  def updateIncomes() = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user => implicit sessionData => implicit request =>
      updateIncomesForNino(Nino(user.getNino.toString))
  }

  def updateIncomesForNino(nino: Nino)(implicit request: Request[AnyContent], user: TaiUser, sessionData: SessionData): Future[Result] = {

    val rule: CustomRule = details => {
      sendActingAttorneyAuditEvent("updateIncomesForNino")
      if (user.authContext.isDelegating) {
        activityLoggerService.updateIncome(nino)
      }

      EditIncomeForm.bind(request).fold(
        formWithErrors => {
          val webChat = true
          Future.successful(
            BadRequest(
              views.html.incomes.editIncome(
                formWithErrors,
                TaxSummaryHelper.hasMultipleIncomes(details),
                formWithErrors("employmentId").value.getOrElse("0").toInt,
                webChat = webChat
              )
            )
          )
        },
        incomes => {
          taiService.updateIncome(nino, TaxYearResolver.currentTaxYear,details.version, incomes.toEmploymentAmount) map {
            response =>
              Ok(views.html.incomes.editSuccess(response, incomes, TaxSummaryHelper.hasMultipleIncomes(details), Some(incomes.name)))
          }
        }
      )
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("updateIncomesForNino", nino)


  def viewIncomeForEdit() = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user => implicit sessionData => implicit request =>
      val page:IncomeIDPage = (id, name) => getIncomeForEdit(Nino(user.taiRoot.nino), id)
      moveToPageWithIncomeID(page)
  }
  def getIncomeForEdit(nino: Nino, incomeId: Int)(implicit request: Request[AnyContent], user: TaiUser, sessionData : SessionData): Future[Result] = {


    val rule: CustomRule = details => {
      val incomeToEdit = taiService.incomeForEdit(details, incomeId)
      incomeToEdit match {
        case Some(employmentAmount) => {
          (employmentAmount.isLive, employmentAmount.isOccupationalPension) match {
            //ToDo - Put Calculator back in
            //case (true, false) => Ok(views.html.incomes.incomeCalculator(CalculateIncomeForm.create(details)))
            case (true, false) => Future.successful(Redirect(routes.IncomeControllerNew.regularIncome()))
            case (false, false) => Future.successful(Redirect(routes.TaxAccountSummaryController.onPageLoad()))
            case _ => Future.successful(Redirect(routes.IncomeControllerNew.pensionIncome()))
          }
        }
        case _ => Future.successful(BadRequest(views.html.error_template(Messages("tai.technical.error.title"), Messages("tai.technical.error.heading"),
          Messages("tai.technical.error.message"))))
      }

    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("getIncomes", nino)


  def moveToPageWithIncomeID(page:IncomeIDPage )(implicit user: TaiUser,request: Request[AnyContent], sessionData : SessionData): Future[Result] =  {
    sessionData.editIncomeForm.map(x => (x.employmentId, x.name)).map { income =>
      page(income._1, income._2)
    }
  }.getOrElse(Future.successful(BadRequest(views.html.error_template(Messages("tai.technical.error.title"), Messages("tai.technical.error.heading"),
    Messages("tai.technical.error.message")))))

}


object IncomeController extends IncomeController with AuthenticationConnectors {
  override val taiService = TaiService
  override val activityLoggerService = ActivityLoggerService
  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}
