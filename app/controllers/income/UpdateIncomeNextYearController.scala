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

package controllers.income

import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import javax.inject.Inject
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.UpdateNextYearsIncomeService
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{DuplicateSubmissionCYPlus1EmploymentViewModel, DuplicateSubmissionCYPlus1PensionViewModel, DuplicateSubmissionEstimatedPay}
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, NextYearPay}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UpdateIncomeNextYearController @Inject()(
  updateNextYearsIncomeService: UpdateNextYearsIncomeService,
  val auditConnector: AuditConnector,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  applicationConfig: ApplicationConfig,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with FormValuesConstants with I18nSupport {

  def onPageLoad(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
        updateNextYearsIncomeService.isEstimatedPayJourneyCompleteForEmployer(employmentId).map {
          case true  => Redirect(routes.UpdateIncomeNextYearController.duplicateWarning(employmentId).url)
          case false => Redirect(routes.UpdateIncomeNextYearController.start(employmentId).url)
        }
      }
  }

  def duplicateWarning(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
        implicit val user: AuthedUser = request.taiUser
        val nino = user.nino

        duplicateWarningGet(
          employmentId,
          nino,
          (employmentId: Int, vm: DuplicateSubmissionEstimatedPay) =>
            Ok(
              views.html.incomes.nextYear
                .updateIncomeCYPlus1Warning(DuplicateSubmissionWarningForm.createForm, vm, employmentId))
        )
      }
  }

  private def duplicateWarningGet(
    employmentId: Int,
    nino: Nino,
    resultFunc: (Int, DuplicateSubmissionEstimatedPay) => Result)(implicit hc: HeaderCarrier, messages: Messages) =
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
        Logger.warn(s"[UpdateIncomeNextYearController]: $error")
        Future.successful(Redirect(controllers.routes.IncomeTaxComparisonController.onPageLoad))
    }

  def submitDuplicateWarning(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
        implicit val user = request.taiUser
        val nino = user.nino

        DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
          formWithErrors => {
            duplicateWarningGet(
              employmentId,
              nino,
              (employmentId: Int, vm: DuplicateSubmissionEstimatedPay) =>
                BadRequest(views.html.incomes.nextYear.updateIncomeCYPlus1Warning(formWithErrors, vm, employmentId))
            )
          },
          success => {
            success.yesNoChoice match {
              case Some(YesValue) =>
                Future.successful(Redirect(routes.UpdateIncomeNextYearController.start(employmentId).url))
              case Some(NoValue) =>
                Future.successful(Redirect(controllers.routes.IncomeTaxComparisonController.onPageLoad().url))
            }
          }
        )
      }
  }

  def start(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    preAction {

      implicit val user = request.taiUser
      val nino = user.nino

      updateNextYearsIncomeService.get(employmentId, nino) map { model =>
        Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Start(model.employmentName, employmentId, model.isPension))
      }
    }
  }

  def edit(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    preAction {
      implicit val user = request.taiUser
      val nino = user.nino

      updateNextYearsIncomeService.get(employmentId, nino) map { model =>
        {
          Ok(
            views.html.incomes.nextYear.updateIncomeCYPlus1Edit(
              model.employmentName,
              employmentId,
              model.isPension,
              model.currentValue,
              AmountComparatorForm.createForm()))
        }
      }
    }
  }

  def same(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    preAction {
      implicit val user = request.taiUser
      val nino = user.nino

      updateNextYearsIncomeService.get(employmentId, nino) map { model =>
        Ok(
          views.html.incomes.nextYear
            .updateIncomeCYPlus1Same(model.employmentName, model.employmentId, model.currentValue))
      }
    }
  }

  def success(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    preAction {
      implicit val user = request.taiUser
      val nino = user.nino

      updateNextYearsIncomeService.get(employmentId, nino) map { model =>
        Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Success(model.employmentName, model.isPension))
      }
    }
  }

  def confirm(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    preAction {
      implicit val user = request.taiUser

      updateNextYearsIncomeService.getNewAmount(employmentId).flatMap {
        case Right(newAmount) =>
          updateNextYearsIncomeService
            .get(employmentId, user.nino)
            .map {
              case UpdateNextYearsIncomeCacheModel(employmentName, _, _, currentValue) => {
                val vm =
                  ConfirmAmountEnteredViewModel(employmentId, employmentName, currentValue, newAmount, NextYearPay)
                Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Confirm(vm))
              }
            }
        case Left(error) =>
          Logger.warn("Could not obtain new amount in confirm: " + error)
          Future.successful(Redirect(controllers.routes.IncomeTaxComparisonController.onPageLoad))
      }
    }
  }

  def handleConfirm(employmentId: Int): Action[AnyContent] =
    (authenticate andThen validatePerson).async { implicit request =>
      implicit val user = request.taiUser

      if (applicationConfig.cyPlusOneEnabled) {
        (updateNextYearsIncomeService.submit(employmentId, user.nino) map {
          case TaiSuccessResponse => Redirect(routes.UpdateIncomeNextYearController.success(employmentId))
          case _                  => throw new RuntimeException(s"Not able to update estimated pay for $employmentId")
        }).recover {
          case NonFatal(e) => internalServerError(e.getMessage)
        }
      } else {
        Future.successful(NotFound(error4xxPageWithLink(Messages("global.error.pageNotFound404.title"))))
      }

    }

  def update(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    val nino = user.nino

    preAction {
      updateNextYearsIncomeService.get(employmentId, nino) flatMap { model =>
        AmountComparatorForm
          .createForm()
          .bindFromRequest()
          .fold(
            formWithErrors => {
              Future.successful(
                BadRequest(
                  views.html.incomes.nextYear.updateIncomeCYPlus1Edit(
                    model.employmentName,
                    employmentId,
                    model.isPension,
                    model.currentValue,
                    formWithErrors)))
            },
            validForm => {
              validForm.income.fold(throw new RuntimeException) {
                newIncome =>
                  if (model.currentValue.toString == newIncome)
                    Future.successful(
                      Redirect(controllers.income.routes.UpdateIncomeNextYearController.same(employmentId)))
                  else {
                    updateNextYearsIncomeService.getNewAmount(employmentId) flatMap {
                      case Right(newAmount) if (newAmount == newIncome.toInt) =>
                        val samePayViewModel = SameEstimatedPayViewModel(
                          model.employmentName,
                          model.employmentId,
                          newAmount,
                          model.isPension,
                          controllers.routes.IncomeTaxComparisonController.onPageLoad.url)

                        Future.successful(Ok(views.html.incomes.sameEstimatedPay(samePayViewModel)))
                      case _ =>
                        updateNextYearsIncomeService.setNewAmount(newIncome, employmentId, nino) map { _ =>
                          Redirect(controllers.income.routes.UpdateIncomeNextYearController.confirm(employmentId))
                        }
                    }
                  }
              }
            }
          )
      }
    }
  }

  private def preAction(action: => Future[Result])(implicit request: Request[AnyContent]): Future[Result] =
    if (applicationConfig.cyPlusOneEnabled) {
      action
    } else {
      Future.successful(NotFound(error4xxPageWithLink(Messages("global.error.pageNotFound404.title"))))
    }
}
