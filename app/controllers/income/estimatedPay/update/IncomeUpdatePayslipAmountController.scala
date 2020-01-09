/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.auth.AuthAction
import javax.inject.{Inject, Named}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.forms.{PayslipDeductionsForm, PayslipForm, TaxablePayslipForm}
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{GrossPayPeriodTitle, PaySlipAmountViewModel, TaxablePaySlipAmountViewModel}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.Function.tupled
import scala.concurrent.Future

class IncomeUpdatePayslipAmountController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)
    extends TaiBaseController with JourneyCacheConstants with UpdatedEstimatedPayJourneyCache {

  def payslipAmountPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
    val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TotalSalaryKey)

    journeyCacheService.collectedValues(mandatoryKeys, optionalKeys) map
      tupled { (mandatorySeq, optionalSeq) =>
        {
          val viewModel = {
            val employer = IncomeSource(mandatorySeq(0).toInt, mandatorySeq(1))

            val payPeriod = optionalSeq(0)
            val payPeriodInDays = optionalSeq(1)
            val totalSalary = optionalSeq(2)

            val errorMessage = "tai.payslip.error.form.totalPay.input.mandatory"

            val paySlipForm = PayslipForm.createForm(errorMessage).fill(PayslipForm(totalSalary))

            PaySlipAmountViewModel(paySlipForm, payPeriod, payPeriodInDays, employer)
          }

          Ok(views.html.incomes.payslipAmount(viewModel))
        }
      }
  }

  def handlePayslipAmount: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    val employerFuture = IncomeSource.create(journeyCacheService)

    val result: Future[Future[Result]] = for {
      employer        <- employerFuture
      payPeriod       <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
      payPeriodInDays <- journeyCacheService.currentValue(UpdateIncome_OtherInDaysKey)
    } yield {
      val errorMessage = GrossPayPeriodTitle.title(payPeriod, payPeriodInDays)
      PayslipForm
        .createForm(errorMessage)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel = PaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, employer)
            Future.successful(BadRequest(views.html.incomes.payslipAmount(viewModel)))
          },
          formData => {
            formData match {
              case PayslipForm(Some(value)) =>
                journeyCache(UpdateIncome_TotalSalaryKey, Map(UpdateIncome_TotalSalaryKey -> value)) map { _ =>
                  Redirect(routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage())
                }
              case _ => Future.successful(Redirect(routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage()))
            }
          }
        )
    }

    result.flatMap(identity)
  }

  def taxablePayslipAmountPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
    val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TaxablePayKey)

    journeyCacheService.collectedValues(mandatoryKeys, optionalKeys) map
      tupled { (mandatorySeq, optionalSeq) =>
        {
          val viewModel = {
            val employer = IncomeSource(id = mandatorySeq(0).toInt, name = mandatorySeq(1))
            val payPeriod = optionalSeq(0)
            val payPeriodInDays = optionalSeq(1)
            val taxablePayKey = optionalSeq(2)

            val form = TaxablePayslipForm.createForm().fill(TaxablePayslipForm(taxablePayKey))
            TaxablePaySlipAmountViewModel(form, payPeriod, payPeriodInDays, employer)
          }
          Ok(views.html.incomes.taxablePayslipAmount(viewModel))
        }
      }
  }

  def handleTaxablePayslipAmount: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    val employerFuture = IncomeSource.create(journeyCacheService)

    val futurePayPeriod = journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
    val futurePayPeriodInDays = journeyCacheService.currentValue(UpdateIncome_OtherInDaysKey)
    val futureTotalSalary = journeyCacheService.currentValue(UpdateIncome_TotalSalaryKey)

    (for {
      employer        <- employerFuture
      payPeriod       <- futurePayPeriod
      payPeriodInDays <- futurePayPeriodInDays
      totalSalary     <- futureTotalSalary
    } yield {
      TaxablePayslipForm
        .createForm(FormHelper.stripNumber(totalSalary), payPeriod, payPeriodInDays)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel = TaxablePaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, employer)
            Future.successful(BadRequest(views.html.incomes.taxablePayslipAmount(viewModel)))
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
    implicit val user = request.taiUser

    val employerFuture = IncomeSource.create(journeyCacheService)

    for {
      employer          <- employerFuture
      payslipDeductions <- journeyCacheService.currentValue(UpdateIncome_PayslipDeductionsKey)
    } yield {
      val form = PayslipDeductionsForm.createForm().fill(PayslipDeductionsForm(payslipDeductions))
      Ok(views.html.incomes.payslipDeductions(form, employer))
    }
  }

  def handlePayslipDeductions: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    PayslipDeductionsForm
      .createForm()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val employerFuture = IncomeSource.create(journeyCacheService)

          for {
            employer <- employerFuture
          } yield {
            BadRequest(views.html.incomes.payslipDeductions(formWithErrors, employer))
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
