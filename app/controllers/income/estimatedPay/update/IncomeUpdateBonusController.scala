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

import javax.inject.{Inject, Named}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.forms.income.incomeCalculator.{BonusOvertimeAmountForm, BonusPaymentsForm}
import uk.gov.hmrc.tai.forms.YesNoForm
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants}
import views.html.incomes._

import scala.concurrent.{ExecutionContext, Future}

class IncomeUpdateBonusController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  bonusPayments: BonusPaymentsView,
  bonusPaymentAmount: BonusPaymentAmountView,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with UpdatedEstimatedPayJourneyCache {
  def bonusPaymentsPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      incomeSourceEither <- IncomeSource.create(journeyCacheService)
      bonusPayment       <- journeyCacheService.currentValue(UpdateIncome_BonusPaymentsKey)
      backUrl            <- bonusPaymentBackUrl
    } yield {
      val form = BonusPaymentsForm.createForm.fill(YesNoForm(bonusPayment))
      incomeSourceEither match {
        case Right(incomeSource) => Ok(bonusPayments(form, incomeSource, backUrl))
        case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      }
    }
  }

  def handleBonusPayments(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      BonusPaymentsForm.createForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            for {
              incomeSourceEither <- IncomeSource.create(journeyCacheService)
              backUrl            <- bonusPaymentBackUrl
            } yield {
              incomeSourceEither match {
                case Right(incomeSource) => BadRequest(bonusPayments(formWithErrors, incomeSource, backUrl))
                case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
              }
            }
          },
          formData => {
            val bonusPaymentsAnswer = formData.yesNoChoice.fold(ifEmpty = Map.empty[String, String]) { bonusPayments =>
              Map(UpdateIncome_BonusPaymentsKey -> bonusPayments)
            }

            journeyCache(UpdateIncome_BonusPaymentsKey, bonusPaymentsAnswer) map { _ =>
              if (formData.yesNoChoice.contains(FormValuesConstants.YesValue)) {
                Redirect(routes.IncomeUpdateBonusController.bonusOvertimeAmountPage())
              } else {
                Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage(empId))
              }
            }
          }
        )
  }

  def bonusOvertimeAmountPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      incomeSourceEither  <- IncomeSource.create(journeyCacheService)
      bonusOvertimeAmount <- journeyCacheService.currentValue(UpdateIncome_BonusOvertimeAmountKey)
    } yield {
      val form = BonusOvertimeAmountForm.createForm().fill(BonusOvertimeAmountForm(bonusOvertimeAmount))
      incomeSourceEither match {
        case Right(incomeSource) => Ok(bonusPaymentAmount(form, incomeSource))
        case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      }

    }
  }

  def handleBonusOvertimeAmount(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      BonusOvertimeAmountForm
        .createForm()
        .bindFromRequest()
        .fold(
          formWithErrors => {
            for {
              incomeSourceEither <- IncomeSource.create(journeyCacheService)
            } yield {
              incomeSourceEither match {
                case Right(incomeSource) =>
                  BadRequest(bonusPaymentAmount(formWithErrors, incomeSource))
                case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
              }
            }
          },
          formData => {
            formData.amount match {
              case Some(amount) =>
                journeyCache(UpdateIncome_BonusOvertimeAmountKey, Map(UpdateIncome_BonusOvertimeAmountKey -> amount)) map {
                  _ =>
                    Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage(empId))
                }
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage(empId)))
            }
          }
        )
  }

  private def bonusPaymentBackUrl(implicit hc: HeaderCarrier) =
    journeyCacheService.currentValue(UpdateIncome_TaxablePayKey).map {
      case None =>
        controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage().url
      case _ =>
        controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.taxablePayslipAmountPage().url
    }
}
