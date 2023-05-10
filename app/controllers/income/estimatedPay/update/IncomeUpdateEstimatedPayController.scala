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

import cats.data._
import cats.implicits._
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.Logger

import java.time.LocalDate
import play.api.mvc._
import uk.gov.hmrc.tai.util.MoneyPounds
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiFailureResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{IncomeService, TaxAccountService}
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.ViewModelHelper.withPoundPrefixAndSign
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.EstimatedPayViewModel
import views.html.incomes.{EstimatedPayLandingPageView, EstimatedPayView, IncorrectTaxableIncomeView}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IncomeUpdateEstimatedPayController @Inject() (
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  incomeService: IncomeService,
  appConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  taxAccountService: TaxAccountService,
  estimatedPayLandingPage: EstimatedPayLandingPageView,
  estimatedPay: EstimatedPayView,
  incorrectTaxableIncome: IncorrectTaxableIncomeView,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with UpdatedEstimatedPayJourneyCache {

  private val logger = Logger(this.getClass)

  def estimatedPayLandingPage(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      journeyCacheService
        .mandatoryJourneyValues(UpdateIncomeConstants.NameKey, UpdateIncomeConstants.IncomeTypeKey)
        .flatMap {
          case Left(errorMessage) =>
            logger.warn(errorMessage)
            Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
          case Right(journeyValues) =>
            taxAccountService
              .taxAccountSummary(user.nino, TaxYear())
              .map { taxAccountSummary =>
                val totalEstimatedIncome =
                  withPoundPrefixAndSign(MoneyPounds(taxAccountSummary.totalEstimatedIncome, 0))
                val incomeName = journeyValues.head
                val incomeType = journeyValues.last
                Ok(
                  estimatedPayLandingPage(
                    incomeName,
                    empId,
                    totalEstimatedIncome,
                    incomeType == TaiConstants.IncomeTypePension,
                    appConfig
                  )
                )
              }
              .recover { case e: Exception =>
                errorPagesHandler.internalServerError(e.getMessage)
              }
        }
  }

  private def isCachedAmountSameAsEnteredAmount(
    cache: Map[String, String],
    newAmount: Option[BigDecimal],
    empId: Int
  ): Boolean =
    FormHelper
      .areEqual(cache.get(s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$empId"), newAmount map (_.toString()))

  def estimatedPayPage(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      val employerFuture = IncomeSource.create(journeyCacheService)

      val result = for {
        incomeSource  <- OptionT(employerFuture.map(_.toOption))
        income        <- OptionT.liftF(incomeService.employmentAmount(nino, incomeSource.id))
        cache         <- OptionT.liftF(journeyCacheService.currentCache)
        calculatedPay <- OptionT.liftF(incomeService.calculateEstimatedPay(cache, income.startDate))
        payment       <- OptionT.liftF(incomeService.latestPayment(nino, incomeSource.id))
      } yield {

        val payYearToDate: BigDecimal = payment.map(_.amountYearToDate).getOrElse(BigDecimal(0))
        val paymentDate: Option[LocalDate] = payment.map(_.date)

        calculatedPay.grossAnnualPay match {
          case newAmount if isCachedAmountSameAsEnteredAmount(cache, newAmount, empId) =>
            Future.successful(Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache(empId)))
          case Some(newAmount) if newAmount > payYearToDate =>
            val cache = Map(
              UpdateIncomeConstants.GrossAnnualPayKey -> calculatedPay.grossAnnualPay.map(_.toString).getOrElse(""),
              UpdateIncomeConstants.NewAmountKey      -> calculatedPay.netAnnualPay.map(_.toString).getOrElse("")
            )

            val isBonusPayment = cache.getOrElse(UpdateIncomeConstants.BonusPaymentsKey, "") == "Yes"

            journeyCache(cacheMap = cache) map { _ =>
              val viewModel = EstimatedPayViewModel(
                calculatedPay.grossAnnualPay,
                calculatedPay.netAnnualPay,
                isBonusPayment,
                calculatedPay.annualAmount,
                calculatedPay.startDate,
                incomeSource
              )

              Ok(estimatedPay(viewModel))
            }
          case _ =>
            Future.successful(
              Ok(incorrectTaxableIncome(payYearToDate, paymentDate.getOrElse(LocalDate.now), incomeSource.id, empId))
            )
        }
      }

      result.value.flatMap(_.sequence).map {
        case Some(result) => result
        case None         => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad)
      }
  }
}
