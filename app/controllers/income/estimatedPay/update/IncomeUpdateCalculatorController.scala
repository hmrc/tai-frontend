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

import javax.inject.{Inject, Named}
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, Payment}
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.TaiConstants.MONTH_AND_YEAR
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.viewModels.GoogleAnalyticsSettings
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.{GrossPayPeriodTitle, _}
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, EditIncomeIrregularHoursViewModel}

import scala.Function.tupled
import scala.concurrent.Future
import scala.util.control.NonFatal

class IncomeUpdateCalculatorController @Inject()(incomeService: IncomeService,
                                                 employmentService: EmploymentService,
                                                 taxAccountService: TaxAccountService,
                                                 estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
                                                 authenticate: AuthAction,
                                                 validatePerson: ValidatePerson,
                                                 @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
                                                 override implicit val partialRetriever: FormPartialRetriever,
                                                 override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with JourneyCacheConstants
  with EditIncomeIrregularPayConstants
  with UpdatedEstimatedPayJourneyCache
  with FormValuesConstants
  with FeatureTogglesConfig {

  def onPageLoad(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      val estimatedPayCompletionFuture = estimatedPayJourneyCompletionService.hasJourneyCompleted(id.toString)
      val cacheEmploymentDetailsFuture = cacheEmploymentDetails(id, employmentService.employment(request.taiUser.nino, id))

      (for {
        estimatedPayCompletion <- estimatedPayCompletionFuture
        _ <- cacheEmploymentDetailsFuture
      } yield {

        if (estimatedPayCompletion) {
          Redirect(routes.IncomeUpdateCalculatorController.duplicateSubmissionWarningPage())
        } else {
          Redirect(routes.IncomeUpdateCalculatorController.estimatedPayLandingPage())
        }
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  private def cacheEmploymentDetails(id: Int, employmentFuture: Future[Option[Employment]])(implicit hc: HeaderCarrier): Future[Map[String, String]] = {
    employmentFuture flatMap {
      case Some(employment) =>
        val incomeType = incomeTypeIdentifier(employment.receivingOccupationalPension)
        journeyCache(cacheMap = Map(
          UpdateIncome_NameKey -> employment.name,
          UpdateIncome_IdKey -> id.toString,
          UpdateIncome_IncomeTypeKey -> incomeType))
      case _ => throw new RuntimeException("Not able to find employment")
    }
  }

 def duplicateSubmissionWarningPage(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser


        journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IdKey, UpdateIncome_ConfirmedNewAmountKey, UpdateIncome_IncomeTypeKey) map { mandatoryValues =>
          val incomeName :: incomeId :: previouslyUpdatedAmount :: incomeType :: Nil = mandatoryValues.toList

          val vm = if (incomeType == TaiConstants.IncomeTypePension) {
            DuplicateSubmissionPensionViewModel(incomeName, previouslyUpdatedAmount.toInt)
          } else {
            DuplicateSubmissionEmploymentViewModel(incomeName, previouslyUpdatedAmount.toInt)
          }

          Ok(views.html.incomes.duplicateSubmissionWarning(
            DuplicateSubmissionWarningForm.createForm, vm, incomeId.toInt)
          )
        }
  }

 def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser


        journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IdKey, UpdateIncome_ConfirmedNewAmountKey,  UpdateIncome_IncomeTypeKey) flatMap { mandatoryValues =>
          val incomeName :: incomeId :: newAmount :: incomeType :: Nil = mandatoryValues.toList

          DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
            formWithErrors => {
              val vm = if (incomeType == TaiConstants.IncomeTypePension) {
                DuplicateSubmissionPensionViewModel(incomeName, newAmount.toInt)
              } else {
                DuplicateSubmissionEmploymentViewModel(incomeName, newAmount.toInt)
              }

              Future.successful(BadRequest(views.html.incomes.
                duplicateSubmissionWarning(formWithErrors, vm, incomeId.toInt)))
            },
            success => {
              success.yesNoChoice match {
                case Some(YesValue) => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.estimatedPayLandingPage()))
                case Some(NoValue) => Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.
                  onPageLoad(incomeId.toInt)))
              }
            }
          )
        }
  }

  def estimatedPayLandingPage(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IdKey, UpdateIncome_IncomeTypeKey) map { mandatoryValues =>
        val incomeName :: incomeId :: incomeType :: Nil = mandatoryValues.toList
        Ok(views.html.incomes.estimatedPayLandingPage(
          incomeName,
          incomeId.toInt,
          incomeType == TaiConstants.IncomeTypePension
        )
        )
      }
  }

  def howToUpdatePage(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser
      val nino = user.nino

      (employmentService.employment(nino, id) flatMap {
        case Some(employment: Employment) =>

          val incomeToEditFuture = incomeService.employmentAmount(nino, id)
          val taxCodeIncomeDetailsFuture = taxAccountService.taxCodeIncomes(nino, TaxYear())
          val cacheEmploymentDetailsFuture = cacheEmploymentDetails(id, employmentService.employment(nino, id))

          for {
            incomeToEdit: EmploymentAmount <- incomeToEditFuture
            taxCodeIncomeDetails <- taxCodeIncomeDetailsFuture
            _ <- cacheEmploymentDetailsFuture
          } yield {
              processHowToUpdatePage(id, employment.name, incomeToEdit, taxCodeIncomeDetails)
          }
        case None => throw new RuntimeException("Not able to find employment")
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  def processHowToUpdatePage(id: Int, employmentName: String, incomeToEdit: EmploymentAmount,
                             taxCodeIncomeDetails: TaiResponse)(implicit request: Request[AnyContent], user: AuthedUser) = {
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

  def handleChooseHowToUpdate: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      HowToUpdateForm.createForm().bindFromRequest().fold(
        formWithErrors => {
          val employerFuture = IncomeSource.create(journeyCacheService)
          for {
            employer <- employerFuture
          } yield {
            BadRequest(views.html.incomes.howToUpdate(formWithErrors, employer.id, employer.name))
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

  def workingHoursPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val employerFuture = IncomeSource.create(journeyCacheService)
      for {
        employer <- employerFuture
      } yield {
        Ok(views.html.incomes.workingHours(HoursWorkedForm.createForm(), employer.id, employer.name))
      }
  }

  def handleWorkingHours: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      HoursWorkedForm.createForm().bindFromRequest().fold(
        formWithErrors => {
          val employerFuture = IncomeSource.create(journeyCacheService)
          for {
            employer <- employerFuture
          } yield {
            BadRequest(views.html.incomes.workingHours(formWithErrors, employer.id, employer.name))
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

  def editIncomeIrregularHours(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser
      val nino = user.nino

      val paymentRequest: Future[Option[Payment]] = incomeService.latestPayment(nino, employmentId)
      val taxCodeIncomeRequest = taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear(), employmentId)

      (paymentRequest flatMap { payment =>
        taxCodeIncomeRequest flatMap {
          case Some(tci) => {
            (taxCodeIncomeInfoToCache.tupled andThen journeyCacheService.cache) (tci, payment) map { _ =>
              val viewModel = EditIncomeIrregularHoursViewModel(employmentId, tci.name, tci.amount)

              Ok(views.html.incomes.editIncomeIrregularHours(AmountComparatorForm.createForm(), viewModel))
            }
          }
          case None => throw new RuntimeException(s"Not able to find employment with id $employmentId")
        }
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  def handleIncomeIrregularHours(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
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

  def confirmIncomeIrregularHours(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      val collectedValues = journeyCacheService.collectedValues(
        Seq(UpdateIncome_NameKey, UpdateIncome_IrregularAnnualPayKey, UpdateIncome_PayToDateKey),
        Seq(UpdateIncome_ConfirmedNewAmountKey))

      (for {
        (mandatoryCache, optionalCache) <- collectedValues
      } yield {
        val name :: newIrregularPay :: paymentToDate :: Nil = mandatoryCache.toList
        val confirmedNewAmount = optionalCache.head

        if (FormHelper.areEqual(confirmedNewAmount, Some(newIrregularPay))) {
          Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache())
        } else if (FormHelper.areEqual(Some(paymentToDate), Some(newIrregularPay))) {
          Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
        } else {
          val vm = ConfirmAmountEnteredViewModel.irregularPayCurrentYear(employmentId, name, paymentToDate.toInt, newIrregularPay.toInt)
          Ok(views.html.incomes.confirmAmountEntered(vm))
        }
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  def submitIncomeIrregularHours(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser
      val nino = user.nino

      val updateJourneyCompletion: String => Future[Map[String, String]] = (incomeId: String) => {
        estimatedPayJourneyCompletionService.journeyCompleted(incomeId)
      }

      val cacheAndRespond = (incomeName: String, incomeId: String, newPay: String) => {
        journeyCacheService.cache(UpdateIncome_ConfirmedNewAmountKey, newPay) map { _ =>
          Ok(views.html.incomes.editSuccess(incomeName, incomeId.toInt))
        }
      }

      journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IrregularAnnualPayKey, UpdateIncome_IdKey).flatMap(cache => {
        val incomeName :: newPay :: incomeId :: Nil = cache.toList

        taxAccountService.updateEstimatedIncome(nino, newPay.toInt, TaxYear(), employmentId) flatMap {
          case TaiSuccessResponse => {
            updateJourneyCompletion(incomeId) flatMap { _ =>
              cacheAndRespond(incomeName, incomeId, newPay)
            }
          }
          case _ => throw new RuntimeException(s"Not able to update estimated pay for $employmentId")
        }

      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  def payPeriodPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val employerFuture = IncomeSource.create(journeyCacheService)

      for {
        employer <- employerFuture
        payPeriod <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
        payPeriodInDays <- journeyCacheService.currentValue(UpdateIncome_OtherInDaysKey)
      } yield {
        val form: Form[PayPeriodForm] = PayPeriodForm.createForm(None).fill(PayPeriodForm(payPeriod, payPeriodInDays))
        Ok(views.html.incomes.payPeriod(form, employer.id, employer.name))
      }
  }

  def handlePayPeriod: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val payPeriod: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("payPeriod").flatMap(_.headOption))

      PayPeriodForm.createForm(None, payPeriod).bindFromRequest().fold(
        formWithErrors => {
          val employerFuture = IncomeSource.create(journeyCacheService)
          for {
            employer <- employerFuture
          } yield {
            val isDaysError = formWithErrors.errors.exists { error => error.key == PayPeriodForm.OTHER_IN_DAYS_KEY }
            BadRequest(views.html.incomes.payPeriod(formWithErrors, employer.id, employer.name, !isDaysError))
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

  def payslipAmountPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
      val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TotalSalaryKey)

      journeyCacheService.collectedValues(mandatoryKeys, optionalKeys) map
        tupled {
          (mandatorySeq, optionalSeq) => {
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

  def handlePayslipAmount: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val employerFuture = IncomeSource.create(journeyCacheService)

      val result: Future[Future[Result]] = for {
        employer <- employerFuture
        payPeriod <- journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
        payPeriodInDays <- journeyCacheService.currentValue(UpdateIncome_OtherInDaysKey)
      } yield {
        val errorMessage = GrossPayPeriodTitle.title(payPeriod, payPeriodInDays)
        PayslipForm.createForm(errorMessage).bindFromRequest().fold(
          formWithErrors => {
            val viewModel = PaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, employer)
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

  def taxablePayslipAmountPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser


      val mandatoryKeys = Seq(UpdateIncome_IdKey, UpdateIncome_NameKey)
      val optionalKeys = Seq(UpdateIncome_PayPeriodKey, UpdateIncome_OtherInDaysKey, UpdateIncome_TaxablePayKey)

      journeyCacheService.collectedValues(mandatoryKeys, optionalKeys) map
        tupled {
          (mandatorySeq, optionalSeq) => {
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

  def handleTaxablePayslipAmount: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val employerFuture = IncomeSource.create(journeyCacheService)

      val futurePayPeriod = journeyCacheService.currentValue(UpdateIncome_PayPeriodKey)
      val futurePayPeriodInDays = journeyCacheService.currentValue(UpdateIncome_OtherInDaysKey)
      val futureTotalSalary = journeyCacheService.currentValue(UpdateIncome_TotalSalaryKey)

      (for {
        employer <- employerFuture
        payPeriod <- futurePayPeriod
        payPeriodInDays <- futurePayPeriodInDays
        totalSalary <- futureTotalSalary
      } yield {
        TaxablePayslipForm.createForm(FormHelper.stripNumber(totalSalary), payPeriod, payPeriodInDays).bindFromRequest().fold(
          formWithErrors => {
            val viewModel = TaxablePaySlipAmountViewModel(formWithErrors, payPeriod, payPeriodInDays, employer)
            Future.successful(BadRequest(views.html.incomes.taxablePayslipAmount(viewModel)))
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
      }).flatMap(identity)
  }

  def payslipDeductionsPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val employerFuture = IncomeSource.create(journeyCacheService)

      for {
        employer <- employerFuture
        payslipDeductions <- journeyCacheService.currentValue(UpdateIncome_PayslipDeductionsKey)
      } yield {
        val form = PayslipDeductionsForm.createForm().fill(PayslipDeductionsForm(payslipDeductions))
        Ok(views.html.incomes.payslipDeductions(form, employer))
      }
  }

  def handlePayslipDeductions: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser


      PayslipDeductionsForm.createForm().bindFromRequest().fold(
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

  def bonusPaymentsPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val employerFuture = IncomeSource.create(journeyCacheService)

      for {
        employer <- employerFuture
        bonusPayment <- journeyCacheService.currentValue(UpdateIncome_BonusPaymentsKey)
      } yield {
        val form = BonusPaymentsForm.createForm.fill(YesNoForm(bonusPayment))
        Ok(views.html.incomes.bonusPayments(form, employer))
      }
  }

  def handleBonusPayments: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      BonusPaymentsForm.createForm.bindFromRequest().fold(
        formWithErrors => {
          val employerFuture = IncomeSource.create(journeyCacheService)
          for {
            employer <- employerFuture
          } yield {
            BadRequest(views.html.incomes.bonusPayments(formWithErrors, employer))
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

  def bonusOvertimeAmountPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val employerFuture = IncomeSource.create(journeyCacheService)
      for {
        employer <- employerFuture
        bonusOvertimeAmount <- journeyCacheService.currentValue(UpdateIncome_BonusOvertimeAmountKey)
      } yield {
        val form = BonusOvertimeAmountForm.createForm().fill(BonusOvertimeAmountForm(bonusOvertimeAmount))
        Ok(views.html.incomes.bonusPaymentAmount(form, employer))
      }
  }

  def handleBonusOvertimeAmount: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      BonusOvertimeAmountForm.createForm().bindFromRequest().fold(
        formWithErrors => {
          val employerFuture = IncomeSource.create(journeyCacheService)
          for {
            employer <- employerFuture
          } yield {
            BadRequest(views.html.incomes.bonusPaymentAmount(formWithErrors, employer))
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

  def checkYourAnswersPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.collectedValues(
        Seq(UpdateIncome_NameKey, UpdateIncome_PayPeriodKey, UpdateIncome_TotalSalaryKey, UpdateIncome_PayslipDeductionsKey,
          UpdateIncome_BonusPaymentsKey, UpdateIncome_IdKey),
        Seq(UpdateIncome_TaxablePayKey, UpdateIncome_BonusOvertimeAmountKey, UpdateIncome_OtherInDaysKey)
      ) map tupled { (mandatorySeq, optionalSeq) => {

        val employer = IncomeSource(id = mandatorySeq(5).toInt, name = mandatorySeq(0))
        val payPeriodFrequency = mandatorySeq(1)
        val totalSalaryAmount = mandatorySeq(2)
        val hasPayslipDeductions = mandatorySeq(3)
        val hasBonusPayments = mandatorySeq(4)

        val taxablePay = optionalSeq(0)
        val bonusPaymentAmount = optionalSeq(1)
        val payPeriodInDays = optionalSeq(2)

        val viewModel = CheckYourAnswersViewModel(
          payPeriodFrequency, payPeriodInDays, totalSalaryAmount, hasPayslipDeductions,
          taxablePay, hasBonusPayments, bonusPaymentAmount, employer)

        Ok(views.html.incomes.estimatedPayment.update.checkYourAnswers(viewModel))
      }
      }
  }

  private def isCachedAmountSameAsEnteredAmount(cache: Map[String, String], newAmount: Option[BigDecimal]): Boolean = {
    FormHelper.areEqual(cache.get(UpdateIncome_ConfirmedNewAmountKey), newAmount map (_.toString()))
  }

  def estimatedPayPage: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      val nino = user.nino

      val employerFuture = IncomeSource.create(journeyCacheService)

      val result: Future[Future[Result]] = for {
        employer <- employerFuture
        income <- incomeService.employmentAmount(nino, employer.id)
        cache <- journeyCacheService.currentCache
        calculatedPay <- incomeService.calculateEstimatedPay(cache, income.startDate)
        payment <- incomeService.latestPayment(nino, employer.id)
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

              val viewModel = EstimatedPayViewModel(calculatedPay.grossAnnualPay, calculatedPay.netAnnualPay, isBonusPayment,
                calculatedPay.annualAmount, calculatedPay.startDate, employer)

              Ok(views.html.incomes.estimatedPay(viewModel))
            }
          case _ => Future.successful(Ok(views.html.incomes.incorrectTaxableIncome(payYearToDate, paymentDate.getOrElse(new LocalDate), employer.id)))
        }
      }

      result.flatMap(identity)
  }

  def handleCalculationResult: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser
      val nino = user.nino

      (for {
        id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
        income <- incomeService.employmentAmount(nino, id)
        netAmount <- journeyCacheService.currentValue(UpdateIncome_NewAmountKey)
      } yield {
        val convertedNetAmount = netAmount.map(BigDecimal(_).intValue()).getOrElse(income.oldAmount)
        val employmentAmount = income.copy(newAmount = convertedNetAmount)

        if (employmentAmount.newAmount == income.oldAmount) {
          Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
        } else {

          val form = EditIncomeForm.create(preFillData = employmentAmount).get
          val gaSetting = GoogleAnalyticsSettings.createForAnnualIncome(GoogleAnalyticsConstants.taiCYEstimatedIncome, form.oldAmount, form.newAmount)

          Ok(views.html.incomes.confirm_save_Income(form, gaSetting))
        }
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }
  }

  private def incomeTypeIdentifier(isPension: Boolean): String = {
    if (isPension) {
      TaiConstants.IncomeTypePension
    } else {
      TaiConstants.IncomeTypeEmployment
    }
  }
}
