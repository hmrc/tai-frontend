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
import controllers.viewModels.YourIncomeCalculationPageVM
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.SessionData
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.{ActivityLoggerService, EmploymentService, TaiService}
import uk.gov.hmrc.tai.viewModels.HistoricIncomeCalculationViewModel

import scala.concurrent.Future

trait YourIncomeCalculationController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTai
  with Auditable
  with FeatureTogglesConfig {

  def taiService: TaiService

  def activityLoggerService: ActivityLoggerService

  def employmentService: EmploymentService

  def yourIncomeCalculationPage(empId: Option[Int] = None): Action[AnyContent] = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user =>
      implicit sessionData =>
        implicit request =>
          if(cyApdNewPageEnabled){
            empId match {
              case Some(id) => Future.successful(Redirect(routes.YourIncomeCalculationControllerNew.yourIncomeCalculationPage(id)))
              case _ => throw new RuntimeException("Employment id not present")
            }
          } else{
            showIncomeCalculationPageForCurrentYear(nino = Nino(user.getNino), empId = empId)
          }
  }

  def printYourIncomeCalculationPage(empId: Option[Int] = None): Action[AnyContent] = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user =>
      implicit sessionData =>
        implicit request =>
          if(cyApdNewPageEnabled){
            empId match {
              case Some(id) => Future.successful(Redirect(routes.YourIncomeCalculationControllerNew.printYourIncomeCalculationPage(empId.getOrElse(-1))))
              case _ => throw new RuntimeException("Employment id not present")
            }
          }else {
            showIncomeCalculationPageForCurrentYear(nino = Nino(user.getNino), empId = empId, printPage = true)
          }
  }

  def showIncomeCalculationPageForCurrentYear(nino: Nino, empId: Option[Int], printPage: Boolean = false)
                                             (implicit request: Request[AnyContent], user: TaiUser,
                                sessionData: SessionData): Future[Result] = {
    val sessionEmpId = sessionData.editIncomeForm.map(_.employmentId).getOrElse(-1)

    val rule: CustomRule = details => {
      val fModel = IncomeViewModelFactory.create(YourIncomeCalculationPageVM, nino, details, empId.getOrElse(sessionEmpId))
      val sessionDataMod = sessionData.copy(editIncomeForm = Some(EditIncomeForm("", "", employmentId = empId.getOrElse(sessionEmpId))))
      taiService.updateTaiSession(sessionDataMod)

      Future.successful {
        if (printPage) {
          Ok(views.html.print.yourIncomeCalculation(fModel, empId.getOrElse(sessionEmpId)))
        } else {
          Ok(views.html.incomes.yourIncomeCalculation(fModel, empId.getOrElse(sessionEmpId)))
        }
      }

    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData, true) {
      Some(rule)
    }

  } recoverWith handleErrorResponse("showIncomeCalculationPage", nino)

  def yourIncomeCalculationPreviousYearPage(empId: Int): Action[AnyContent] = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user =>
      implicit sessionData =>
        implicit request =>
          showHistoricIncomeCalculation(Nino(user.getNino), empId)
  }

  def printYourIncomeCalculationPreviousYearPage(empId: Int): Action[AnyContent] = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user =>
      implicit sessionData =>
        implicit request =>
          showHistoricIncomeCalculation(Nino(user.getNino), empId, printPage = true)
  }


  def yourIncomeCalculation(year: TaxYear, empId: Int): Action[AnyContent] = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user =>
      implicit sessionData =>
        implicit request => {
          if(year == TaxYear()){
            showIncomeCalculationPageForCurrentYear(nino = Nino(user.getNino), empId = Some(empId))
          }else if(year <= TaxYear().prev){
            showHistoricIncomeCalculation(Nino(user.getNino), empId, year = year)
          } else{
            Future.failed(new BadRequestException(s"Doesn't support year $year"))
          }
        }
  }

  def printYourIncomeCalculation(year: TaxYear, empId: Int): Action[AnyContent] = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user =>
      implicit sessionData =>
        implicit request => {
          if(year == TaxYear()){
            showIncomeCalculationPageForCurrentYear(nino = Nino(user.getNino), printPage = true, empId = Some(empId))
          }else if(year <= TaxYear().prev){
            showHistoricIncomeCalculation(Nino(user.getNino), empId, printPage = true, year = year)
          } else{
            Future.failed(new BadRequestException(s"Doesn't support year $year"))
          }
        }
  }

  def showHistoricIncomeCalculation(nino: Nino, empId: Int, printPage: Boolean = false, year: TaxYear = TaxYear().prev)(implicit request: Request[AnyContent],
                                                                                                                        user: TaiUser,
                                                                                                                        sessionData: SessionData): Future[Result] = {
    val rule: CustomRule = _ => {
      for {
        employment <- employmentService.employments(nino, year)
      } yield {
        val historicIncomeCalculationViewModel = HistoricIncomeCalculationViewModel(employment, empId, year)
        if (printPage) {
          Ok(views.html.print.historicIncomeCalculation(historicIncomeCalculationViewModel))
        } else {
          Ok(views.html.incomes.historicIncomeCalculation(historicIncomeCalculationViewModel))
        }
      }
    }

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData, true) {
      Some(rule)
    }
  } recoverWith handleErrorResponse("showIncomeCalculationPreviousYearPage", nino)
}

object YourIncomeCalculationController extends YourIncomeCalculationController with AuthenticationConnectors {
  override val taiService = TaiService
  override val activityLoggerService = ActivityLoggerService
  override val employmentService = EmploymentService

  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

}
