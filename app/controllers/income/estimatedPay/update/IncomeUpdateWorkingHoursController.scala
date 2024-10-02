/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.implicits._
import controllers.TaiBaseController
import controllers.auth.{AuthJourney, AuthedUser}
import pages.income.{UpdateIncomeIdPage, UpdateIncomeWorkingHoursPage}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.forms.income.incomeCalculator.HoursWorkedForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.EditIncomeIrregularPayConstants
import views.html.incomes.WorkingHoursView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeUpdateWorkingHoursController @Inject() (
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  workingHoursView: WorkingHoursView,
  journeyCacheNewRepository: JourneyCacheNewRepository
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def workingHoursPage: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    println("\n ====== INSIDE IncomeUpdateWorkingHoursController.workingHoursPage (GET) -------")
    val userAnswers = request.userAnswers
    println("\n ====== USERanswers in workingHoursPage :  " + userAnswers)
    val workingHours = userAnswers.get(UpdateIncomeWorkingHoursPage)
    println("\n ====== workingHours :  " + workingHours)

    (IncomeSource.create(journeyCacheNewRepository, userAnswers), Future.successful(workingHours)).mapN {
      case (Right(incomeSource), hours) =>
        println("\n--------------- INSIDE OK ------------- ")
        println("\n--------------- incomeSource  ------------- " + incomeSource)
        println("\n--------------- workingHours  ------------- " + hours)
        Ok(
          workingHoursView(
            HoursWorkedForm.createForm().fill(HoursWorkedForm(hours)),
            incomeSource.id,
            incomeSource.name
          )
        )
      case _ =>
        println("\n--------- GOING DEFAULT onPageLoad")
        Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handleWorkingHours: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    println("\n ====== INSIDE IncomeUpdateWorkingHoursController.handleWorkingHours (POST) -------")
    HoursWorkedForm
      .createForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          IncomeSource.create(journeyCacheNewRepository, request.userAnswers).map {
            case Right(incomeSource) =>
              BadRequest(workingHoursView(formWithErrors, incomeSource.id, incomeSource.name))
            case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
          },
        (formData: HoursWorkedForm) =>
          for {
            id <- Future.successful(request.userAnswers.get(UpdateIncomeIdPage))
            _ <- {
              println("\n ----- SETTING UpdateIncomeWorkingHoursPage to UserANSWERS ")
              val updatedAnswers = request.userAnswers.copy(
                data = request.userAnswers.data ++ Json
                  .obj(UpdateIncomeWorkingHoursPage.toString -> JsString(formData.workingHours.getOrElse("")))
              )
              journeyCacheNewRepository.set(updatedAnswers)
            }
          } yield id match {
            case Some(id) =>
              formData.workingHours match {
                case Some(EditIncomeIrregularPayConstants.RegularHours) =>
                  Redirect(routes.IncomeUpdatePayPeriodController.payPeriodPage())
                case Some(EditIncomeIrregularPayConstants.IrregularHours) =>
                  Redirect(routes.IncomeUpdateIrregularHoursController.editIncomeIrregularHours(id))
              }
            case None => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
          }
      )
  }

}
