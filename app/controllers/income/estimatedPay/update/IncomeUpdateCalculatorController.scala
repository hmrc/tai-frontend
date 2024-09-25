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

import cats.implicits._
import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.TrackingJourneyConstantsEstimatedPayPage
import pages.income.{UpdateIncomeNamePage, _}
import play.api.Logger
import play.api.libs.json.Format.GenericFormat
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update._
import views.html.incomes.estimatedPayment.update.CheckYourAnswersView
import views.html.incomes.DuplicateSubmissionWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeUpdateCalculatorController @Inject() (
  incomeService: IncomeService,
  employmentService: EmploymentService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  duplicateSubmissionWarning: DuplicateSubmissionWarningView,
  checkYourAnswers: CheckYourAnswersView,
  journeyCacheNewRepository: JourneyCacheNewRepository,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  val logger: Logger = Logger(this.getClass)

  def onPageLoad(id: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val journeyCompleted =
      Future.successful(request.userAnswers.get(TrackingJourneyConstantsEstimatedPayPage(id)).contains("true"))

    (
      journeyCompleted,
      employmentService.employment(request.taiUser.nino, id).flatMap(cacheEmploymentDetails(id, request.userAnswers))
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
    id: Int,
    userAnswers: UserAnswers
  )(maybeEmployment: Option[Employment]): Future[UserAnswers] =
    maybeEmployment match {
      case Some(employment) =>
        val incomeType = incomeTypeIdentifier(employment.receivingOccupationalPension)
        val updatedUserAnswers = userAnswers
          .setOrException(UpdateIncomeNamePage, employment.name)
          .setOrException(UpdateIncomeIdPage, id)
          .setOrException(UpdateIncomeTypePage, incomeType)

        journeyCacheNewRepository.set(updatedUserAnswers).map(_ => updatedUserAnswers)

      case _ =>
        Future.failed(new RuntimeException("Not able to find employment"))
    }

  def duplicateSubmissionWarningPage(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val userAnswers = request.userAnswers

      val incomeNameOpt = userAnswers.get(UpdateIncomeNamePage)
      val incomeIdOpt = userAnswers.get(UpdateIncomeIdPage)
      val previouslyUpdatedAmountOpt = userAnswers.get(UpdateIncomeConfirmedNewAmountPage(empId))
      val incomeTypeOpt = userAnswers.get(UpdateIncomeTypePage)

      (incomeNameOpt, incomeIdOpt, previouslyUpdatedAmountOpt, incomeTypeOpt) match {
        case (Some(incomeName), Some(incomeId), Some(previouslyUpdatedAmount), Some(incomeType)) =>
          val vm = if (incomeType == TaiConstants.IncomeTypePension) {
            DuplicateSubmissionPensionViewModel(incomeName, previouslyUpdatedAmount.toInt)
          } else {
            DuplicateSubmissionEmploymentViewModel(incomeName, previouslyUpdatedAmount.toInt)
          }
          Future.successful(Ok(duplicateSubmissionWarning(DuplicateSubmissionWarningForm.createForm, vm, incomeId)))
        case _ => Future.successful(errorPagesHandler.internalServerError("Mandatory values missing"))
      }
  }

  def submitDuplicateSubmissionWarning(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val userAnswers = request.userAnswers

      val incomeNameOpt = userAnswers.get(UpdateIncomeNamePage)
      val newAmountOpt = userAnswers.get(UpdateIncomeConfirmedNewAmountPage(empId))
      val incomeTypeOpt = userAnswers.get(UpdateIncomeTypePage)

      (incomeNameOpt, newAmountOpt, incomeTypeOpt) match {
        case (Some(incomeName), Some(newAmount), Some(incomeType)) =>
          DuplicateSubmissionWarningForm.createForm
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val vm = if (incomeType == TaiConstants.IncomeTypePension) {
                  DuplicateSubmissionPensionViewModel(incomeName, newAmount.toInt)
                } else {
                  DuplicateSubmissionEmploymentViewModel(incomeName, newAmount.toInt)
                }
                Future.successful(BadRequest(duplicateSubmissionWarning(formWithErrors, vm, empId)))
              },
              success =>
                success.yesNoChoice match {
                  case Some(FormValuesConstants.YesValue) =>
                    Future
                      .successful(Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage(empId)))
                  case Some(FormValuesConstants.NoValue) =>
                    Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
                }
            )
        case _ => Future.successful(errorPagesHandler.internalServerError("Mandatory values missing"))
      }
  }

  // scalastyle:off method.length
  def checkYourAnswersPage(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val userAnswers = request.userAnswers

      val mandatoryJourneyValues = Seq(
        userAnswers.get(UpdateIncomeNamePage),
        userAnswers.get(UpdateIncomePayPeriodPage),
        userAnswers.get(UpdateIncomeTotalSalaryPage),
        userAnswers.get(UpdateIncomePayslipDeductionsPage),
        userAnswers.get(UpdateIncomeBonusPaymentsPage),
        userAnswers.get(UpdateIncomeIdPage)
      )
      val optionalSeq = Seq(
        userAnswers.get(UpdateIncomeTaxablePayPage),
        userAnswers.get(UpdateIncomeBonusOvertimeAmountPage),
        userAnswers.get(UpdateIncomeOtherInDaysPage)
      )

      (mandatoryJourneyValues, optionalSeq) match {
        case (mandatory, _) if mandatory.forall(_.isDefined) =>
          val employer =
            IncomeSource(id = mandatory(5).getOrElse("").toString.toInt, name = mandatory.head.getOrElse("").toString)
          val payPeriodFrequency = mandatory(1).getOrElse("")
          val totalSalaryAmount = mandatory(2).getOrElse("")
          val hasPayslipDeductions = mandatory(3).getOrElse("")
          val hasBonusPayments = mandatory(4).getOrElse("")

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
            payPeriodFrequency.toString,
            payPeriodInDays,
            totalSalaryAmount.toString,
            hasPayslipDeductions.toString,
            taxablePay,
            hasBonusPayments.toString,
            bonusPaymentAmount,
            employer,
            backUrl
          )

          Future.successful(Ok(checkYourAnswers(viewModel)))
        case _ =>
          Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
      }
  }

  def handleCalculationResult: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino = user.nino
    val userAnswers: UserAnswers = request.userAnswers

    val netAmountOpt = userAnswers.get(UpdateIncomeNewAmountPage)
    val employmentNameOpt = userAnswers.get(UpdateIncomeNamePage)
    val idStrOpt = userAnswers.get(UpdateIncomeIdPage)

    (netAmountOpt, employmentNameOpt, idStrOpt) match {
      case (Some(netAmount), Some(_), Some(idStr)) =>
        val id = idStr.toString.toInt
        incomeService
          .employmentAmount(nino, id)
          .map { income =>
            val convertedNetAmount = BigDecimal(netAmount).intValue
            val employmentAmount = income.copy(newAmount = convertedNetAmount)

            if (employmentAmount.newAmount == income.oldAmount) {
              Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
            } else {
              Redirect(controllers.routes.IncomeController.updateEstimatedIncome(id))
            }
          }
          .recover { case NonFatal(e) =>
            errorPagesHandler.internalServerError(e.getMessage)
          }

      case _ =>
        Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(1).url))
    }
  }

  private def incomeTypeIdentifier(isPension: Boolean): String =
    if (isPension) {
      TaiConstants.IncomeTypePension
    } else {
      TaiConstants.IncomeTypeEmployment
    }
}
