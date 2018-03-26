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
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, TaiService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.incomeTaxComparison.{EstimatedIncomeTaxComparisonItem, EstimatedIncomeTaxComparisonViewModel, IncomeTaxComparisonViewModel}

trait IncomeTaxComparisonController extends TaiBaseController
  with WithAuthorisedForTaiLite {

  def taiService: TaiService
  def taxAccountService: TaxAccountService
  def codingComponentService: CodingComponentService
  def employmentService: EmploymentService

  def onPageLoad(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            val nino = Nino(user.getNino)
            val currentTaxYear = TaxYear()
            val nextTaxYear = currentTaxYear.next
            val taxSummaryCYFuture = taxAccountService.taxAccountSummary(nino, currentTaxYear)
            val taxSummaryCYPlusOneFuture = taxAccountService.taxAccountSummary(nino, nextTaxYear)
            val taxCodeIncomesCYFuture = taxAccountService.taxCodeIncomes(nino, currentTaxYear)
            val taxCodeIncomesCYPlusOneFuture = taxAccountService.taxCodeIncomes(nino, nextTaxYear)
            val taxComponentsCYFuture = codingComponentService.taxFreeAmountComponents(nino, currentTaxYear)
            val taxComponentsCYPlusOneFuture = codingComponentService.taxFreeAmountComponents(nino, nextTaxYear)
            val employmentsCYFuture = employmentService.employments(nino, currentTaxYear)
            val employmentsCYPlusOneFuture = employmentService.employments(nino, nextTaxYear)
            for {
              taxSummaryCY <- taxSummaryCYFuture
              taxSummaryCyPlusOne <- taxSummaryCYPlusOneFuture
              taxCodeIncomesForCy <- taxCodeIncomesCYFuture
              taxCodeIncomesForCyPlusOne <- taxCodeIncomesCYPlusOneFuture
              codingComponentsCY <- taxComponentsCYFuture
              codingComponentsCYPlusOne <- taxComponentsCYPlusOneFuture
              employmentsCY <- employmentsCYFuture
              employmentsCYPlusOne <- employmentsCYPlusOneFuture

            } yield {
              (taxSummaryCY, taxSummaryCyPlusOne, taxCodeIncomesForCy, taxCodeIncomesForCyPlusOne) match {
                case (TaiSuccessResponseWithPayload(taxAccountSummaryCY: TaxAccountSummary),
                TaiSuccessResponseWithPayload(taxAccountSummaryCYPlusOne: TaxAccountSummary),
                TaiSuccessResponseWithPayload(taxCodeIncomesCY: Seq[TaxCodeIncome]),
                TaiSuccessResponseWithPayload(taxCodeIncomesCYPlusOne: Seq[TaxCodeIncome])
                ) => {

                  val cyEstimatedTax = EstimatedIncomeTaxComparisonItem(currentTaxYear, taxAccountSummaryCY.totalEstimatedTax)
                  val cyPlusOneEstimatedTax = EstimatedIncomeTaxComparisonItem(nextTaxYear, taxAccountSummaryCYPlusOne.totalEstimatedTax)
                  val estimatedIncomeTaxComparisonViewModel = EstimatedIncomeTaxComparisonViewModel(Seq(cyEstimatedTax, cyPlusOneEstimatedTax))

                  val cyTaxCodeIncomeSources = TaxCodeForYear(currentTaxYear, taxCodeIncomesCY)
                  val cyPlusOneTaxCodeIncomeSources = TaxCodeForYear(nextTaxYear, taxCodeIncomesCYPlusOne)
                  val taxCodeComparisonModel = TaxCodeComparisonViewModel(Seq(cyTaxCodeIncomeSources, cyPlusOneTaxCodeIncomeSources))

                  val cyCodingComponents = CodingComponentForYear(currentTaxYear, codingComponentsCY)
                  val cyPlusOneTaxComponents = CodingComponentForYear(nextTaxYear, codingComponentsCYPlusOne)
                  val cyTaxSummary = TaxAccountSummaryForYear(currentTaxYear, taxAccountSummaryCY)
                  val cyPlusOneTaxSummary = TaxAccountSummaryForYear(currentTaxYear, taxAccountSummaryCYPlusOne)
                  val taxFreeAmountComparisonModel = TaxFreeAmountComparisonViewModel(Seq(cyCodingComponents, cyPlusOneTaxComponents),
                    Seq(cyTaxSummary, cyPlusOneTaxSummary))

                  val employmentViewModel = IncomeSourceComparisonViewModel(taxCodeIncomesCY,employmentsCY,taxCodeIncomesCYPlusOne,employmentsCYPlusOne)

                  val model = IncomeTaxComparisonViewModel(user.getDisplayName, estimatedIncomeTaxComparisonViewModel,
                    taxCodeComparisonModel, taxFreeAmountComparisonModel, employmentViewModel)
                  Ok(views.html.incomeTaxComparison.Main(model))
                }
                case _ => throw new RuntimeException("Not able to fetch income tax comparision details")
              }
            }
          }
  }

}

object IncomeTaxComparisonController extends IncomeTaxComparisonController with AuthenticationConnectors {
  override val taiService = TaiService
  override val taxAccountService = TaxAccountService
  override val codingComponentService = CodingComponentService
  override val employmentService: EmploymentService = EmploymentService

  override implicit def templateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever = TaiHtmlPartialRetriever


}
