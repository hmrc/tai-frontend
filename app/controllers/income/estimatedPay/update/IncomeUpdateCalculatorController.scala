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

import cats.data.EitherT
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import controllers.{ErrorPagesHandler, TaiBaseController}
import cats.implicits._
import javax.inject.{Inject, Named}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update._
import views.html.incomes.estimatedPayment.update.CheckYourAnswersView
import views.html.incomes.{ConfirmAmountEnteredView, DuplicateSubmissionWarningView}

import scala.Function.tupled
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeUpdateCalculatorController @Inject()(
  incomeService: IncomeService,
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService,
  estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  duplicateSubmissionWarning: DuplicateSubmissionWarningView,
  checkYourAnswers: CheckYourAnswersView,
  confirmAmountEntered: ConfirmAmountEnteredView,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with EditIncomeIrregularPayConstants
    with UpdatedEstimatedPayJourneyCache with FormValuesConstants {

  def onPageLoad(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val estimatedPayCompletionFuture = estimatedPayJourneyCompletionService.hasJourneyCompleted(id.toString)
    val cacheEmploymentDetailsFuture =
      cacheEmploymentDetails(id, employmentService.employment(request.taiUser.nino, id))

    (for {
      estimatedPayCompletion <- estimatedPayCompletionFuture
      _                      <- cacheEmploymentDetailsFuture
    } yield {

      if (estimatedPayCompletion) {
        Redirect(routes.IncomeUpdateCalculatorController.duplicateSubmissionWarningPage())
      } else {
        Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage())
      }
    }).recover {
      case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
    }
  }

  private def cacheEmploymentDetails(id: Int, employmentFuture: Future[Option[Employment]])(
    implicit hc: HeaderCarrier): Future[Map[String, String]] =
    employmentFuture flatMap {
      case Some(employment) =>
        val incomeType = incomeTypeIdentifier(employment.receivingOccupationalPension)
        journeyCache(
          cacheMap = Map(
            UpdateIncome_NameKey       -> employment.name,
            UpdateIncome_IdKey         -> id.toString,
            UpdateIncome_IncomeTypeKey -> incomeType))
      case _ => throw new RuntimeException("Not able to find employment")
    }

  def duplicateSubmissionWarningPage(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.mandatoryJourneyValues(
        UpdateIncome_NameKey,
        UpdateIncome_IdKey,
        UpdateIncome_ConfirmedNewAmountKey,
        UpdateIncome_IncomeTypeKey) map {
        case Right(incomeName :: incomeId :: previouslyUpdatedAmount :: incomeType :: Nil) =>
          val vm = if (incomeType == TaiConstants.IncomeTypePension) {
            DuplicateSubmissionPensionViewModel(incomeName, previouslyUpdatedAmount.toInt)
          } else {
            DuplicateSubmissionEmploymentViewModel(incomeName, previouslyUpdatedAmount.toInt)
          }
          Ok(duplicateSubmissionWarning(DuplicateSubmissionWarningForm.createForm, vm, incomeId.toInt))
        case Left(message) => errorPagesHandler.internalServerError(message)
      }
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.mandatoryValues(
        UpdateIncome_NameKey,
        UpdateIncome_IdKey,
        UpdateIncome_ConfirmedNewAmountKey,
        UpdateIncome_IncomeTypeKey) flatMap { mandatoryValues =>
        val incomeName :: incomeId :: newAmount :: incomeType :: Nil = mandatoryValues.toList

        DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
          formWithErrors => {
            val vm = if (incomeType == TaiConstants.IncomeTypePension) {
              DuplicateSubmissionPensionViewModel(incomeName, newAmount.toInt)
            } else {
              DuplicateSubmissionEmploymentViewModel(incomeName, newAmount.toInt)
            }

            Future.successful(BadRequest(duplicateSubmissionWarning(formWithErrors, vm, incomeId.toInt)))
          },
          success => {
            success.yesNoChoice match {
              case Some(YesValue) =>
                Future.successful(Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage()))
              case Some(NoValue) =>
                Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(incomeId.toInt)))
            }
          }
        )
      }
  }

  def checkYourAnswersPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    journeyCacheService.collectedValues(
      Seq(
        UpdateIncome_NameKey,
        UpdateIncome_PayPeriodKey,
        UpdateIncome_TotalSalaryKey,
        UpdateIncome_PayslipDeductionsKey,
        UpdateIncome_BonusPaymentsKey,
        UpdateIncome_IdKey
      ),
      Seq(UpdateIncome_TaxablePayKey, UpdateIncome_BonusOvertimeAmountKey, UpdateIncome_OtherInDaysKey)
    ) map tupled { (mandatorySeq, optionalSeq) =>
      {

        val employer = IncomeSource(id = mandatorySeq(5).toInt, name = mandatorySeq(0))
        val payPeriodFrequency = mandatorySeq(1)
        val totalSalaryAmount = mandatorySeq(2)
        val hasPayslipDeductions = mandatorySeq(3)
        val hasBonusPayments = mandatorySeq(4)

        val taxablePay = optionalSeq(0)
        val bonusPaymentAmount = optionalSeq(1)
        val payPeriodInDays = optionalSeq(2)

        val viewModel = CheckYourAnswersViewModel(
          payPeriodFrequency,
          payPeriodInDays,
          totalSalaryAmount,
          hasPayslipDeductions,
          taxablePay,
          hasBonusPayments,
          bonusPaymentAmount,
          employer)

        Ok(checkYourAnswers(viewModel))
      }
    }
  }

  def handleCalculationResult: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser
    val nino = user.nino

    (for {
      employmentName <- EitherT(journeyCacheService.mandatoryJourneyValue(UpdateIncome_NameKey))
      id             <- EitherT(journeyCacheService.mandatoryJourneyValueAsInt(UpdateIncome_IdKey))
      income         <- EitherT.right[String](incomeService.employmentAmount(nino, id))
      netAmount      <- EitherT.right[String](journeyCacheService.currentValue(UpdateIncome_NewAmountKey))
    } yield {
      val convertedNetAmount = netAmount.map(BigDecimal(_).intValue()).getOrElse(income.oldAmount)
      val employmentAmount = income.copy(newAmount = convertedNetAmount)

      if (employmentAmount.newAmount == income.oldAmount) {
        Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
      } else {

        val vm = ConfirmAmountEnteredViewModel(employmentName, employmentAmount.oldAmount, employmentAmount.newAmount)
        Ok(confirmAmountEntered(vm))
      }
    }).fold(errorPagesHandler.internalServerError(_, None), identity _)
      .recover {
        case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
      }

  }

  private def incomeTypeIdentifier(isPension: Boolean): String =
    if (isPension) {
      TaiConstants.IncomeTypePension
    } else {
      TaiConstants.IncomeTypeEmployment
    }
}
