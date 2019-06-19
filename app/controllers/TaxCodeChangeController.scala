/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.yourTaxFreeAmount.{DescribedYourTaxFreeAmountService, TaxCodeChangeReasonsService}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{IabdTaxCodeChangeReasons, YourTaxFreeAmount}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.TaxCodeChangeViewModel

import scala.concurrent.Future

class TaxCodeChangeController @Inject()(taxCodeChangeService: TaxCodeChangeService,
                                        taxAccountService: TaxAccountService,
                                        describedYourTaxFreeAmountService: DescribedYourTaxFreeAmountService,
                                        authenticate: AuthAction,
                                        validatePerson: ValidatePerson,
                                        yourTaxFreeAmountService: YourTaxFreeAmountService,
                                        taxCodeChangeReasonsService: TaxCodeChangeReasonsService,
                                        override implicit val partialRetriever: FormPartialRetriever,
                                        override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with FeatureTogglesConfig
  with YourTaxFreeAmount {

  def taxCodeComparison: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      val nino: Nino = request.taiUser.nino

      val totalTaxFuture = taxAccountService.totalTax(nino, TaxYear())
      val yourTaxFreeAmountComparisonFuture = yourTaxFreeAmountService.taxFreeAmountComparison(nino)

      for {
        yourTaxFreeAmountComparison <- yourTaxFreeAmountComparisonFuture
        totalTax <- totalTaxFuture
        taxCodeChange <- taxCodeChangeService.taxCodeChange(nino)
        scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, TaxYear(), taxCodeChange.uniqueTaxCodes)
      } yield {
        (totalTax) match {
          case (TaiSuccessResponseWithPayload(totalTax: TotalTax)) =>
            val iabdTaxCodeChangeReasons: IabdTaxCodeChangeReasons = new IabdTaxCodeChangeReasons(totalTax)
            val taxCodeChangeReasons = taxCodeChangeReasonsService.combineTaxCodeChangeReasons(
              iabdTaxCodeChangeReasons,
              yourTaxFreeAmountComparison.iabdPairs,
              taxCodeChange)
            val isAGenericReason = taxCodeChangeReasonsService.isAGenericReason(taxCodeChangeReasons)

            val viewModel = TaxCodeChangeViewModel(taxCodeChange, scottishTaxRateBands, taxCodeChangeReasons, isAGenericReason)

            implicit val user = request.taiUser
            Ok(views.html.taxCodeChange.taxCodeComparison(viewModel))
          case _ => throw new RuntimeException("Failed to fetch total tax details for tax code comparison")
        }
      }
  }

  def yourTaxFreeAmount: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      val nino: Nino = request.taiUser.nino
      val taxFreeAmountViewModel = describedYourTaxFreeAmountService.taxFreeAmountComparison(nino)

      implicit val user = request.taiUser

      taxFreeAmountViewModel.map(viewModel => {
        Ok(views.html.taxCodeChange.yourTaxFreeAmount(viewModel))
      })
  }

  def whatHappensNext: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      Future.successful(Ok(views.html.taxCodeChange.whatHappensNext()))
  }
}
