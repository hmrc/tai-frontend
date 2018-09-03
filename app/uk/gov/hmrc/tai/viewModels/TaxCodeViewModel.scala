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
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.viewModels.TaxCodeDescriptor


case class TaxCodeViewModel(title: String,
                            mainHeading: String,
                            ledeMessage: String,
                            taxCodeDetails: Seq[DescriptionListViewModel])

object TaxCodeViewModel extends ViewModelHelper with TaxCodeDescriptor {

  def apply(taxCodeIncomes: Seq[TaxCodeIncome], scottishTaxRateBands: Map[String, BigDecimal])(implicit messages: Messages): TaxCodeViewModel = {

    val descriptionListViewModels = taxCodeIncomes.map { taxCodeIncome =>
      val taxCode = taxCodeIncome.taxCodeWithEmergencySuffix
      val explanation = describeTaxCode(taxCode, taxCodeIncome.basisOperation, scottishTaxRateBands)
      DescriptionListViewModel(Messages("tai.taxCode.subheading", taxCodeIncome.name, taxCode), explanation)
    }

    val taxCodesPrefix = if (taxCodeIncomes.size > 1) Messages("tai.taxCode.multiple.code.title.pt1") else Messages("tai.taxCode.single.code.title.pt1")

    val title = s"$taxCodesPrefix $currentTaxYearRange"
    val mainHeading = s"$taxCodesPrefix $currentTaxYearRangeHtmlNonBreak"
    val ledeMessage = if (taxCodeIncomes.size > 1) Messages("tai.taxCode.multiple.info") else Messages("tai.taxCode.single.info")

    TaxCodeViewModel(title, mainHeading, ledeMessage, descriptionListViewModels)
  }

}
