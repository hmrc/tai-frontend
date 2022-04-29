/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.income.{BasisOfOperation, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.viewModels.{DescriptionListViewModel, TaxCodeDescriptor}
import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants._
import uk.gov.hmrc.tai.util.constants.TaiConstants

case class TaxCodeChangeViewModel(
  pairs: TaxCodePairs,
  changeDate: LocalDate,
  scottishTaxRateBands: Map[String, BigDecimal],
  taxCodeChangeReasons: Seq[String],
  isAGenericReason: Boolean)

object TaxCodeChangeViewModel extends TaxCodeDescriptor {

  def apply(
    taxCodeChange: TaxCodeChange,
    scottishTaxRateBands: Map[String, BigDecimal],
    taxCodeChangeReasons: Seq[String] = Seq.empty[String],
    isAGenericReason: Boolean = true): TaxCodeChangeViewModel = {

    val taxCodePairs = TaxCodePairs(taxCodeChange)
    val changeDate = taxCodeChange.mostRecentTaxCodeChangeDate

    TaxCodeChangeViewModel(
      taxCodePairs,
      changeDate,
      scottishTaxRateBands,
      taxCodeChangeReasons,
      isAGenericReason
    )
  }

  def getTaxCodeExplanations(
    taxCodeRecord: TaxCodeRecord,
    scottishTaxRateBands: Map[String, BigDecimal],
    identifier: String,
    appConfig: ApplicationConfig)(implicit messages: Messages): DescriptionListViewModel = {

    val isCurrentTaxCode = identifier == "current"

    val taxCode = taxCodeWithEmergencySuffix(taxCodeRecord.taxCode, taxCodeRecord.basisOfOperation)

    val explanation =
      describeTaxCode(taxCode, taxCodeRecord.basisOfOperation, scottishTaxRateBands, isCurrentTaxCode, appConfig)

    DescriptionListViewModel(messages("taxCode.change.yourTaxCodeChanged.whatTaxCodeMeans", taxCode), explanation)
  }

  def taxCodeWithEmergencySuffix(taxCode: String, basisOfOperation: BasisOfOperation): String =
    basisOfOperation match {
      case Week1Month1BasisOfOperation => taxCode + TaiConstants.EmergencyTaxCodeSuffix
      case _                           => taxCode
    }
}
