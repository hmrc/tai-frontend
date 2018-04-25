/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.{PersonService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.TaxCodeViewModel

import scala.concurrent.Future

trait YourTaxCodeController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with FeatureTogglesConfig {

  def personService: PersonService

  def taxAccountService: TaxAccountService

  def taxCodes(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.taiRoot.nino)

            for {
              TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]) <- taxAccountService.taxCodeIncomes(nino, TaxYear())
              scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, TaxYear(), taxCodeIncomes)
            } yield {
              val taxCodeViewModel = TaxCodeViewModel(taxCodeIncomes, scottishTaxRateBands)
              Ok(views.html.taxCodeDetails(taxCodeViewModel))
            }
          }
  }
}

object YourTaxCodeController extends YourTaxCodeController with AuthenticationConnectors {
  override val personService = PersonService
  override val taxAccountService: TaxAccountService = TaxAccountService

  override implicit def templateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}

