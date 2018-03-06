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

import controllers.ServiceChecks.CustomRule
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTai, WithAuthorisedForTaiLite}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.{EmploymentAmount, IncomeCalculation, SessionData}
import uk.gov.hmrc.tai.service.TaiService.IncomeIDPage
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.{FormHelper, JourneyCacheConstants, TaxSummaryHelper}
import views.html.incomes.howToUpdate

import scala.concurrent.Future

trait IncomeUpdateCalculatorNewController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants {

  def taiService: TaiService

  def journeyCacheService: JourneyCacheService

  def employmentService: EmploymentService

  def activityLoggerService: ActivityLoggerService

  val incomeService: IncomeService

  def handleChooseHowToUpdate: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processChooseHowToUpdate")
        HowToUpdateForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.howToUpdate(formWithErrors, id, Some(employerName)))
            }
          },
          formData => {
            formData.howToUpdate match {
              case Some("incomeCalculator") => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.workingHoursPage()))
              case _ => Future.successful(Redirect(routes.IncomeController.viewIncomeForEdit()))
            }
          }
        )
  }

  def workingHoursPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getWorkingHours")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.workingHours(HoursWorkedForm.createForm(), id, Some(employerName)))
        }
  }

  def handleWorkingHours: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processWorkedHours")
        HoursWorkedForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.workingHours(formWithErrors, id, Some(employerName)))
            }
          },
          formData => {
            formData.workingHours match {
              case Some("same") => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.payPeriodPage()))
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.calcUnavailablePage()))
            }
          }
        )
  }

  def payPeriodPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getPayPeriodPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.payPeriod(PayPeriodForm.createForm(None), id, employerName = Some(employerName)))
        }
  }

  def handlePayPeriod: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processPayPeriod")
        val payPeriod: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("payPeriod").flatMap(_.headOption))

        PayPeriodForm.createForm(None, payPeriod).bindFromRequest().fold(
          formWithErrors => {
            val isNotDaysError = formWithErrors.errors.filter { error => error.key == "otherInDays" }.isEmpty
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.payPeriod(formWithErrors, id, isNotDaysError, Some(employerName)))
            }
          },
          formData => {
            journeyCacheService.cache(incomeService.cachePayPeriod(formData)).map { _ =>
              Redirect(routes.IncomeUpdateCalculatorController.payslipAmountPage())
            }
          }
        )
  }

  def payslipAmountPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getPayslipAmountPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          payPeriod <- journeyCacheService.mandatoryValue(UpdateIncome_PayPeriod)
        } yield {
          Ok(views.html.incomes.payslipAmount(PayslipForm.createForm(), payPeriod, id, Some(employerName)))
        }
  }

  def calcUnavailablePage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.calcUnavailable(id, Some(employerName)))
        }
  }
}

object IncomeUpdateCalculatorNewController extends IncomeUpdateCalculatorNewController with AuthenticationConnectors {
  override val taiService: TaiService = TaiService
  override val activityLoggerService: ActivityLoggerService = ActivityLoggerService
  override val journeyCacheService = JourneyCacheService(UpdateIncome_JourneyKey)
  override val employmentService: EmploymentService = EmploymentService
  override val incomeService: IncomeService = IncomeService

  override implicit def templateRenderer: TemplateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever: PartialRetriever = TaiHtmlPartialRetriever

}
