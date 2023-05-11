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

package controllers

import com.google.inject.name.Named
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import cats._
import cats.implicits._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TemporarilyUnavailable
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.service.{EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.util.constants.TaiConstants.UpdateIncomeConfirmedAmountKey
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel
import views.html.IncomeSourceSummaryView

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class IncomeSourceSummaryController @Inject()(
  val auditConnector: AuditConnector,
  @Named("Update Income") journeyCacheService: JourneyCacheService,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  benefitsService: BenefitsService,
  estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  incomeSourceSummary: IncomeSourceSummaryView,
  implicit val errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def onPageLoad(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    val cacheUpdatedIncomeAmountFuture =
      journeyCacheService.currentValueAsInt(s"$UpdateIncomeConfirmedAmountKey-$empId")

    (
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      employmentService.employment(nino, empId),
      benefitsService.benefits(nino, TaxYear().year),
      estimatedPayJourneyCompletionService.hasJourneyCompleted(empId.toString),
      cacheUpdatedIncomeAmountFuture).mapN {
      case (
          Right(taxCodeIncomes),
          Some(employment),
          benefitsDetails,
          estimatedPayCompletion,
          cacheUpdatedIncomeAmount) =>
        val rtiAvailable = employment.latestAnnualAccount.exists(_.realTimeStatus != TemporarilyUnavailable)

        val incomeDetailsViewModel = IncomeSourceSummaryViewModel(
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
          journeyCacheService.flushWithEmpId(empId)
        }

        Ok(incomeSourceSummary(incomeDetailsViewModel))
      case _ => errorPagesHandler.internalServerError("Error while fetching income summary details")
    } recover {
      case NonFatal(e) => errorPagesHandler.internalServerError("IncomeSourceSummaryController exception", Some(e))
    }
  }
}
