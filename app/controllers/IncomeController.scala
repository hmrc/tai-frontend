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
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import views.html.incomes._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

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
    (for {
      employmentAmount  <- EitherT.right[String](incomeService.employmentAmount(nino, empId))
      latestPayment     <- EitherT.right[String](incomeService.latestPayment(nino, empId))
      updatedUserAnswers = incomeService.cachePaymentForRegularIncome(latestPayment, request.userAnswers)
      _                 <- EitherT.right[String](journeyCacheRepository.set(updatedUserAnswers))
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

  def sameEstimatedPayInCache(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      val userAnswers = request.userAnswers

      val nameOpt               = userAnswers.get(UpdateIncomeNamePage)
      val idOpt                 = userAnswers.get(UpdateIncomeIdPage)
      val confirmedNewAmountOpt = userAnswers.get(UpdateIncomeConfirmedNewAmountPage(empId))

      (nameOpt, idOpt, confirmedNewAmountOpt) match {
        case (Some(name), Some(employerId), Some(confirmedNewAmount)) =>
          val model = SameEstimatedPayViewModel(
            name,
            employerId,
            Some(confirmedNewAmount.toInt),
            isPension = false,
            routes.IncomeSourceSummaryController.onPageLoad(employerId).url
          )

          Future.successful(Ok(sameEstimatedPay(model)))

        case _ =>
          logger.warn(s"Mandatory value missing from UserAnswers for empId: $empId")
          Future.successful(errorPagesHandler.internalServerError("Mandatory values missing from UserAnswers"))
      }
  }

  def sameAnnualEstimatedPay(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers
    val nino        = request.taiUser.nino

    val nameOpt = userAnswers.get(UpdateIncomeNamePage)
    val idOpt   = userAnswers.get(UpdateIncomeIdPage)

    (nameOpt, idOpt) match {
      case (Some(name), Some(id)) =>
        incomeService
          .employmentAmount(nino, id)
          .map { income =>
            val model = SameEstimatedPayViewModel(
              name,
              id,
              income.oldAmount,
              income.isOccupationalPension,
              routes.IncomeSourceSummaryController.onPageLoad(id).url
            )
            Ok(sameEstimatedPay(model))
          }
          .recover { case NonFatal(e) =>
            errorPagesHandler.internalServerError(e.getMessage)
          }

      case _ =>
        Future.successful(errorPagesHandler.internalServerError("Mandatory values missing from UserAnswers"))
    }
  }

  def editRegularIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers  = request.userAnswers
    val payToDateOpt = userAnswers.get(UpdateIncomePayToDatePage)
    val nameOpt      = userAnswers.get(UpdateIncomeNamePage)
    val dateOpt      = userAnswers.get(UpdatedIncomeDatePage)

    (payToDateOpt, nameOpt, dateOpt) match {
      case (payToDate, Some(employerName), dateOpt) =>
        val date           = Try(dateOpt.map(date => LocalDate.parse(date))) match {
          case Success(optDate)   => optDate
          case Failure(exception) =>
            logger.warn(s"Unable to parse updateIncomeDateKey  $exception")
            None
        }
        val payToDateValue = payToDate.getOrElse("0")

        EditIncomeForm
          .bind(employerName, BigDecimal(payToDateValue), date)
          .fold(
            (formWithErrors: Form[EditIncomeForm]) =>
              Future.successful(
                BadRequest(editIncome(formWithErrors, hasMultipleIncomes = false, empId, payToDateValue))
              ),
            (income: EditIncomeForm) =>
              pickRedirectLocation(income, routes.IncomeController.confirmRegularIncome(empId), empId)
          )
      case _                                        =>
        logger.warn(s"Mandatory value missing from UserAnswers for empId: $empId")
        Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
    }
  }

  def confirmRegularIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino
      val userAnswers               = request.userAnswers

      userAnswers.get(UpdateIncomeNewAmountPage) match {
        case Some(newAmount) =>
          employmentService
            .employment(nino, empId)
            .flatMap {
              case Some(employment) =>
                val employmentAmount = EmploymentAmount(taxCodeIncome = None, employment = employment)

                val vm = ConfirmAmountEnteredViewModel(
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
              userAnswers.get(UpdateIncomeConfirmedNewAmountPage(empId)) match {
                case Some(_) =>
                  journeyCacheRepository
                    .clear(userAnswers.sessionId, request.userAnswers.nino)
                    .map(_ => Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
                case None    =>
                  Future.successful(errorPagesHandler.internalServerError(e.getMessage))
              }
            }

        case _ =>
          logger.warn(s"Mandatory value missing from UserAnswers for empId $empId")
          Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url))
      }
  }

  def updateEstimatedIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      def respondWithSuccess(employerName: String, employerId: Int, incomeType: String, newAmount: String)(implicit
        user: AuthedUser,
        request: DataRequest[AnyContent]
      ): Result = {
        val updatedUserAnswers =
          request.userAnswers
            .setOrException(UpdateIncomeConfirmedNewAmountPage(employerId), newAmount)
            .setOrException(TrackSuccessfulJourneyUpdateEstimatedPayPage(employerId), true)

        journeyCacheRepository.set(updatedUserAnswers)

        incomeType match {
          case TaiConstants.IncomeTypePension => Ok(editPensionSuccess(employerName, employerId))
          case _                              => Ok(editSuccess(employerName, employerId))
        }
      }

      val userAnswers = request.userAnswers

      val incomeNameOpt = userAnswers.get(UpdateIncomeNamePage)
      val newAmountOpt  = userAnswers.get(UpdateIncomeNewAmountPage)
      val incomeIdOpt   = userAnswers.get(UpdateIncomeIdPage)
      val incomeTypeOpt = userAnswers.get(UpdateIncomeTypePage)

      (incomeNameOpt, newAmountOpt, incomeIdOpt, incomeTypeOpt) match {
        case (Some(incomeName), Some(newAmount), Some(incomeId), Some(incomeType)) =>
          val newAmountInt = FormHelper.stripNumber(newAmount).toInt
          (for {
            _ <- journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
            _ <- taxAccountService
                   .updateEstimatedIncome(user.nino, newAmountInt, TaxYear(), incomeId)
          } yield respondWithSuccess(incomeName, incomeId, incomeType, newAmountInt.toString)).recover {
            case NonFatal(e) =>
              errorPagesHandler.internalServerError(e.getMessage, Some(e))
          }

        case _ =>
          logger.warn(s"Mandatory value missing from UserAnswers for empId: $empId")
          Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
      }
  }

  def pensionIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino                      = user.nino

    (for {
      employmentAmount <- incomeService.employmentAmount(nino, empId)
      latestPayment    <- incomeService.latestPayment(nino, empId)
      _                <- {
        val updatedUserAnswers = incomeService.cachePaymentForRegularIncome(latestPayment, request.userAnswers)
        journeyCacheRepository.set(updatedUserAnswers)
      }

    } yield {
      val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)
      Ok(
        editPension(
          EditIncomeForm.create(employmentAmount, None),
          hasMultipleIncomes = false,
          employmentAmount.employmentId,
          amountYearToDate.toString()
        )
      )
    }).recover { case NonFatal(e) =>
      errorPagesHandler.internalServerError(e.getMessage)
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
      Future.successful(Redirect(routes.IncomeController.sameAnnualEstimatedPay()))
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

  def editPensionIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers  = request.userAnswers
    val payToDateOpt = userAnswers.get(UpdateIncomePayToDatePage)
    val idOpt        = userAnswers.get(UpdateIncomeIdPage)
    val nameOpt      = userAnswers.get(UpdateIncomeNamePage)
    val dateOpt      = userAnswers.get(UpdatedIncomeDatePage)

    (payToDateOpt, idOpt, nameOpt, dateOpt) match {
      case (payToDate, Some(id), Some(employerName), dateOpt) =>
        val date           = Try(dateOpt.map(date => LocalDate.parse(date))) match {
          case Success(optDate)   => optDate
          case Failure(exception) =>
            logger.warn(s"Unable to parse updateIncomeDateKey  $exception")
            None
        }
        val payToDateValue = payToDate.getOrElse("0")

        EditIncomeForm
          .bind(employerName, BigDecimal(payToDateValue), date)
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  editPension(formWithErrors, hasMultipleIncomes = false, id, payToDateValue)
                )
              ),
            (income: EditIncomeForm) =>
              pickRedirectLocation(income, routes.IncomeController.confirmPensionIncome(empId), empId)
          )
      case _                                                  =>
        logger.warn(s"Mandatory value missing from UserAnswers for empId: $empId")
        Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
    }
  }

  def confirmPensionIncome(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino

      request.userAnswers.get(UpdateIncomeNewAmountPage) match {
        case Some(newAmount) =>
          employmentService
            .employment(nino, empId)
            .map {
              case Some(employment) =>
                val employmentAmount = EmploymentAmount(taxCodeIncome = None, employment = employment)

                val vm = ConfirmAmountEnteredViewModel(
                  empName = employment.name,
                  currentAmount = employmentAmount.oldAmount,
                  estIncome = newAmount.toInt,
                  backUrl = "#",
                  empId = empId
                )

                Ok(confirmAmountEntered(vm))

              case None =>
                throw new RuntimeException("Error while reading employment and tax code details")
            }
            .recover { case NonFatal(e) =>
              errorPagesHandler.internalServerError(e.getMessage)
            }

        case _ =>
          logger.warn(s"Mandatory value missing from UserAnswers for empId: $empId")
          Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
      }
  }

  def viewIncomeForEdit: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    request.userAnswers.get(UpdateIncomeIdPage) match {
      case Some(id) =>
        processEmploymentAmount(id)
      case None     =>
        Future.successful(Redirect(routes.TaxAccountSummaryController.onPageLoad()))
    }
  }

  private def processEmploymentAmount(id: Int)(implicit request: DataRequest[AnyContent]): Future[Result] =
    (for {
      employmentAmount <- EitherT.right[String](incomeService.employmentAmount(request.taiUser.nino, id))
    } yield (employmentAmount.isLive, employmentAmount.isOccupationalPension) match {
      case (true, false)  => Redirect(routes.IncomeController.regularIncome(id))
      case (false, false) => Redirect(routes.TaxAccountSummaryController.onPageLoad())
      case _              => Redirect(routes.IncomeController.pensionIncome(id))
    }).fold(errorPagesHandler.internalServerError(_, None), identity)
      .recover { case NonFatal(e) =>
        errorPagesHandler.internalServerError(e.getMessage)
      }
}
