/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxCodeRecord
import uk.gov.hmrc.tai.util.ViewModelHelper

case class TaxCodeViewModelPreviousYears(
  title: String,
  mainHeading: String,
  ledeMessage: String,
  taxCodeDetails: Seq[DescriptionListViewModel],
  preHeader: String)

object TaxCodeViewModelPreviousYears extends ViewModelHelper with TaxCodeDescriptor {

  def apply(
    taxCodeRecords: Seq[TaxCodeRecord],
    scottishTaxRateBands: Map[String, BigDecimal],
    year: TaxYear = TaxYear(),
    appConfig: ApplicationConfig)(implicit messages: Messages): TaxCodeViewModelPreviousYears = {

    val preHeader = messages(s"tai.taxCode.prev.preHeader")

    val descriptionListViewModels = sortedTaxCodeRecords(taxCodeRecords).map {
      recordToDescriptionListViewModel(_, scottishTaxRateBands, appConfig)
    }

    val titleMessageKey =
      if (taxCodeRecords.size > 1) "tai.taxCode.prev.multiple.code.title" else "tai.taxCode.prev.single.code.title"
    val startOfTaxYearNonBroken = htmlNonBroken(Dates.formatDate(year.start))
    val endOfTaxYearNonBroken = htmlNonBroken(Dates.formatDate(year.end))
    val taxCodesTitle = messages(titleMessageKey, startOfTaxYearNonBroken, endOfTaxYearNonBroken)

    val title = taxCodesTitle
    val mainHeading = taxCodesTitle

    val ledeMessage = if (taxCodeRecords.size > 1) {
      messages(s"tai.taxCode.prev.multiple.info")
    } else {
      messages(s"tai.taxCode.prev.single.info")
    }

    TaxCodeViewModelPreviousYears(title, mainHeading, ledeMessage, descriptionListViewModels, preHeader)
  }

  private def sortedTaxCodeRecords(records: Seq[TaxCodeRecord]): Seq[TaxCodeRecord] = {
    val primarySecondaryPensionSort: TaxCodeRecord => (Boolean, Boolean) = (record: TaxCodeRecord) =>
      (!record.primary, record.pensionIndicator)
    records.sortBy(primarySecondaryPensionSort)
  }

  private def recordToDescriptionListViewModel(
    record: TaxCodeRecord,
    scottishTaxRateBands: Map[String, BigDecimal],
    appConfig: ApplicationConfig)(implicit messages: Messages): DescriptionListViewModel = {
    val taxCode = record.taxCode
    val explanation =
      describeTaxCode(taxCode, record.basisOfOperation, scottishTaxRateBands, isCurrentYear = false, appConfig)

    DescriptionListViewModel(
      messages(
        s"tai.taxCode.prev.subheading",
        record.employerName,
        htmlNonBroken(Dates.formatDate(record.startDate)),
        htmlNonBroken(Dates.formatDate(record.endDate)),
        taxCode
      ),
      explanation
    )
  }
}
