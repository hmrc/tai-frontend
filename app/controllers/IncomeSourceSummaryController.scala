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

import cats.implicits._
import controllers.auth.{AuthJourney, DataRequest}
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import pages.income.UpdateIncomeConfirmedNewAmountPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repository.JourneyCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TemporarilyUnavailable
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.{EmploymentService, RtiService, TaxAccountService}
import uk.gov.hmrc.tai.util.ApiBackendChoice
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel
import views.html.IncomeSourceSummaryView

import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

class IncomeSourceSummaryController @Inject() (
  val auditConnector: AuditConnector,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  benefitsService: BenefitsService,
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  incomeSourceSummary: IncomeSourceSummaryView,
  journeyCacheRepository: JourneyCacheRepository,
  rtiService: RtiService,
  apiBackendChoice: ApiBackendChoice, // TODO: DDCNL-10086 New API
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def onPageLoad(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    if (apiBackendChoice.isNewApiBackendEnabled) {
      onPageLoadNew(empId)
    } else {
      onPageLoadOld(empId)
    }
  }

  private def onPageLoadOld(empId: Int)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val nino = request.taiUser.nino

    val cacheUpdatedIncomeAmountFuture =
      Future.successful(request.userAnswers.get(EndCompanyBenefitsUpdateIncomePage(empId)).map(_.toInt))

    val hasJourneyCompleted: Boolean = request.userAnswers
      .get(TrackSuccessfulJourneyUpdateEstimatedPayPage(empId))
      .getOrElse(false)

    (
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      employmentService.employment(nino, empId),
      benefitsService.benefits(nino, TaxYear().year),
      Future.successful(hasJourneyCompleted),
      cacheUpdatedIncomeAmountFuture
    ).mapN {
      case (
            Right(taxCodeIncomes),
            Some(employment),
            benefitsDetails,
            estimatedPayCompletion,
            cacheUpdatedIncomeAmount
          ) =>
        val rtiAvailable = employment.latestAnnualAccount.exists(_.realTimeStatus != TemporarilyUnavailable)
        val incomeDetailsViewModel = IncomeSourceSummaryViewModel.applyOld(
          empId,
          request.fullName,
          taxCodeIncomes,
          employment,
          benefitsDetails,
          estimatedPayCompletion,
          rtiAvailable,
          applicationConfig,
          cacheUpdatedIncomeAmount
        )

        if (!incomeDetailsViewModel.isUpdateInProgress) {
          val updatedUserAnswers = request.userAnswers.remove(UpdateIncomeConfirmedNewAmountPage(empId))
          journeyCacheRepository.set(updatedUserAnswers)
        }

        Ok(incomeSourceSummary(incomeDetailsViewModel))
      case _ => errorPagesHandler.internalServerError("Error while fetching income summary details")
    } recover { case NonFatal(e) =>
      errorPagesHandler.internalServerError("IncomeSourceSummaryController exception", Some(e))
    }
  }

  private def onPageLoadNew(empId: Int)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val nino = request.taiUser.nino

    val cacheUpdatedIncomeAmountFuture =
      Future.successful(request.userAnswers.get(EndCompanyBenefitsUpdateIncomePage(empId)).map(_.toInt))

    val hasJourneyCompleted: Boolean = request.userAnswers
      .get(TrackSuccessfulJourneyUpdateEstimatedPayPage(empId))
      .getOrElse(false)

    println(
      "\ntaxCodeIncomes response:" + Try(Await.result(taxAccountService.taxCodeIncomes(nino, TaxYear()), Duration.Inf))
    )
    println("\nRTI response:" + Try(Await.result(rtiService.getPaymentsForYear(nino, TaxYear()).value, Duration.Inf)))
    println(
      "\nemploymentsOnly response:" + Try(
        Await.result(employmentService.employmentOnly(nino, empId, TaxYear()), Duration.Inf)
      )
    )

    (
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      employmentService.employmentOnly(nino, empId, TaxYear()),
      rtiService.getPaymentsForYear(nino, TaxYear()).value,
      benefitsService.benefits(nino, TaxYear().year),
      Future.successful(hasJourneyCompleted),
      cacheUpdatedIncomeAmountFuture
    ).mapN {
      case (
            taxCodeIncomes,
            Some(employment),
            payments,
            benefitsDetails,
            estimatedPayCompletion,
            cacheUpdatedIncomeAmount
          ) =>
        val estimatedPay = taxCodeIncomes.fold(
          _ => None,
          incomes => incomes.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)).map(_.amount)
        ) // todo pull from iabd if missing from tax account

        val taxCode = taxCodeIncomes.fold(
          _ => None,
          incomes => incomes.find(_.employmentId.fold(false)(_ == employment.sequenceNumber)).map(_.taxCode)
        )

        val incomeDetailsViewModel = IncomeSourceSummaryViewModel.applyNew(
          empId = empId,
          displayName = request.fullName,
          estimatedPayAmount = estimatedPay,
          taxCode = taxCode,
          employment = employment,
          benefits = benefitsDetails,
          estimatedPayJourneyCompleted = estimatedPayCompletion,
          rtiAvailable = payments.isRight,
          applicationConfig = applicationConfig,
          cacheUpdatedIncomeAmount = cacheUpdatedIncomeAmount
        )

        if (!incomeDetailsViewModel.isUpdateInProgress) {
          val updatedUserAnswers = request.userAnswers.remove(UpdateIncomeConfirmedNewAmountPage(empId))
          journeyCacheRepository.set(updatedUserAnswers)
        }

        Ok(incomeSourceSummary(incomeDetailsViewModel))
      case _ => errorPagesHandler.internalServerError("Error while fetching income summary details")
    } recover { case NonFatal(e) =>
      errorPagesHandler.internalServerError("IncomeSourceSummaryController exception", Some(e))
    }
  }
}
