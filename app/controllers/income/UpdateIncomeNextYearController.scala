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

package controllers.income

import com.google.inject.Inject
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import controllers.{ServiceCheckLite, TaiBaseController}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.{PersonService, UpdateNextYearsIncomeService}
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel

import scala.concurrent.Future

class UpdateIncomeNextYearController @Inject()(updateNextYearsIncomeService: UpdateNextYearsIncomeService,
                                               personService: PersonService,
                                               val auditConnector: AuditConnector,
                                               val delegationConnector: DelegationConnector,
                                               val authConnector: AuthConnector,
                                               override implicit val partialRetriever: FormPartialRetriever,
                                               override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with FeatureTogglesConfig
  with Auditable {

  def start(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          preAction {
            updateNextYearsIncomeService.reset flatMap { _ =>
              updateNextYearsIncomeService.get(employmentId, Nino(user.getNino)) map { model =>
                Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Start(model.employmentName, employmentId, model.isPension))
              }
            }
          }
  }

  def edit(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          preAction {
            updateNextYearsIncomeService.get(employmentId, Nino(user.getNino)) map {
              model => {
                Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Edit(model.employmentName, employmentId, model.isPension, model.currentValue, AmountComparatorForm.createForm()))
              }
            }
          }
  }

  def same(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          preAction {
            updateNextYearsIncomeService.get(employmentId, Nino(user.getNino)) map { model =>
              Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Same(model.employmentName, model.employmentId, model.currentValue))
            }
          }
  }

  def success(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          preAction {
            updateNextYearsIncomeService.reset flatMap { _ =>
              updateNextYearsIncomeService.get(employmentId, Nino(user.getNino)) map { model =>
                Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Success(model.employmentName, model.isPension))
              }
            }
          }
  }

  def confirm(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          preAction {
            updateNextYearsIncomeService.get(employmentId, user.nino).map {
              case UpdateNextYearsIncomeCacheModel(employmentName, _, _, _, Some(estimatedAmount)) => {
                val vm = ConfirmAmountEnteredViewModel.nextYearEstimatedPay(employmentId, employmentName, estimatedAmount)
                Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Confirm(vm))
              }
              case UpdateNextYearsIncomeCacheModel(_, _, _, _, None) => {
                throw new RuntimeException("[UpdateIncomeNextYear] Estimated income for next year not found for user.")
              }
            }
          }

  }

  def handleConfirm(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          if (cyPlusOneEnabled) {
            ServiceCheckLite.personDetailsCheck {
              updateNextYearsIncomeService.submit(employmentId, user.nino) map {
                case TaiSuccessResponse => Redirect(routes.UpdateIncomeNextYearController.success(employmentId))
                case _ => throw new RuntimeException(s"Not able to update estimated pay for $employmentId")
              }
            }
          } else {
            Future.successful(NotFound(error4xxPageWithLink(Messages("global.error.pageNotFound404.title"))))
          }

  }

  def update(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          preAction {
            AmountComparatorForm.createForm().bindFromRequest().fold(

              formWithErrors => {
                updateNextYearsIncomeService.get(employmentId, Nino(user.getNino)) map { model =>
                  BadRequest(views.html.incomes.nextYear.updateIncomeCYPlus1Edit(model.employmentName, employmentId, model.isPension, model.currentValue, formWithErrors))
                }
              },
              validForm => {
                validForm.income.fold(throw new RuntimeException) { income =>
                  updateNextYearsIncomeService.setNewAmount(income, employmentId, Nino(user.getNino)) map { model =>

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

  private def preAction(action: => Future[Result])(implicit user: TaiUser, person: Person, request: Request[AnyContent]): Future[Result] = {
    if (cyPlusOneEnabled) {
      ServiceCheckLite.personDetailsCheck {
        action
      }
    } else {
      Future.successful(NotFound(error4xxPageWithLink(Messages("global.error.pageNotFound404.title"))))
    }
  }
}
