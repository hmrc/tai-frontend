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

import com.google.inject.Inject
import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.yourTaxFreeAmount.DescribedYourTaxFreeAmountService
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{YourTaxFreeAmount, YourTaxFreeAmountComparison}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}

import scala.concurrent.Future

class TaxCodeChangeController @Inject()(personService: PersonService,
                                        taxCodeChangeService: TaxCodeChangeService,
                                        taxAccountService: TaxAccountService,
                                        describedYourTaxFreeAmountService: DescribedYourTaxFreeAmountService,
                                        yourTaxFreeAmountService: YourTaxFreeAmountService,
                                        val auditConnector: AuditConnector,
                                        val delegationConnector: DelegationConnector,
                                        val authConnector: AuthConnector,
                                        override implicit val partialRetriever: FormPartialRetriever,
                                        override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with WithAuthorisedForTaiLite
  with DelegationAwareActions
  with Auditable
  with FeatureTogglesConfig
  with YourTaxFreeAmount {

  def taxCodeComparison: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino: Nino = Nino(user.getNino)

//            val taxFreeAmountFuture = yourTaxFreeAmountService.taxFreeAmountComparison(nino)

            for {
              taxCodeChange <- taxCodeChangeService.taxCodeChange(nino)
//              taxFreeAmountViewModel <- taxFreeAmountFuture
              scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, TaxYear(), taxCodeChange.uniqueTaxCodes)
            } yield {
              val taxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, scottishTaxRateBands)
//              val taxCodeChangeDynamicTextViewModel = TaxCodeChangeDynamicTextViewModel(taxFreeAmountViewModel)
              Ok(views.html.taxCodeChange.taxCodeComparison(taxCodeChangeViewModel))
            }
          }
  }

  def yourTaxFreeAmount: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)

            val taxFreeAmountViewModel = if(taxFreeAmountComparisonEnabled) {
              describedYourTaxFreeAmountService.taxFreeAmountComparison(nino)
            } else {
              describedYourTaxFreeAmountService.taxFreeAmount(nino)
            }

            taxFreeAmountViewModel.map(viewModel => {
              Ok(views.html.taxCodeChange.yourTaxFreeAmount(viewModel))
            })
          }
  }

  def whatHappensNext: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.taxCodeChange.whatHappensNext()))
          }
  }
}
