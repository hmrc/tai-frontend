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


import controllers.{ErrorPagesHandler, TaiBaseController}
import controllers.auth.{AuthJourney, AuthenticatedRequest}
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{Person, TemporarilyUnavailable}
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCompletion.EstimatedPayJourneyCompletionService
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel
import views.html.benefits.CompanyBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class CompanyBenefitsSummaryController @Inject() (
  val auditConnector: AuditConnector,
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  benefitsService: BenefitsService,
  estimatedPayJourneyCompletionService: EstimatedPayJourneyCompletionService,
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  personService: PersonService,
  companyBenefits: CompanyBenefitsView,
  journeyCacheNewRepository: JourneyCacheNewRepository,
  errorPagesHandler: ErrorPagesHandler,
  implicit val messages: Messages
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with I18nSupport {


  // scalastyle:off method.length
  def onPageLoad(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val nino = request.taiUser.nino

    val cacheUpdatedIncomeAmountFuture =
      request.userAnswers.get(EndCompanyBenefitsUpdateIncomePage(empId)).map(_.toInt)

    val incomeDetailsResult = for {
      taxCodeIncomes           <- taxAccountService.taxCodeIncomes(nino, TaxYear())
      employment               <- employmentService.employment(nino, empId)
      benefitsDetails          <- benefitsService.benefits(nino, TaxYear().year)
      estimatedPayCompletion   <- estimatedPayJourneyCompletionService.hasJourneyCompleted(empId.toString)
      cacheUpdatedIncomeAmount <- Future.successful(cacheUpdatedIncomeAmountFuture)
    } yield (taxCodeIncomes, employment, benefitsDetails, estimatedPayCompletion, cacheUpdatedIncomeAmount)

    incomeDetailsResult.flatMap {
      case (Right(taxCodeIncomes), Some(employment), benefitsDetails, estimatedPayCompletion, cacheUpdatedIncomeAmount) =>
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
          journeyCacheNewRepository.clear(request.userAnswers.sessionId, nino.nino).map(_ => (): Unit)
        } else {
          Future.successful((): Unit)
        }
        val person: Future[Person] = personService.personDetailsFuture(nino)
        person.flatMap { person =>
          val authRequest = AuthenticatedRequest(request.request, request.taiUser, person)
          result.map(_ => Ok(companyBenefits(incomeDetailsViewModel)(authRequest, messages)))
        }

      case _ =>
        Future.successful(errorPagesHandler.internalServerError("Error while fetching company benefits details"))
    }.recover { case NonFatal(e) =>
     errorPagesHandler.internalServerError("CompanyBenefitsSummaryController exception", Some(e))
    }
  }

}
