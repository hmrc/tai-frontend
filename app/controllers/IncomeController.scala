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

package controllers

import cats.data.EitherT
import cats._
import cats.implicits._
import com.google.inject.name.Named
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import play.api.Logging
import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses._
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.{EmploymentAmount, TaxYear}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.FutureOps._
import uk.gov.hmrc.tai.util._
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.viewModels.SameEstimatedPayViewModel
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import views.html.incomes._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

@Singleton
class IncomeController @Inject()(
  @Named("Update Income") journeyCacheService: JourneyCacheService,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  incomeService: IncomeService,
  estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  confirmAmountEntered: ConfirmAmountEnteredView,
  editSuccess: EditSuccessView,
  editPension: EditPensionView,
  editPensionSuccess: EditPensionSuccessView,
  editIncome: EditIncomeView,
  sameEstimatedPay: SameEstimatedPayView,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Logging {

  def cancel(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.flush() map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }

  def regularIncome(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino = user.nino

    (for {
      employmentAmount <- EitherT.right[String](incomeService.employmentAmount(nino, empId))
      latestPayment    <- EitherT.right[String](incomeService.latestPayment(nino, empId))
      cacheData = incomeService.cachePaymentForRegularIncome(latestPayment)
      _ <- EitherT.right[String](journeyCacheService.cache(cacheData))
    } yield {
      val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)
      Ok(
        editIncome(
          EditIncomeForm.create(employmentAmount),
          hasMultipleIncomes = false,
          employmentAmount.employmentId,
          amountYearToDate.toString))
    }).fold(errorPagesHandler.internalServerError(_, None), identity _)
      .recover {
        case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
      }
  }

  def sameEstimatedPayInCache(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      (for {
        cachedData <- journeyCacheService
                       .mandatoryJourneyValues(
                         UpdateIncomeConstants.NameKey,
                         UpdateIncomeConstants.IdKey,
                         s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$empId")
                       .getOrFail
      } yield {
        val employerId = cachedData(1).toInt
        val model = SameEstimatedPayViewModel(
          cachedData.head,
          employerId,
          cachedData(2).toInt,
          isPension = false,
          routes.IncomeSourceSummaryController.onPageLoad(employerId).url)
        Ok(sameEstimatedPay(model))
      }).recover {
        case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
      }
  }

  def sameAnnualEstimatedPay(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val cachedDataFuture = journeyCacheService.mandatoryJourneyValues(UpdateIncomeConstants.NameKey).getOrFail

    val idFuture = journeyCacheService.mandatoryJourneyValueAsInt(UpdateIncomeConstants.IdKey).getOrFail
    val nino = request.taiUser.nino

    for {
      cachedData <- cachedDataFuture
      id         <- idFuture
      income     <- incomeService.employmentAmount(nino, id)
    } yield {
      val model = SameEstimatedPayViewModel(
        cachedData.head,
        id,
        income.oldAmount,
        income.isOccupationalPension,
        routes.IncomeSourceSummaryController.onPageLoad(id).url)
      Ok(sameEstimatedPay(model))
    }
  }

  def editRegularIncome(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      journeyCacheService
        .collectedJourneyValues(
          Seq(UpdateIncomeConstants.PayToDateKey, UpdateIncomeConstants.NameKey),
          Seq(UpdateIncomeConstants.DateKey))
        .flatMap {
          case Left(errorMessage) =>
            logger.warn(errorMessage)
            Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
          case Right((mandatorySeq, optionalSeq)) =>
            val date = Try(optionalSeq.head.map(date => LocalDate.parse(date))) match {
              case Success(optDate) => optDate
              case Failure(exception) =>
                logger.warn(s"Unable to parse updateIncomeDateKey  $exception")
                None
            }

            val employerName = mandatorySeq(1)
            val payToDate = BigDecimal(mandatorySeq.head)

            EditIncomeForm
              .bind(employerName, payToDate, date)
              .fold(
                (formWithErrors: Form[EditIncomeForm]) =>
                  Future.successful(
                    BadRequest(editIncome(formWithErrors, hasMultipleIncomes = false, empId, mandatorySeq.head))),
                (income: EditIncomeForm) =>
                  determineEditRedirect(income, routes.IncomeController.confirmRegularIncome(empId), empId)
              )
        }
  }

  private def isCachedIncomeTheSame(currentCache: Map[String, String], newAmount: Option[String], empId: Int): Boolean =
    FormHelper.areEqual(currentCache.get(s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$empId"), newAmount)

  private def isIncomeTheSame(income: EditIncomeForm): Boolean =
    FormHelper.areEqual(Some(income.oldAmount.toString), income.newAmount)

  def confirmRegularIncome(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      journeyCacheService
        .mandatoryJourneyValueAsInt(UpdateIncomeConstants.NewAmountKey)
        .flatMap {
          case Left(errorMessage) =>
            logger.warn(errorMessage)
            Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url))
          case Right(cachedData) =>
            (taxAccountService.taxCodeIncomes(nino, TaxYear()), employmentService.employment(nino, empId))
              .mapN {
                case (
                    TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]),
                    Some(employment)
                    ) =>
                  taxCodeIncomes.find(_.employmentId.contains(empId)) match {
                    case Some(taxCodeIncome) =>
                      val employmentAmount = EmploymentAmount(taxCodeIncome, employment)

                      val vm =
                        ConfirmAmountEnteredViewModel(
                          employment.name,
                          employmentAmount.oldAmount,
                          cachedData,
                          controllers.routes.IncomeController.regularIncome(empId).url,
                          empId
                        )
                      Ok(confirmAmountEntered(vm))

                    case _ => throw new RuntimeException(s"Not able to found employment with id $empId")
                  }
                case _ => throw new RuntimeException("Exception while reading employment and tax code details")
              }
        }
        .recoverWith {
          case NonFatal(e) =>
            journeyCacheService
              .mandatoryJourneyValueAsInt(s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$empId")
              .flatMap {
                case Right(_) =>
                  journeyCacheService.flush().map { _ =>
                    Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
                  }
                case _ => Future.successful(errorPagesHandler.internalServerError(e.getMessage))
              }
        }
  }

  def updateEstimatedIncome(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      def respondWithSuccess(employerName: String, employerId: Int, incomeType: String, newAmount: String)(
        implicit user: AuthedUser,
        request: Request[AnyContent]
      ): Result = {
        journeyCacheService.cache(s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$employerId", newAmount)
        incomeType match {
          case TaiConstants.IncomeTypePension => Ok(editPensionSuccess(employerName, employerId))
          case _                              => Ok(editSuccess(employerName, employerId))
        }
      }

      val collectedValues = journeyCacheService
        .mandatoryJourneyValues(
          UpdateIncomeConstants.NameKey,
          UpdateIncomeConstants.NewAmountKey,
          UpdateIncomeConstants.IdKey,
          UpdateIncomeConstants.IncomeTypeKey)

      collectedValues
        .flatMap {
          case Left(errorMessage) =>
            logger.warn(errorMessage)
            Future(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
          case Right(cache) =>
            val incomeName :: newAmount :: incomeId :: incomeType :: Nil = cache.toList

            journeyCacheService.flush().flatMap { _ =>
              taxAccountService.updateEstimatedIncome(
                user.nino,
                FormHelper.stripNumber(newAmount).toInt,
                TaxYear(),
                incomeId.toInt) flatMap {
                case TaiSuccessResponse =>
                  estimatedPayJourneyCompletionService.journeyCompleted(incomeId).map { _ =>
                    respondWithSuccess(incomeName, incomeId.toInt, incomeType, newAmount)
                  }
                case failure: TaiFailureResponse =>
                  throw new RuntimeException(s"Failed to update estimated income with exception: ${failure.message}")
              }
            }
        }
        .recover {
          case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage, Some(e))
        }
  }

  def pensionIncome(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino = user.nino

    (for {
      employmentAmount <- incomeService.employmentAmount(nino, empId)
      latestPayment    <- incomeService.latestPayment(nino, empId)
      cacheData = incomeService.cachePaymentForRegularIncome(latestPayment)
      _ <- journeyCacheService.cache(cacheData)
    } yield {
      val amountYearToDate: BigDecimal = latestPayment.map(_.amountYearToDate).getOrElse(0)
      Ok(
        editPension(
          EditIncomeForm.create(employmentAmount),
          hasMultipleIncomes = false,
          employmentAmount.employmentId,
          amountYearToDate.toString()))
    }).recover {
      case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
    }
  }

  private def determineEditRedirect(income: EditIncomeForm, confirmationCallback: Call, empId: Int)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    for {
      currentCache <- journeyCacheService.currentCache
      result       <- pickRedirectLocation(currentCache, income, confirmationCallback, empId)
    } yield {
      result
    }

  private def pickRedirectLocation(
    currentCache: Map[String, String],
    income: EditIncomeForm,
    confirmationCallback: Call,
    empId: Int
  )(implicit hc: HeaderCarrier): Future[Result] =
    if (isCachedIncomeTheSame(currentCache, income.newAmount, empId)) {
      Future.successful(Redirect(routes.IncomeController.sameEstimatedPayInCache(empId)))
    } else if (isIncomeTheSame(income)) {
      Future.successful(Redirect(routes.IncomeController.sameAnnualEstimatedPay()))
    } else {
      cacheAndRedirect(income, confirmationCallback)
    }

  private def cacheAndRedirect(income: EditIncomeForm, confirmationCallback: Call)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    journeyCacheService
      .cache(UpdateIncomeConstants.NewAmountKey, income.toEmploymentAmount().newAmount.toString)
      .map(_ => Redirect(confirmationCallback))

  def editPensionIncome(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      journeyCacheService
        .collectedJourneyValues(
          Seq(UpdateIncomeConstants.PayToDateKey, UpdateIncomeConstants.IdKey, UpdateIncomeConstants.NameKey),
          Seq(UpdateIncomeConstants.DateKey))
        .flatMap {
          case Left(errorMessage) =>
            logger.warn(errorMessage)
            Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
          case Right((mandatorySeq, optionalSeq)) =>
            val date = Try(optionalSeq.head.map(date => LocalDate.parse(date))) match {
              case Success(optDate) => optDate
              case Failure(exception) =>
                logger.warn(s"Unable to parse updateIncomeDateKey $exception")
                None
            }
            EditIncomeForm
              .bind(mandatorySeq(2), BigDecimal(mandatorySeq.head), date)
              .fold(
                formWithErrors => {
                  Future.successful(BadRequest(
                    editPension(formWithErrors, hasMultipleIncomes = false, mandatorySeq(1).toInt, mandatorySeq.head)))
                },
                (income: EditIncomeForm) =>
                  determineEditRedirect(income, routes.IncomeController.confirmPensionIncome(empId), empId)
              )
        }
  }

  def confirmPensionIncome(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      journeyCacheService
        .mandatoryJourneyValue(UpdateIncomeConstants.NewAmountKey)
        .flatMap {
          case Left(_) =>
            Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
          case Right(newAmountKey) =>
            (taxAccountService.taxCodeIncomes(nino, TaxYear()), employmentService.employment(nino, empId))
              .mapN {
                case (
                    TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]),
                    Some(employment)
                    ) =>
                  taxCodeIncomes.find(_.employmentId.contains(empId)) match {
                    case Some(taxCodeIncome) =>
                      val employmentAmount = EmploymentAmount(taxCodeIncome, employment)

                      val vm = ConfirmAmountEnteredViewModel(
                        employment.name,
                        employmentAmount.oldAmount,
                        newAmountKey.toInt,
                        "javascript:history.go(-1)", //TODO this is temporary
                        empId
                      )

                      Ok(confirmAmountEntered(vm))
                    case _ => throw new RuntimeException(s"Not able to found employment with id $empId")
                  }
                case _ => throw new RuntimeException("Exception while reading employment and tax code details")
              }
        }
        .recover {
          case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
        }
  }

  def viewIncomeForEdit: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    (for {
      id               <- EitherT(journeyCacheService.mandatoryJourneyValueAsInt(UpdateIncomeConstants.IdKey))
      employmentAmount <- EitherT.right[String](incomeService.employmentAmount(request.taiUser.nino, id))
    } yield {
      (employmentAmount.isLive, employmentAmount.isOccupationalPension) match {
        case (true, false)  => Redirect(routes.IncomeController.regularIncome(id))
        case (false, false) => Redirect(routes.TaxAccountSummaryController.onPageLoad())
        case _              => Redirect(routes.IncomeController.pensionIncome(id))
      }
    }).fold(errorPagesHandler.internalServerError(_, None), identity _)
      .recover {
        case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
      }

  }

}
