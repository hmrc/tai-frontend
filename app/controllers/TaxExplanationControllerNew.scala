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
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.tai.service.{TaiService, TaxAccountService}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.viewModels.TaxExplanationViewModelNew

trait TaxExplanationControllerNew extends TaiBaseController
with DelegationAwareActions
with WithAuthorisedForTaiLite {

  def taiService: TaiService
  def taxAccountService: TaxAccountService

  def taxExplanationPage(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)
            val taxCodeIncomeFuture = taxAccountService.taxCodeIncomes(nino, TaxYear())
            val totalTaxFuture = taxAccountService.totalTax(nino, TaxYear())
            for{
              totalTax <- totalTaxFuture
              taxCodeIncomes <- taxCodeIncomeFuture
            } yield {
              (totalTax, taxCodeIncomes) match {
                case (TaiSuccessResponseWithPayload(totalTax: TotalTax),
                TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome])) =>
                  val model = TaxExplanationViewModelNew(totalTax, taxCodeIncomes)
                  Ok(views.html.howIncomeTaxIsCalculatedNew(model))
                case _ => throw new RuntimeException("Failed to fetch total tax details")
              }
            }
          }
  }

}

object TaxExplanationControllerNew extends TaxExplanationControllerNew with AuthenticationConnectors {
  override val taiService: TaiService = TaiService
  override val taxAccountService: TaxAccountService = TaxAccountService
  override implicit val templateRenderer: TemplateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}