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
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.twirl.api.Html
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.service.estimatedIncomeTax.EstimatedIncomeTaxService
import uk.gov.hmrc.tai.service.{CodingComponentService, HasFormPartialService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax._

class EstimatedIncomeTaxController @Inject()(codingComponentService: CodingComponentService,
                                             partialService: HasFormPartialService,
                                             taxAccountService: TaxAccountService,
                                             authenticate: AuthAction,
                                             validatePerson: ValidatePerson,
                                             override implicit val partialRetriever: FormPartialRetriever,
                                             override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController {

  def estimatedIncomeTax(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      val nino = request.taiUser.nino

      for {
        taxSummary <- taxAccountService.taxAccountSummary(nino, TaxYear())
        codingComponents <- codingComponentService.taxFreeAmountComponents(nino, TaxYear())
        totalTax <- taxAccountService.totalTax(nino, TaxYear())
        nonTaxCode <- taxAccountService.nonTaxCodeIncomes(nino, TaxYear())
        taxCodeIncomes <- taxAccountService.taxCodeIncomes(nino, TaxYear())
        iFormLinks <- partialService.getIncomeTaxPartial
      } yield {
        (taxSummary, totalTax, nonTaxCode, taxCodeIncomes) match {
          case (TaiSuccessResponseWithPayload(taxAccountSummary: TaxAccountSummary),
          TaiSuccessResponseWithPayload(totalTaxDetails: TotalTax),
          TaiSuccessResponseWithPayload(nonTaxCodeIncome: NonTaxCodeIncome),
          TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome])) =>

            implicit val user = request.taiUser

            val taxBands = totalTaxDetails.incomeCategories.flatMap(_.taxBands).toList
            val taxViewType = EstimatedIncomeTaxService.taxViewType(codingComponents, totalTaxDetails, nonTaxCodeIncome,
              taxAccountSummary.totalEstimatedIncome, taxAccountSummary.taxFreeAllowance, taxAccountSummary.totalEstimatedTax,
              taxCodeIncomes.nonEmpty)
            taxViewType match {
              case NoIncomeTaxView => Ok(views.html.estimatedIncomeTax.noCurrentIncome())
              case ComplexTaxView => {
                val model = ComplexEstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, taxCodeIncomes, taxBands)
                Ok(views.html.estimatedIncomeTax.complexEstimatedIncomeTax(model, iFormLinks successfulContentOrElse Html("")))
              }
              case SimpleTaxView => {
                val model = SimpleEstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, taxCodeIncomes, taxBands)
                Ok(views.html.estimatedIncomeTax.simpleEstimatedIncomeTax(model, iFormLinks successfulContentOrElse Html("")))
              }
              case ZeroTaxView => {
                val model = ZeroTaxEstimatedIncomeTaxViewModel(codingComponents, taxAccountSummary, taxCodeIncomes, taxBands)
                Ok(views.html.estimatedIncomeTax.zeroTaxEstimatedIncomeTax(model, iFormLinks successfulContentOrElse Html("")))
              }
            }
          case _ => {
            internalServerError("Failed to get estimated income tax")
          }
        }
      }
  }
}
