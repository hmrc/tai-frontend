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
import repository.JourneyCacheNewRepository
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
  journeyCacheNewRepository: JourneyCacheNewRepository
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def payslipAmountPage: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers

    val mandatoryValues = Seq(userAnswers.get(UpdateIncomeIdPage), userAnswers.get(UpdateIncomeNamePage))

    val optionalValues = Seq(
      userAnswers.get(UpdateIncomePayPeriodPage),
      userAnswers.get(UpdateIncomeOtherInDaysPage),
      userAnswers.get(UpdateIncomeTotalSalaryPage)
    )

    (mandatoryValues, optionalValues) match {
      case (Seq(Some(incomeId), Some(incomeName)), Seq(payPeriod, payPeriodInDays, totalSalary)) =>
        val employer = IncomeSource(incomeId.toString.toInt, incomeName.toString)
        val errorMessage = "tai.payslip.error.form.totalPay.input.mandatory"
        val paySlipForm = PayslipForm.createForm(errorMessage).fill(PayslipForm(totalSalary))

        val viewModel = PaySlipAmountViewModel(paySlipForm, payPeriod, payPeriodInDays, employer)
        Future.successful(Ok(payslipAmount(viewModel)))

      case _ =>
        Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
    }
  }

  def handlePayslipAmount: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers
    val payPeriod = userAnswers.get(UpdateIncomePayPeriodPage)
    val payPeriodInDays = userAnswers.get(UpdateIncomeOtherInDaysPage)

    IncomeSource.create(journeyCacheNewRepository, userAnswers).flatMap {
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
                val updatedUserAnswers = userAnswers.set(UpdateIncomeTotalSalaryPage, value)
                journeyCacheNewRepository.set(updatedUserAnswers.get).map { _ =>
                  Redirect(routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage())
                }
              case _ => Future.successful(Redirect(routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage()))
            }
          )
      case Left(_) => Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
    }
  }

  def taxablePayslipAmountPage: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers

    val mandatoryValues = Seq(
      userAnswers.get(UpdateIncomeIdPage),
      userAnswers.get(UpdateIncomeNamePage)
    )

    val optionalValues = Seq(
      userAnswers.get(UpdateIncomePayPeriodPage),
      userAnswers.get(UpdateIncomeOtherInDaysPage),
      userAnswers.get(UpdateIncomeTaxablePayPage)
    )

    (mandatoryValues, optionalValues) match {
      case (Seq(Some(incomeId), Some(incomeName)), Seq(payPeriod, payPeriodInDays, taxablePayKey)) =>
        val incomeSource = IncomeSource(id = incomeId.toString.toInt, name = incomeName.toString)
        val form = TaxablePayslipForm.createForm().fill(TaxablePayslipForm(taxablePayKey))
        val viewModel = TaxablePaySlipAmountViewModel(form, payPeriod, payPeriodInDays, incomeSource)
        Future.successful(Ok(taxablePayslipAmount(viewModel)))

      case _ =>
        Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
    }
  }

  def handleTaxablePayslipAmount: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers
    val payPeriod = userAnswers.get(UpdateIncomePayPeriodPage)
    val payPeriodInDays = userAnswers.get(UpdateIncomeOtherInDaysPage)
    val totalSalary = userAnswers.get(UpdateIncomeTotalSalaryPage)

    IncomeSource.create(journeyCacheNewRepository, userAnswers).flatMap {
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
                  val updatedUserAnswers = userAnswers.set(UpdateIncomeTaxablePayPage, taxablePay)
                  journeyCacheNewRepository.set(updatedUserAnswers.get).map { _ =>
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

    IncomeSource.create(journeyCacheNewRepository, userAnswers).map {
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
            incomeSourceEither <- IncomeSource.create(journeyCacheNewRepository, request.userAnswers)
          } yield incomeSourceEither match {
            case Right(incomeSource) => BadRequest(payslipDeductionsView(formWithErrors, incomeSource))
            case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
          },
        formData =>
          formData.payslipDeductions match {
            case Some(payslipDeductions) if payslipDeductions == "Yes" =>
              val updatedUserAnswers = request.userAnswers.set(UpdateIncomePayslipDeductionsPage, payslipDeductions)
              journeyCacheNewRepository.set(updatedUserAnswers.get).map { _ =>
                Redirect(routes.IncomeUpdatePayslipAmountController.taxablePayslipAmountPage())
              }
            case Some(payslipDeductions) =>
              val updatedUserAnswers = request.userAnswers.set(UpdateIncomePayslipDeductionsPage, payslipDeductions)
              journeyCacheNewRepository.set(updatedUserAnswers.get).map { _ =>
                Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage())
              }
            case _ => Future.successful(Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage()))
          }
      )
  }

}
