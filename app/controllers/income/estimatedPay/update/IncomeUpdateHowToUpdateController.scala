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

import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.income.{UpdateIncomeIdPage, UpdateIncomeNamePage, UpdateIncomeTypePage, UpdateIncomeUpdateKeyPage}
import play.api.libs.json.Json
import play.api.mvc._
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.income.incomeCalculator.HowToUpdateForm
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.model.{EmploymentAmount, UserAnswers}
import uk.gov.hmrc.tai.service.{EmploymentService, IncomeService}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import views.html.incomes.HowToUpdateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeUpdateHowToUpdateController @Inject() (
  authenticate: AuthJourney,
  employmentService: EmploymentService,
  incomeService: IncomeService,
  mcc: MessagesControllerComponents,
  howToUpdateView: HowToUpdateView,
  journeyCacheRepository: JourneyCacheRepository,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  private def incomeTypeIdentifier(isPension: Boolean): String =
    if (isPension) {
      TaiConstants.IncomeTypePension
    } else {
      TaiConstants.IncomeTypeEmployment
    }

  private def cacheEmploymentDetails(
    id: Int,
    employmentFuture: Future[Option[Employment]],
    userAnswers: UserAnswers
  ): Future[UserAnswers] =
    employmentFuture flatMap {
      case Some(employment) =>
        val incomeType         = incomeTypeIdentifier(employment.receivingOccupationalPension)
        val updatedUserAnswers = userAnswers
          .setOrException(UpdateIncomeNamePage, employment.name)
          .setOrException(UpdateIncomeIdPage, id)
          .setOrException(UpdateIncomeTypePage, incomeType)

        journeyCacheRepository.set(updatedUserAnswers).map(_ => updatedUserAnswers)

      case _ => throw new RuntimeException("Not able to find employment")
    }

  def howToUpdatePage(id: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino                      = user.nino

    employmentService.employmentOnly(nino, id).flatMap {
      case Some(employment) =>
        val incomeToEditFuture           = incomeService.employmentAmount(nino, id)
        val cacheEmploymentDetailsFuture =
          cacheEmploymentDetails(id, Future.successful(Some(employment)), request.userAnswers)

        for {
          incomeToEdit <- incomeToEditFuture
          _            <- cacheEmploymentDetailsFuture
          result       <- processHowToUpdatePage(id, employment.name, incomeToEdit, request.userAnswers)
        } yield result

      case None =>
        Future.failed(new RuntimeException("Not able to find employment"))
    } recover { case NonFatal(e) =>
      errorPagesHandler.internalServerError(e.getMessage)
    }
  }

  def processHowToUpdatePage(
    id: Int,
    employmentName: String,
    incomeToEdit: EmploymentAmount,
    userAnswers: UserAnswers
  )(implicit request: Request[AnyContent], user: AuthedUser): Future[Result] =
    (incomeToEdit.isLive, incomeToEdit.isOccupationalPension) match {
      case (true, false) =>
        val howToUpdate = userAnswers.get(UpdateIncomeUpdateKeyPage)
        val form        = HowToUpdateForm.createForm().fill(HowToUpdateForm(howToUpdate))
        Future.successful(Ok(howToUpdateView(form, id, employmentName)))

      case (false, false) =>
        Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))

      case _ =>
        Future.successful(Redirect(controllers.routes.IncomeController.pensionIncome(id)))
    }

  def handleChooseHowToUpdate: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    HowToUpdateForm
      .createForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          for {
            incomeSourceEither <- IncomeSource.create(journeyCacheRepository, request.userAnswers)
          } yield incomeSourceEither match {
            case Right(incomeSource) =>
              BadRequest(howToUpdateView(formWithErrors, incomeSource.id, incomeSource.name))
            case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
          },
        formData => {
          val updatedAnswers = request.userAnswers.copy(
            data = request.userAnswers.data ++ Json
              .obj(UpdateIncomeUpdateKeyPage.toString -> formData.howToUpdate)
          )

          journeyCacheRepository.set(updatedAnswers).map { _ =>
            formData.howToUpdate match {
              case Some("incomeCalculator") =>
                Redirect(routes.IncomeUpdateWorkingHoursController.workingHoursPage())
              case _                        => Redirect(controllers.routes.IncomeController.viewIncomeForEdit())
            }
          }
        }
      )
  }

}
