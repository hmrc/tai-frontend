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
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome}
import uk.gov.hmrc.tai.util.TaiConstants.ScottishTaxCodePrefix
import uk.gov.hmrc.tai.util.{DateFormatConstants, ViewModelHelper}

case class TaxCodeComparisonViewModel(employmentTaxCodes: Seq[TaxCodeDetail], pensionTaxCodes: Seq[TaxCodeDetail])
  extends ViewModelHelper with DateFormatConstants {
  lazy val currentTaxYearHeader: String = currentTaxYearHeaderHtmlNonBreak(DateWithoutYearFormat)
  lazy val nextTaxYearHeader: String = nextTaxYearHeaderHtmlNonBreak(DateWithYearFormat)

  private val employmentHasNotScottishTaxCodeCurrentYear = !employmentTaxCodes.exists(_.taxCodes.head.startsWith(ScottishTaxCodePrefix))
  private val employmentHasScottishTaxCodeNextYear = employmentTaxCodes.exists(_.taxCodes.last.startsWith(ScottishTaxCodePrefix))


  private val pensionHasNotScottishTaxCodeCurrentYear = !pensionTaxCodes.exists(_.taxCodes.head.startsWith(ScottishTaxCodePrefix))
  private val pensionHasScottishTaxCodeNextYear = pensionTaxCodes.exists(_.taxCodes.last.startsWith(ScottishTaxCodePrefix))

  val hasScottishTaxCodeNextYear: Boolean = (employmentHasNotScottishTaxCodeCurrentYear && pensionHasNotScottishTaxCodeCurrentYear) &&
    (employmentHasScottishTaxCodeNextYear || pensionHasScottishTaxCodeNextYear)
}

object TaxCodeComparisonViewModel {
  def apply(taxCodeForYears: Seq[TaxCodeForYear])(implicit messages: Messages): TaxCodeComparisonViewModel = {

    val employmentTaxCodes = taxCodeDetails(taxCodeForYears, EmploymentIncome)
    val pensionTaxCodes = taxCodeDetails(taxCodeForYears, PensionIncome)

    TaxCodeComparisonViewModel(employmentTaxCodes, pensionTaxCodes)
  }

  private def taxCodeDetails(taxCodeForYears: Seq[TaxCodeForYear], taxComponentType: TaxComponentType) = {
    val taxCodeIncomeSources = taxCodeForYears.sortBy(_.year)
      .flatMap(_.taxCodeIncomeSources.filter(taxCodeIncomeSource =>
        taxCodeIncomeSource.componentType == taxComponentType && taxCodeIncomeSource.status == Live))

    val empIdNameAndTaxCodes = taxCodeIncomeSources.map(incomeSource =>
      (incomeSource.employmentId.getOrElse(-1), incomeSource.name, incomeSource.taxCode))

    val groupByEmpId = empIdNameAndTaxCodes.groupBy(_._1)
    val groupByName = groupByEmpId.values.flatMap(_.groupBy(_._2))

    groupByName.toSeq.map(incomeDetails => {
      TaxCodeDetail(incomeDetails._1, incomeDetails._2.map(_._3))
    })
  }
}

case class TaxCodeForYear(year: uk.gov.hmrc.tai.model.tai.TaxYear, taxCodeIncomeSources: Seq[TaxCodeIncome])

case class TaxCodeDetail(name: String, taxCodes: Seq[String])