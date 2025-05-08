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
import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.income._
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service.{EmploymentService, IncomeService, TaxAccountService}
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants.MonthAndYear
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, EditIncomeIrregularHoursViewModel, IrregularPay}
import views.html.incomes.{ConfirmAmountEnteredView, EditIncomeIrregularHoursView, EditSuccessView}

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeUpdateIrregularHoursController @Inject() (
  authenticate: AuthJourney,
  incomeService: IncomeService,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  mcc: MessagesControllerComponents,
  editSuccess: EditSuccessView,
  editIncomeIrregularHours: EditIncomeIrregularHoursView,
  confirmAmountEntered: ConfirmAmountEnteredView,
  journeyCacheRepository: JourneyCacheRepository,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  private val logger = Logger(this.getClass)

  def editIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      (
        incomeService.latestPayment(nino, employmentId),
        employmentService.employment(nino, employmentId)
      ).mapN {
        case (_, None) =>
          logger.warn(s"Employment not found for IDs: $employmentId")
          Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))

        case (maybePayment, Some(employment)) =>
          val estimatedPay = employment.latestAnnualAccount
            .flatMap(_.latestPayment)
            .map(_.amount)
            .getOrElse(BigDecimal(0))// Check with Pascal is it editing the latest Payment
          val cacheMap = Map(
            UpdateIncomeNamePage.toString      -> employment.name,
            UpdateIncomePayToDatePage.toString -> estimatedPay.toString
          ) ++ maybePayment
            .map(p => UpdatedIncomeDatePage.toString -> p.date.format(DateTimeFormatter.ofPattern(MonthAndYear)))
            .toMap

          val updatedAnswers = request.userAnswers.copy(
            data = request.userAnswers.data ++ Json.toJson(cacheMap).as[JsObject]
          )

          journeyCacheRepository.set(updatedAnswers).map { _ =>
            val viewModel = EditIncomeIrregularHoursViewModel(employmentId, employment.name, estimatedPay)
            Ok(editIncomeIrregularHours(AmountComparatorForm.createForm(), viewModel))
          }

        case _ =>
          Future.successful(errorPagesHandler.internalServerError("Failed to find tax code income for employment"))
      }.flatten
  }

  def confirmIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      val userAnswers = request.userAnswers

      val name = userAnswers.get(UpdateIncomeNamePage)
      val newIrregularPay = userAnswers.get(UpdateIncomeIrregularAnnualPayPage)
      val paymentToDate = userAnswers.get(UpdateIncomePayToDatePage)
      val confirmedNewAmount = userAnswers.get(UpdateIncomeConfirmedNewAmountPage(employmentId))

      (name, newIrregularPay, paymentToDate) match {
        case (Some(name), Some(newIrregularPay), Some(paymentToDate)) =>
          if (FormHelper.areEqual(confirmedNewAmount, Some(newIrregularPay))) {
            Future.successful(Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache(employmentId)))
          } else if (FormHelper.areEqual(Some(paymentToDate), Some(newIrregularPay))) {
            Future.successful(Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay()))
          } else {
            val vm = ConfirmAmountEnteredViewModel(
              employmentId,
              name,
              paymentToDate.toInt,
              newIrregularPay.toInt,
              IrregularPay,
              controllers.income.estimatedPay.update.routes.IncomeUpdateIrregularHoursController
                .editIncomeIrregularHours(employmentId)
                .url
            )
            Future.successful(Ok(confirmAmountEntered(vm)))
          }
        case _ =>
          logger.warn("Required values not found in user answers")
          Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId)))
      }

  }

  def handleIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      val userAnswers: UserAnswers = request.userAnswers
      val name: String = userAnswers.get(UpdateIncomeNamePage).toString
      val paymentToDate: String = userAnswers.get(UpdateIncomePayToDatePage).getOrElse("")
      val latestPayDate: Option[String] = userAnswers.get(UpdatedIncomeDatePage)

      AmountComparatorForm
        .createForm(latestPayDate, Some(paymentToDate.toInt))
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel = EditIncomeIrregularHoursViewModel(employmentId, name, paymentToDate)
            Future.successful(BadRequest(editIncomeIrregularHours(formWithErrors, viewModel)))
          },
          validForm =>
            validForm.income.fold(throw new RuntimeException) { income =>
              journeyCacheRepository.set(userAnswers.set(UpdateIncomeIrregularAnnualPayPage, income).get) map { _ =>
                Redirect(routes.IncomeUpdateIrregularHoursController.confirmIncomeIrregularHours(employmentId))
              }
            }
        )
  }

  // Check with Pascal as there is no estimated payment update API in employment. There is update for previous years
  def submitIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      val cacheAndRespond = (incomeName: String, incomeId: Int, newPay: String) => {

        val updatedUserAnswers = request.userAnswers
          .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(employmentId), true)
          .setOrException(UpdateIncomeConfirmedNewAmountPage(employmentId), newPay)

        journeyCacheRepository
          .set(updatedUserAnswers)
          .map { _ =>
            Ok(editSuccess(incomeName, incomeId))
          }
      }
      val userAnswers = request.userAnswers
      val incomeNameOpt = userAnswers.get(UpdateIncomeNamePage)
      val newPayOpt = userAnswers.get(UpdateIncomeIrregularAnnualPayPage)
      val incomeIdOpt = userAnswers.get(UpdateIncomeIdPage)

      (incomeNameOpt, newPayOpt, incomeIdOpt) match {
        case (Some(incomeName), Some(newPay), Some(incomeId)) =>
          (for {
            _      <- taxAccountService.updateEstimatedIncome(nino, newPay.toInt, TaxYear(), employmentId) //To Check with Pascal
            result <- cacheAndRespond(incomeName, incomeId, newPay)
          } yield result).recover { case NonFatal(e) =>
            errorPagesHandler.internalServerError(e.getMessage)
          }
        case _ =>
          logger.warn("Mandatory values missing from user answers")
          Future.successful(errorPagesHandler.internalServerError("Mandatory values missing from user answers"))
      }
  }

}
