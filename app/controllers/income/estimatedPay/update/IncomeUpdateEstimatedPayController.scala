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

import cats.data._
import cats.implicits._
import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.income._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.service.{IncomeService, TaxAccountService}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.EstimatedPayViewModel
import views.html.incomes.{EstimatedPayLandingPageView, EstimatedPayView, IncorrectTaxableIncomeView}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeUpdateEstimatedPayController @Inject() (
  authenticate: AuthJourney,
  incomeService: IncomeService,
  appConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  taxAccountService: TaxAccountService,
  estimatedPayLandingPage: EstimatedPayLandingPageView,
  estimatedPay: EstimatedPayView,
  incorrectTaxableIncome: IncorrectTaxableIncomeView,
  journeyCacheNewRepository: JourneyCacheNewRepository,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def estimatedPayLandingPage(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val userAnswers = request.userAnswers

      val incomeName = userAnswers.get(UpdateIncomeNamePage).getOrElse("")
      val incomeType = userAnswers.get(UpdateIncomeTypePage).getOrElse("")
      taxAccountService
        .taxAccountSummary(user.nino, TaxYear())
        .map { _ =>
          Ok(
            estimatedPayLandingPage(
              incomeName,
              empId,
              incomeType == TaiConstants.IncomeTypePension,
              appConfig
            )
          )
        }
        .recover { case e: Exception =>
          errorPagesHandler.internalServerError(e.getMessage)
        }
  }

  private def isCachedAmountSameAsEnteredAmount(
    cache: Map[String, String],
    newAmount: Option[BigDecimal],
    empId: Int
  ): Boolean =
    FormHelper
      .areEqual(cache.get(s"${UpdateIncomeConstants.ConfirmedNewAmountKey}-$empId"), newAmount map (_.toString()))

  def estimatedPayPage(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val nino = user.nino

    val employerFuture = IncomeSource.create(journeyCacheNewRepository, request.userAnswers)

    val result = for {
      incomeSource  <- OptionT(employerFuture.map(_.toOption))
      income        <- OptionT.liftF(incomeService.employmentAmount(nino, incomeSource.id))
      cache         <- OptionT.liftF(Future.successful(request.userAnswers.data.as[Map[String, String]]))
      calculatedPay <- OptionT.liftF(incomeService.calculateEstimatedPay(cache, income.startDate))
      payment       <- OptionT.liftF(incomeService.latestPayment(nino, incomeSource.id))
    } yield {
      val payYearToDate: BigDecimal = payment.map(_.amountYearToDate).getOrElse(BigDecimal(0))
      val paymentDate: Option[LocalDate] = payment.map(_.date)

      calculatedPay.grossAnnualPay match {
        case newAmount if isCachedAmountSameAsEnteredAmount(cache, newAmount, empId) =>
          Future.successful(Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache(empId)))
        case Some(newAmount) if newAmount > payYearToDate =>
          val cacheMap = Map(
            UpdateIncomeGrossAnnualPayPage.toString -> calculatedPay.grossAnnualPay.map(_.toString).getOrElse(""),
            UpdateIncomeNewAmountPage.toString      -> calculatedPay.netAnnualPay.map(_.toString).getOrElse("")
          )

          val isBonusPayment = cacheMap.getOrElse(UpdateIncomeConstants.BonusPaymentsKey, "") == "Yes"
          val updatedAnswers =
            request.userAnswers.copy(data = request.userAnswers.data ++ Json.toJson(cacheMap).as[JsObject])

          journeyCacheNewRepository.set(updatedAnswers) map { _ =>
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
      case None         => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

}
