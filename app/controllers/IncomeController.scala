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

package controllers

import cats.data.EitherT
import cats.implicits._
import controllers.auth.{AuthJourney, AuthedUser, DataRequest}
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.income._
import play.api.Logging
import play.api.data.Form
import play.api.mvc._
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util._
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import views.html.incomes._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class IncomeController @Inject() (
                                   taxAccountService: TaxAccountService,
                                   employmentService: EmploymentService,
                                   incomeService: IncomeService,
                                   authenticate: AuthJourney,
                                   mcc: MessagesControllerComponents,
                                   confirmAmountEntered: ConfirmAmountEnteredView,
                                   editSuccess: EditSuccessView,
                                   editPension: EditPensionView,
                                   editPensionSuccess: EditPensionSuccessView,
                                   editIncome: EditIncomeView,
                                   sameEstimatedPay: SameEstimatedPayView,
                                   journeyCacheRepository: JourneyCacheRepository,
                                   empIdCheck: EmpIdCheck,
                                   implicit val errorPagesHandler: ErrorPagesHandler
                                 )(implicit ec: ExecutionContext)
  extends TaiBaseController(mcc)
    with Logging {

  def cancel(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino).map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }

  def regularIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino                      = user.nino

    empIdCheck.checkValidId(empId).flatMap {
      case Some(result) => Future.successful(result)
      case _ =>
        (for {
          employmentAmount <- EitherT.right[String](incomeService.employmentAmount(nino, empId))
          latestPayment    <- EitherT.right[String](incomeService.latestPayment(nino, empId))
        } yield {
          val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)
          Ok(
            editIncome(
              EditIncomeForm.create(employmentAmount, None),
              hasMultipleIncomes = false,
              employmentAmount.employmentId,
              amountYearToDate.toString
            )
          )
        }).fold(errorPagesHandler.internalServerError(_, None), identity)
          .recover { case NonFatal(e) =>
            errorPagesHandler.internalServerError(e.getMessage)
          }
    }
  }

  def sameEstimatedPayInCache(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino

      val confirmedNewAmountOpt = request.userAnswers.get(UpdateIncomeConfirmedNewAmountPage(empId)).map(_.toInt)

      empIdCheck.checkValidId(empId).flatMap {
        case Some(result) => Future.successful(result)
        case _ =>
          employmentService
            .employment(nino, empId)
            .map {
              case Some(emp) if confirmedNewAmountOpt.isDefined =>
                val model = SameEstimatedPayViewModel(
                  emp.name,
                  empId,
                  confirmedNewAmountOpt,
                  emp.receivingOccupationalPension,
                  routes.IncomeSourceSummaryController.onPageLoad(empId).url
                )
                Ok(sameEstimatedPay(model))
              case _ =>
                logger.warn(s"Mandatory value missing for empId: $empId")
                errorPagesHandler.internalServerError("Mandatory values missing from UserAnswers")
            }
      }
  }

  def sameAnnualEstimatedPay(employmentId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      val nino = request.taiUser.nino

      empIdCheck.checkValidId(employmentId).flatMap {
        case Some(result) => Future.successful(result)
        case _ =>
          incomeService
            .employmentAmount(nino, employmentId)
            .flatMap { income =>
              employmentService.employment(nino, employmentId).map {
                case Some(emp) =>
                  val model = SameEstimatedPayViewModel(
                    employerName = emp.name,
                    employerId = employmentId,
                    amount = income.oldAmount,
                    isPension = income.isOccupationalPension,
                    returnLinkUrl = routes.IncomeSourceSummaryController.onPageLoad(employmentId).url
                  )
                  Ok(sameEstimatedPay(model))
                case None =>
                  errorPagesHandler.internalServerError("Employment not found")
              }
            }
            .recover { case NonFatal(e) =>
              errorPagesHandler.internalServerError(e.getMessage)
            }
      }
    }

  def editRegularIncome(empId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino

      empIdCheck.checkValidId(empId).flatMap {
        case Some(result) => Future.successful(result)
        case _ =>
          employmentService
            .employment(nino, empId)
            .flatMap {
              case Some(emp) =>
                incomeService.latestPayment(nino, empId).flatMap { paymentOpt =>
                  val payToDateValue: BigDecimal = paymentOpt.map(_.amountYearToDate).getOrElse(BigDecimal(0))
                  val date: Option[LocalDate]    = paymentOpt.map(_.date)
                  if (paymentOpt.isEmpty) logger.info(s"No latest payment for empId $empId, defaulting YTD to 0")
                  EditIncomeForm
                    .bind(emp.name, payToDateValue, date)
                    .fold(
                      (formWithErrors: Form[EditIncomeForm]) =>
                        Future.successful(
                          BadRequest(
                            editIncome(formWithErrors, hasMultipleIncomes = false, empId, payToDateValue.toString)
                          )
                        ),
                      (income: EditIncomeForm) =>
                        pickRedirectLocation(income, routes.IncomeController.confirmRegularIncome(empId), empId)
                    )
                }

              case None =>
                Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
            }
      }
    }

  def confirmRegularIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino

      empIdCheck.checkValidId(empId).flatMap {
        case Some(result) => Future.successful(result)
        case _ =>
          request.userAnswers.get(UpdateIncomeNewAmountPage) match {
            case Some(newAmount) =>
              employmentService
                .employment(nino, empId)
                .flatMap {
                  case Some(employment) =>
                    val employmentAmount = EmploymentAmount(taxCodeIncome = None, employment = employment)
                    val vm               = ConfirmAmountEnteredViewModel(
                      empName = employment.name,
                      currentAmount = employmentAmount.oldAmount,
                      estIncome = newAmount.toInt,
                      backUrl = controllers.routes.IncomeController.regularIncome(empId).url,
                      empId = empId
                    )
                    Future.successful(Ok(confirmAmountEntered(vm)))

                  case None =>
                    Future.successful(
                      errorPagesHandler.internalServerError("Exception while reading employment and tax code details")
                    )
                }
                .recoverWith { case NonFatal(e) =>
                  request.userAnswers.get(UpdateIncomeConfirmedNewAmountPage(empId)) match {
                    case Some(_) =>
                      journeyCacheRepository
                        .clear(request.userAnswers.sessionId, request.userAnswers.nino)
                        .map(_ => Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
                    case None =>
                      Future.successful(errorPagesHandler.internalServerError(e.getMessage))
                  }
                }

            case _ =>
              logger.warn(s"Mandatory value missing from UserAnswers for empId $empId")
              Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url))
          }
      }
  }

  def updateEstimatedIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino

      empIdCheck.checkValidId(empId).flatMap {
        case Some(result) => Future.successful(result)
        case _ =>
          request.userAnswers.get(UpdateIncomeNewAmountPage) match {
            case Some(newAmountRaw) =>
              val newAmountInt = FormHelper.stripNumber(newAmountRaw).toInt

              def respondWithSuccess(empName: String, isPension: Boolean): Result = {
                val updatedUserAnswers =
                  request.userAnswers
                    .setOrException(UpdateIncomeConfirmedNewAmountPage(empId), newAmountInt.toString)
                    .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(empId), true)

                journeyCacheRepository.set(updatedUserAnswers)
                if (isPension) Ok(editPensionSuccess(empName, empId)) else Ok(editSuccess(empName, empId))
              }

              for {
                _      <- journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
                _      <- taxAccountService.updateEstimatedIncome(nino, newAmountInt, TaxYear(), empId)
                empOpt <- employmentService.employment(nino, empId)
              } yield empOpt match {
                case Some(emp) => respondWithSuccess(emp.name, emp.receivingOccupationalPension)
                case None      => errorPagesHandler.internalServerError("Employment not found")
              }

            case None =>
              logger.warn(s"Mandatory value missing from UserAnswers for empId: $empId (UpdateIncomeNewAmountPage)")
              Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
          }
      }
  }

  def pensionIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino                      = user.nino

    empIdCheck.checkValidId(empId).flatMap {
      case Some(result) => Future.successful(result)
      case _ =>
        (for {
          employmentAmount <- incomeService.employmentAmount(nino, empId)
          latestPayment    <- incomeService.latestPayment(nino, empId)
        } yield {
          val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)
          Ok(
            editPension(
              EditIncomeForm.create(employmentAmount, None),
              hasMultipleIncomes = false,
              employmentAmount.employmentId,
              amountYearToDate.toString
            )
          )
        }).recover { case NonFatal(e) =>
          errorPagesHandler.internalServerError(e.getMessage)
        }
    }
  }

  private def pickRedirectLocation(
                                    income: EditIncomeForm,
                                    confirmationCallback: Call,
                                    empId: Int
                                  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    if (isCachedIncomeTheSame(request.userAnswers, income.newAmount, empId)) {
      Future.successful(Redirect(routes.IncomeController.sameEstimatedPayInCache(empId)))
    } else if (isIncomeTheSame(income)) {
      Future.successful(Redirect(routes.IncomeController.sameAnnualEstimatedPay(empId)))
    } else {
      cacheAndRedirect(income, confirmationCallback)
    }

  private def isCachedIncomeTheSame(userAnswers: UserAnswers, newAmount: Option[String], empId: Int): Boolean = {
    val cachedAmount = userAnswers.get(UpdateIncomeConfirmedNewAmountPage(empId))
    FormHelper.areEqual(cachedAmount, newAmount)
  }

  private def isIncomeTheSame(income: EditIncomeForm): Boolean =
    income.oldAmount match {
      case Some(oldAmt) => FormHelper.areEqual(Some(oldAmt.toString), income.newAmount)
      case None         => false
    }

  private def cacheAndRedirect(income: EditIncomeForm, confirmationCallback: Call)(implicit
                                                                                   request: DataRequest[AnyContent]
  ): Future[Result] = {
    val newAmount      = FormHelper.convertCurrencyToInt(income.newAmount).toString
    val updatedAnswers = request.userAnswers.setOrException(UpdateIncomeNewAmountPage, newAmount)
    journeyCacheRepository.set(updatedAnswers).map(_ => Redirect(confirmationCallback))
  }
}
