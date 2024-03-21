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

package controllers.benefits

import cats.implicits._
import com.google.inject.name.Named
import controllers.{ErrorPagesHandler, TaiBaseController}
import controllers.auth.AuthJourney
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TemporarilyUnavailable
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.service.{EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.util.constants.TaiConstants.UpdateIncomeConfirmedAmountKey
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel
import views.html.benefits.CompanyBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class CompanyBenefitsSummaryController @Inject() (
  val auditConnector: AuditConnector,
  @Named("Update Income") journeyCacheService: JourneyCacheService,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  benefitsService: BenefitsService,
  estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  companyBenefits: CompanyBenefitsView,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def onPageLoad(empId: Int): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino = request.taiUser.nino

    val cacheUpdatedIncomeAmountFuture =
      journeyCacheService.currentValueAsInt(s"$UpdateIncomeConfirmedAmountKey-$empId")

    val incomeDetailsResult = (
      taxAccountService.taxCodeIncomes(nino, TaxYear()),
      employmentService.employment(nino, empId),
      benefitsService.benefits(nino, TaxYear().year),
      estimatedPayJourneyCompletionService.hasJourneyCompleted(empId.toString),
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

        val result = if (!incomeDetailsViewModel.isUpdateInProgress) {
          journeyCacheService.flushWithEmpId(empId).map(_ => (): Unit)
        } else {
          Future.successful((): Unit)
        }
        result.map(_ => Ok(companyBenefits(incomeDetailsViewModel)))

      case _ =>
        Future.successful(errorPagesHandler.internalServerError("Error while fetching company benefits details"))
    } recover { case NonFatal(e) =>
      Future.successful(errorPagesHandler.internalServerError("CompanyBenefitsSummaryController exception", Some(e)))
    }
    incomeDetailsResult.flatten
  }

}
