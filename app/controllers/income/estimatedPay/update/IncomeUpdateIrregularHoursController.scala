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

import cats.implicits._
import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Payment
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.service.{IncomeService, TaxAccountService}
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.FutureOps.FutureEitherStringOps
import uk.gov.hmrc.tai.util.constants.TaiConstants.MonthAndYear
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, EditIncomeIrregularHoursViewModel, IrregularPay}
import views.html.incomes.{ConfirmAmountEnteredView, EditIncomeIrregularHoursView, EditSuccessView}

import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeUpdateIrregularHoursController @Inject() (
  authenticate: AuthJourney,
  incomeService: IncomeService,
  taxAccountService: TaxAccountService,
  estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
  mcc: MessagesControllerComponents,
  editSuccess: EditSuccessView,
  editIncomeIrregularHours: EditIncomeIrregularHoursView,
  confirmAmountEntered: ConfirmAmountEnteredView,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  private val logger = Logger(this.getClass)

  private val taxCodeIncomeInfoToCache: (TaxCodeIncome, Option[Payment]) => Map[String, String] =
    (taxCodeIncome: TaxCodeIncome, payment: Option[Payment]) => {
      val defaultCaching = Map[String, String](
        UpdateIncomeConstants.NameKey      -> taxCodeIncome.name,
        UpdateIncomeConstants.PayToDateKey -> taxCodeIncome.amount.toString
      )

      payment.fold(defaultCaching)(payment =>
        defaultCaching + (UpdateIncomeConstants.DateKey -> payment.date.format(
          DateTimeFormatter.ofPattern(MonthAndYear)
        ))
      )
    }

  def editIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      (
        incomeService.latestPayment(nino, employmentId),
        taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear(), employmentId)
      ).mapN {
        case (_, Left(value)) =>
          logger.error(value)
          Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
        case (maybePayment, Right(Some(tci))) =>
          journeyCacheService.cache(taxCodeIncomeInfoToCache(tci, maybePayment)).map { _ =>
            val viewModel = EditIncomeIrregularHoursViewModel(employmentId, tci.name, tci.amount)
            Ok(editIncomeIrregularHours(AmountComparatorForm.createForm(), viewModel))
          }
        case _ =>
          Future.successful(errorPagesHandler.internalServerError("Failed to find tax code income for employment"))
      }.flatten
  }

  def confirmIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      val collectedValues = journeyCacheService
        .collectedJourneyValues(
          Seq(
            UpdateIncomeConstants.NameKey,
            UpdateIncomeConstants.IrregularAnnualPayKey,
            UpdateIncomeConstants.PayToDateKey
          ),
          Seq(s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$employmentId")
        )

      collectedValues
        .map {
          case Left(errorMessage) =>
            logger.warn(errorMessage)
            Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId))
          case Right((mandatoryCache, optionalCache)) =>
            val name :: newIrregularPay :: paymentToDate :: Nil = mandatoryCache.toList
            val confirmedNewAmount = optionalCache.head

            if (FormHelper.areEqual(confirmedNewAmount, Some(newIrregularPay))) {
              Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache(employmentId))
            } else if (FormHelper.areEqual(Some(paymentToDate), Some(newIrregularPay))) {
              Redirect(controllers.routes.IncomeController.sameAnnualEstimatedPay())
            } else {
              val vm =
                ConfirmAmountEnteredViewModel(
                  employmentId,
                  name,
                  newIrregularPay.toInt,
                  IrregularPay,
                  controllers.income.estimatedPay.update.routes.IncomeUpdateIrregularHoursController
                    .editIncomeIrregularHours(employmentId)
                    .url
                )
              Ok(confirmAmountEntered(vm))
            }
        }
        .recover { case NonFatal(e) =>
          errorPagesHandler.internalServerError(e.getMessage)
        }
  }

  def handleIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      journeyCacheService.currentCache flatMap { cache =>
        val name = cache(UpdateIncomeConstants.NameKey)
        val paymentToDate: String = cache(UpdateIncomeConstants.PayToDateKey)
        val latestPayDate = cache.get(UpdateIncomeConstants.DateKey)

        AmountComparatorForm
          .createForm(latestPayDate, Some(paymentToDate.toInt))
          .bindFromRequest()
          .fold(
            formWithErrors => {

              val viewModel = EditIncomeIrregularHoursViewModel(employmentId, name, paymentToDate)
              Future.successful(BadRequest(editIncomeIrregularHours(formWithErrors, viewModel)))
            },
            validForm =>
              validForm.income.fold(throw new RuntimeException) { income =>
                journeyCacheService.cache(UpdateIncomeConstants.IrregularAnnualPayKey, income) map { _ =>
                  Redirect(routes.IncomeUpdateIrregularHoursController.confirmIncomeIrregularHours(employmentId))
                }
              }
          )
      }
  }

  def submitIncomeIrregularHours(employmentId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      val updateJourneyCompletion: String => Future[Map[String, String]] =
        (incomeId: String) => estimatedPayJourneyCompletionService.journeyCompleted(incomeId)

      val cacheAndRespond = (incomeName: String, incomeId: String, newPay: String) =>
        journeyCacheService.cache(s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$employmentId", newPay) map { _ =>
          Ok(editSuccess(incomeName, incomeId.toInt))
        }

      (for {
        cache <- journeyCacheService
                   .mandatoryJourneyValues(
                     Seq(
                       UpdateIncomeConstants.NameKey,
                       UpdateIncomeConstants.IrregularAnnualPayKey,
                       UpdateIncomeConstants.IdKey
                     )
                   )
                   .getOrFail
        incomeName :: newPay :: incomeId :: Nil = cache.toList
        _      <- taxAccountService.updateEstimatedIncome(nino, newPay.toInt, TaxYear(), employmentId)
        _      <- updateJourneyCompletion(incomeId)
        result <- cacheAndRespond(incomeName, incomeId, newPay)
      } yield result).recover { case NonFatal(e) =>
        errorPagesHandler.internalServerError(e.getMessage)
      }

  }

}
