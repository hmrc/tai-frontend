/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.audit.Auditable
import controllers.auth.{AuthAction, AuthedUser}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.UpdateNextYearsIncomeService
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{DuplicateSubmissionCYPlus1EmploymentViewModel, DuplicateSubmissionCYPlus1PensionViewModel, DuplicateSubmissionEstimatedPay}

import scala.concurrent.Future
import scala.util.control.NonFatal

class UpdateIncomeNextYearController @Inject()(updateNextYearsIncomeService: UpdateNextYearsIncomeService,
                                               val auditConnector: AuditConnector,
                                               authenticate: AuthAction,
                                               validatePerson: ValidatePerson,
                                               override implicit val partialRetriever: FormPartialRetriever,
                                               override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with FeatureTogglesConfig
  with FormValuesConstants
  with Auditable {


  def onPageLoad(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
          updateNextYearsIncomeService.isEstimatedPayJourneyComplete map { isEstimatedPayJourneyComplete =>
            if (isEstimatedPayJourneyComplete) {
              Redirect(routes.UpdateIncomeNextYearController.duplicateWarning(employmentId).url)
            }
            else {
              Redirect(routes.UpdateIncomeNextYearController.start(employmentId).url)
            }
          }
      }
  }

  private def determineViewModel(isPension: Boolean, employmentName: String, newValue: Int): DuplicateSubmissionEstimatedPay = {
    if (isPension) {
      DuplicateSubmissionCYPlus1PensionViewModel(employmentName, newValue)
    } else {
      DuplicateSubmissionCYPlus1EmploymentViewModel(employmentName, newValue)
    }
  }

  def duplicateWarning(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
          implicit val user: AuthedUser = request.taiUser
          val nino = user.nino

          updateNextYearsIncomeService.get(employmentId, nino) map { model =>
            val vm = determineViewModel(model.isPension, model.employmentName, model.newValue.get)
            Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Warning(DuplicateSubmissionWarningForm.createForm, vm, employmentId))
          }
      }
  }

  def submitDuplicateWarning(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
        implicit val user = request.taiUser
        val nino = user.nino

          DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
            formWithErrors => {
              updateNextYearsIncomeService.get(employmentId, nino) flatMap { model =>
                val vm = determineViewModel(model.isPension, model.employmentName, model.newValue.get)
                Future.successful(BadRequest(views.html.incomes.nextYear.updateIncomeCYPlus1Warning(formWithErrors, vm, employmentId)))
              }
            },
            success => {
              success.yesNoChoice match {
                case Some(YesValue) => Future.successful(Redirect(routes.UpdateIncomeNextYearController.start(employmentId).url))
                case Some(NoValue) => Future.successful(Redirect(controllers.routes.IncomeTaxComparisonController.onPageLoad().url))
              }
            }

          )
        }
  }

  def start(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {

          implicit val user = request.taiUser
          val nino = user.nino

          updateNextYearsIncomeService.get(employmentId, nino) map { model =>
            Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Start(model.employmentName, employmentId, model.isPension))
          }
      }
  }

  def edit(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
        implicit val user = request.taiUser
        val nino = user.nino

        updateNextYearsIncomeService.get(employmentId, nino) map {
          model => {
            Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Edit(model.employmentName, employmentId, model.isPension, model.currentValue, AmountComparatorForm.createForm()))
          }
        }
      }
  }

  def same(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
        implicit val user = request.taiUser
        val nino = user.nino

        updateNextYearsIncomeService.get(employmentId, nino) map { model =>
          Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Same(model.employmentName, model.employmentId, model.currentValue))
        }
      }
  }

  def success(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
        implicit val user = request.taiUser
        val nino = user.nino

          updateNextYearsIncomeService.get(employmentId, nino) map { model =>
            Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Success(model.employmentName, model.isPension))
          }
        }
  }

  def confirm(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      preAction {
        implicit val user = request.taiUser
        val nino = user.nino

        (updateNextYearsIncomeService.get(employmentId, user.nino).map {
          case UpdateNextYearsIncomeCacheModel(employmentName, _, _, currentValue, Some(estimatedAmount)) => {
            val vm = ConfirmAmountEnteredViewModel.nextYearEstimatedPay(employmentId, employmentName, currentValue, estimatedAmount)
            Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Confirm(vm))
          }
          case UpdateNextYearsIncomeCacheModel(_, _, _, _, None) => {
            throw new RuntimeException("[UpdateIncomeNextYear] Estimated income for next year not found for user.")
          }
        }).recover {
          case NonFatal(e) => internalServerError(e.getMessage)
        }
      }
  }

  def handleConfirm(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      val nino = user.nino

      if (cyPlusOneEnabled) {
        (updateNextYearsIncomeService.submit(employmentId, user.nino) map {
          case TaiSuccessResponse => Redirect(routes.UpdateIncomeNextYearController.success(employmentId))
          case _ => throw new RuntimeException(s"Not able to update estimated pay for $employmentId")
        }).recover {
          case NonFatal(e) => internalServerError(e.getMessage)
        }
      } else {
        Future.successful(NotFound(error4xxPageWithLink(Messages("global.error.pageNotFound404.title"))))
      }

  }

  def update(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      val nino = user.nino

      preAction {

        AmountComparatorForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            updateNextYearsIncomeService.get(employmentId, nino) map { model =>
              BadRequest(views.html.incomes.nextYear.updateIncomeCYPlus1Edit(model.employmentName, employmentId, model.isPension, model.currentValue, formWithErrors))
            }
          },
          validForm => {
            validForm.income.fold(throw new RuntimeException) { income =>
              updateNextYearsIncomeService.setNewAmount(income, employmentId, nino) map { model =>

                model.hasEstimatedIncomeChanged match {
                  case Some(result) => {
                    if (result) {
                      Redirect(controllers.income.routes.UpdateIncomeNextYearController.confirm(employmentId))
                    } else {
                      Redirect(controllers.income.routes.UpdateIncomeNextYearController.same(employmentId))
                    }
                  }
                  case None => {
                    Redirect(controllers.income.routes.UpdateIncomeNextYearController.edit(employmentId))
                  }
                }
              }
            }
          }
        )
      }
  }

  private def preAction(action: => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    if (cyPlusOneEnabled) {
      action
    } else {
      Future.successful(NotFound(error4xxPageWithLink(Messages("global.error.pageNotFound404.title"))))
    }
  }
}
