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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.TaxCodeChange
import uk.gov.hmrc.tai.model.domain.income.{BasisOperation, OtherBasisOperation}
import uk.gov.hmrc.tai.viewModels.{DescriptionListViewModel, TaxCodeDescription}
import uk.gov.hmrc.tai.viewModels.TaxCodeDescriptor.{emergencyTaxCodeExplanation, fetchTaxCodeExplanation, scottishTaxCodeExplanation, untaxedTaxCodeExplanation}

import scala.collection.immutable.ListMap

case class TaxCodeChangeViewModel(pairs: TaxCodePairs, changeDate: LocalDate, taxCodeExplanations: Map[String, DescriptionListViewModel])

object TaxCodeChangeViewModel {
  def apply(taxCodeChange: TaxCodeChange)(implicit messages: Messages): TaxCodeChangeViewModel = {
    val taxCodePairs = TaxCodePairs(taxCodeChange.previous, taxCodeChange.current)
    val changeDate = taxCodeChange.mostRecentTaxCodeChangeDate
    val explanations = getTaxCodeExplanations(taxCodeChange)

    TaxCodeChangeViewModel(taxCodePairs, changeDate, explanations)
  }

  private def getTaxCodes(taxCodeChange: TaxCodeChange): Seq[String] = {
    (taxCodeChange.previous ++ taxCodeChange.current).map(record => record.taxCode).distinct
  }

  private def getTaxCodeExplanations(taxCodeChange: TaxCodeChange)(implicit messages: Messages): Map[String, DescriptionListViewModel] = {
    val taxCodes = getTaxCodes(taxCodeChange)

    val explanationRules: Seq[TaxCodeDescription => ListMap[String, String]] = Seq(
      scottishTaxCodeExplanation,
      untaxedTaxCodeExplanation,
      fetchTaxCodeExplanation,
      emergencyTaxCodeExplanation
    )

    taxCodes.map { taxCode =>
      // TODO: Maybe this should use the taxCodeViewModel
      // TODO: Check if tax code is emergency, get basis operation from backend, sort out scottish tax codes
      val taxDescription = TaxCodeDescription(taxCode, OtherBasisOperation, Map[String, BigDecimal]())
      val explanation = explanationRules.foldLeft(ListMap[String, String]())((expl, rule) => expl ++ rule(taxDescription))
      taxCode -> DescriptionListViewModel(Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", taxCode, taxCode), explanation)
    }.toMap
  }
}