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

package controllers.income.estimatedPay.update

import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.HoursWorkedForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{EditIncomeIrregularPayConstants, JourneyCacheConstants}
import views.html.incomes.workingHours

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class IncomeUpdateWorkingHoursController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  workingHours: workingHours,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with EditIncomeIrregularPayConstants {

  def workingHoursPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    for {
      incomeSourceEither <- IncomeSource.create(journeyCacheService)
      workingHours       <- journeyCacheService.currentValue(UpdateIncome_WorkingHoursKey)
    } yield {

      incomeSourceEither match {
        case Right(incomeSource) =>
          Ok(
            workingHours(
              HoursWorkedForm.createForm().fill(HoursWorkedForm(workingHours)),
              incomeSource.id,
              incomeSource.name))
        case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      }
    }
  }

  def handleWorkingHours: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    HoursWorkedForm
      .createForm()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          IncomeSource.create(journeyCacheService).map {
            case Right(incomeSource) =>
              BadRequest(workingHours(formWithErrors, incomeSource.id, incomeSource.name))
            case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
          }
        },
        (formData: HoursWorkedForm) => {
          for {
            id <- journeyCacheService.mandatoryJourneyValueAsInt(UpdateIncome_IdKey)
            _  <- journeyCacheService.cache(UpdateIncome_WorkingHoursKey, formData.workingHours.getOrElse(""))
          } yield {

            id match {
              case Right(id) => {
                formData.workingHours match {
                  case Some(REGULAR_HOURS) => Redirect(routes.IncomeUpdatePayPeriodController.payPeriodPage())
                  case Some(IRREGULAR_HOURS) =>
                    Redirect(routes.IncomeUpdateIrregularHoursController.editIncomeIrregularHours(id))
                }
              }
              case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
            }
          }
        }
      )
  }

}
