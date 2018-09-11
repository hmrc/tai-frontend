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
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.viewModels.TaxCodeDescriptor


case class TaxCodeViewModel(title: String,
                            mainHeading: String,
                            ledeMessage: String,
                            taxCodeDetails: Seq[DescriptionListViewModel],
                            preHeader: String)

object TaxCodeViewModel extends ViewModelHelper with TaxCodeDescriptor {

  def apply(taxCodeIncomes: Seq[TaxCodeIncome], scottishTaxRateBands: Map[String, BigDecimal], year: TaxYear = TaxYear())(implicit messages: Messages): TaxCodeViewModel = {

    val previousOrCurrent = if (year <= TaxYear().prev) ".prev" else ""
    val preHeader =  messages(s"tai.taxCode$previousOrCurrent.preHeader")

    val descriptionListViewModels = taxCodeIncomes.map { taxCodeIncome =>
      val taxCode = taxCodeIncome.taxCodeWithEmergencySuffix
      val explanation = describeTaxCode(taxCode, taxCodeIncome.basisOperation, scottishTaxRateBands, year)

      DescriptionListViewModel(Messages(s"tai.taxCode$previousOrCurrent.subheading", taxCodeIncome.name, taxCode), explanation)
    }

    val taxCodesPrefix = if (taxCodeIncomes.size > 1) Messages(s"tai.taxCode$previousOrCurrent.multiple.code.title.pt1") else Messages(s"tai.taxCode$previousOrCurrent.single.code.title.pt1")

    val TaxYearRange = messages("tai.taxYear",
      Dates.formatDate(year.start),
      Dates.formatDate(year.end))

    val TaxYearRangeHtmlNonBreak = messages("tai.taxYear",
      htmlNonBroken( Dates.formatDate(year.start) ),
      htmlNonBroken( Dates.formatDate(year.end) ))

    val title = s"$taxCodesPrefix $TaxYearRange"
    val mainHeading = s"$taxCodesPrefix $TaxYearRangeHtmlNonBreak"
    val ledeMessage = if (taxCodeIncomes.size > 1) Messages(s"tai.taxCode$previousOrCurrent.multiple.info") else Messages(s"tai.taxCode$previousOrCurrent.single.info")

    TaxCodeViewModel(title, mainHeading, ledeMessage, descriptionListViewModels, preHeader)
  }

}
