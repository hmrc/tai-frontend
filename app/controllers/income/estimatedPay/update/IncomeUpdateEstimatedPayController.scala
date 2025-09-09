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

import cats.data.OptionT
import cats.implicits._
import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.income._
import play.api.mvc._
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import uk.gov.hmrc.tai.model.{CalculatedPay, EmploymentAmount}
import uk.gov.hmrc.tai.service.{EmploymentService, IncomeService}
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.EstimatedPayViewModel
import views.html.incomes.{EstimatedPayLandingPageView, EstimatedPayView, IncorrectTaxableIncomeView}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class IncomeUpdateEstimatedPayController @Inject() (
  authenticate: AuthJourney,
  incomeService: IncomeService,
  employmentService: EmploymentService,
  appConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  estimatedPayLandingPage: EstimatedPayLandingPageView,
  estimatedPay: EstimatedPayView,
  incorrectTaxableIncome: IncorrectTaxableIncomeView,
  journeyCacheRepository: JourneyCacheRepository,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def estimatedPayLandingPage(empId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino

      employmentService
        .employment(nino, empId)
        .map {
          case Some(emp) =>
            Ok(
              estimatedPayLandingPage(
                emp.name,
                empId,
                emp.receivingOccupationalPension,
                appConfig
              )
            )
          case None      =>
            Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
        }
    }

  def estimatedPayPage(empId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino                      = user.nino
      val ua                        = request.userAnswers

      val prep: OptionT[
        Future,
        (
          uk.gov.hmrc.tai.model.domain.Employment,
          EmploymentAmount,
          Map[String, String],
          CalculatedPay,
          Option[uk.gov.hmrc.tai.model.domain.Payment]
        )
      ] =
        for {
          emp           <- OptionT(employmentService.employment(nino, empId))
          incomeAmount  <- OptionT.liftF(incomeService.employmentAmount(nino, empId))
          cacheMap       = ua.data.fields.collect {
                             case (k, v: play.api.libs.json.JsString) => k -> v.value
                             case (k, v: play.api.libs.json.JsNumber) => k -> v.value.toString
                           }.toMap
          calculatedPay <- OptionT.liftF(incomeService.calculateEstimatedPay(cacheMap, incomeAmount.startDate))
          latestPayment <- OptionT.liftF(incomeService.latestPayment(nino, empId))
        } yield (emp, incomeAmount, cacheMap, calculatedPay, latestPayment)

      prep.value
        .flatMap {
          case Some((emp, _incomeAmount, cacheMap, calculatedPay, latestPayment)) =>
            val payYtd: BigDecimal                   = latestPayment.map(_.amountYearToDate).getOrElse(BigDecimal(0))
            val latestPaymentDate: Option[LocalDate] = latestPayment.map(_.date)

            val confirmedNewAmountStr                                                                        = ua.get(UpdateIncomeConfirmedNewAmountPage(empId))
            def isConfirmedAmountSameAs(grossOpt: Option[BigDecimal], cachedStrOpt: Option[String]): Boolean =
              (grossOpt, cachedStrOpt) match {
                case (Some(g), Some(s)) => Try(BigDecimal(FormHelper.stripNumber(s))).toOption.contains(g)
                case _                  => false
              }

            if (isConfirmedAmountSameAs(calculatedPay.grossAnnualPay, confirmedNewAmountStr)) {
              Future.successful(Redirect(controllers.routes.IncomeController.sameEstimatedPayInCache(empId)))
            } else {
              calculatedPay.grossAnnualPay match {
                case Some(gross) if gross > payYtd =>
                  val cacheToWrite = Map(
                    UpdateIncomeGrossAnnualPayPage.toString -> calculatedPay.grossAnnualPay
                      .map(_.toString)
                      .getOrElse(""),
                    UpdateIncomeNewAmountPage.toString      -> calculatedPay.netAnnualPay.map(_.toString).getOrElse("")
                  )

                  val updatedAnswers =
                    ua.copy(data =
                      ua.data ++ play.api.libs.json.Json.toJson(cacheToWrite).as[play.api.libs.json.JsObject]
                    )

                  val isBonusPayment =
                    cacheMap.getOrElse(UpdateIncomeBonusPaymentsPage, "") == FormValuesConstants.YesValue

                  val viewModel = EstimatedPayViewModel(
                    calculatedPay.grossAnnualPay,
                    calculatedPay.netAnnualPay,
                    isBonusPayment,
                    calculatedPay.annualAmount,
                    calculatedPay.startDate,
                    IncomeSource(id = empId, name = emp.name)
                  )

                  journeyCacheRepository
                    .set(updatedAnswers)
                    .map(_ => Ok(estimatedPay(viewModel)))

                case _ =>
                  Future.successful(
                    Ok(
                      incorrectTaxableIncome(
                        payYtd,
                        latestPaymentDate.getOrElse(LocalDate.now),
                        empId,
                        empId
                      )
                    )
                  )
              }
            }

          case None =>
            Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
        }
    }
}
