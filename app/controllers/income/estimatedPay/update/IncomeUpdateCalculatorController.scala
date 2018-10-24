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

package controllers.income.estimatedPay.update

import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import controllers.{AuthenticationConnectors, TaiBaseController}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome, PensionIncome}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.{FormHelper, JourneyCacheConstants, TaiConstants}
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.CheckYourAnswersViewModel

import scala.Function.tupled
import scala.concurrent.Future

trait IncomeUpdateCalculatorController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants
  with UpdatedEstimatedPayJourneyCache {

  def personService: PersonService

  implicit def journeyCacheService: JourneyCacheService

  def employmentService: EmploymentService

  def activityLoggerService: ActivityLoggerService

  def taxAccountService: TaxAccountService

  val incomeService: IncomeService


  def estimatedPayLandingPage(id: Int): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>

        val taxCodeIncomesFuture = taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear())
        val employmentFuture = employmentService.employment(Nino(user.getNino), id)


        for {
          taxCodeIncomeDetails <- taxCodeIncomesFuture
          employmentDetails <- employmentFuture
        } yield {
          (taxCodeIncomeDetails, employmentDetails) match {
            case (TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]), Some(employment)) =>
              val taxCodeIncomeSource = taxCodeIncomes.find(_.employmentId.contains(id)).
                getOrElse(throw new RuntimeException(s"Income details not found for employment id $id"))
              val isPension = taxCodeIncomeSource.componentType == PensionIncome
              Ok(views.html.incomes.estimatedPayLandingPage(employment.name, id, isPension))
            case _ => throw new RuntimeException("Not able to find employment")
          }
        }
  }

  def howToUpdatePage(id: Int): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>

        employmentService.employment(Nino(user.getNino), id) flatMap {
          case Some(employment: Employment) =>

            val incomeType = incomeTypeIdentifier(employment.receivingOccupationalPension)

            for {
              incomeToEdit: EmploymentAmount <- incomeService.employmentAmount(Nino(user.getNino), id)
              taxCodeIncomeDetails <- taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear())
              _ <- journeyCache(cacheMap = Map(
                UpdateIncome_NameKey -> employment.name,
                UpdateIncome_IdKey -> id.toString,
                UpdateIncome_IncomeTypeKey -> incomeType)
              )
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
      case (false, false, _) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      case _ => Redirect(controllers.routes.IncomeController.pensionIncome())
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
              case _ => Future.successful(Redirect(controllers.routes.IncomeController.viewIncomeForEdit()))
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
            formData.workingHours match {
              case Some("same") => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.payPeriodPage()))
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.calcUnavailablePage()))
            }
          }
        )
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

            val cacheMap = formData.otherInDays match {
              case Some(days) => Map(UpdateIncome_PayPeriodKey -> formData.payPeriod.getOrElse(""), UpdateIncome_OtherInDaysKey -> days.toString)
              case _ => Map(UpdateIncome_PayPeriodKey -> formData.payPeriod.getOrElse(""))
            }

            journeyCache(cacheMap = cacheMap) map { _ =>
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
              case PayslipForm(Some(value)) => journeyCache(UpdateIncome_TotalSalaryKey,Map(UpdateIncome_TotalSalaryKey -> value)) map { _ =>
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
                case Some(taxablePay) => journeyCache(UpdateIncome_TaxablePayKey, Map(UpdateIncome_TaxablePayKey -> taxablePay)) map { _ =>
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
                journeyCache(UpdateIncome_PayslipDeductionsKey, Map(UpdateIncome_PayslipDeductionsKey -> payslipDeductions)) map { _ =>
                  Redirect(routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage())
                }
              case Some(payslipDeductions) =>

                journeyCache(UpdateIncome_PayslipDeductionsKey, Map(UpdateIncome_PayslipDeductionsKey -> payslipDeductions)) map { _ =>
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

            val bonusPaymentsAnswer = formData.bonusPayments.fold(ifEmpty = Map.empty[String, String]){ bonusPayments =>
              Map(UpdateIncome_BonusPaymentsKey -> bonusPayments)
            }

            val yearlyBonusPaymentsAnswer = formData.bonusPaymentsMoreThisYear.fold(ifEmpty = Map.empty[String, String]) { bonusPayments =>
              Map(UpdateIncome_BonusPaymentsThisYearKey -> bonusPayments)
            }

            val cacheMap = bonusPaymentsAnswer ++ yearlyBonusPaymentsAnswer

            journeyCache(UpdateIncome_BonusPaymentsKey, cacheMap) map { _ =>
              if (formData.bonusPayments.contains("Yes")) {
                Redirect(routes.IncomeUpdateCalculatorController.bonusOvertimeAmountPage())
              } else {
                Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage())
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
          BonusOvertimeAmountForm.createForm(Some(BonusOvertimeAmountForm.bonusPaymentsAmountErrorMessage(moreThisYear, payPeriod)),
            Some(BonusOvertimeAmountForm.notAmountMessage(payPeriod))).bindFromRequest().fold(
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
                  journeyCache(UpdateIncome_BonusOvertimeAmountKey, Map(UpdateIncome_BonusOvertimeAmountKey -> amount)) map { _ =>
                    Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage())
                  }
                case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.checkYourAnswersPage()))
              }
            }
          )
        }
  }

  def checkYourAnswersPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        journeyCacheService.collectedValues(
          Seq(UpdateIncome_NameKey, UpdateIncome_PayPeriodKey, UpdateIncome_TotalSalaryKey, UpdateIncome_PayslipDeductionsKey,
            UpdateIncome_BonusPaymentsKey),
          Seq(UpdateIncome_TaxablePayKey, UpdateIncome_BonusPaymentsThisYearKey, UpdateIncome_BonusOvertimeAmountKey)
        ) map tupled { (mandatorySeq, optionalSeq) => {
          val viewModel = CheckYourAnswersViewModel(mandatorySeq(1), mandatorySeq(2), mandatorySeq(3),
            optionalSeq(0), mandatorySeq(4), optionalSeq(1), optionalSeq(2))
          Ok(views.html.incomes.estimatedPayment.update.checkYourAnswers(viewModel, mandatorySeq(0)))
        }
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

            journeyCache(cacheMap = cache) map { _ =>
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

  private def incomeTypeIdentifier(isPension: Boolean): String = {
    if (isPension) { TaiConstants.IncomeTypePension } else { TaiConstants.IncomeTypeEmployment }
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
