/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.implicits.*
import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.income.*
import play.api.Logger
import play.api.libs.json.Format.GenericFormat
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.tai.forms.employments.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.util.constants.*
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.*
import views.html.incomes.DuplicateSubmissionWarningView
import views.html.incomes.estimatedPayment.update.CheckYourAnswersView

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
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  val logger: Logger = Logger(this.getClass)

  def onPageLoad(employmentId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      val nino                 = request.taiUser.nino
      val showDuplicateWarning =
        request.userAnswers.get(TrackSuccessfulJourneyUpdateEstimatedPayPage(employmentId)).getOrElse(false)

      (
        Future.successful(showDuplicateWarning),
        employmentService.employment(nino, employmentId)
      ).mapN {
        case (_, None)        =>
          errorPagesHandler.internalServerError("Not able to find employment")
        case (true, Some(_))  =>
          Redirect(routes.IncomeUpdateCalculatorController.duplicateSubmissionWarningPage(employmentId))
        case (false, Some(_)) =>
          Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage(employmentId))
      }.recover { case NonFatal(e) =>
        errorPagesHandler.internalServerError(e.getMessage)
      }
    }

  def duplicateSubmissionWarningPage(empId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino
      val ua                        = request.userAnswers
      val confirmedAmountOpt        = ua.get(UpdateIncomeConfirmedNewAmountPage(empId)).map(_.toInt)

      (employmentService.employment(nino, empId), Future.successful(confirmedAmountOpt))
        .mapN {
          case (Some(emp), Some(confirmedAmount)) =>
            val vm = if (emp.receivingOccupationalPension) {
              DuplicateSubmissionPensionViewModel(emp.name, confirmedAmount)
            } else {
              DuplicateSubmissionEmploymentViewModel(emp.name, confirmedAmount)
            }

            Ok(duplicateSubmissionWarning(DuplicateSubmissionWarningForm.createForm, vm, empId))

          case _ =>
            errorPagesHandler.internalServerError("Mandatory values missing")
        }
        .recover { case NonFatal(e) =>
          errorPagesHandler.internalServerError(e.getMessage)
        }
    }

  def submitDuplicateSubmissionWarning(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino
      val ua                        = request.userAnswers
      val confirmedAmountOpt        = ua.get(UpdateIncomeConfirmedNewAmountPage(empId)).map(_.toInt)

      DuplicateSubmissionWarningForm.createForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            (employmentService.employment(nino, empId), Future.successful(confirmedAmountOpt)).mapN {
              case (Some(emp), Some(confirmedAmount)) =>
                val vm = if (emp.receivingOccupationalPension) {
                  DuplicateSubmissionPensionViewModel(emp.name, confirmedAmount)
                } else {
                  DuplicateSubmissionEmploymentViewModel(emp.name, confirmedAmount)
                }

                BadRequest(duplicateSubmissionWarning(formWithErrors, vm, empId))

              case _ =>
                errorPagesHandler.internalServerError("Mandatory values missing")
            },
          success =>
            success.yesNoChoice match {
              case Some(FormValuesConstants.YesValue) =>
                Future.successful(Redirect(routes.IncomeUpdateEstimatedPayController.estimatedPayLandingPage(empId)))
              case Some(FormValuesConstants.NoValue)  =>
                Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
              case _                                  =>
                Future.successful(errorPagesHandler.internalServerError("Unexpected form state"))
            }
        )
        .recover { case NonFatal(e) =>
          errorPagesHandler.internalServerError(e.getMessage)
        }
  }

  // scalastyle:off method.length
  def checkYourAnswersPage(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino
      val ua                        = request.userAnswers

      val payPeriodOpt            = ua.get(UpdateIncomePayPeriodPage)
      val totalSalaryAmountOpt    = ua.get(UpdateIncomeTotalSalaryPage)
      val hasPayslipDeductionsOpt = ua.get(UpdateIncomePayslipDeductionsPage)
      val hasBonusPaymentsOpt     = ua.get(UpdateIncomeBonusPaymentsPage)

      val taxablePayOpt         = ua.get(UpdateIncomeTaxablePayPage)
      val bonusPaymentAmountOpt = ua.get(UpdateIncomeBonusOvertimeAmountPage)
      val payPeriodInDaysOpt    = ua.get(UpdateIncomeOtherInDaysPage)

      val mandatoryDefined =
        Seq(payPeriodOpt, totalSalaryAmountOpt, hasPayslipDeductionsOpt, hasBonusPaymentsOpt).forall(_.isDefined)

      if (!mandatoryDefined) {
        Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
      } else {
        employmentService
          .employment(nino, empId)
          .map {
            case Some(emp) =>
              val employer = IncomeSource(id = empId, name = emp.name)

              val backUrl =
                if (bonusPaymentAmountOpt.isEmpty)
                  controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusPaymentsPage().url
                else
                  controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController
                    .bonusOvertimeAmountPage()
                    .url

              val viewModel = CheckYourAnswersViewModel(
                payPeriodOpt.get.toString,
                payPeriodInDaysOpt,
                totalSalaryAmountOpt.get.toString,
                hasPayslipDeductionsOpt.get.toString,
                taxablePayOpt,
                hasBonusPaymentsOpt.get.toString,
                bonusPaymentAmountOpt,
                employer,
                backUrl
              )

              Ok(checkYourAnswers(viewModel))

            case None =>
              Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
          }
          .recover { case NonFatal(e) =>
            errorPagesHandler.internalServerError(e.getMessage)
          }
      }
  }

  def handleCalculationResult(empId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino
      val ua: UserAnswers           = request.userAnswers

      ua.get(UpdateIncomeNewAmountPage) match {
        case Some(newAmountStr) =>
          incomeService
            .employmentAmount(nino, empId)
            .map { employmentAmount =>
              val newEstimatedPay = BigDecimal(newAmountStr).intValue
              employmentAmount.oldAmount match {
                case Some(currentEstimatedPay) if newEstimatedPay == currentEstimatedPay =>
                  Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
                case _                                                                   =>
                  Redirect(controllers.routes.IncomeController.updateEstimatedIncome(empId))
              }
            }
            .recover { case NonFatal(e) =>
              errorPagesHandler.internalServerError(e.getMessage)
            }

        case _ =>
          Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
      }
    }

}
