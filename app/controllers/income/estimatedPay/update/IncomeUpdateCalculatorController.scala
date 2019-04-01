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

package controllers.income.estimatedPay.update

import com.google.inject.Inject
import com.google.inject.name.Named
import controllers.{ServiceCheckLite, TaiBaseController}
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{Employment, Payment, PensionIncome}
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants.MONTH_AND_YEAR
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update._
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, EditIncomeIrregularHoursViewModel}
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService

import scala.Function.tupled
import scala.concurrent.Future

class IncomeUpdateCalculatorController @Inject()(incomeService: IncomeService,
                                                 employmentService: EmploymentService,
                                                 taxAccountService: TaxAccountService,
                                                 personService: PersonService,
                                                 estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
                                                 val auditConnector: AuditConnector,
                                                 val delegationConnector: DelegationConnector,
                                                 val authConnector: AuthConnector,
                                                 @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
                                                 override implicit val partialRetriever: FormPartialRetriever,
                                                 override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants
  with EditIncomeIrregularPayConstants
  with UpdatedEstimatedPayJourneyCache
  with FormValuesConstants
  with FeatureTogglesConfig {

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
            val incomeToEditFuture = incomeService.employmentAmount(Nino(user.getNino), id)
            val taxCodeIncomeDetailsFuture = taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear())

            for {
              incomeToEdit: EmploymentAmount <- incomeToEditFuture
              taxCodeIncomeDetails <- taxCodeIncomeDetailsFuture
              _ <- {
                journeyCache(cacheMap = Map(
                  UpdateIncome_NameKey -> employment.name,
                  UpdateIncome_IdKey -> id.toString,
                  UpdateIncome_IncomeTypeKey -> incomeType)
                )
              }
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
          (formData: HoursWorkedForm) => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
            } yield formData.workingHours match {
              case Some(REGULAR_HOURS) => Redirect(routes.IncomeUpdateCalculatorController.payPeriodPage())
              case Some(IRREGULAR_HOURS) => Redirect(routes.IncomeUpdateCalculatorController.editIncomeIrregularHours(id))
            }
          }
        )
  }


  private val taxCodeIncomeInfoToCache = (taxCodeIncome: TaxCodeIncome, payment: Option[Payment]) => {
    val defaultCaching = Map[String, String](
      UpdateIncome_NameKey -> taxCodeIncome.name,
      UpdateIncome_PayToDateKey -> taxCodeIncome.amount.toString
    )

    payment.fold(defaultCaching)(payment => defaultCaching + (UpdateIncome_DateKey -> payment.date.toString(MONTH_AND_YEAR)))
  }

  def editIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request => {
        val paymentRequest: Future[Option[Payment]] = incomeService.latestPayment(Nino(user.getNino), employmentId)
        val taxCodeIncomeRequest = taxAccountService.taxCodeIncomeForEmployment(Nino(user.getNino), TaxYear(), employmentId)


        paymentRequest flatMap { payment =>
          taxCodeIncomeRequest flatMap {
            case Some(tci) => {
              (taxCodeIncomeInfoToCache.tupled andThen journeyCacheService.cache) (tci, payment) map { _ =>
                val viewModel = EditIncomeIrregularHoursViewModel(employmentId, tci.name, tci.amount)

                Ok(views.html.incomes.editIncomeIrregularHours(AmountComparatorForm.createForm(), viewModel))
              }
            }
            case None => throw new RuntimeException(s"Not able to find employment with id $employmentId")
          }
        }
      }
  }

  def handleIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request => {
          journeyCacheService.currentCache flatMap { cache =>
            val name = cache(UpdateIncome_NameKey)
            val paymentToDate: String = cache(UpdateIncome_PayToDateKey)
            val latestPayDate = cache.get(UpdateIncome_DateKey)

            AmountComparatorForm.createForm(latestPayDate, Some(paymentToDate.toInt)).bindFromRequest().fold(

              formWithErrors => {
                val viewModel = EditIncomeIrregularHoursViewModel(employmentId, name, paymentToDate)
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

  def confirmIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request => {
          journeyCacheService.collectedValues(Seq(UpdateIncome_NameKey, UpdateIncome_IrregularAnnualPayKey, UpdateIncome_PayToDateKey), Seq(UpdateIncome_ConfirmedNewAmountKey)) map tupled { (mandatoryCache, optionalCache) =>
            val name :: newIrregularPay :: paymentToDate :: Nil = mandatoryCache.toList
            val confirmedNewAmount = optionalCache.head
            if (FormHelper.areEqual(confirmedNewAmount, Some(newIrregularPay))) {
              Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache())
            } else if (FormHelper.areEqual(Some(paymentToDate), Some(newIrregularPay))) {
              Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
            } else {
              val vm = ConfirmAmountEnteredViewModel.irregularPayCurrentYear(employmentId, name, newIrregularPay.toInt)
              Ok(views.html.incomes.confirmAmountEntered(vm))
            }
          }
        }
  }

  def submitIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>

          val updateJourneyCompletion: String => Future[Map[String, String]] = (incomeId: String) => {
            estimatedPayJourneyCompletionService.journeyCompleted(incomeId)
          }

          val cacheAndRespond = (incomeName: String, incomeId: String, newPay: String) => {
            journeyCacheService.cache(UpdateIncome_ConfirmedNewAmountKey, newPay) map { _ =>
              if (confirmedAPIEnabled) {

                Ok(views.html.incomes.editSuccess(incomeName, incomeId.toInt))
              } else {
                Ok(views.html.incomes.oldEditSuccess(incomeName, incomeId.toInt))
              }
            }

          }

          journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IrregularAnnualPayKey, UpdateIncome_IdKey).flatMap(cache => {
            val incomeName :: newPay :: incomeId :: Nil = cache.toList

            taxAccountService.updateEstimatedIncome(Nino(user.getNino), newPay.toInt, TaxYear(), employmentId) flatMap {
              case TaiSuccessResponse => {
                updateJourneyCompletion(incomeId) flatMap { _ =>
                  cacheAndRespond(incomeName, incomeId, newPay)
                }
              }
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
            val isDaysError = formWithErrors.errors.exists { error => error.key == PayPeriodForm.OTHER_IN_DAYS_KEY }
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.payPeriod(formWithErrors, id, employerName, !isDaysError))
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

  def payslipAmountPage: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          sendActingAttorneyAuditEvent("getPayslipAmountPage")

          val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
          val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey)

          journeyCacheService.collectedValues(mandatoryKeys, optionalKeys) map
            tupled {
              (mandatorySeq, optionalSeq) => {
                val viewModel = {
                  val id = mandatorySeq(0).toInt
                  val employerName = mandatorySeq(1)

                  val payPeriod = optionalSeq(0)
                  val payPeriodInDays = optionalSeq(1)
                  val errorMessage = "tai.payslip.error.form.totalPay.input.mandatory"
                  PaySlipAmountViewModel(PayslipForm.createForm(errorMessage), payPeriod, payPeriodInDays, id, employerName)
                }

                Ok(views.html.incomes.payslipAmount(viewModel))
              }
            }
  }

  def handlePayslipAmount: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processPayslipAmount")

        val result: Future[Future[Result]] = for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          payPeriod <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
          payPeriodInDays <- journeyCacheService.currentValue(UpdateIncome_OtherInDaysKey)
        } yield {
          val errorMessage = CommonPayPeriodTitle.title(payPeriod, payPeriodInDays)
          PayslipForm.createForm(errorMessage).bindFromRequest().fold(
            formWithErrors => {
              val viewModel = PaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, id, employerName)
              Future.successful(BadRequest(views.html.incomes.payslipAmount(viewModel)))
            },
            formData => {
              formData match {
                case PayslipForm(Some(value)) =>
                   journeyCache(UpdateIncome_TotalSalaryKey, Map(UpdateIncome_TotalSalaryKey -> value)) map { _ =>
                     Redirect(routes.IncomeUpdateCalculatorController.payslipDeductionsPage())
                   }
                case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.payslipDeductionsPage()))
              }
            }
          )
        }

    result.flatMap(identity)
  }

  def taxablePayslipAmountPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getTaxablePayslipAmountPage")

        val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
        val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey)

        journeyCacheService.collectedValues(mandatoryKeys, optionalKeys) map
          tupled {
            (mandatorySeq, optionalSeq) => {
              val viewModel = {
                val id = mandatorySeq(0).toInt
                val employerName = mandatorySeq(1)

                val payPeriod = optionalSeq(0)
                val payPeriodInDays = optionalSeq(1)
                TaxablePaySlipAmountViewModel(TaxablePayslipForm.createForm(), payPeriod, payPeriodInDays, id, employerName)
              }
              Ok(views.html.incomes.taxablePayslipAmount(viewModel))
            }
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

              val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
              val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey)

              journeyCacheService.collectedValues(mandatoryKeys, optionalKeys) map
                tupled {
                  (mandatorySeq, optionalSeq) => {
                    val viewModel = {
                      val id = mandatorySeq(0).toInt
                      val employerName = mandatorySeq(1)

                      val payPeriod = optionalSeq(0)
                      val payPeriodInDays = optionalSeq(1)
                      TaxablePaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, id, employerName)
                    }
                    BadRequest(views.html.incomes.taxablePayslipAmount(viewModel))
                  }
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
        journeyCacheService.mandatoryValues(UpdateIncome_IdKey, UpdateIncome_NameKey) map {
          mandatoryValues =>
            Ok(views.html.incomes.bonusPayments(BonusPaymentsForm.createForm, mandatoryValues(0).toInt, mandatoryValues(1)))
        }
  }

  def handleBonusPayments: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processBonusPayments")
        BonusPaymentsForm.createForm.bindFromRequest().fold(
          formWithErrors => {
            journeyCacheService.mandatoryValues(UpdateIncome_IdKey, UpdateIncome_NameKey) map {
              mandatoryValues =>
                BadRequest(views.html.incomes.bonusPayments(formWithErrors, mandatoryValues(0).toInt, mandatoryValues(1)))
            }
          },
          formData => {
            val bonusPaymentsAnswer = formData.yesNoChoice.fold(ifEmpty = Map.empty[String, String]) { bonusPayments =>
              Map(UpdateIncome_BonusPaymentsKey -> bonusPayments)
            }

            journeyCache(UpdateIncome_BonusPaymentsKey, bonusPaymentsAnswer) map { _ =>
              if (formData.yesNoChoice.contains(YesValue)) {
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
        journeyCacheService.mandatoryValues(UpdateIncome_IdKey, UpdateIncome_NameKey) map {
          mandatoryValues =>
            Ok(views.html.incomes.bonusPaymentAmount(BonusOvertimeAmountForm.createForm(), mandatoryValues(0).toInt, mandatoryValues(1)))
        }
  }

  def handleBonusOvertimeAmount: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("processBonusOvertimeAmount")
        BonusOvertimeAmountForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            journeyCacheService.mandatoryValues(UpdateIncome_IdKey, UpdateIncome_NameKey) map {
              mandatoryValues =>
                BadRequest(views.html.incomes.bonusPaymentAmount(formWithErrors, mandatoryValues(0).toInt, mandatoryValues(1)))
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

  def checkYourAnswersPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        journeyCacheService.collectedValues(
          Seq(UpdateIncome_NameKey, UpdateIncome_PayPeriodKey, UpdateIncome_TotalSalaryKey, UpdateIncome_PayslipDeductionsKey,
            UpdateIncome_BonusPaymentsKey),
          Seq(UpdateIncome_TaxablePayKey, UpdateIncome_BonusOvertimeAmountKey, UpdateIncome_OtherInDaysKey)
        ) map tupled { (mandatorySeq, optionalSeq) => {

          val incomeId = mandatorySeq(0)
          val payPeriodFrequency = mandatorySeq(1)
          val totalSalaryAmount = mandatorySeq(2)
          val hasPayslipDeductions = mandatorySeq(3)
          val taxablePay = optionalSeq(0)
          val hasBonusPayments = mandatorySeq(4)
          val bonusPaymentAmount = optionalSeq(1)
          val payPeriodInDays = optionalSeq(2)

          val viewModel = CheckYourAnswersViewModel(payPeriodFrequency, payPeriodInDays, totalSalaryAmount, hasPayslipDeductions,
            taxablePay, hasBonusPayments, bonusPaymentAmount)

          Ok(views.html.incomes.estimatedPayment.update.checkYourAnswers(viewModel, incomeId))
        }
        }
  }

  private def isCachedAmountSameAsEnteredAmount(cache: Map[String, String], newAmount: Option[BigDecimal]): Boolean

  = {
    FormHelper.areEqual(cache.get(UpdateIncome_ConfirmedNewAmountKey), newAmount map (_.toString()))
  }

  def estimatedPayPage: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        sendActingAttorneyAuditEvent("getEstimatedPayPage")

        val result: Future[Future[Result]] = for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          income <- incomeService.employmentAmount(Nino(user.getNino), id)
          cache <- journeyCacheService.currentCache
          calculatedPay <- incomeService.calculateEstimatedPay(cache, income.startDate)
          payment <- incomeService.latestPayment(Nino(user.getNino), id)
        } yield {

          val payYearToDate: BigDecimal = payment.map(_.amountYearToDate).getOrElse(BigDecimal(0))
          val paymentDate: Option[LocalDate] = payment.map(_.date)

          calculatedPay.grossAnnualPay match {
            case newAmount if (isCachedAmountSameAsEnteredAmount(cache, newAmount)) =>
              Future.successful(Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache()))
            case Some(newAmount) if newAmount > payYearToDate =>
              val cache = Map(UpdateIncome_GrossAnnualPayKey -> calculatedPay.grossAnnualPay.map(_.toString).getOrElse(""),
                UpdateIncome_NewAmountKey -> calculatedPay.netAnnualPay.map(_.toString).getOrElse(""))

              val isBonusPayment = cache.getOrElse(UpdateIncome_BonusPaymentsKey, "") == "Yes"

              journeyCache(cacheMap = cache) map { _ =>

                val viewModel = EstimatedPayViewModel(calculatedPay.grossAnnualPay, calculatedPay.netAnnualPay, id, isBonusPayment,
                  calculatedPay.annualAmount, calculatedPay.startDate, employerName)

                Ok(views.html.incomes.estimatedPay(viewModel))
              }
            case _ => Future.successful(Ok(views.html.incomes.incorrectTaxableIncome(payYearToDate, paymentDate.getOrElse(new LocalDate), id)))
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
          val convertedNetAmount = netAmount.map(BigDecimal(_).intValue()).getOrElse(income.oldAmount)
          val employmentAmount = income.copy(newAmount = convertedNetAmount)

          if(employmentAmount.newAmount == income.oldAmount) {
            Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
          } else {
            Ok(views.html.incomes.confirm_save_Income(EditIncomeForm.create(preFillData = employmentAmount).get))
          }
        }
  }

  private def incomeTypeIdentifier(isPension: Boolean): String

  = {
    if (isPension) {
      TaiConstants.IncomeTypePension
    } else {
      TaiConstants.IncomeTypeEmployment
    }
  }
}
