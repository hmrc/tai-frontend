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
import controllers.auth.AuthJourney
import pages.TrackSuccessfulJourneyUpdateEstimatedPayPage
import pages.benefits.EndCompanyBenefitsUpdateIncomePage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TemporarilyUnavailable
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.{EmploymentService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.IncomeSourceSummaryViewModel
import views.html.benefits.CompanyBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class CompanyBenefitsSummaryController @Inject() (
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  benefitsService: BenefitsService,
  authenticate: AuthJourney,
  applicationConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  companyBenefits: CompanyBenefitsView,
  journeyCacheNewRepository: JourneyCacheNewRepository,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  // scalastyle:off method.length
  def onPageLoad(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val nino = request.taiUser.nino

    val cacheUpdatedIncomeAmountFuture =
      request.userAnswers.get(EndCompanyBenefitsUpdateIncomePage(empId)).map(_.toInt)

    val hasJourneyCompleted =
      request.userAnswers.get(TrackSuccessfulJourneyUpdateEstimatedPayPage(empId)).contains("true")

    val incomeDetailsResult = for {
      taxCodeIncomes           <- taxAccountService.taxCodeIncomes(nino, TaxYear())
      employment               <- employmentService.employment(nino, empId)
      benefitsDetails          <- benefitsService.benefits(nino, TaxYear().year)
      estimatedPayCompletion   <- Future.successful(hasJourneyCompleted)
      cacheUpdatedIncomeAmount <- Future.successful(cacheUpdatedIncomeAmountFuture)
    } yield (taxCodeIncomes, employment, benefitsDetails, estimatedPayCompletion, cacheUpdatedIncomeAmount)

    incomeDetailsResult
      .flatMap {
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
            journeyCacheNewRepository.clear(request.userAnswers.id).map(_ => (): Unit)
          } else {
            Future.successful((): Unit)
          }
          result.map(_ => Ok(companyBenefits(incomeDetailsViewModel)))

        case _ =>
          Future.successful(errorPagesHandler.internalServerError("Error while fetching company benefits details"))
      }
      .recover { case NonFatal(e) =>
        errorPagesHandler.internalServerError("CompanyBenefitsSummaryController exception", Some(e))
      }
  }

}
