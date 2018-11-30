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
import uk.gov.hmrc.tai.model.domain.income.{BasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.util.ViewModelHelper

case class TaxCodeViewModel(title: String,
                            mainHeading: String,
                            ledeMessage: String,
                            taxCodeDetails: Seq[DescriptionListViewModel],
                            preHeader: String)

object TaxCodeViewModel extends ViewModelHelper with TaxCodeDescriptor {

  def apply(taxCodeIncomes: Seq[TaxCodeIncome],
            scottishTaxRateBands: Map[String, BigDecimal])
           (implicit messages: Messages): TaxCodeViewModel = {

    val descriptionListViewModels: Seq[DescriptionListViewModel] = taxCodeIncomes.map { income =>
      val taxCode = income.taxCodeWithEmergencySuffix
      createDescriptionListViewModel(taxCode, income.basisOperation, scottishTaxRateBands, income.name)
    }

    TaxCodeViewModel(descriptionListViewModels)
  }

  def apply(descriptions: Seq[DescriptionListViewModel])(implicit messages: Messages): TaxCodeViewModel = {

    val size = descriptions.size
    val title = taxCodesTitle(size, TaxYear())
    val mainHeading = title
    val introMessage = ledeMessage(size)
    val preHeading = messages(s"tai.taxCode.preHeader")

    TaxCodeViewModel(title, mainHeading, introMessage, descriptions, preHeading)
  }

  private def createDescriptionListViewModel(taxCode: String,
                                     operation: BasisOfOperation,
                                     scottishTaxRateBands: Map[String, BigDecimal],
                                     employerName: String)(implicit messages: Messages): DescriptionListViewModel = {

    val explanation = describeTaxCode(taxCode, operation, scottishTaxRateBands, isCurrentYear = true)

    DescriptionListViewModel(messages(s"tai.taxCode.subheading", employerName, taxCode), explanation)
  }

  private def taxCodesTitle(numberOfRecords: Int, year: TaxYear)(implicit messages: Messages): String = {
    val titleMessageKey = if (numberOfRecords > 1) "tai.taxCode.multiple.code.title" else "tai.taxCode.single.code.title"
    val startOfTaxYearNonBroken = htmlNonBroken(Dates.formatDate(year.start))
    val endOfTaxYearNonBroken = htmlNonBroken(Dates.formatDate(year.end))
    messages(titleMessageKey, startOfTaxYearNonBroken, endOfTaxYearNonBroken)
  }

  private def ledeMessage(numberOfRecords: Int)(implicit messages: Messages): String = {
    if (numberOfRecords > 1) {
      messages(s"tai.taxCode.multiple.info")
    } else {
      messages(s"tai.taxCode.single.info")
    }
  }
}
