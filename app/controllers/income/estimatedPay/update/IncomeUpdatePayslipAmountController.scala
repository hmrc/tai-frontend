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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.forms.{PayslipDeductionsForm, PayslipForm, TaxablePayslipForm}
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{GrossPayPeriodTitle, PaySlipAmountViewModel, TaxablePaySlipAmountViewModel}
import views.html.incomes.{PayslipAmountView, PayslipDeductionsView, TaxablePayslipAmountView}

import scala.concurrent.{ExecutionContext, Future}

class IncomeUpdatePayslipAmountController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  payslipAmount: PayslipAmountView,
  taxablePayslipAmount: TaxablePayslipAmountView,
  payslipDeductionsView: PayslipDeductionsView,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with UpdatedEstimatedPayJourneyCache {

  def payslipAmountPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
    val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TotalSalaryKey)

    journeyCacheService.collectedJourneyValues(mandatoryKeys, optionalKeys).map {
      case Right((mandatorySeq, optionalSeq)) =>
        val viewModel = {
          val employer = IncomeSource(mandatorySeq.head.toInt, mandatorySeq(1))

          val payPeriod = optionalSeq.head
          val payPeriodInDays = optionalSeq(1)
          val totalSalary = optionalSeq(2)

          val errorMessage = "tai.payslip.error.form.totalPay.input.mandatory"

          val paySlipForm = PayslipForm.createForm(errorMessage).fill(PayslipForm(totalSalary))

          PaySlipAmountViewModel(paySlipForm, payPeriod, payPeriodInDays, employer)
        }

        Ok(payslipAmount(viewModel))

      case Left(_) =>
        Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handlePayslipAmount: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (for {
      incomeSourceEither <- IncomeSource.create(journeyCacheService)
      payPeriod :: payPeriodInDays :: _ <- journeyCacheService
                                            .optionalValues(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey)
    } yield {
      val errorMessage = GrossPayPeriodTitle.title(payPeriod, payPeriodInDays)
      PayslipForm
        .createForm(errorMessage)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            incomeSourceEither match {
              case Right(incomeSource) =>
                val viewModel = PaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, incomeSource)
                Future.successful(BadRequest(payslipAmount(viewModel)))
              case Left(_) => Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
            }
          }, {
            case PayslipForm(Some(value)) =>
              journeyCache(UpdateIncome_TotalSalaryKey, Map(UpdateIncome_TotalSalaryKey -> value)) map { _ =>
                Redirect(routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage())
              }
            case _ => Future.successful(Redirect(routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage()))
          }
        )
    }).flatten

  }

  def taxablePayslipAmountPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
    val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TaxablePayKey)

    journeyCacheService.collectedJourneyValues(mandatoryKeys, optionalKeys) map {
      case Right((mandotorySeq, optionalSeq)) =>
        val viewModel = {
          val incomeSource = IncomeSource(id = mandotorySeq.head.toInt, name = mandotorySeq(1))
          val payPeriod = optionalSeq.head
          val payPeriodInDays = optionalSeq(1)
          val taxablePayKey = optionalSeq(2)

          val form = TaxablePayslipForm.createForm().fill(TaxablePayslipForm(taxablePayKey))
          TaxablePaySlipAmountViewModel(form, payPeriod, payPeriodInDays, incomeSource)
        }
        Ok(taxablePayslipAmount(viewModel))

      case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def handleTaxablePayslipAmount: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (for {
      incomeSourceEither <- IncomeSource.create(journeyCacheService)
      payPeriod :: payPeriodInDays :: totalSalary :: _ <- journeyCacheService.optionalValues(
                                                           UpdateIncome_PayPeriodKey,
                                                           UpdateIncome_OtherInDaysKey,
                                                           UpdateIncome_TotalSalaryKey)
    } yield {
      TaxablePayslipForm
        .createForm(FormHelper.stripNumber(totalSalary), payPeriod, payPeriodInDays)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            incomeSourceEither match {
              case Right(incomeSource) =>
                val viewModel = TaxablePaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, incomeSource)
                Future.successful(BadRequest(taxablePayslipAmount(viewModel)))
              case Left(_) => Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
            }
          },
          formData => {
            formData.taxablePay match {
              case Some(taxablePay) =>
                journeyCache(UpdateIncome_TaxablePayKey, Map(UpdateIncome_TaxablePayKey -> taxablePay)) map { _ =>
                  Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage())
                }
              case _ => Future.successful(Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage()))
            }
          }
        )
    }).flatMap(identity)
  }

  def payslipDeductionsPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      incomeSourceEither <- IncomeSource.create(journeyCacheService)
      payslipDeductions  <- journeyCacheService.currentValue(UpdateIncome_PayslipDeductionsKey)
    } yield {
      val form = PayslipDeductionsForm.createForm().fill(PayslipDeductionsForm(payslipDeductions))
      incomeSourceEither match {
        case Right(incomeSource) => Ok(payslipDeductionsView(form, incomeSource))
        case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      }
    }
  }

  def handlePayslipDeductions: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    PayslipDeductionsForm
      .createForm()
      .bindFromRequest()
      .fold(
        formWithErrors => {

          for {
            incomeSourceEither <- IncomeSource.create(journeyCacheService)
          } yield {
            incomeSourceEither match {
              case Right(incomeSource) => BadRequest(payslipDeductionsView(formWithErrors, incomeSource))
              case Left(_)             => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
            }
          }
        },
        formData => {
          formData.payslipDeductions match {
            case Some(payslipDeductions) if payslipDeductions == "Yes" =>
              journeyCache(
                UpdateIncome_PayslipDeductionsKey,
                Map(UpdateIncome_PayslipDeductionsKey -> payslipDeductions)) map { _ =>
                Redirect(routes.IncomeUpdatePayslipAmountController.taxablePayslipAmountPage())
              }
            case Some(payslipDeductions) =>
              journeyCache(
                UpdateIncome_PayslipDeductionsKey,
                Map(UpdateIncome_PayslipDeductionsKey -> payslipDeductions)) map { _ =>
                Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage())
              }

            case _ => Future.successful(Redirect(routes.IncomeUpdateBonusController.bonusPaymentsPage()))
          }
        }
      )
  }

}
