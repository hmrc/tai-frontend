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

package uk.gov.hmrc.tai.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxCodeRecord
import uk.gov.hmrc.tai.util.ViewModelHelper


case class TaxCodeViewModel(title: String,
                            mainHeading: String,
                            ledeMessage: String,
                            taxCodeDetails: Seq[DescriptionListViewModel],
                            preHeader: String)

object TaxCodeViewModel extends ViewModelHelper with TaxCodeDescriptor {

  def apply(taxCodeRecords: Seq[TaxCodeRecord],
            scottishTaxRateBands: Map[String, BigDecimal],
            year: TaxYear = TaxYear())(implicit messages: Messages): TaxCodeViewModel = {

    val isCurrentTaxCode = year == TaxYear()
    val preHeader = messages(s"tai.taxCode.preHeader")

    val descriptionListViewModels = taxCodeRecords.map { taxCodeRecord =>
      val taxCode = taxCodeRecord.taxCode
      val explanation = describeTaxCode(taxCode, taxCodeRecord.basisOfOperation, scottishTaxRateBands, isCurrentTaxCode)

      DescriptionListViewModel(messages(s"tai.taxCode.subheading", taxCodeRecord.employerName, taxCode), explanation)
    }

    val taxCodesPrefix = if (taxCodeRecords.size > 1) {
      messages(s"tai.taxCode.multiple.code.title.pt1")
    } else {
      messages(s"tai.taxCode.single.code.title.pt1")
    }

    val TaxYearRange = messages("tai.taxYear",
      Dates.formatDate(year.start),
      Dates.formatDate(year.end))

    val TaxYearRangeHtmlNonBreak = messages("tai.taxYear",
      htmlNonBroken(Dates.formatDate(year.start)),
      htmlNonBroken(Dates.formatDate(year.end)))

    val title = s"$taxCodesPrefix $TaxYearRange"
    val mainHeading = s"$taxCodesPrefix $TaxYearRangeHtmlNonBreak"
    val ledeMessage = if (taxCodeRecords.size > 1) {
      messages(s"tai.taxCode.multiple.info")
    } else {
      messages(s"tai.taxCode.single.info")
    }

    TaxCodeViewModel(title, mainHeading, ledeMessage, descriptionListViewModels, preHeader)
  }

}
