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
import org.joda.time.LocalDate
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.cacheResolver.estimatedPay.UpdatedEstimatedPayJourneyCache
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.IncomeService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.{JourneyCacheConstants, TaiConstants}
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.EstimatedPayViewModel
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future

class IncomeUpdateEstimatedPayController @Inject()(
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  incomeService: IncomeService,
  @Named("Update Income") implicit val journeyCacheService: JourneyCacheService,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)
    extends TaiBaseController with JourneyCacheConstants with UpdatedEstimatedPayJourneyCache {

  def estimatedPayLandingPage(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    journeyCacheService.mandatoryValues(UpdateIncome_NameKey, UpdateIncome_IdKey, UpdateIncome_IncomeTypeKey) map {
      mandatoryValues =>
        val incomeName :: incomeId :: incomeType :: Nil = mandatoryValues.toList
        Ok(
          views.html.incomes.estimatedPayLandingPage(
            incomeName,
            incomeId.toInt,
            incomeType == TaiConstants.IncomeTypePension
          ))
    }
  }

  private def isCachedAmountSameAsEnteredAmount(cache: Map[String, String], newAmount: Option[BigDecimal]): Boolean =
    FormHelper.areEqual(cache.get(UpdateIncome_ConfirmedNewAmountKey), newAmount map (_.toString()))

  def estimatedPayPage: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser
    val nino = user.nino

    val employerFuture = IncomeSource.create(journeyCacheService)

    val result: Future[Future[Result]] = for {
      employer      <- employerFuture
      income        <- incomeService.employmentAmount(nino, employer.id)
      cache         <- journeyCacheService.currentCache
      calculatedPay <- incomeService.calculateEstimatedPay(cache, income.startDate)
      payment       <- incomeService.latestPayment(nino, employer.id)
    } yield {

      val payYearToDate: BigDecimal = payment.map(_.amountYearToDate).getOrElse(BigDecimal(0))
      val paymentDate: Option[LocalDate] = payment.map(_.date)

      calculatedPay.grossAnnualPay match {
        case newAmount if (isCachedAmountSameAsEnteredAmount(cache, newAmount)) =>
          Future.successful(Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache()))
        case Some(newAmount) if newAmount > payYearToDate =>
          val cache = Map(
            UpdateIncome_GrossAnnualPayKey -> calculatedPay.grossAnnualPay.map(_.toString).getOrElse(""),
            UpdateIncome_NewAmountKey      -> calculatedPay.netAnnualPay.map(_.toString).getOrElse("")
          )

          val isBonusPayment = cache.getOrElse(UpdateIncome_BonusPaymentsKey, "") == "Yes"

          journeyCache(cacheMap = cache) map { _ =>
            val viewModel = EstimatedPayViewModel(
              calculatedPay.grossAnnualPay,
              calculatedPay.netAnnualPay,
              isBonusPayment,
              calculatedPay.annualAmount,
              calculatedPay.startDate,
              employer)

            Ok(views.html.incomes.estimatedPay(viewModel))
          }
        case _ =>
          Future.successful(
            Ok(views.html.incomes
              .incorrectTaxableIncome(payYearToDate, paymentDate.getOrElse(new LocalDate), employer.id)))
      }
    }

    result.flatMap(identity)
  }
}
