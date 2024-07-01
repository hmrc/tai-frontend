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

package controllers.income

import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.admin.CyPlusOneToggle
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.UpdateNextYearsIncomeService
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update._
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, NextYearPay}
import views.html.incomes.SameEstimatedPayView
import views.html.incomes.nextYear._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UpdateIncomeNextYearController @Inject() (
  updateNextYearsIncomeService: UpdateNextYearsIncomeService,
  val auditConnector: AuditConnector,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  updateIncomeCYPlus1Success: UpdateIncomeCYPlus1SuccessView,
  updateIncomeCYPlus1Confirm: UpdateIncomeCYPlus1ConfirmView,
  updateIncomeCYPlus1Warning: UpdateIncomeCYPlus1WarningView,
  updateIncomeCYPlus1Start: UpdateIncomeCYPlus1StartView,
  updateIncomeCYPlus1Edit: UpdateIncomeCYPlus1EditView,
  updateIncomeCYPlus1Same: UpdateIncomeCYPlus1SameView,
  sameEstimatedPay: SameEstimatedPayView,
  featureFlagService: FeatureFlagService,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with I18nSupport with Logging {

  def onPageLoad(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      preAction {
        updateNextYearsIncomeService.isEstimatedPayJourneyCompleteForEmployer(employmentId).map {
          case true  => Redirect(routes.UpdateIncomeNextYearController.duplicateWarning(employmentId).url)
          case false => Redirect(routes.UpdateIncomeNextYearController.start(employmentId).url)
        }
      }
  }

  def duplicateWarning(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      preAction {
        implicit val user: AuthedUser = request.taiUser
        val nino = user.nino

        duplicateWarningGet(
          employmentId,
          nino,
          (employmentId: Int, vm: DuplicateSubmissionEstimatedPay) =>
            Ok(updateIncomeCYPlus1Warning(DuplicateSubmissionWarningForm.createForm, vm, employmentId))
        )
      }
  }

  private def duplicateWarningGet(
    employmentId: Int,
    nino: Nino,
    resultFunc: (Int, DuplicateSubmissionEstimatedPay) => Result
  )(implicit hc: HeaderCarrier, messages: Messages) =
    updateNextYearsIncomeService.getNewAmount(employmentId).flatMap {
      case Right(newAmount) =>
        updateNextYearsIncomeService.get(employmentId, nino) map { model =>
          val vm = if (model.isPension) {
            DuplicateSubmissionCYPlus1PensionViewModel(model.employmentName, newAmount)
          } else {
            DuplicateSubmissionCYPlus1EmploymentViewModel(model.employmentName, newAmount)
          }

          resultFunc(employmentId, vm)
        }
      case Left(error) =>
        logger.warn(s"[UpdateIncomeNextYearController]: $error")
        Future.successful(Redirect(controllers.routes.IncomeTaxComparisonController.onPageLoad()))
    }

  def submitDuplicateWarning(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      preAction {
        implicit val user: AuthedUser = request.taiUser
        val nino = user.nino

        DuplicateSubmissionWarningForm.createForm
          .bindFromRequest()
          .fold(
            formWithErrors =>
              duplicateWarningGet(
                employmentId,
                nino,
                (employmentId: Int, vm: DuplicateSubmissionEstimatedPay) =>
                  BadRequest(updateIncomeCYPlus1Warning(formWithErrors, vm, employmentId))
              ),
            success =>
              success.yesNoChoice match {
                case Some(FormValuesConstants.YesValue) =>
                  Future.successful(Redirect(routes.UpdateIncomeNextYearController.start(employmentId).url))
                case Some(FormValuesConstants.NoValue) =>
                  Future.successful(Redirect(controllers.routes.IncomeTaxComparisonController.onPageLoad().url))
              }
          )
      }
  }

  def start(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    preAction {

      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      updateNextYearsIncomeService.get(employmentId, nino) map { model =>
        Ok(updateIncomeCYPlus1Start(model.employmentName, employmentId, model.isPension))
      }
    }
  }

  def edit(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    preAction {
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      updateNextYearsIncomeService.get(employmentId, nino) map { model =>
        Ok(
          updateIncomeCYPlus1Edit(
            model.employmentName,
            employmentId,
            model.isPension,
            model.currentValue,
            AmountComparatorForm.createForm()
          )
        )
      }
    }
  }

  def same(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    preAction {
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      updateNextYearsIncomeService.get(employmentId, nino) map { model =>
        Ok(updateIncomeCYPlus1Same(model.employmentName, model.currentValue))
      }
    }
  }

  def success(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    preAction {
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      updateNextYearsIncomeService.get(employmentId, nino) map { model =>
        Ok(updateIncomeCYPlus1Success(model.employmentName, model.isPension))
      }
    }
  }

  def confirm(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    preAction {
      implicit val user: AuthedUser = request.taiUser

      updateNextYearsIncomeService.getNewAmount(employmentId).flatMap {
        case Right(newAmount) =>
          updateNextYearsIncomeService
            .get(employmentId, user.nino)
            .map { case UpdateNextYearsIncomeCacheModel(employmentName, _, _, currentValue) =>
              val vm =
                ConfirmAmountEnteredViewModel(
                  employmentId = employmentId,
                  empName = employmentName,
                  currentAmount = currentValue,
                  estIncome = newAmount,
                  payType = NextYearPay,
                  backUrl = "#"
                )
              Ok(updateIncomeCYPlus1Confirm(vm))
            }
        case Left(error) =>
          logger.warn("Could not obtain new amount in confirm: " + error)
          Future.successful(Redirect(controllers.routes.IncomeTaxComparisonController.onPageLoad()))
      }
    }
  }

  def handleConfirm(employmentId: Int): Action[AnyContent] =
    authenticate.authWithValidatePerson.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      featureFlagService.get(CyPlusOneToggle).flatMap { toggle =>
        if (toggle.isEnabled) {
          updateNextYearsIncomeService
            .submit(employmentId, user.nino)
            .map(_ => Redirect(routes.UpdateIncomeNextYearController.success(employmentId)))
            .recover { case NonFatal(e) =>
              errorPagesHandler.internalServerError(e.getMessage)
            }
        } else {
          Future.successful(
            NotFound(errorPagesHandler.error4xxPageWithLink(Messages("global.error.pageNotFound404.title")))
          )
        }
      }
    }

  def update(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val nino = user.nino

    preAction {
      updateNextYearsIncomeService.get(employmentId, nino) flatMap { model =>
        AmountComparatorForm
          .createForm()
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  updateIncomeCYPlus1Edit(
                    model.employmentName,
                    employmentId,
                    model.isPension,
                    model.currentValue,
                    formWithErrors
                  )
                )
              ),
            validForm =>
              validForm.income.fold(throw new RuntimeException) { newIncome =>
                if (model.currentValue.toString == newIncome) {
                  Future
                    .successful(Redirect(controllers.income.routes.UpdateIncomeNextYearController.same(employmentId)))
                } else {
                  updateNextYearsIncomeService.getNewAmount(employmentId) flatMap {
                    case Right(newAmount) if newAmount == newIncome.toInt =>
                      val samePayViewModel = SameEstimatedPayViewModel(
                        model.employmentName,
                        model.employmentId,
                        newAmount,
                        model.isPension,
                        controllers.routes.IncomeTaxComparisonController.onPageLoad().url
                      )

                      Future.successful(Ok(sameEstimatedPay(samePayViewModel)))
                    case _ =>
                      updateNextYearsIncomeService.setNewAmount(newIncome, employmentId) map { _ =>
                        Redirect(controllers.income.routes.UpdateIncomeNextYearController.confirm(employmentId))
                      }
                  }
                }
              }
          )
      }
    }
  }

  private def preAction(action: => Future[Result])(implicit request: Request[AnyContent]): Future[Result] =
    featureFlagService.get(CyPlusOneToggle).flatMap { toggle =>
      if (toggle.isEnabled) {
        action
      } else {
        Future.successful(
          NotFound(errorPagesHandler.error4xxPageWithLink(Messages("global.error.pageNotFound404.title")))
        )
      }
    }
}
