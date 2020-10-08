/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import javax.inject.{Inject, Named}
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiUnauthorisedResponse}
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Payment
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.{IncomeService, TaxAccountService}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import uk.gov.hmrc.tai.util.constants.TaiConstants.MONTH_AND_YEAR
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, EditIncomeIrregularHoursViewModel, IrregularPay}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeUpdateIrregularHoursController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  incomeService: IncomeService,
  taxAccountService: TaxAccountService,
  estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
  mcc: MessagesControllerComponents,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants {

  private val taxCodeIncomeInfoToCache = (taxCodeIncome: TaxCodeIncome, payment: Option[Payment]) => {
    val defaultCaching = Map[String, String](
      UpdateIncome_NameKey      -> taxCodeIncome.name,
      UpdateIncome_PayToDateKey -> taxCodeIncome.amount.toString
    )

    payment.fold(defaultCaching)(payment =>
      defaultCaching + (UpdateIncome_DateKey -> payment.date.toString(MONTH_AND_YEAR)))
  }

  def editIncomeIrregularHours(employmentId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val nino = user.nino

      val paymentRequest: Future[Option[Payment]] = incomeService.latestPayment(nino, employmentId)
      val taxCodeIncomeRequest = taxAccountService.taxCodeIncomeForEmployment(nino, TaxYear(), employmentId)

      paymentRequest flatMap { payment =>
        taxCodeIncomeRequest flatMap {
          case Right(Some(tci)) => {
            (taxCodeIncomeInfoToCache.tupled andThen journeyCacheService.cache)(tci, payment) map { _ =>
              val viewModel = EditIncomeIrregularHoursViewModel(employmentId, tci.name, tci.amount)

              Ok(views.html.incomes.editIncomeIrregularHours(AmountComparatorForm.createForm(), viewModel))
            }
          }
          case Left(TaiUnauthorisedResponse(_)) =>
            Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
          case _ => Future.successful(internalServerError("Failed to find tax code income for employment"))
        }
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
          val vm =
            ConfirmAmountEnteredViewModel(employmentId, name, paymentToDate.toInt, newIrregularPay.toInt, IrregularPay)
          Ok(views.html.incomes.confirmAmountEntered(vm))
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

        AmountComparatorForm
          .createForm(latestPayDate, Some(paymentToDate.toInt))
          .bindFromRequest()
          .fold(
            formWithErrors => {

              val viewModel = EditIncomeIrregularHoursViewModel(employmentId, name, paymentToDate)
              Future.successful(BadRequest(views.html.incomes.editIncomeIrregularHours(formWithErrors, viewModel)))
            },
            validForm =>
              validForm.income.fold(throw new RuntimeException) { income =>
                journeyCacheService.cache(UpdateIncome_IrregularAnnualPayKey, income) map { _ =>
                  Redirect(routes.IncomeUpdateIrregularHoursController.confirmIncomeIrregularHours(employmentId))
                }
            }
          )
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

      journeyCacheService
        .mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IrregularAnnualPayKey, UpdateIncome_IdKey)
        .flatMap(cache => {
          val incomeName :: newPay :: incomeId :: Nil = cache.toList

          taxAccountService.updateEstimatedIncome(nino, newPay.toInt, TaxYear(), employmentId) flatMap {
            case TaiSuccessResponse => {
              updateJourneyCompletion(incomeId) flatMap { _ =>
                cacheAndRespond(incomeName, incomeId, newPay)
              }
            }
            case _ => throw new RuntimeException(s"Not able to update estimated pay for $employmentId")
          }

        })
        .recover {
          case NonFatal(e) => internalServerError(e.getMessage)
        }
  }

}
