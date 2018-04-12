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
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.util.TaiConstants.ScottishTaxCodePrefix
import uk.gov.hmrc.tai.util.ViewModelHelper

import scala.collection.immutable.ListMap

case class TaxCodeComparisonViewModel(employmentTaxCodes: Seq[TaxCodeDetail], pensionTaxCodes: Seq[TaxCodeDetail])
  extends ViewModelHelper {
  def currentTaxYearHeader(implicit messages: Messages): String = currentTaxYearHeaderHtmlNonBreak
  def nextTaxYearHeader(implicit messages: Messages): String = nextTaxYearHeaderHtmlNonBreak

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

    val sortedTaxYearMap: Map[TaxYear, Seq[TaxCodeIncome]] = {
      val mapSeq = taxCodeForYears map { tcfy =>
        (tcfy.year -> tcfy.taxCodeIncomeSources.filter(tci => tci.componentType == taxComponentType && tci.status == Live))
      }
      ListMap(mapSeq.sortBy(_._1): _*)

    }
    val uniqueIncomeSourceNames = sortedTaxYearMap.values.flatMap(_.map(_.name)).toSet
    uniqueIncomeSourceNames map { isn =>
      val taxCodesInYearOrder =
        sortedTaxYearMap map {case (ty, incomes) =>
          incomes.find({_.name == isn}).headOption.map(_.taxCode).getOrElse("tai.taxCode.comparision.noCode")
        }
      TaxCodeDetail(isn, taxCodesInYearOrder.toList)
    } toList


  }
}

case class TaxCodeForYear(year: uk.gov.hmrc.tai.model.tai.TaxYear, taxCodeIncomeSources: Seq[TaxCodeIncome])

case class TaxCodeDetail(name: String, taxCodes: Seq[String])