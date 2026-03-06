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

import controllers.auth.{AuthJourney, AuthedUser, AuthenticatedRequest}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.*
import uk.gov.hmrc.tai.service.yourTaxFreeAmount.{DescribedYourTaxFreeAmountService, TaxCodeChangeReasonsService}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{IabdTaxCodeChangeReasons, YourTaxFreeAmount}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.TaxCodeChangeViewModel
import views.html.taxCodeChange.{TaxCodeComparisonView, WhatHappensNextView, YourTaxFreeAmountView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.tai.util.EitherTExtensions.EitherTThrowableOps

class TaxCodeChangeController @Inject() (
  taxCodeChangeService: TaxCodeChangeService,
  taxAccountService: TaxAccountService,
  describedYourTaxFreeAmountService: DescribedYourTaxFreeAmountService,
  authenticate: AuthJourney,
  yourTaxFreeAmountService: YourTaxFreeAmountService,
  taxCodeChangeReasonsService: TaxCodeChangeReasonsService,
  appConfig: ApplicationConfig,
  mcc: MessagesControllerComponents,
  taxCodeComparisonView: TaxCodeComparisonView,
  yourTaxFreeAmountView: YourTaxFreeAmountView,
  whatHappensNextView: WhatHappensNextView,
  errorPagesHandler: ErrorPagesHandler
)(implicit val ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with YourTaxFreeAmount
    with Logging {

  def taxCodeComparison: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val messages = request2Messages
    val nino     = request.taiUser.nino

    taxCodeChangeService
      .hasTaxCodeChanged(nino)
      .value
      .flatMap {
        case Right(false) => Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
        case Right(true)  => buildTaxCodeComparisonResult
        case Left(error)  =>
          logger.error(error.message)
          Future.successful(InternalServerError(errorPagesHandler.error5xx(messages("tai.technical.error.message"))))
      }
      .recover { case exception =>
        logger.error("Failed to build tax code comparison result", exception)
        InternalServerError(errorPagesHandler.error5xx(messages("tai.technical.error.message")))
      }
  }

  private def buildTaxCodeComparisonResult(implicit request: AuthenticatedRequest[_]): Future[Result] = {
    val nino: Nino = request.taiUser.nino
    for {
      taxCodeChange               <- taxCodeChangeService.taxCodeChange(nino).toFutureOrThrow
      yourTaxFreeAmountComparison <- yourTaxFreeAmountService.taxFreeAmountComparison(nino, taxCodeChange)
      scottishTaxRateBands        <- taxAccountService.scottishBandRates(nino, TaxYear(), taxCodeChange.uniqueTaxCodes)
    } yield {
      val iabdTaxCodeChangeReasons: IabdTaxCodeChangeReasons = new IabdTaxCodeChangeReasons
      val taxCodeChangeReasons                               = taxCodeChangeReasonsService
        .combineTaxCodeChangeReasons(iabdTaxCodeChangeReasons, yourTaxFreeAmountComparison.iabdPairs, taxCodeChange)
      val isAGenericReason                                   = taxCodeChangeReasonsService.isAGenericReason(taxCodeChangeReasons)
      val maybeUserName                                      = Some(request.fullName)

      val viewModel =
        TaxCodeChangeViewModel(
          taxCodeChange,
          scottishTaxRateBands,
          taxCodeChangeReasons,
          isAGenericReason,
          maybeUserName
        )

      implicit val user: AuthedUser = request.taiUser
      Ok(taxCodeComparisonView(viewModel, appConfig))
    }
  }

  def yourTaxFreeAmount: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino: Nino             = request.taiUser.nino
    val taxFreeAmountViewModel = describedYourTaxFreeAmountService.taxFreeAmountComparison(nino)

    implicit val user: AuthedUser = request.taiUser

    taxFreeAmountViewModel.map(viewModel => Ok(yourTaxFreeAmountView(viewModel)))
  }

  def whatHappensNext: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    Future.successful(Ok(whatHappensNextView()))
  }
}
