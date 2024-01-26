/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.{HtmlFormatter, MonetaryUtil, MoneyPounds, TaxYearRangeUtil => Dates, ViewModelHelper}

case class TaxFreeAmountComparisonViewModel(
  personalAllowance: PersonalAllowance,
  additions: Additions,
  deductions: Deductions,
  footer: Footer
)(implicit messages: Messages)
    extends ViewModelHelper {
  def currentTaxYearHeader(implicit messages: Messages): String = currentTaxYearHeaderHtmlNonBreak
  def nextTaxYearHeader(implicit messages: Messages): String = nextTaxYearHeaderHtmlNonBreak
  val hasAdditions: Boolean = additions.additions.nonEmpty
  val hasDeductions: Boolean = deductions.deductions.nonEmpty
  private val PersonalAllowanceCy = 0
  private val PersonalAllowanceCyPlusOne = 1
  val hasPersonalAllowanceIncrease: Boolean =
    personalAllowance.values(PersonalAllowanceCyPlusOne) > personalAllowance.values(PersonalAllowanceCy)

  def personalAllowanceIncreaseInfo(implicit messages: Messages): Option[String] =
    if (hasPersonalAllowanceIncrease) {
      val personallAllowanceCYPlusOneAmount =
        MonetaryUtil.withPoundPrefixAndSign(MoneyPounds(personalAllowance.values(PersonalAllowanceCyPlusOne), 0))
      Some(
        messages(
          "tai.incomeTaxComparison.taxFreeAmount.PA.information1",
          personallAllowanceCYPlusOneAmount,
          HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().next.start))
        )
      )
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
    taxAccountSummaryForYears: Seq[TaxAccountSummaryForYear]
  )(implicit messages: Messages): TaxFreeAmountComparisonViewModel = {
    val sortedcodingComponentsByYear = codingComponentForYears.sortBy(_.year)
    val sortedTaxAccountSummaryByYear = taxAccountSummaryForYears.sortBy(_.year)
    val personalAllowance = createPersonalAllowanceRow(sortedcodingComponentsByYear)
    val additions: Additions = createAdditionsRow(sortedcodingComponentsByYear)
    val deductions = createDeductionsRow(sortedcodingComponentsByYear)
    val footer = createFooterRow(sortedTaxAccountSummaryByYear)
    TaxFreeAmountComparisonViewModel(personalAllowance, additions, deductions, footer)
  }

  private def createPersonalAllowanceRow(codingComponentForYears: Seq[CodingComponentForYear]): PersonalAllowance = {
    val amounts = codingComponentForYears.map(
      _.codingComponents.find(_.componentType == PersonalAllowancePA).map(_.amount) getOrElse BigDecimal(0)
    )

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

    Additions(additions)
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

    Deductions(deductions)
  }

  private def createFooterRow(taxAccountSummaryForYears: Seq[TaxAccountSummaryForYear]) = {
    val taxFreeAmountTotals = taxAccountSummaryForYears.map(_.taxAccountSummary.taxFreeAmount)
    Footer(taxFreeAmountTotals)
  }

  private def componentTypeToRow(
    componentType: TaxComponentType,
    codingComponentForYears: Seq[CodingComponentForYear]
  ): Row = {
    val amounts = codingComponentForYears.map(_.codingComponents.find(_.componentType == componentType).map(_.amount))

    Row(componentType.toString, amounts)
  }

}

case class CodingComponentForYear(year: TaxYear, codingComponents: Seq[CodingComponent])

case class TaxAccountSummaryForYear(year: TaxYear, taxAccountSummary: TaxAccountSummary)

case class PersonalAllowance(values: Seq[BigDecimal])

case class Additions(additions: Seq[Row]) extends WithRowTotal {
  def totals: Seq[BigDecimal] = getTotals(additions)
}
case class Deductions(deductions: Seq[Row]) extends WithRowTotal {
  def totals: Seq[BigDecimal] = getTotals(deductions)
}

case class Footer(values: Seq[BigDecimal])

case class Row(label: String, values: Seq[Option[BigDecimal]])

trait WithRowTotal {

  /* Sum the vectors of values element wise.
     total = Seq(a1, a2) + Seq(b1, b2) = Seq(a1 + b1, a2 + b2)
   */
  def getTotals(components: Seq[Row]): Seq[BigDecimal] =
    components
      .map(_.values)
      .foldLeft(Seq.empty[BigDecimal]) { (runningTotals, currentsOption) =>
        runningTotals
          .zipAll(
            currentsOption.map(_.getOrElse(BigDecimal(0))),
            BigDecimal(0),
            BigDecimal(0)
          )
          .map((tuple: (BigDecimal, BigDecimal)) => tuple._1 + tuple._2)
      }
}
