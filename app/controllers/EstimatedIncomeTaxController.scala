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
import play.twirl.api.Html
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.{CodingComponentService, HasFormPartialService, TaiService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.{EstimatedIncomeTaxViewModel, TaxReliefViewModel}

trait EstimatedIncomeTaxController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite {

  def taiService: TaiService

  def partialService: HasFormPartialService

  def codingComponentService: CodingComponentService

  def taxAccountService: TaxAccountService


  def estimatedIncomeTax(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)
            val taxSummaryFuture = taxAccountService.taxAccountSummary(nino, TaxYear())
            val totalTaxFuture = taxAccountService.totalTax(nino, TaxYear())
            val codingComponentFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())
            val nonTaxCodeIncomeFuture = taxAccountService.nonTaxCodeIncomes(nino, TaxYear())
            val taxCodeIncomeFuture = taxAccountService.taxCodeIncomes(nino, TaxYear())

            for {
              taxSummary <- taxSummaryFuture
              codingComponents <- codingComponentFuture
              totalTax <- totalTaxFuture
              nonTaxCode <- nonTaxCodeIncomeFuture
              taxCodeIncomes <- taxCodeIncomeFuture
              iFormLinks <- partialService.getIncomeTaxPartial
            } yield {
              (taxSummary, totalTax, nonTaxCode, taxCodeIncomes) match {
                case (TaiSuccessResponseWithPayload(taxAccountSummary: TaxAccountSummary),
                TaiSuccessResponseWithPayload(totalTaxDetails: TotalTax),
                TaiSuccessResponseWithPayload(nonTaxCodeIncome: NonTaxCodeIncome),
                TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome])) =>
                  val model = EstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, totalTaxDetails, nonTaxCodeIncome, taxCodeIncomes)
                  Ok(views.html.estimatedIncomeTaxNew(model, iFormLinks successfulContentOrElse Html("")))
                case _ => throw new RuntimeException("Failed to get tax summary details")
              }
            }
          }
  }

  def taxRelief(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)
            val totalTaxFuture = taxAccountService.totalTax(nino, TaxYear())
            val codingComponentFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())
            for {
              codingComponents <- codingComponentFuture
              totalTax <- totalTaxFuture
            } yield {
              totalTax match {
                case TaiSuccessResponseWithPayload(totalTaxDetails: TotalTax) =>
                  val model = TaxReliefViewModel(codingComponents, totalTaxDetails)
                  Ok(views.html.reliefs(model))
                case _ => throw new RuntimeException("Failed to get total tax details")
              }
            }
          }
  }

}

object EstimatedIncomeTaxController extends EstimatedIncomeTaxController with AuthenticationConnectors {
  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

  override val taiService = TaiService
  override val partialService: HasFormPartialService = HasFormPartialService
  override val codingComponentService: CodingComponentService = CodingComponentService
  override val taxAccountService: TaxAccountService = TaxAccountService
}