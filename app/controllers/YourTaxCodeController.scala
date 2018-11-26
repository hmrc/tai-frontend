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
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.{PersonService, TaxAccountService, TaxCodeChangeService}
import uk.gov.hmrc.tai.viewModels.{TaxCodeViewModel, TaxCodeViewModelPreviousYears}

import scala.concurrent.Future

trait YourTaxCodeController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with FeatureTogglesConfig {

  def personService: PersonService

  def taxAccountService: TaxAccountService

  def taxCodeChangeService: TaxCodeChangeService

  def taxCodes(year: TaxYear = TaxYear()): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = user.person.nino

            for {
              TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]) <- taxAccountService.taxCodeIncomes(nino, year)
              scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, year, taxCodeIncomes.map(_.taxCode))
            } yield {
              val taxCodeViewModel = TaxCodeViewModel.apply(taxCodeIncomes, scottishTaxRateBands)
              Ok(views.html.taxCodeDetails(taxCodeViewModel))
            }
          }
  }

  def prevTaxCodes(year: TaxYear): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>

          if (taxCodeChangeEnabled) {
            ServiceCheckLite.personDetailsCheck {
              val nino = user.person.nino

              for {
                taxCodeRecords <- taxCodeChangeService.lastTaxCodeRecordsInYearPerEmployment(nino, year)
                scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, year, taxCodeRecords.map(_.taxCode))
              } yield {
                val taxCodeViewModel = TaxCodeViewModelPreviousYears(taxCodeRecords, scottishTaxRateBands, year)
                Ok(views.html.taxCodeDetailsPreviousYears(taxCodeViewModel))
              }
            }
          } else {
            ServiceCheckLite.personDetailsCheck {
              Future.successful(NotFound)
            }
          }
  }
}

// $COVERAGE-OFF$
object YourTaxCodeController extends YourTaxCodeController with AuthenticationConnectors {
  override val personService = PersonService
  override val taxAccountService: TaxAccountService = TaxAccountService
  override val taxCodeChangeService: TaxCodeChangeService = TaxCodeChangeService

  override implicit def templateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}

// $COVERAGE-ON$

