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
import pages.income.{UpdateIncomeBonusOvertimeAmountPage, UpdateIncomeBonusPaymentsPage, UpdateIncomeTaxablePayPage}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.forms.YesNoForm
import uk.gov.hmrc.tai.forms.income.incomeCalculator.{BonusOvertimeAmountForm, BonusPaymentsForm}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import views.html.incomes._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeUpdateBonusController @Inject() (
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  bonusPayments: BonusPaymentsView,
  bonusPaymentAmount: BonusPaymentAmountView,
  journeyCacheNewRepository: JourneyCacheNewRepository
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def bonusPaymentsPage: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    (
      IncomeSource.create(journeyCacheNewRepository, request.userAnswers),
      Future.successful(request.userAnswers.get(UpdateIncomeBonusPaymentsPage)),
      bonusPaymentBackUrl(request.userAnswers)
    ).mapN {
      case (Right(incomeSource), bonusPayment, backUrl) =>
        val form = BonusPaymentsForm.createForm.fill(YesNoForm(bonusPayment))
        Ok(bonusPayments(form, incomeSource, backUrl))
      case _ => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handleBonusPayments(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      BonusPaymentsForm.createForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            (
              IncomeSource.create(journeyCacheNewRepository, request.userAnswers),
              bonusPaymentBackUrl(request.userAnswers)
            ).mapN {
              case (Right(incomeSource), backUrl) =>
                BadRequest(bonusPayments(formWithErrors, incomeSource, backUrl))
              case _ => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
            },
          formData => {
            val bonusPaymentsAnswer = formData.yesNoChoice.fold(Map.empty[String, String]) { bonusPayments =>
              Map(UpdateIncomeBonusPaymentsPage.toString -> bonusPayments)
            }

            journeyCacheNewRepository.set(
              request.userAnswers.copy(data = request.userAnswers.data ++ Json.toJson(bonusPaymentsAnswer).as[JsObject])
            ) map { _ =>
              if (formData.yesNoChoice.contains(FormValuesConstants.YesValue)) {
                Redirect(routes.IncomeUpdateBonusController.bonusOvertimeAmountPage())
              } else {
                Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage(empId))
              }
            }
          }
        )
  }

  def bonusOvertimeAmountPage: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    (
      IncomeSource.create(journeyCacheNewRepository, request.userAnswers),
      Future.successful(request.userAnswers.get(UpdateIncomeBonusOvertimeAmountPage))
    ).mapN {
      case (Right(incomeSource), bonusOvertimeAmount) =>
        val form = BonusOvertimeAmountForm.createForm.fill(BonusOvertimeAmountForm(bonusOvertimeAmount))
        Ok(bonusPaymentAmount(form, incomeSource))
      case _ => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handleBonusOvertimeAmount(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      BonusOvertimeAmountForm.createForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            for {
              incomeSourceEither <- IncomeSource.create(journeyCacheNewRepository, request.userAnswers)
            } yield incomeSourceEither match {
              case Right(incomeSource) =>
                BadRequest(bonusPaymentAmount(formWithErrors, incomeSource))
              case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
            },
          formData =>
            formData.amount match {
              case Some(amount) =>
                journeyCacheNewRepository.set(
                  request.userAnswers.copy(data =
                    request.userAnswers.data ++ Json.obj(UpdateIncomeBonusOvertimeAmountPage.toString -> amount)
                  )
                ) map { _ =>
                  Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage(empId))
                }
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage(empId)))
            }
        )
  }

  private def bonusPaymentBackUrl(userAnswers: UserAnswers): Future[String] =
    Future.successful {
      userAnswers.get(UpdateIncomeTaxablePayPage) match {
        case None =>
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage().url
        case _ =>
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController
            .taxablePayslipAmountPage()
            .url
      }
    }

}
