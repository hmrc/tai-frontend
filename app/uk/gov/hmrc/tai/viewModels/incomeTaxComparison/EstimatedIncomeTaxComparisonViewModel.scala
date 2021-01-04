/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.incomeTaxComparison

import org.joda.time.LocalDate

import play.api.i18n.Messages
import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.ViewModelHelper

case class EstimatedIncomeTaxComparisonViewModel(items: Seq[EstimatedIncomeTaxComparisonItem]) extends ViewModelHelper {

  lazy val taxComparison: TaxComparison[BigDecimal] = {
    changeInTaxAmount match {
      case gt if gt > 0 => GT(gt)
      case lt if lt < 0 => LT(lt)
      case _            => EQ
    }
  }

  private def formatDate(date: LocalDate)(implicit messages: Messages) = htmlNonBroken(Dates.formatDate(date))

  override def nextTaxYearHeaderHtmlNonBreak(implicit messages: Messages): String =
    taxComparison.fold(
      _ => messages("tai.incomeTaxComparison.dateWithoutWelshAmendment", formatDate(TaxYear().next.start)),
      _ => messages("tai.incomeTaxComparison.welshAmendmentToDate", formatDate(TaxYear().next.start)),
      messages("tai.incomeTaxComparison.welshAmendmentToDate", formatDate(TaxYear().next.start))
    )

  def currentTaxYearHeader(implicit messages: Messages): String = currentTaxYearHeaderHtmlNonBreak
  def nextTaxYearHeader(implicit messages: Messages): String = nextTaxYearHeaderHtmlNonBreak

  lazy val comparisonItemsByYear = items.sortBy(_.year)

  lazy val changeInTaxAmount = comparisonItemsByYear(1).estimatedIncomeTax - comparisonItemsByYear(0).estimatedIncomeTax
}

case class EstimatedIncomeTaxComparisonItem(year: TaxYear, estimatedIncomeTax: BigDecimal)

sealed trait TaxComparison[+A] {

  def fold[B](gt: A => B, lt: A => B, eq: => B): B =
    this match {
      case GT(value) => gt(value)
      case LT(value) => lt(value)
      case EQ        => eq
    }

}

case class GT[A](value: A) extends TaxComparison[A]
case class LT[A](value: A) extends TaxComparison[A]
case object EQ extends TaxComparison[Nothing]
