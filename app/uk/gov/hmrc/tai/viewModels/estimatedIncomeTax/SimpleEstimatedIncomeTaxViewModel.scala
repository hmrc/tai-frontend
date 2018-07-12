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

package uk.gov.hmrc.tai.viewModels.estimatedIncomeTax

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.tax.TaxBand
import uk.gov.hmrc.tai.util._
import scala.math.BigDecimal

case class SimpleEstimatedIncomeTaxViewModel(
                                              incomeTaxEstimate: BigDecimal,
                                              incomeEstimate: BigDecimal,
                                              taxFreeAllowance: BigDecimal,
                                              graph: BandedGraph,
                                              taxRegion: String,
                                              mergedTaxBands:List[TaxBand]
                                      ) extends ViewModelHelper

object SimpleEstimatedIncomeTaxViewModel extends EstimatedIncomeTaxBand{

  def apply(codingComponents: Seq[CodingComponent], taxAccountSummary: TaxAccountSummary, taxCodeIncomes: Seq[TaxCodeIncome],
            taxBands:List[TaxBand])(implicit messages: Messages): SimpleEstimatedIncomeTaxViewModel = {

    val paBand = createPABand(taxAccountSummary.taxFreeAllowance)
    val mergedTaxBands = retrieveTaxBands(taxBands :+ paBand)
    val graph = BandedGraph(codingComponents,mergedTaxBands, taxAccountSummary.taxFreeAllowance, taxAccountSummary.totalEstimatedTax, taxViewType = SimpleTaxView)
    val taxRegion = findTaxRegion(taxCodeIncomes)

    SimpleEstimatedIncomeTaxViewModel(
      taxAccountSummary.totalEstimatedTax,
      taxAccountSummary.totalEstimatedIncome,
      taxAccountSummary.taxFreeAllowance,
      graph,
      taxRegion,
      mergedTaxBands
    )
  }
}
