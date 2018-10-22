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

package controllers

import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiFailureResponse}
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.{FormHelper, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.income.{ConfirmIncomeIrregularHoursViewModel, EditIncomeIrregularHoursViewModel}

import scala.concurrent.Future

trait IncomeUpdateCalculatorController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants {

  def personService: PersonService

  def journeyCacheService: JourneyCacheService

  def employmentService: EmploymentService

  def activityLoggerService: ActivityLoggerService

  def taxAccountService: TaxAccountService

  val incomeService: IncomeService

  def howToUpdatePage(id: Int): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>

        employmentService.employment(Nino(user.getNino), id) flatMap {
          case Some(employment) =>
            for {
              incomeToEdit <- incomeService.employmentAmount(Nino(user.getNino), id)
              taxCodeIncomeDetails <- taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear())
              _ <- journeyCacheService.cache(Map(UpdateIncome_NameKey -> employment.name, UpdateIncome_IdKey -> id.toString))
            } yield {
              processHowToUpdatePage(id, employment.name, incomeToEdit, taxCodeIncomeDetails)
            }
          case None => throw new RuntimeException("Not able to find employment")
        }
  }

  def processHowToUpdatePage(id: Int, employmentName: String, incomeToEdit: EmploymentAmount,
                             taxCodeIncomeDetails: TaiResponse)(implicit request: Request[AnyContent], user: TaiUser) = {
    (incomeToEdit.isLive, incomeToEdit.isOccupationalPension, taxCodeIncomeDetails) match {
      case (true, false, TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome])) => {
        if (incomeService.editableIncomes(taxCodeIncomes).size > 1) {
          Ok(views.html.incomes.howToUpdate(HowToUpdateForm.createForm(), id, employmentName))
        } else {
          incomeService.singularIncomeId(taxCodeIncomes) match {
            case Some(incomeId) => Ok(views.html.incomes.howToUpdate(HowToUpdateForm.createForm(), incomeId, employmentName))
            case None => throw new RuntimeException("Employment id not present")
          }
        }
      }
      case (false, false, _) => Redirect(routes.TaxAccountSummaryController.onPageLoad())
      case _ => Redirect(routes.IncomeController.pensionIncome())
    }
  }

  def handleChooseHowToUpdate: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processChooseHowToUpdate")
        HowToUpdateForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.howToUpdate(formWithErrors, id, employerName))
            }
          },
          formData => {
            formData.howToUpdate match {
              case Some("incomeCalculator") => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.workingHoursPage()))
              case _ => Future.successful(Redirect(routes.IncomeController.viewIncomeForEdit()))
            }
          }
        )
  }

  def workingHoursPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getWorkingHours")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.workingHours(HoursWorkedForm.createForm(), id, employerName))
        }
  }

  def handleWorkingHours: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processWorkedHours")
        HoursWorkedForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.workingHours(formWithErrors, id, employerName))
            }
          },
          formData => {
            val idRequest = journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
            val employerNameRequest = journeyCacheService.mandatoryValue(UpdateIncome_NameKey)

            for {
              id <- idRequest
              employerName <- employerNameRequest
            } yield {
              formData.workingHours match {
                case Some("regularHours") => Redirect(routes.IncomeUpdateCalculatorController.payPeriodPage())
                case Some("irregularHours") => Redirect(routes.IncomeUpdateCalculatorController.editIncomeIrregularHours(id))
                case _ => Redirect(routes.IncomeUpdateCalculatorController.calcUnavailablePage())
              }
            }
          }
        )
  }


  private val taxCodeIncomeInfoToCache = (taxCodeIncome: TaxCodeIncome) =>  Map[String, String](
    UpdateIncome_NameKey -> taxCodeIncome.name,
    UpdateIncome_PayToDateKey -> taxCodeIncome.amount.toString
  )

  def editIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        taxAccountService.taxCodeIncomeForEmployment(Nino(user.getNino), TaxYear(), employmentId) flatMap {
          case Some(tci) => {
            (taxCodeIncomeInfoToCache andThen journeyCacheService.cache)(tci) map { _ =>
              val viewModel = EditIncomeIrregularHoursViewModel(employmentId, tci.name, tci.amount.toInt)

              Ok(views.html.incomes.editIncomeIrregularHours(EditIncomeIrregularHoursForm.createForm(), viewModel))
            }
          }
          case None => throw new RuntimeException(s"Not able to find employment with id $employmentId")
        }
  }

  def handleIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request => {
        journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_PayToDateKey) flatMap {
          values => {
            val name :: ptd :: Nil = values.toList

            EditIncomeIrregularHoursForm.createForm(Some(ptd.toInt)).bindFromRequest().fold(

              formWithErrors => {
                val viewModel = EditIncomeIrregularHoursViewModel(employmentId, name, ptd.toInt)

                Future.successful(BadRequest(views.html.incomes.editIncomeIrregularHours(formWithErrors, viewModel)))
              },

              validForm =>
                validForm.income.fold(throw new RuntimeException) { income =>
                  journeyCacheService.cache(UpdateIncome_IrregularAnnualPayKey, income) map { _ =>
                    Redirect(routes.IncomeUpdateCalculatorController.confirmIncomeIrregularHours(employmentId))
                  }
                }
            )
          }
        }
      }
  }

  def confirmIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request => {
          journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IrregularAnnualPayKey).map { cacheValues => {
            val name :: newIrregularPay :: Nil = cacheValues.toList

            val vm = ConfirmIncomeIrregularHoursViewModel(employmentId, name, newIrregularPay.toInt)

            Ok(views.html.incomes.confirmIncomeIrregularHours(vm))
          }}
        }
  }

  def submitIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IrregularAnnualPayKey).flatMap(cache => {
            val employerName :: newPay :: Nil = cache.toList

            taxAccountService.updateEstimatedIncome(Nino(user.getNino), newPay.toInt, TaxYear(), employmentId) map {
              case TaiSuccessResponse => Ok(views.html.incomes.editSuccess(employerName))
              case _ => throw new RuntimeException(s"Not able to update estimated pay for $employmentId")
            }
          })
  }

  def payPeriodPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getPayPeriodPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.payPeriod(PayPeriodForm.createForm(None), id, employerName))
        }
  }

  def handlePayPeriod: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processPayPeriod")
        val payPeriod: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("payPeriod").flatMap(_.headOption))

        PayPeriodForm.createForm(None, payPeriod).bindFromRequest().fold(
          formWithErrors => {
            val isNotDaysError = formWithErrors.errors.filter { error => error.key == "otherInDays" }.isEmpty
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.payPeriod(formWithErrors, id, employerName, isNotDaysError))
            }
          },
          formData => {
            journeyCacheService.cache(incomeService.cachePayPeriod(formData)).map { _ =>
              Redirect(routes.IncomeUpdateCalculatorController.payslipAmountPage())
            }
          }
        )
  }

  def payslipAmountPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getPayslipAmountPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          payPeriod <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
        } yield {
          Ok(views.html.incomes.payslipAmount(PayslipForm.createForm(), payPeriod.getOrElse(""), id, employerName))
        }
  }

  def handlePayslipAmount: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processPayslipAmount")
        PayslipForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
              payPeriod <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
            } yield {
              BadRequest(views.html.incomes.payslipAmount(formWithErrors, payPeriod.getOrElse(""), id, employerName))
            }
          },
          formData => {
            formData match {
              case PayslipForm(Some(value)) => journeyCacheService.cache(UpdateIncome_TotalSalaryKey, value).map { _ =>
                Redirect(routes.IncomeUpdateCalculatorController.payslipDeductionsPage())
              }
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.payslipDeductionsPage()))
            }
          }
        )
  }

  def taxablePayslipAmountPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getTaxablePayslipAmountPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          payPeriod <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
        } yield {
          Ok(views.html.incomes.taxablePayslipAmount(TaxablePayslipForm.createForm(), payPeriod.getOrElse(""), id, employerName))
        }
  }

  def handleTaxablePayslipAmount: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processTaxablePayslipAmount")

        journeyCacheService.currentValue(UpdateIncome_TotalSalaryKey) flatMap { cacheTotalSalary =>
          val totalSalary = FormHelper.stripNumber(cacheTotalSalary)
          TaxablePayslipForm.createForm(totalSalary).bindFromRequest().fold(
            formWithErrors => {
              for {
                id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
                employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
                payPeriod <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
              } yield {
                BadRequest(views.html.incomes.taxablePayslipAmount(formWithErrors, payPeriod.getOrElse(""), id, employerName))
              }
            },
            formData => {
              formData.taxablePay match {
                case Some(taxablePay) => journeyCacheService.cache(UpdateIncome_TaxablePayKey, taxablePay) map { _ =>
                  Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage())
                }
                case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage()))
              }
            }
          )
        }
  }

  def payslipDeductionsPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getPayslipDeductionsPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.payslipDeductions(PayslipDeductionsForm.createForm(), id, employerName))
        }
  }

  def handlePayslipDeductions: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processPayslipDeductions")

        PayslipDeductionsForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.payslipDeductions(formWithErrors, id, employerName))
            }
          },
          formData => {
            formData.payslipDeductions match {
              case Some(payslipDeductions) if payslipDeductions == "Yes" =>
                journeyCacheService.cache(UpdateIncome_PayslipDeductionsKey, payslipDeductions).map { _ =>
                  Redirect(routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage())
                }
              case Some(payslipDeductions) => journeyCacheService.cache(UpdateIncome_PayslipDeductionsKey, payslipDeductions) map { _ =>
                Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage())
              }
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage()))
            }
          }
        )
  }

  def bonusPaymentsPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getBonusPaymentsPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          paySlipDeductions <- journeyCacheService.currentValue(UpdateIncome_PayslipDeductionsKey)
        } yield {
          val isPaySlipDeductions = paySlipDeductions.contains("Yes")
          Ok(views.html.incomes.bonusPayments(BonusPaymentsForm.createForm(), id, employerName, isPaySlipDeductions, false))
        }
  }

  def handleBonusPayments: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processBonusPayments")

        val bonusPayments: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("bonusPayments").flatMap(_.headOption))
        val bonusPaymentsSelected = bonusPayments.contains("Yes")
        BonusPaymentsForm.createForm(bonusPayments = bonusPayments).bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
              paySlipDeductions <- journeyCacheService.currentValue(UpdateIncome_PayslipDeductionsKey)
            } yield {
              val isPaySlipDeductions = paySlipDeductions.contains("Yes")
              BadRequest(views.html.incomes.bonusPayments(formWithErrors, id, employerName, isPaySlipDeductions, bonusPaymentsSelected))
            }
          },
          formData => {
            journeyCacheService.cache(incomeService.cacheBonusPayments(formData)) map { _ =>
              if (formData.bonusPayments.contains("Yes")) {
                Redirect(routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage())
              } else {
                Redirect(routes.IncomeUpdateCalculatorController.estimatedPayPage())
              }
            }
          }
        )
  }

  def bonusOvertimeAmountPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          moreThisYear <- journeyCacheService.currentValue(UpdateIncome_BonusPaymentsThisYearKey)
          payPeriod <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
        } yield {
          if (moreThisYear.contains("Yes")) {
            Ok(views.html.incomes.bonusPaymentAmount(BonusOvertimeAmountForm.createForm(), "year", id, employerName))
          } else {
            Ok(views.html.incomes.bonusPaymentAmount(BonusOvertimeAmountForm.createForm(), payPeriod.getOrElse(""), id, employerName))
          }
        }
  }

  def handleBonusOvertimeAmount: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processBonusOvertimeAmount")
        journeyCacheService.currentCache.flatMap { cache =>
          val moreThisYear = cache.get(UpdateIncome_BonusPaymentsThisYearKey)
          val payPeriod = cache.get(UpdateIncome_PayPeriodKey)
          BonusOvertimeAmountForm.createForm(
            Some(BonusOvertimeAmountForm.bonusPaymentsAmountErrorMessage(moreThisYear, payPeriod)),
            Some(BonusOvertimeAmountForm.notAmountMessage(payPeriod))
          ).bindFromRequest().fold(
            formWithErrors => {
              val id = cache(UpdateIncome_IdKey).toInt
              val employerName = cache.get(UpdateIncome_NameKey).toString
              if (moreThisYear.contains("Yes")) {
                Future.successful(BadRequest(views.html.incomes.bonusPaymentAmount(formWithErrors, "year", id, employerName)))
              } else {
                Future.successful(BadRequest(views.html.incomes.bonusPaymentAmount(formWithErrors, payPeriod.getOrElse(""), id, employerName)))
              }
            },
            formData => {
              formData.amount match {
                case Some(amount) =>
                  journeyCacheService.cache(UpdateIncome_BonusOvertimeAmountKey, amount) map { _ =>
                    Redirect(routes.IncomeUpdateCalculatorController.estimatedPayPage())
                  }
                case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.estimatedPayPage()))
              }
            }
          )
        }
  }

  def estimatedPayPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getEstimatedPayPage")

        val result = for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          income <- incomeService.employmentAmount(Nino(user.getNino), id)
          cache <- journeyCacheService.currentCache
          calculatedPay <- incomeService.calculateEstimatedPay(cache, income.startDate)
          payment <- incomeService.latestPayment(Nino(user.getNino), id)
        } yield {
          val payYearToDate: BigDecimal = payment.map(_.amountYearToDate).getOrElse(BigDecimal(0))
          val paymentDate: Option[LocalDate] = payment.map(_.date)

          if (calculatedPay.grossAnnualPay.get > payYearToDate) {
            val cache = Map(UpdateIncome_GrossAnnualPayKey -> calculatedPay.grossAnnualPay.map(_.toString).getOrElse(""),
              UpdateIncome_NewAmountKey -> calculatedPay.netAnnualPay.map(_.toString).getOrElse(""))
            val isBonusPayment = cache.getOrElse(UpdateIncome_BonusPaymentsKey, "") == "Yes"

            journeyCacheService.cache(cache).map { _ =>
              Ok(views.html.incomes.estimatedPay(calculatedPay.grossAnnualPay, calculatedPay.netAnnualPay, id, isBonusPayment,
                calculatedPay.annualAmount, calculatedPay.startDate, employerName, calculatedPay.grossAnnualPay == calculatedPay.netAnnualPay))
            }
          } else {
            Future.successful(Ok(views.html.incomes.incorrectTaxableIncome(payYearToDate, paymentDate.getOrElse(new LocalDate), id)))
          }
        }

        result.flatMap(identity)
  }

  def handleCalculationResult: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processCalculationResult")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          income <- incomeService.employmentAmount(Nino(user.getNino), id)
          netAmount <- journeyCacheService.currentValue(UpdateIncome_NewAmountKey)
        } yield {
          val newAmount = income.copy(newAmount = netAmount.map(netAmountValue => BigDecimal(netAmountValue).intValue()).getOrElse(income.oldAmount))
          Ok(views.html.incomes.confirm_save_Income(EditIncomeForm.create(preFillData = newAmount).get))
        }
  }

  def calcUnavailablePage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.calcUnavailable(id, employerName))
        }
  }
}

// $COVERAGE-OFF$
object IncomeUpdateCalculatorController extends IncomeUpdateCalculatorController with AuthenticationConnectors {
  override val personService: PersonService = PersonService
  override val activityLoggerService: ActivityLoggerService = ActivityLoggerService
  override val journeyCacheService = JourneyCacheService(UpdateIncome_JourneyKey)
  override val employmentService: EmploymentService = EmploymentService
  override val incomeService: IncomeService = IncomeService
  override val taxAccountService: TaxAccountService = TaxAccountService

  override implicit def templateRenderer: TemplateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

}

// $COVERAGE-ON$
