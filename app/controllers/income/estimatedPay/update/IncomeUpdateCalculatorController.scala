/*
 * Copyright 2023 HM Revenue & Customs
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
import cats.implicits._
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.FutureOps._
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update._
import views.html.incomes.estimatedPayment.update.CheckYourAnswersView
import views.html.incomes.{ConfirmAmountEnteredView, DuplicateSubmissionWarningView}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeUpdateCalculatorController @Inject() (
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
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with UpdatedEstimatedPayJourneyCache {

  val logger: Logger = Logger(this.getClass)

  def onPageLoad(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    (
      estimatedPayJourneyCompletionService.hasJourneyCompleted(id.toString),
      employmentService.employment(request.taiUser.nino, id).flatMap(cacheEmploymentDetails(id))
    ).mapN {
      case (true, _) =>
        Redirect(routes.IncomeUpdateCalculatorController.duplicateSubmissionWarningPage(id))
      case _ =>
        Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage(id))
    }.recover { case NonFatal(e) =>
      errorPagesHandler.internalServerError(e.getMessage)
    }
  }

  private def cacheEmploymentDetails(
    id: Int
  )(maybeEmployment: Option[Employment])(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    maybeEmployment match {
      case Some(employment) =>
        val incomeType = incomeTypeIdentifier(employment.receivingOccupationalPension)
        journeyCache(
          cacheMap = Map(
            UpdateIncomeConstants.NameKey       -> employment.name,
            UpdateIncomeConstants.IdKey         -> id.toString,
            UpdateIncomeConstants.IncomeTypeKey -> incomeType
          )
        )
      case _ =>
        Future.failed(new RuntimeException("Not able to find employment"))
    }

  def duplicateSubmissionWarningPage(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.mandatoryJourneyValues(
        UpdateIncomeConstants.NameKey,
        UpdateIncomeConstants.IdKey,
        s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$empId",
        UpdateIncomeConstants.IncomeTypeKey
      ) map {
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
          UpdateIncomeConstants.NameKey,
          s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$empId",
          UpdateIncomeConstants.IncomeTypeKey
        )
        .getOrFail
        .map { mandatoryJourneyValues =>
          val incomeName :: newAmount :: incomeType :: _ = mandatoryJourneyValues.toList

          DuplicateSubmissionWarningForm.createForm
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val vm = if (incomeType == TaiConstants.IncomeTypePension) {
                  DuplicateSubmissionPensionViewModel(incomeName, newAmount.toInt)
                } else {
                  DuplicateSubmissionEmploymentViewModel(incomeName, newAmount.toInt)
                }

                BadRequest(duplicateSubmissionWarning(formWithErrors, vm, empId))
              },
              success =>
                success.yesNoChoice match {
                  case Some(FormValuesConstants.YesValue) =>
                    Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage(empId))
                  case Some(FormValuesConstants.NoValue) =>
                    Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
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
            UpdateIncomeConstants.NameKey,
            UpdateIncomeConstants.PayPeriodKey,
            UpdateIncomeConstants.TotalSalaryKey,
            UpdateIncomeConstants.PayslipDeductionsKey,
            UpdateIncomeConstants.BonusPaymentsKey,
            UpdateIncomeConstants.IdKey
          ),
          Seq(
            UpdateIncomeConstants.TaxablePayKey,
            UpdateIncomeConstants.BonusOvertimeAmountKey,
            UpdateIncomeConstants.OtherInDaysKey
          )
        )

      collectedValues.map {
        case Left(errorMessage) =>
          logger.warn(errorMessage)
          Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url)
        case Right((mandatorySeq, optionalSeq)) =>
          val employer = IncomeSource(id = mandatorySeq(5).toInt, name = mandatorySeq.head)
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

    val netAmountFuture = journeyCacheService.currentValue(UpdateIncomeConstants.NewAmountKey)

    (for {
      mandatoryValues <- EitherT(
                           journeyCacheService
                             .mandatoryJourneyValues(UpdateIncomeConstants.NameKey, UpdateIncomeConstants.IdKey)
                         )
      employmentName :: idStr :: _ = mandatoryValues.toList
      id = idStr.toInt
      income    <- EitherT.right[String](incomeService.employmentAmount(nino, id))
      netAmount <- EitherT.right[String](netAmountFuture)
    } yield {
      val convertedNetAmount = netAmount.map(BigDecimal(_).intValue).getOrElse(income.oldAmount)
      val employmentAmount = income.copy(newAmount = convertedNetAmount)

      if (employmentAmount.newAmount == income.oldAmount) {
        Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
      } else {
        Redirect(controllers.routes.IncomeController.updateEstimatedIncome(id))
      }
    }).getOrElse(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(1).url))
      .recover { case NonFatal(e) =>
        errorPagesHandler.internalServerError(e.getMessage)
      }

  }

  private def incomeTypeIdentifier(isPension: Boolean): String =
    if (isPension) {
      TaiConstants.IncomeTypePension
    } else {
      TaiConstants.IncomeTypeEmployment
    }
}
