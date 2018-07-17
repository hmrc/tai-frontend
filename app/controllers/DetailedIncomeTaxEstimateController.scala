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

import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.service.{CodingComponentService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.TaxExplanationViewModel
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.DetailedIncomeTaxEstimateViewModel

import scala.concurrent.Future
import scala.util.Failure

trait DetailedIncomeTaxEstimateController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite {

  def personService: PersonService

  def taxAccountService: TaxAccountService

  def codingComponentService: CodingComponentService

  def taxExplanationPage(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)
            val totalTaxFuture = taxAccountService.totalTax(nino, TaxYear())
            val taxCodeIncomeFuture = taxAccountService.taxCodeIncomes(nino, TaxYear())
            val taxSummaryFuture = taxAccountService.taxAccountSummary(nino, TaxYear())
            val codingComponentFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())
            val nonTaxCodeIncomeFuture = taxAccountService.nonTaxCodeIncomes(nino, TaxYear())

            for {
              totalTax <- totalTaxFuture
              taxCodeIncomes <- taxCodeIncomeFuture
              taxSummary <- taxSummaryFuture
              codingComponents <- codingComponentFuture
              nonTaxCode <- nonTaxCodeIncomeFuture
            } yield {
              (totalTax, taxCodeIncomes, taxSummary, nonTaxCode) match {
                case (
                  TaiSuccessResponseWithPayload(totalTax: TotalTax),
                  TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]),
                  TaiSuccessResponseWithPayload(taxAccountSummary: TaxAccountSummary),
                  TaiSuccessResponseWithPayload(nonTaxCodeIncome: NonTaxCodeIncome)
                ) =>
                  val model = DetailedIncomeTaxEstimateViewModel(totalTax, taxCodeIncomes, taxAccountSummary, codingComponents, nonTaxCodeIncome)
                  Ok(views.html.estimatedIncomeTax.detailedIncomeTaxEstimate(model))

                case _ => throw new RuntimeException("Failed to fetch total tax details")
              }
            }
          }
  }

}

object DetailedIncomeTaxEstimateController extends DetailedIncomeTaxEstimateController with AuthenticationConnectors {
  override val personService: PersonService = PersonService
  override val taxAccountService: TaxAccountService = TaxAccountService
  override val codingComponentService: CodingComponentService = CodingComponentService
  override implicit val templateRenderer: TemplateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}

