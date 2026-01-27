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

import cats.data.EitherT
import controllers.auth.{AuthJourney, DataRequest}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.{IncomeSources, TaxYear}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, PensionIncome, TaxAccountSummary, TaxedIncome}
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome}
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.util.constants.AuditConstants
import uk.gov.hmrc.tai.viewModels.TaxAccountSummaryViewModel
import views.html.IncomeTaxSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class TaxAccountSummaryController @Inject() (
  taxAccountService: TaxAccountService,
  employmentService: EmploymentService,
  iabdService: IabdService,
  auditService: AuditService,
  authenticate: AuthJourney,
  appConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  incomeTaxSummary: IncomeTaxSummaryView,
  trackingService: TrackingService,
  implicit val
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with Logging {

  private def optionalTaxCodeIncomes(nino: Nino, year: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Seq[TaxCodeIncome]] =
    taxAccountService
      .newTaxCodeIncomes(nino, year)
      .transform {
        case Right(response)                                                             => Right(response)
        case Left(UpstreamErrorResponse(_, statusCode, _, _)) if statusCode == NOT_FOUND => Right(Seq.empty)
        case Left(error)                                                                 => Left(error)
      }

  private def optionalTaxAccountSummary(nino: Nino, taxYear: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Option[TaxAccountSummary]] =
    taxAccountService
      .taxAccountSummary(nino, taxYear)
      .transform {
        case Left(error: UpstreamErrorResponse) if error.statusCode == NOT_FOUND => Right(None)
        case Right(account)                                                      => Right(Some(account))
        case Left(error)                                                         => Left(error)
      }

  private def optionalIsAnyIFormInProgress(nino: Nino)(implicit
    hc: HeaderCarrier,
    request: DataRequest[AnyContent]
  ): EitherT[Future, UpstreamErrorResponse, TimeToProcess] =
    EitherT(
      trackingService
        .isAnyIFormInProgress(nino.nino)
        .map { tracking =>
          Right(tracking)
        }
        .recover { case NonFatal(_) =>
          Right(NoTimeToProcess)
        }
    )

  def onPageLoad: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val nino = request.taiUser.nino

    auditService
      .createAndSendAuditEvent(AuditConstants.TaxAccountSummaryUserEntersSummaryPage, Map("nino" -> nino.toString()))

    (for {
      employments               <- employmentService.employments(nino, TaxYear())
      nonTaxCodeIncomes         <- taxAccountService.newNonTaxCodeIncomes(nino, TaxYear())
      employmentsFromTaxAccount <- optionalTaxCodeIncomes(nino, TaxYear())
      taxAccountSummary         <- optionalTaxAccountSummary(nino, TaxYear())
      isAnyFormInProgress       <- optionalIsAnyIFormInProgress(nino)
      iabds                     <- iabdService.getIabds(nino, TaxYear())
    } yield {
      val livePensions = employments
        .filter(employment => employment.employmentType == PensionIncome && employment.employmentStatus == Live)
        .map { employment =>
          val tci = employmentsFromTaxAccount.find(_.employmentId.contains(employment.sequenceNumber))
          TaxedIncome(tci, employment)
        }

      val liveEmployments = employments
        .filter(employment => employment.employmentType == EmploymentIncome && employment.employmentStatus == Live)
        .map { employment =>
          val tci = employmentsFromTaxAccount.find(_.employmentId.contains(employment.sequenceNumber))
          TaxedIncome(tci, employment)
        }

      val ceasedEmployments = employments
        .filter(employment => employment.employmentType == EmploymentIncome && employment.employmentStatus != Live)
        .map { employment =>
          val tci = employmentsFromTaxAccount.find(_.employmentId.contains(employment.sequenceNumber))
          TaxedIncome(tci, employment)
        }

      val incomeSources = IncomeSources(
        livePensionIncomeSources = livePensions,
        liveEmploymentIncomeSources = liveEmployments,
        ceasedEmploymentIncomeSources = ceasedEmployments
      )

      val vm = TaxAccountSummaryViewModel(
        taxAccountSummary,
        isAnyFormInProgress,
        nonTaxCodeIncomes,
        incomeSources,
        iabds
      )

      Ok(incomeTaxSummary(vm, appConfig))
    }).leftMap(error => errorPagesHandler.internalServerError(error.message)).merge
  }
}
