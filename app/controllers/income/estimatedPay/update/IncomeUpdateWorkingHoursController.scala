/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.income.estimatedPay.update

import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import cats.implicits._
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.HoursWorkedForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{EditIncomeIrregularPayConstants, JourneyCacheConstants}
import views.html.incomes.WorkingHoursView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class IncomeUpdateWorkingHoursController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  workingHoursView: WorkingHoursView,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants {

  def workingHoursPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (IncomeSource.create(journeyCacheService), journeyCacheService.currentValue(UpdateIncome_WorkingHoursKey)).mapN {
      case (incomeSourceEither, workingHours) =>
        incomeSourceEither match {
          case Right(incomeSource) =>
            Ok(
              workingHoursView(
                HoursWorkedForm.createForm().fill(HoursWorkedForm(workingHours)),
                incomeSource.id,
                incomeSource.name))
          case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
        }
    }
  }

  def handleWorkingHours: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    HoursWorkedForm
      .createForm()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          IncomeSource.create(journeyCacheService).map {
            case Right(incomeSource) =>
              BadRequest(workingHoursView(formWithErrors, incomeSource.id, incomeSource.name))
            case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
          }
        },
        (formData: HoursWorkedForm) => {
          for {
            id <- journeyCacheService.mandatoryJourneyValueAsInt(UpdateIncome_IdKey)
            _  <- journeyCacheService.cache(UpdateIncome_WorkingHoursKey, formData.workingHours.getOrElse(""))
          } yield {

            id match {
              case Right(id) =>
                formData.workingHours match {
                  case Some(EditIncomeIrregularPayConstants.RegularHours) =>
                    Redirect(routes.IncomeUpdatePayPeriodController.payPeriodPage())
                  case Some(EditIncomeIrregularPayConstants.IrregularHours) =>
                    Redirect(routes.IncomeUpdateIrregularHoursController.editIncomeIrregularHours(id))
                }
              case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
            }
          }
        }
      )
  }

}
