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

import cats.data.{EitherT, OptionT}
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import controllers.{ErrorPagesHandler, TaiBaseController}
import cats.implicits._
import play.api.Logger
import play.api.i18n.Messages

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
import uk.gov.hmrc.tai.util.FutureOps._

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

  val logger = Logger(this.getClass)

  def onPageLoad(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val estimatedPayCompletionFuture = estimatedPayJourneyCompletionService.hasJourneyCompleted(id.toString)
    val cacheEmploymentDetailsFuture =
      cacheEmploymentDetails(id, employmentService.employment(request.taiUser.nino, id))

    (for {
      estimatedPayCompletion <- estimatedPayCompletionFuture
      _                      <- cacheEmploymentDetailsFuture
    } yield {

      if (estimatedPayCompletion) {
        Redirect(routes.IncomeUpdateCalculatorController.duplicateSubmissionWarningPage(id))
      } else {
        Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage(id))
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

  def duplicateSubmissionWarningPage(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.mandatoryJourneyValues(
        UpdateIncome_NameKey,
        UpdateIncome_IdKey,
        s"$UpdateIncome_ConfirmedNewAmountKey-$empId",
        UpdateIncome_IncomeTypeKey) map {
        case Right(mandatoryValues) =>
          val incomeName :: incomeId :: previouslyUpdatedAmount :: incomeType :: Nil = mandatoryValues.toList
          val vm = if (incomeType == TaiConstants.IncomeTypePension) {
            DuplicateSubmissionPensionViewModel(incomeName, previouslyUpdatedAmount.toInt)
          } else {
            DuplicateSubmissionEmploymentViewModel(incomeName, previouslyUpdatedAmount.toInt)
          }
          Ok(duplicateSubmissionWarning(DuplicateSubmissionWarningForm.createForm, vm, incomeId.toInt))
        case Left(message) => errorPagesHandler.internalServerError(message)
      }
  }

  def submitDuplicateSubmissionWarning(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService
        .mandatoryJourneyValues(
          UpdateIncome_NameKey,
          s"$UpdateIncome_ConfirmedNewAmountKey-$empId",
          UpdateIncome_IncomeTypeKey)
        .getOrFail
        .map { mandatoryJourneyValues =>
          val incomeName :: newAmount :: incomeType :: _ = mandatoryJourneyValues.toList

          DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
            formWithErrors => {
              val vm = if (incomeType == TaiConstants.IncomeTypePension) {
                DuplicateSubmissionPensionViewModel(incomeName, newAmount.toInt)
              } else {
                DuplicateSubmissionEmploymentViewModel(incomeName, newAmount.toInt)
              }

              BadRequest(duplicateSubmissionWarning(formWithErrors, vm, empId))
            },
            success => {
              success.yesNoChoice match {
                case Some(YesValue) =>
                  Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage(empId))
                case Some(NoValue) =>
                  Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
              }
            }
          )
        }
  }

  def checkYourAnswersPage(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val collectedValues = journeyCacheService
        .collectedJourneyValues(
          Seq(
            UpdateIncome_NameKey,
            UpdateIncome_PayPeriodKey,
            UpdateIncome_TotalSalaryKey,
            UpdateIncome_PayslipDeductionsKey,
            UpdateIncome_BonusPaymentsKey,
            UpdateIncome_IdKey
          ),
          Seq(UpdateIncome_TaxablePayKey, UpdateIncome_BonusOvertimeAmountKey, UpdateIncome_OtherInDaysKey)
        )

      collectedValues.map {
        case Left(errorMessage) =>
          logger.warn(errorMessage)
          Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url)
        case Right((mandatorySeq, optionalSeq)) =>
          val employer = IncomeSource(id = mandatorySeq(5).toInt, name = mandatorySeq(0))
          val payPeriodFrequency = mandatorySeq(1)
          val totalSalaryAmount = mandatorySeq(2)
          val hasPayslipDeductions = mandatorySeq(3)
          val hasBonusPayments = mandatorySeq(4)

          val taxablePay = optionalSeq.head
          val bonusPaymentAmount = optionalSeq(1)
          val payPeriodInDays = optionalSeq(2)

          val backUrl = bonusPaymentAmount match {
            case None =>
              controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url
            case _ =>
              controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusOvertimeAmountPage().url
          }

          val viewModel = CheckYourAnswersViewModel(
            payPeriodFrequency,
            payPeriodInDays,
            totalSalaryAmount,
            hasPayslipDeductions,
            taxablePay,
            hasBonusPayments,
            bonusPaymentAmount,
            employer,
            backUrl
          )

          Ok(checkYourAnswers(viewModel))
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

        val vm = ConfirmAmountEnteredViewModel(
          employmentName,
          employmentAmount.oldAmount,
          employmentAmount.newAmount,
          controllers.income.estimatedPay.update.routes.IncomeUpdateEstimatedPayController.estimatedPayPage(id).url,
          id
        )
        Ok(confirmAmountEntered(vm))
      }
    }).fold(_ => Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(1).url), identity _)
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
