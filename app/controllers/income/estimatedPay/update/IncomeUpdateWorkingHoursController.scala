/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Named}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.tai.forms.HoursWorkedForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{EditIncomeIrregularPayConstants, JourneyCacheConstants}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class IncomeUpdateWorkingHoursController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)
    extends TaiBaseController with JourneyCacheConstants with EditIncomeIrregularPayConstants {

  def workingHoursPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    val employerFuture = IncomeSource.create(journeyCacheService)
    for {
      employer     <- employerFuture
      workingHours <- journeyCacheService.currentValue(UpdateIncome_WorkingHoursKey)
    } yield {
      Ok(
        views.html.incomes
          .workingHours(HoursWorkedForm.createForm().fill(HoursWorkedForm(workingHours)), employer.id, employer.name))
    }
  }

  def handleWorkingHours: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    HoursWorkedForm
      .createForm()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val employerFuture = IncomeSource.create(journeyCacheService)
          for {
            employer <- employerFuture
          } yield {
            BadRequest(views.html.incomes.workingHours(formWithErrors, employer.id, employer.name))
          }
        },
        (formData: HoursWorkedForm) => {
          for {
            id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
            _  <- journeyCacheService.cache(UpdateIncome_WorkingHoursKey, formData.workingHours.getOrElse(""))
          } yield {
            formData.workingHours match {
              case Some(REGULAR_HOURS) => Redirect(routes.IncomeUpdatePayPeriodController.payPeriodPage())
              case Some(IRREGULAR_HOURS) =>
                Redirect(routes.IncomeUpdateIrregularHoursController.editIncomeIrregularHours(id))
            }
          }
        }
      )
  }

}
