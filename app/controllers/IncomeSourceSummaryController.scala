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

import cats.implicits.*
import controllers.auth.AuthJourney
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import pages.income.UpdateIncomeConfirmedNewAmountPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repository.JourneyCacheRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, TemporarilyUnavailable}
import uk.gov.hmrc.tai.service.{EmploymentService, RtiService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel
import views.html.IncomeSourceSummaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class IncomeSourceSummaryController @Inject() (
  val auditConnector: AuditConnector,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  incomeSourceSummary: IncomeSourceSummaryView,
  journeyCacheRepository: JourneyCacheRepository,
  rtiService: RtiService,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  private def isRTIAvailable(payments: Either[UpstreamErrorResponse, Seq[AnnualAccount]]): Boolean =
    payments.fold(
      _ => false,
      seqAA => {
        val latestAnnualAccount: Option[AnnualAccount] = if (seqAA.isEmpty) None else Some(seqAA.max)
        latestAnnualAccount.exists(_.realTimeStatus != TemporarilyUnavailable)
      }
    )

  def onPageLoad(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>

    val nino = request.taiUser.nino

    val cacheUpdatedIncomeAmountFuture =
      Future.successful(request.userAnswers.get(EndCompanyBenefitsUpdateIncomePage(empId)).map(_.toInt))

    val hasJourneyCompleted: Boolean = request.userAnswers
      .get(TrackSuccessfulJourneyUpdateEstimatedPayPage(empId))
      .getOrElse(false)

    (
      employmentService.employmentOnly(nino, empId, TaxYear()),
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      rtiService.getPaymentsForYear(nino, TaxYear()).value,
      Future.successful(hasJourneyCompleted),
      cacheUpdatedIncomeAmountFuture
    ).mapN {
      case (
            Some(employment),
            taxCodeIncomes,
            payments,
            estimatedPayCompletion,
            cacheUpdatedIncomeAmount
          ) =>
        val incomeDetailsViewModel = IncomeSourceSummaryViewModel.applyNew(
          empId = empId,
          displayName = request.fullName,
          taxCodeIncomes.fold(_ => None, _.find(_.employmentId.fold(false)(_ == employment.sequenceNumber))),
          employment = employment,
          payments = payments.toOption.flatMap(_.find(_.sequenceNumber == employment.sequenceNumber)),
          estimatedPayJourneyCompleted = estimatedPayCompletion,
          rtiAvailable = isRTIAvailable(payments),
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
