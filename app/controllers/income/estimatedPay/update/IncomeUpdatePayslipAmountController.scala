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

import controllers.TaiBaseController
import controllers.auth.{AuthJourney, AuthedUser}
import pages.income._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.income.incomeCalculator.{PayslipDeductionsForm, PayslipForm, TaxablePayslipForm}
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{GrossPayPeriodTitle, PaySlipAmountViewModel, TaxablePaySlipAmountViewModel}
import views.html.incomes.{PayslipAmountView, PayslipDeductionsView, TaxablePayslipAmountView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeUpdatePayslipAmountController @Inject() (
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  payslipAmount: PayslipAmountView,
  taxablePayslipAmount: TaxablePayslipAmountView,
  payslipDeductionsView: PayslipDeductionsView,
  journeyCacheRepository: JourneyCacheRepository
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def payslipAmountPage: Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers

    val incomeIdOpt = userAnswers.get(UpdateIncomeIdPage)
    val incomeNameOpt = userAnswers.get(UpdateIncomeNamePage)

    val payPeriodOpt = userAnswers.get(UpdateIncomePayPeriodPage)
    val payPeriodInDaysOpt = userAnswers.get(UpdateIncomeOtherInDaysPage)
    val totalSalaryOpt = userAnswers.get(UpdateIncomeTotalSalaryPage)

    (incomeIdOpt, incomeNameOpt, payPeriodOpt, payPeriodInDaysOpt, totalSalaryOpt) match {
      case (Some(incomeId), Some(incomeName), payPeriod, payPeriodInDays, totalSalary) =>
        val employer = IncomeSource(incomeId.toString.toInt, incomeName)
        val errorMessage = "tai.payslip.error.form.totalPay.input.mandatory"
        val paySlipForm = PayslipForm.createForm(errorMessage).fill(PayslipForm(totalSalary))

        val viewModel = PaySlipAmountViewModel(paySlipForm, payPeriod, payPeriodInDays, employer)
        Ok(payslipAmount(viewModel))

      case _ =>
        Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handlePayslipAmount: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers
    val payPeriod = userAnswers.get(UpdateIncomePayPeriodPage)
    val payPeriodInDays = userAnswers.get(UpdateIncomeOtherInDaysPage)

    IncomeSource.create(journeyCacheRepository, userAnswers).flatMap {
      case Right(incomeSource) =>
        val errorMessage = GrossPayPeriodTitle.title(payPeriod, payPeriodInDays)
        PayslipForm
          .createForm(errorMessage)
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val viewModel = PaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, incomeSource)
              Future.successful(BadRequest(payslipAmount(viewModel)))
            },
            {
              case PayslipForm(Some(value)) =>
                val updatedUserAnswers = userAnswers.setOrException(UpdateIncomeTotalSalaryPage, value)
                journeyCacheRepository.set(updatedUserAnswers).map { _ =>
                  Redirect(routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage())
                }
              case _ => Future.successful(Redirect(routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage()))
            }
          )
      case Left(_) => Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
    }
  }

  def taxablePayslipAmountPage: Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    val userAnswers = request.userAnswers

    val incomeIdOpt = userAnswers.get(UpdateIncomeIdPage)
    val incomeNameOpt = userAnswers.get(UpdateIncomeNamePage)

    val payPeriodOpt = userAnswers.get(UpdateIncomePayPeriodPage)
    val payPeriodInDaysOpt = userAnswers.get(UpdateIncomeOtherInDaysPage)
    val taxablePayOpt = userAnswers.get(UpdateIncomeTaxablePayPage)

    (incomeIdOpt, incomeNameOpt, payPeriodOpt, payPeriodInDaysOpt, taxablePayOpt) match {
      case (Some(incomeId), Some(incomeName), payPeriod, payPeriodInDays, taxablePay) =>
        val incomeSource = IncomeSource(id = incomeId.toString.toInt, name = incomeName)
        val form = TaxablePayslipForm.createForm().fill(TaxablePayslipForm(taxablePay))
        val viewModel = TaxablePaySlipAmountViewModel(form, payPeriod, payPeriodInDays, incomeSource)
        Ok(taxablePayslipAmount(viewModel))

      case _ =>
        Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handleTaxablePayslipAmount: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers
    val payPeriod = userAnswers.get(UpdateIncomePayPeriodPage)
    val payPeriodInDays = userAnswers.get(UpdateIncomeOtherInDaysPage)
    val totalSalary = userAnswers.get(UpdateIncomeTotalSalaryPage)

    IncomeSource.create(journeyCacheRepository, userAnswers).flatMap {
      case Right(incomeSource) =>
        TaxablePayslipForm
          .createForm(FormHelper.stripNumber(totalSalary), payPeriod, payPeriodInDays)
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val viewModel = TaxablePaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, incomeSource)
              Future.successful(BadRequest(taxablePayslipAmount(viewModel)))
            },
            formData =>
              formData.taxablePay match {
                case Some(taxablePay) =>
                  val updatedUserAnswers = userAnswers.setOrException(UpdateIncomeTaxablePayPage, taxablePay)
                  journeyCacheRepository.set(updatedUserAnswers).map { _ =>
                    Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage())
                  }
                case _ => Future.successful(Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage()))
              }
          )
      case Left(_) => Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
    }
  }

  def payslipDeductionsPage: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers
    val payslipDeductions = userAnswers.get(UpdateIncomePayslipDeductionsPage)

    IncomeSource.create(journeyCacheRepository, userAnswers).map {
      case Right(incomeSource) =>
        val form = PayslipDeductionsForm.createForm().fill(PayslipDeductionsForm(payslipDeductions))
        Ok(payslipDeductionsView(form, incomeSource))
      case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handlePayslipDeductions: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    PayslipDeductionsForm
      .createForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          for {
            incomeSourceEither <- IncomeSource.create(journeyCacheRepository, request.userAnswers)
          } yield incomeSourceEither match {
            case Right(incomeSource) => BadRequest(payslipDeductionsView(formWithErrors, incomeSource))
            case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
          },
        formData =>
          formData.payslipDeductions match {
            case Some(payslipDeductions) if payslipDeductions == "Yes" =>
              val updatedUserAnswers =
                request.userAnswers.setOrException(UpdateIncomePayslipDeductionsPage, payslipDeductions)
              journeyCacheRepository.set(updatedUserAnswers).map { _ =>
                Redirect(routes.IncomeUpdatePayslipAmountController.taxablePayslipAmountPage())
              }
            case Some(payslipDeductions) =>
              val updatedUserAnswers =
                request.userAnswers.setOrException(UpdateIncomePayslipDeductionsPage, payslipDeductions)
              journeyCacheRepository.set(updatedUserAnswers).map { _ =>
                Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage())
              }
            case _ => Future.successful(Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage()))
          }
      )
  }

}
