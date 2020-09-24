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
import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.{HtmlFormatter, MonetaryUtil, ViewModelHelper}

case class TaxFreeAmountComparisonViewModel(
  personalAllowance: PersonalAllowance,
  additions: Additions,
  deductions: Deductions,
  footer: Footer)(implicit messages: Messages)
    extends ViewModelHelper {
  def currentTaxYearHeader(implicit messages: Messages): String = currentTaxYearHeaderHtmlNonBreak
  def nextTaxYearHeader(implicit messages: Messages): String = nextTaxYearHeaderHtmlNonBreak
  val hasAdditions: Boolean = additions.additions.nonEmpty
  val hasDeductions: Boolean = deductions.deductions.nonEmpty
  private val PERSONAL_ALLOWANCE_CY = 0
  private val PERSONAL_ALLOWANCE_CY_PLUS_ONE = 1
  val hasPersonalAllowanceIncrease: Boolean =
    personalAllowance.values(PERSONAL_ALLOWANCE_CY_PLUS_ONE) > personalAllowance.values(PERSONAL_ALLOWANCE_CY)

  def personalAllowanceIncreaseInfo(implicit messages: Messages): Option[String] =
    if (hasPersonalAllowanceIncrease) {
      val personallAllowanceCYPlusOneAmount =
        MonetaryUtil.withPoundPrefixAndSign(MoneyPounds(personalAllowance.values(PERSONAL_ALLOWANCE_CY_PLUS_ONE), 0))
      Some(
        messages(
          "tai.incomeTaxComparison.taxFreeAmount.PA.information1",
          personallAllowanceCYPlusOneAmount,
          HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().next.start))
        ))
    } else {
      None
    }

  def prettyPrint(number: Option[BigDecimal]): String =
    number
      .map(x => withPoundPrefixAndSign(MoneyPounds(x, 0)))
      .getOrElse(Messages("tai.incomeTaxComparison.taxFreeAmount.NA"))
}

object TaxFreeAmountComparisonViewModel {

  def apply(
    codingComponentForYears: Seq[CodingComponentForYear],
    taxAccountSummaryForYears: Seq[TaxAccountSummaryForYear])(
    implicit messages: Messages): TaxFreeAmountComparisonViewModel = {
    val sortedcodingComponentsByYear = codingComponentForYears.sortBy(_.year)
    val sortedTaxAccountSummaryByYear = taxAccountSummaryForYears.sortBy(_.year)
    val personalAllowance = createPersonalAllowanceRow(sortedcodingComponentsByYear)
    val additions = createAdditionsRow(sortedcodingComponentsByYear)
    val deductions = createDeductionsRow(sortedcodingComponentsByYear)
    val footer = createFooterRow(sortedTaxAccountSummaryByYear)
    TaxFreeAmountComparisonViewModel(personalAllowance, additions, deductions, footer)
  }

  private def createPersonalAllowanceRow(codingComponentForYears: Seq[CodingComponentForYear]): PersonalAllowance = {
    val amounts = codingComponentForYears.map(
      _.codingComponents.find(_.componentType == PersonalAllowancePA).map(_.amount) getOrElse BigDecimal(0))

    if (amounts.nonEmpty) PersonalAllowance(amounts) else PersonalAllowance(Seq(0, 0))
  }

  private def createAdditionsRow(codingComponentForYears: Seq[CodingComponentForYear]) = {

    def isAdditionsWithoutPA(codingComponentForYear: CodingComponentForYear) =
      codingComponentForYear.codingComponents.filter {
        _.componentType match {
          case a: AllowanceComponentType if a != PersonalAllowancePA => true
          case _                                                     => false
        }
      }

    val allowances = codingComponentForYears
      .flatMap(codingComponentForYear => isAdditionsWithoutPA(codingComponentForYear).map(_.componentType))
      .distinct

    val additions =
      allowances.map(codingComponentType => componentTypeToRow(codingComponentType, codingComponentForYears))

    Additions(additions, createTotalRow(additions))
  }

  private def createDeductionsRow(codingComponentForYears: Seq[CodingComponentForYear]) = {

    def isDeductions(codingComponentForYear: CodingComponentForYear) =
      codingComponentForYear.codingComponents.filter {
        _.componentType match {
          case _: AllowanceComponentType => false
          case _                         => true
        }
      }

    val deduction = codingComponentForYears
      .flatMap(codingComponentForYear => isDeductions(codingComponentForYear).map(_.componentType))
      .distinct

    val deductions =
      deduction.map(codingComponentType => componentTypeToRow(codingComponentType, codingComponentForYears))

    Deductions(deductions, createTotalRow(deductions))
  }

  private def createFooterRow(taxAccountSummaryForYears: Seq[TaxAccountSummaryForYear]) = {
    val taxFreeAmountTotals = taxAccountSummaryForYears.map(_.taxAccountSummary.taxFreeAmount)
    Footer(taxFreeAmountTotals)
  }

  private def createTotalRow(rows: Seq[Row]): Total = {
    val numberOfYearsToCompare = 2
    val addCyNyValues = (firstRowValues: Seq[BigDecimal], secondRowValues: Seq[BigDecimal]) =>
      firstRowValues.zip(secondRowValues).map(t => t._1 + t._2)
    val totals = rows
      .map(_.values.map(_.getOrElse(BigDecimal(0))))
      .foldLeft(Seq.fill[BigDecimal](numberOfYearsToCompare)(0))(addCyNyValues)

    Total(totals)
  }

  private def componentTypeToRow(
    componentType: TaxComponentType,
    codingComponentForYears: Seq[CodingComponentForYear]) = {
    val amounts = codingComponentForYears.map(_.codingComponents.find(_.componentType == componentType).map(_.amount))

    Row(componentType.toString, amounts)
  }

}

case class CodingComponentForYear(year: TaxYear, codingComponents: Seq[CodingComponent])

case class TaxAccountSummaryForYear(year: TaxYear, taxAccountSummary: TaxAccountSummary)

case class PersonalAllowance(values: Seq[BigDecimal])

case class Additions(additions: Seq[Row], totalRow: Total)

case class Deductions(deductions: Seq[Row], totalRow: Total)

case class Footer(values: Seq[BigDecimal])

case class Row(label: String, values: Seq[Option[BigDecimal]])

case class Total(totals: Seq[BigDecimal])
