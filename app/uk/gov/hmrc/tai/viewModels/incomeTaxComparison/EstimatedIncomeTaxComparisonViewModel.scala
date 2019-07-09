/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.ViewModelHelper

case class EstimatedIncomeTaxComparisonViewModel(items: Seq[EstimatedIncomeTaxComparisonItem]) extends ViewModelHelper {

  override def nextTaxYearHeaderHtmlNonBreak(implicit messages: Messages): String = {

    changeInTaxAmount match {
      case gt if gt > 0 => Messages("tai.incomeTaxComparison.dateWithoutWelshAmendment",htmlNonBroken(Dates.formatDate(TaxYear().next.start)))
      case lt if lt < 0 => Messages("tai.incomeTaxComparison.welshAmendmentToDate",htmlNonBroken(Dates.formatDate(TaxYear().next.start)))
      case _ => Messages("tai.incomeTaxComparison.welshAmendmentToDate",htmlNonBroken(Dates.formatDate(TaxYear().next.start)))
    }

    // if they pay less or the same display this message
//    Messages("tai.incomeTaxComparison.welshAmmendmentToDate",htmlNonBroken(Dates.formatDate(TaxYear().next.start)))
  //if they are expected to pay mor tax yo dont want the ylmaen on the end
  }

  def currentTaxYearHeader(implicit messages: Messages): String = currentTaxYearHeaderHtmlNonBreak
  def nextTaxYearHeader(implicit messages: Messages): String = nextTaxYearHeaderHtmlNonBreak

  lazy val comparisonItemsByYear = items.sortBy(_.year)

  lazy val changeInTaxAmount = comparisonItemsByYear(1).estimatedIncomeTax - comparisonItemsByYear(0).estimatedIncomeTax
}

case class EstimatedIncomeTaxComparisonItem(year: TaxYear, estimatedIncomeTax: BigDecimal)
