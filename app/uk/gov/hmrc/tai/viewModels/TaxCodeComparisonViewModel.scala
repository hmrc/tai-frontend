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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome}
import uk.gov.hmrc.tai.util.constants.TaiConstants.ScottishTaxCodePrefix
import uk.gov.hmrc.tai.util.ViewModelHelper

import scala.collection.immutable.ListMap

case class TaxCodeComparisonViewModel(employmentTaxCodes: Seq[TaxCodeDetail], pensionTaxCodes: Seq[TaxCodeDetail])
    extends ViewModelHelper {
  def currentTaxYearHeader(implicit messages: Messages): String = currentTaxYearHeaderHtmlNonBreak
  def nextTaxYearHeader(implicit messages: Messages): String = nextTaxYearHeaderHtmlNonBreak

  private val employmentHasNotScottishTaxCodeCurrentYear =
    !employmentTaxCodes.exists(_.taxCodes.head.startsWith(ScottishTaxCodePrefix))
  private val employmentHasScottishTaxCodeNextYear =
    employmentTaxCodes.exists(_.taxCodes.last.startsWith(ScottishTaxCodePrefix))

  private val pensionHasNotScottishTaxCodeCurrentYear =
    !pensionTaxCodes.exists(_.taxCodes.head.startsWith(ScottishTaxCodePrefix))
  private val pensionHasScottishTaxCodeNextYear =
    pensionTaxCodes.exists(_.taxCodes.last.startsWith(ScottishTaxCodePrefix))

  val hasScottishTaxCodeNextYear
    : Boolean = (employmentHasNotScottishTaxCodeCurrentYear && pensionHasNotScottishTaxCodeCurrentYear) &&
    (employmentHasScottishTaxCodeNextYear || pensionHasScottishTaxCodeNextYear)
}

object TaxCodeComparisonViewModel {
  def apply(taxCodeForYears: Seq[TaxCodeIncomesForYear])(implicit messages: Messages): TaxCodeComparisonViewModel = {

    val employmentTaxCodes = taxCodeDetails(taxCodeForYears, EmploymentIncome)
    val pensionTaxCodes = taxCodeDetails(taxCodeForYears, PensionIncome)

    TaxCodeComparisonViewModel(employmentTaxCodes, pensionTaxCodes)
  }

  private def taxCodeDetails(taxCodeForYears: Seq[TaxCodeIncomesForYear], taxComponentType: TaxComponentType)(
    implicit messages: Messages): Seq[TaxCodeDetail] = {

    val filteredTaxCodeIncomes = filterTaxCodeIncomesForYear(taxCodeForYears, taxComponentType)
    val sortedTaxCodeIncomes = filteredTaxCodeIncomes.sortWith(_.year < _.year)
    val uniqueIncomeSourceNames = sortedTaxCodeIncomes.flatMap(_.taxCodeIncomeSources.map(_.name)).distinct

    for {
      incomeSourceName <- uniqueIncomeSourceNames
    } yield {
      TaxCodeDetail(incomeSourceName, taxCodesForIncomeSource(sortedTaxCodeIncomes, incomeSourceName))
    }
  }

  private def taxCodesForIncomeSource(taxCodeForYears: Seq[TaxCodeIncomesForYear], incomeSourceName: String)(
    implicit messages: Messages) =
    for {
      taxYear: TaxCodeIncomesForYear <- taxCodeForYears
    } yield {
      val incomeSources: Option[TaxCodeIncome] = taxYear.taxCodeIncomeSources.find(_.name == incomeSourceName)
      incomeSources.map(_.taxCode).getOrElse(Messages("tai.incomeTaxComparison.incomeSourceAbsent"))
    }

  private def filterTaxCodeIncomesForYear(
    taxCodeForYears: Seq[TaxCodeIncomesForYear],
    taxComponentType: TaxComponentType): Seq[TaxCodeIncomesForYear] =
    taxCodeForYears map { taxCodeForYear =>
      TaxCodeIncomesForYear(
        taxCodeForYear.year,
        taxCodeForYear.taxCodeIncomeSources.filter(incomeSource => filterIncomeSources(incomeSource, taxComponentType)))
    }

  // TODO:// Either add employment as a param or reinvestigate why this is required, to relook at this when CY+1 is reenabled
  private def filterIncomeSources(incomeSource: TaxCodeIncome, taxComponentType: TaxComponentType): Boolean =
    incomeSource.componentType == taxComponentType && incomeSource.status == Live

}

case class TaxCodeIncomesForYear(year: TaxYear, taxCodeIncomeSources: Seq[TaxCodeIncome])

case class TaxCodeDetail(name: String, taxCodes: Seq[String])
