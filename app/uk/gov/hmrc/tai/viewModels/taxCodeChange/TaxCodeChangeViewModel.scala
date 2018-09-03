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
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.model.domain.income.{BasisOperation, OtherBasisOperation, Week1Month1BasisOperation}
import uk.gov.hmrc.tai.util.TaiConstants
import uk.gov.hmrc.tai.viewModels.{DescriptionListViewModel, TaxCodeDescription}
import uk.gov.hmrc.tai.viewModels.TaxCodeDescriptor.{emergencyTaxCodeExplanation, fetchTaxCodeExplanation, scottishTaxCodeExplanation, untaxedTaxCodeExplanation}

import scala.collection.immutable.ListMap

case class TaxCodeChangeViewModel(pairs: TaxCodePairs, changeDate: LocalDate, scottishTaxRateBands: Map[String, BigDecimal])

object TaxCodeChangeViewModel {
  def apply(taxCodeChange: TaxCodeChange, scottishTaxRateBands: Map[String, BigDecimal])(implicit messages: Messages): TaxCodeChangeViewModel = {
    val taxCodePairs = TaxCodePairs(taxCodeChange.previous, taxCodeChange.current)
    val changeDate = taxCodeChange.mostRecentTaxCodeChangeDate

    TaxCodeChangeViewModel(taxCodePairs, changeDate, scottishTaxRateBands)
  }

  def getTaxCodeExplanations(taxCodeRecord: TaxCodeRecord, scottishTaxRateBands: Map[String, BigDecimal])(implicit messages: Messages): DescriptionListViewModel = {
    val taxCode = taxCodeWithEmergencySuffix(taxCodeRecord.taxCode, taxCodeRecord.basisOfOperation)

    // TODO: Maybe some of this should be moved into the taxCodeDescriptor
    val explanationRules: Seq[TaxCodeDescription => ListMap[String, String]] = Seq(
      scottishTaxCodeExplanation,
      untaxedTaxCodeExplanation,
      fetchTaxCodeExplanation,
      emergencyTaxCodeExplanation
    )

    val taxDescription = TaxCodeDescription(taxCode, taxCodeRecord.basisOfOperation, scottishTaxRateBands)
    val explanation = explanationRules.foldLeft(ListMap[String, String]())((expl, rule) => expl ++ rule(taxDescription))
    DescriptionListViewModel(Messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", taxCode), explanation)
  }

  def taxCodeWithEmergencySuffix(taxCode: String, basisOfOperation: BasisOperation): String = {
    basisOfOperation match {
      case Week1Month1BasisOperation => taxCode + TaiConstants.EmergencyTaxCodeSuffix
      case _ => taxCode
    }
  }
}